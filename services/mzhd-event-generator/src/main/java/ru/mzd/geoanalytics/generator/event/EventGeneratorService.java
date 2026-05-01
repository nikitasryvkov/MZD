package ru.mzd.geoanalytics.generator.event;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.generator.common.client.GeneratorContracts;
import ru.mzd.geoanalytics.generator.common.client.GeneratorGatewayClient;
import ru.mzd.geoanalytics.generator.common.domain.ReferenceNetworkIndex;

@Component
public class EventGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(EventGeneratorService.class);

    private final GeneratorGatewayClient gatewayClient;
    private final EventGeneratorProperties properties;

    private final Map<UUID, GeneratedEventState> activeEvents = new ConcurrentHashMap<>();

    private volatile ReferenceNetworkIndex referenceNetworkIndex;
    private volatile boolean restored;

    public EventGeneratorService(
        GeneratorGatewayClient gatewayClient,
        EventGeneratorProperties properties
    ) {
        this.gatewayClient = gatewayClient;
        this.properties = properties;
    }

    @Scheduled(
        initialDelay = 20000L,
        fixedDelayString = "#{@eventGeneratorProperties.tickInterval.toMillis()}"
    )
    public void synchronize() {
        try {
            ReferenceNetworkIndex networkIndex = ensureReferenceNetwork();
            restoreActiveEventsIfNeeded();

            Instant now = Instant.now();
            ZonedDateTime localNow = ZonedDateTime.ofInstant(now, ZoneId.of(properties.getTimeZone()));
            List<GeneratedEventState> changedEvents = new ArrayList<>();

            for (GeneratedEventState state : List.copyOf(activeEvents.values())) {
                GeneratedEventState updated = advanceState(state, now);
                if (!updated.equals(state)) {
                    activeEvents.put(updated.id(), updated);
                    changedEvents.add(updated);
                }
                if (isTerminal(updated.status())) {
                    activeEvents.remove(updated.id());
                }
            }

            if (activeEvents.size() < properties.getMaxActiveEvents() && ThreadLocalRandom.current().nextDouble() < spawnProbability(localNow)) {
                GeneratedEventState created = createEvent(networkIndex, now, localNow);
                activeEvents.put(created.id(), created);
                changedEvents.add(created);
            }

            if (!changedEvents.isEmpty()) {
                gatewayClient.syncEvents(new GeneratorContracts.SyncEventsRequest(
                    now,
                    properties.getSourceSystem(),
                    changedEvents.stream()
                        .sorted(Comparator.comparing(GeneratedEventState::startedAt))
                        .map(this::toRequest)
                        .toList()
                ));
                log.info("Synchronized {} operational events.", changedEvents.size());
            }
        } catch (Exception exception) {
            log.error("Failed to generate operational events.", exception);
        }
    }

    private void restoreActiveEventsIfNeeded() {
        if (restored) {
            return;
        }

        synchronized (this) {
            if (restored) {
                return;
            }

            for (GeneratorContracts.ActiveEventResponse event : gatewayClient.fetchActiveEvents()) {
                if (!properties.getSourceSystem().equals(event.lastChangedBy())) {
                    continue;
                }
                GeneratedEventState restoredState = new GeneratedEventState(
                    event.id(),
                    event.eventType(),
                    event.title(),
                    event.description(),
                    event.severity(),
                    event.status(),
                    event.affectedObjectId(),
                    event.departmentCode(),
                    event.latitude(),
                    event.longitude(),
                    event.startedAt() != null ? event.startedAt() : Instant.now(),
                    event.updatedAt() != null ? event.updatedAt() : Instant.now(),
                    event.updatedAt() != null ? event.updatedAt().plusSeconds(progressDelaySeconds(event.id())) : Instant.now().plusSeconds(180),
                    event.updatedAt() != null ? event.updatedAt().plusSeconds(resolveDelaySeconds(event.eventType(), event.severity(), event.id())) : Instant.now().plusSeconds(1200),
                    "Восстановление состояния генератора"
                );
                activeEvents.put(restoredState.id(), restoredState);
            }
            restored = true;
        }
    }

    private GeneratedEventState advanceState(GeneratedEventState state, Instant now) {
        if (state.status().equals("REGISTERED") && !now.isBefore(state.progressAt())) {
            return state.withStatus("IN_PROGRESS", now, "Автоматический перевод события в работу");
        }
        if (state.status().equals("IN_PROGRESS") && !now.isBefore(state.resolveAt())) {
            String terminalStatus = terminalStatus(state);
            return state.withStatus(terminalStatus, now, "Автоматическое завершение события генератором");
        }
        return state;
    }

    private GeneratedEventState createEvent(
        ReferenceNetworkIndex networkIndex,
        Instant now,
        ZonedDateTime localNow
    ) {
        String eventType = pickEventType(localNow);
        InfrastructureAnchor anchor = chooseAnchor(networkIndex, eventType, localNow);
        String severity = pickSeverity(eventType, anchor);
        UUID eventId = UUID.randomUUID();

        return new GeneratedEventState(
            eventId,
            eventType,
            title(eventType, anchor),
            description(eventType, anchor),
            severity,
            "REGISTERED",
            anchor.objectId(),
            anchor.departmentCode(),
            anchor.latitude(),
            anchor.longitude(),
            now,
            now,
            now.plusSeconds(progressDelaySeconds(eventId)),
            now.plusSeconds(resolveDelaySeconds(eventType, severity, eventId)),
            "Автоматическая генерация события"
        );
    }

    private InfrastructureAnchor chooseAnchor(
        ReferenceNetworkIndex networkIndex,
        String eventType,
        ZonedDateTime now
    ) {
        List<GeneratorContracts.ReferenceRouteSegmentResponse> segments = networkIndex.routeSegments();
        List<GeneratorContracts.ReferenceStationResponse> stations = networkIndex.stations();

        if (eventType.equals("REPAIR") || eventType.equals("OVERLOAD")) {
            GeneratorContracts.ReferenceRouteSegmentResponse segment = segments.stream()
                .sorted(Comparator.comparingDouble(candidate ->
                    -segmentPriority(candidate, eventType, now)
                ))
                .findFirst()
                .orElse(segments.get(0));
            GeneratorContracts.ReferenceStationResponse fromStation = networkIndex.station(segment.fromStationId());
            GeneratorContracts.ReferenceStationResponse toStation = networkIndex.station(segment.toStationId());
            return new InfrastructureAnchor(
                segment.id(),
                segment.departmentCode(),
                (fromStation.latitude() + toStation.latitude()) / 2.0,
                (fromStation.longitude() + toStation.longitude()) / 2.0,
                fromStation.name() + " - " + toStation.name()
            );
        }

        if (eventType.equals("DELAY")) {
            GeneratorContracts.ReferenceRouteSegmentResponse segment = segments.stream()
                .filter(candidate -> "OVERLOADED".equalsIgnoreCase(candidate.status()))
                .findAny()
                .orElse(segments.get(ThreadLocalRandom.current().nextInt(segments.size())));
            GeneratorContracts.ReferenceStationResponse fromStation = networkIndex.station(segment.fromStationId());
            GeneratorContracts.ReferenceStationResponse toStation = networkIndex.station(segment.toStationId());
            return new InfrastructureAnchor(
                segment.id(),
                segment.departmentCode(),
                (fromStation.latitude() + toStation.latitude()) / 2.0,
                (fromStation.longitude() + toStation.longitude()) / 2.0,
                fromStation.name() + " - " + toStation.name()
            );
        }

        GeneratorContracts.ReferenceStationResponse station = stations.get(ThreadLocalRandom.current().nextInt(stations.size()));
        return new InfrastructureAnchor(
            station.id(),
            station.departmentCode(),
            station.latitude(),
            station.longitude(),
            station.name()
        );
    }

    private double segmentPriority(
        GeneratorContracts.ReferenceRouteSegmentResponse segment,
        String eventType,
        ZonedDateTime now
    ) {
        double risk = "OVERLOADED".equalsIgnoreCase(segment.status()) ? 1.4 : 1.0;
        int hour = now.getHour();
        if (eventType.equals("REPAIR") && hour >= 1 && hour < 5) {
            risk = risk * 1.35;
        }
        return risk + ThreadLocalRandom.current().nextDouble(0.0, 0.2);
    }

    private String pickEventType(ZonedDateTime now) {
        int hour = now.getHour();
        double random = ThreadLocalRandom.current().nextDouble();
        if (hour >= 1 && hour < 5) {
            return random < 0.55 ? "REPAIR" : random < 0.78 ? "INCIDENT" : "OVERLOAD";
        }
        if ((hour >= 7 && hour < 10) || (hour >= 17 && hour < 20)) {
            return random < 0.42 ? "DELAY" : random < 0.70 ? "OVERLOAD" : random < 0.90 ? "INCIDENT" : "REPAIR";
        }
        return random < 0.32 ? "INCIDENT" : random < 0.56 ? "DELAY" : random < 0.78 ? "REPAIR" : "OVERLOAD";
    }

    private String pickSeverity(String eventType, InfrastructureAnchor anchor) {
        double random = ThreadLocalRandom.current().nextDouble();
        if (eventType.equals("OVERLOAD")) {
            return random < 0.45 ? "HIGH" : "MEDIUM";
        }
        if (eventType.equals("REPAIR")) {
            return random < 0.70 ? "MEDIUM" : "LOW";
        }
        if (eventType.equals("INCIDENT") && anchor.name().toLowerCase().contains("вокзал")) {
            return random < 0.55 ? "HIGH" : "CRITICAL";
        }
        return random < 0.20 ? "CRITICAL" : random < 0.60 ? "HIGH" : "MEDIUM";
    }

    private double spawnProbability(ZonedDateTime localNow) {
        int hour = localNow.getHour();
        if (hour >= 1 && hour < 5) {
            return 0.16;
        }
        if ((hour >= 7 && hour < 10) || (hour >= 17 && hour < 20)) {
            return 0.13;
        }
        return 0.07;
    }

    private long progressDelaySeconds(UUID eventId) {
        return 180L + Math.floorMod(eventId.hashCode(), 300);
    }

    private long resolveDelaySeconds(String eventType, String severity, UUID eventId) {
        long base = switch (eventType) {
            case "REPAIR" -> 4200L;
            case "INCIDENT" -> 2400L;
            case "OVERLOAD" -> 1800L;
            default -> 1500L;
        };
        long severityFactor = switch (severity) {
            case "CRITICAL" -> 1800L;
            case "HIGH" -> 900L;
            case "MEDIUM" -> 300L;
            default -> 0L;
        };
        return base + severityFactor + Math.floorMod(eventId.hashCode(), 600);
    }

    private String terminalStatus(GeneratedEventState state) {
        double resolveProbability = state.eventType().equals("INCIDENT") && state.severity().equals("CRITICAL") ? 0.65 : 0.82;
        return ThreadLocalRandom.current().nextDouble() < resolveProbability ? "RESOLVED" : "CANCELED";
    }

    private String title(String eventType, InfrastructureAnchor anchor) {
        return switch (eventType) {
            case "REPAIR" -> "Плановое ремонтное окно на участке " + anchor.name();
            case "OVERLOAD" -> "Перегрузка пропускной способности на участке " + anchor.name();
            case "DELAY" -> "Задержка движения поездов на участке " + anchor.name();
            default -> "Инцидент эксплуатационного характера на объекте " + anchor.name();
        };
    }

    private String description(String eventType, InfrastructureAnchor anchor) {
        return switch (eventType) {
            case "REPAIR" -> "Выполняется технологическое окно для поддержания надежности инфраструктуры на полигоне МЖД.";
            case "OVERLOAD" -> "Выявлена повышенная плотность движения поездов и снижение резервов пропускной способности.";
            case "DELAY" -> "Зафиксировано отклонение поездного графика, требующее оперативного контроля диспетчера.";
            default -> "Требуется локализация и оценка влияния инцидента на движение на полигоне Московской железной дороги.";
        };
    }

    private GeneratorContracts.EventUpsertRequest toRequest(GeneratedEventState state) {
        return new GeneratorContracts.EventUpsertRequest(
            state.id(),
            state.eventType(),
            state.title(),
            state.description(),
            state.severity(),
            state.status(),
            state.affectedObjectId(),
            state.departmentCode(),
            state.latitude(),
            state.longitude(),
            state.startedAt(),
            isTerminal(state.status()) ? state.updatedAt() : null,
            state.comment()
        );
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

    private boolean isTerminal(String status) {
        return "RESOLVED".equals(status) || "CANCELED".equals(status);
    }

    private record InfrastructureAnchor(
        UUID objectId,
        String departmentCode,
        double latitude,
        double longitude,
        String name
    ) {
    }

    private record GeneratedEventState(
        UUID id,
        String eventType,
        String title,
        String description,
        String severity,
        String status,
        UUID affectedObjectId,
        String departmentCode,
        double latitude,
        double longitude,
        Instant startedAt,
        Instant updatedAt,
        Instant progressAt,
        Instant resolveAt,
        String comment
    ) {
        GeneratedEventState withStatus(String newStatus, Instant changedAt, String newComment) {
            return new GeneratedEventState(
                id,
                eventType,
                title,
                description,
                severity,
                newStatus,
                affectedObjectId,
                departmentCode,
                latitude,
                longitude,
                startedAt,
                changedAt,
                progressAt,
                resolveAt,
                newComment
            );
        }
    }
}
