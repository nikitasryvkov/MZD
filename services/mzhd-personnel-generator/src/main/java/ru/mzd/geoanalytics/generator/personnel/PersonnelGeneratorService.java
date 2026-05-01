package ru.mzd.geoanalytics.generator.personnel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.generator.common.client.GeneratorContracts;
import ru.mzd.geoanalytics.generator.common.client.GeneratorGatewayClient;
import ru.mzd.geoanalytics.generator.common.domain.ReferenceNetworkIndex;

@Component
public class PersonnelGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PersonnelGeneratorService.class);

    private final GeneratorGatewayClient gatewayClient;
    private final PersonnelGeneratorProperties properties;

    private volatile ReferenceNetworkIndex referenceNetworkIndex;

    public PersonnelGeneratorService(
        GeneratorGatewayClient gatewayClient,
        PersonnelGeneratorProperties properties
    ) {
        this.gatewayClient = gatewayClient;
        this.properties = properties;
    }

    @Scheduled(
        initialDelay = 25000L,
        fixedDelayString = "#{@personnelGeneratorProperties.tickInterval.toMillis()}"
    )
    public void synchronize() {
        try {
            ReferenceNetworkIndex networkIndex = ensureReferenceNetwork();
            ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of(properties.getTimeZone()));
            LocalDate periodMonth = now.toLocalDate().withDayOfMonth(1);

            Map<String, Integer> headcountByDepartment = new LinkedHashMap<>();
            for (GeneratorContracts.ReferenceStationResponse station : networkIndex.stations()) {
                headcountByDepartment.merge(
                    station.departmentCode(),
                    estimatedHeadcount(station, periodMonth),
                    Integer::sum
                );
            }

            List<GeneratorContracts.PersonnelSnapshotItemRequest> items = headcountByDepartment.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new GeneratorContracts.PersonnelSnapshotItemRequest(
                    entry.getKey(),
                    entry.getValue(),
                    monthlyDelta(periodMonth, entry.getKey())
                ))
                .toList();

            gatewayClient.syncPersonnelSnapshot(new GeneratorContracts.SyncPersonnelSnapshotRequest(
                periodMonth,
                properties.getSourceSystem(),
                items
            ));
            log.info("Synchronized personnel snapshot for {} with {} department rows.", periodMonth, items.size());
        } catch (Exception exception) {
            log.error("Failed to generate personnel snapshot.", exception);
        }
    }

    private int estimatedHeadcount(GeneratorContracts.ReferenceStationResponse station, LocalDate periodMonth) {
        double seasonalFactor = switch (periodMonth.getMonth()) {
            case JANUARY, FEBRUARY -> 0.98;
            case JUNE, JULY, AUGUST -> 0.96;
            case OCTOBER, NOVEMBER, DECEMBER -> 1.03;
            default -> 1.0;
        };

        int base = switch (station.stationType()) {
            case "TERMINAL" -> 2200;
            case "HUB" -> 1250;
            case "RING" -> 680;
            default -> 480;
        };

        int variation = Math.floorMod((station.code() + periodMonth).hashCode(), 180);
        return Math.max(120, (int) Math.round((base + variation) * seasonalFactor));
    }

    private double monthlyDelta(LocalDate periodMonth, String departmentCode) {
        int bucket = Math.floorMod((departmentCode + periodMonth).hashCode(), 650);
        return -2.5 + (bucket / 100.0);
    }

    private ReferenceNetworkIndex ensureReferenceNetwork() {
        ReferenceNetworkIndex cached = referenceNetworkIndex;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (referenceNetworkIndex == null) {
                referenceNetworkIndex = ReferenceNetworkIndex.of(gatewayClient.fetchReferenceNetwork());
            }
            return referenceNetworkIndex;
        }
    }
}
