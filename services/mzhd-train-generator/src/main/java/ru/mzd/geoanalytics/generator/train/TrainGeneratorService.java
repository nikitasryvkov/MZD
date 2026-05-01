package ru.mzd.geoanalytics.generator.train;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.generator.common.client.GeneratorContracts;
import ru.mzd.geoanalytics.generator.common.client.GeneratorGatewayClient;
import ru.mzd.geoanalytics.generator.common.domain.GeoMath;
import ru.mzd.geoanalytics.generator.common.domain.MoscowRailwayOperationalProfiles;
import ru.mzd.geoanalytics.generator.common.domain.ReferenceNetworkIndex;
import ru.mzd.geoanalytics.generator.common.domain.StableIds;

@Component
public class TrainGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TrainGeneratorService.class);

    private static final Map<String, String> TRAIN_PREFIXES = Map.ofEntries(
        Map.entry("MZD-LEN", "71"),
        Map.entry("MZD-YAR", "72"),
        Map.entry("MZD-KAZ", "73"),
        Map.entry("MZD-GOR", "74"),
        Map.entry("MZD-KUR", "75"),
        Map.entry("MZD-KIE", "76"),
        Map.entry("MZD-BEL", "77"),
        Map.entry("MZD-SAV", "78"),
        Map.entry("MZD-RIG", "79"),
        Map.entry("MZD-PAV", "67"),
        Map.entry("MZD-MCC", "90"),
        Map.entry("MZD-BMO", "95"),
        Map.entry("MZD-MCD", "96")
    );

    private final GeneratorGatewayClient gatewayClient;
    private final TrainGeneratorProperties properties;

    private volatile ReferenceNetworkIndex referenceNetworkIndex;

    public TrainGeneratorService(
        GeneratorGatewayClient gatewayClient,
        TrainGeneratorProperties properties
    ) {
        this.gatewayClient = gatewayClient;
        this.properties = properties;
    }

    @Scheduled(
        initialDelay = 15000L,
        fixedDelayString = "#{@trainGeneratorProperties.tickInterval.toMillis()}"
    )
    public void synchronize() {
        try {
            ReferenceNetworkIndex networkIndex = ensureReferenceNetwork();
            ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of(properties.getTimeZone()));

            List<GeneratorContracts.TrainUpsertRequest> trains = new ArrayList<>();
            for (GeneratorContracts.ReferenceRouteResponse route : networkIndex.routes()) {
                MoscowRailwayOperationalProfiles.RouteOperationalProfile profile =
                    MoscowRailwayOperationalProfiles.profileFor(route.code());
                List<GeneratorContracts.ReferenceRouteStopResponse> orderedStops = networkIndex.orderedStops(route.id());
                if (orderedStops.size() < 2) {
                    continue;
                }

                for (int slot = 0; slot < profile.trainCount(); slot++) {
                    trains.add(generateTrain(route, orderedStops, networkIndex, profile, now, slot));
                }
            }

            gatewayClient.syncTrains(new GeneratorContracts.SyncTrainsRequest(
                now.toInstant(),
                properties.getSourceSystem(),
                trains
            ));
            log.info("Synchronized {} train positions.", trains.size());
        } catch (Exception exception) {
            log.error("Failed to generate train positions.", exception);
        }
    }

    private GeneratorContracts.TrainUpsertRequest generateTrain(
        GeneratorContracts.ReferenceRouteResponse route,
        List<GeneratorContracts.ReferenceRouteStopResponse> orderedStops,
        ReferenceNetworkIndex networkIndex,
        MoscowRailwayOperationalProfiles.RouteOperationalProfile profile,
        ZonedDateTime now,
        int slot
    ) {
        List<TravelLeg> legs = buildLegs(orderedStops, networkIndex, profile);
        long cycleSeconds = Math.max(1L, legs.stream().mapToLong(TravelLeg::cycleDurationSeconds).sum());
        long headwaySeconds = Math.max(180L, cycleSeconds / Math.max(1, profile.trainCount()));
        long phaseOffset = (long) slot * headwaySeconds + Math.floorMod(route.code().hashCode(), 173);
        long phaseSeconds = Math.floorMod(now.toEpochSecond() + phaseOffset, cycleSeconds);
        double loadFactor = MoscowRailwayOperationalProfiles.commuterLoadFactor(route.code(), now);

        TrainPosition position = locatePosition(legs, phaseSeconds, profile, loadFactor, slot);
        return new GeneratorContracts.TrainUpsertRequest(
            StableIds.nameUuid("train:" + route.code() + ":" + slot),
            trainNumber(route.code(), slot),
            route.id(),
            position.currentStationId(),
            position.nextStationId(),
            position.progressPercent(),
            position.latitude(),
            position.longitude(),
            position.speedKmh(),
            position.status()
        );
    }

    private TrainPosition locatePosition(
        List<TravelLeg> legs,
        long phaseSeconds,
        MoscowRailwayOperationalProfiles.RouteOperationalProfile profile,
        double loadFactor,
        int slot
    ) {
        long remaining = phaseSeconds;
        for (int index = 0; index < legs.size(); index++) {
            TravelLeg leg = legs.get(index);
            if (remaining < leg.travelSeconds()) {
                double factor = leg.travelSeconds() == 0 ? 0.0 : (double) remaining / (double) leg.travelSeconds();
                GeneratorContracts.ReferencePointResponse point = interpolateAlongShape(leg.shapePoints(), factor);
                double speedKmh = profile.cruiseSpeedKmh() * loadFactor * leg.speedVariationFactor();
                String status = "ON_ROUTE";

                if (profile.riskWeight() > 0.72 && loadFactor > 1.1 && slot % 4 == 0) {
                    speedKmh = speedKmh * 0.72;
                    status = "DELAYED";
                }

                return new TrainPosition(
                    leg.fromStationId(),
                    leg.toStationId(),
                    Math.min(99.9, factor * 100.0),
                    point.latitude(),
                    point.longitude(),
                    speedKmh,
                    status
                );
            }

            remaining -= leg.travelSeconds();
            if (remaining < leg.dwellSeconds()) {
                TravelLeg nextLeg = legs.get((index + 1) % legs.size());
                return new TrainPosition(
                    leg.toStationId(),
                    nextLeg.toStationId(),
                    0.0,
                    leg.toLatitude(),
                    leg.toLongitude(),
                    0.0,
                    "AT_STATION"
                );
            }
            remaining -= leg.dwellSeconds();
        }

        TravelLeg firstLeg = legs.get(0);
        return new TrainPosition(
            firstLeg.fromStationId(),
            firstLeg.toStationId(),
            0.0,
            firstLeg.fromLatitude(),
            firstLeg.fromLongitude(),
            0.0,
            "AT_STATION"
        );
    }

    private List<TravelLeg> buildLegs(
        List<GeneratorContracts.ReferenceRouteStopResponse> orderedStops,
        ReferenceNetworkIndex networkIndex,
        MoscowRailwayOperationalProfiles.RouteOperationalProfile profile
    ) {
        List<TravelLeg> forwardLegs = new ArrayList<>();
        for (int index = 0; index < orderedStops.size() - 1; index++) {
            forwardLegs.add(buildLeg(orderedStops.get(index), orderedStops.get(index + 1), networkIndex, profile, index));
        }

        if (profile.loopRoute()) {
            return forwardLegs;
        }

        List<TravelLeg> fullPath = new ArrayList<>(forwardLegs);
        int offset = forwardLegs.size();
        for (int index = orderedStops.size() - 1; index > 0; index--) {
            fullPath.add(buildLeg(orderedStops.get(index), orderedStops.get(index - 1), networkIndex, profile, offset + index));
        }
        return fullPath;
    }

    private TravelLeg buildLeg(
        GeneratorContracts.ReferenceRouteStopResponse fromStop,
        GeneratorContracts.ReferenceRouteStopResponse toStop,
        ReferenceNetworkIndex networkIndex,
        MoscowRailwayOperationalProfiles.RouteOperationalProfile profile,
        int seed
    ) {
        GeneratorContracts.ReferenceStationResponse fromStation = networkIndex.station(fromStop.stationId());
        GeneratorContracts.ReferenceStationResponse toStation = networkIndex.station(toStop.stationId());
        GeneratorContracts.ReferenceRouteSegmentResponse routeSegment = networkIndex.routeSegment(
            fromStop.stationId(),
            toStop.stationId()
        );
        List<GeneratorContracts.ReferencePointResponse> shapePoints = routeSegment != null
            ? routeSegment.shapePoints()
            : List.of(
                new GeneratorContracts.ReferencePointResponse(fromStation.latitude(), fromStation.longitude()),
                new GeneratorContracts.ReferencePointResponse(toStation.latitude(), toStation.longitude())
            );
        double distanceKm = Math.max(3.5, polylineDistanceKm(shapePoints));
        double variation = 0.92 + (Math.floorMod((fromStation.code() + toStation.code() + seed).hashCode(), 16) / 100.0);
        double speedKmh = profile.cruiseSpeedKmh() * variation;
        long travelSeconds = Math.max(180L, Math.round((distanceKm / speedKmh) * 3600.0));
        long dwellSeconds = fromStation.stationType().equals("TERMINAL") || toStation.stationType().equals("TERMINAL")
            ? Math.max(profile.dwellSeconds(), 110L)
            : profile.dwellSeconds();

        return new TravelLeg(
            fromStation.id(),
            toStation.id(),
            fromStation.latitude(),
            fromStation.longitude(),
            toStation.latitude(),
            toStation.longitude(),
            travelSeconds,
            dwellSeconds,
            variation,
            shapePoints
        );
    }

    private double polylineDistanceKm(List<GeneratorContracts.ReferencePointResponse> shapePoints) {
        double distanceKm = 0.0;
        for (int index = 0; index < shapePoints.size() - 1; index++) {
            GeneratorContracts.ReferencePointResponse fromPoint = shapePoints.get(index);
            GeneratorContracts.ReferencePointResponse toPoint = shapePoints.get(index + 1);
            distanceKm += GeoMath.distanceKm(
                fromPoint.latitude(),
                fromPoint.longitude(),
                toPoint.latitude(),
                toPoint.longitude()
            );
        }
        return distanceKm;
    }

    private GeneratorContracts.ReferencePointResponse interpolateAlongShape(
        List<GeneratorContracts.ReferencePointResponse> shapePoints,
        double factor
    ) {
        GeneratorContracts.ReferencePointResponse firstPoint = shapePoints.get(0);
        if (shapePoints.size() < 2) {
            return new GeneratorContracts.ReferencePointResponse(firstPoint.latitude(), firstPoint.longitude());
        }

        double totalDistanceKm = polylineDistanceKm(shapePoints);
        if (totalDistanceKm <= 0.0) {
            return new GeneratorContracts.ReferencePointResponse(firstPoint.latitude(), firstPoint.longitude());
        }

        double targetDistanceKm = totalDistanceKm * Math.max(0.0, Math.min(1.0, factor));
        double traversedDistanceKm = 0.0;
        for (int index = 0; index < shapePoints.size() - 1; index++) {
            GeneratorContracts.ReferencePointResponse fromPoint = shapePoints.get(index);
            GeneratorContracts.ReferencePointResponse toPoint = shapePoints.get(index + 1);
            double segmentDistanceKm = GeoMath.distanceKm(
                fromPoint.latitude(),
                fromPoint.longitude(),
                toPoint.latitude(),
                toPoint.longitude()
            );
            if (traversedDistanceKm + segmentDistanceKm >= targetDistanceKm) {
                double localFactor = segmentDistanceKm == 0.0
                    ? 0.0
                    : (targetDistanceKm - traversedDistanceKm) / segmentDistanceKm;
                return new GeneratorContracts.ReferencePointResponse(
                    GeoMath.interpolate(fromPoint.latitude(), toPoint.latitude(), localFactor),
                    GeoMath.interpolate(fromPoint.longitude(), toPoint.longitude(), localFactor)
                );
            }
            traversedDistanceKm += segmentDistanceKm;
        }

        GeneratorContracts.ReferencePointResponse lastPoint = shapePoints.get(shapePoints.size() - 1);
        return new GeneratorContracts.ReferencePointResponse(lastPoint.latitude(), lastPoint.longitude());
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

    private String trainNumber(String routeCode, int slot) {
        String prefix = TRAIN_PREFIXES.get(routeCode);
        if (prefix != null) {
            return prefix + String.format("%02d", slot + 1);
        }

        int routeBucket = Math.floorMod(routeCode != null ? routeCode.hashCode() : 0, 1_000_000);
        return String.format("%06d%02d", routeBucket, slot + 1);
    }

    private record TravelLeg(
        UUID fromStationId,
        UUID toStationId,
        double fromLatitude,
        double fromLongitude,
        double toLatitude,
        double toLongitude,
        long travelSeconds,
        long dwellSeconds,
        double speedVariationFactor,
        List<GeneratorContracts.ReferencePointResponse> shapePoints
    ) {
        long cycleDurationSeconds() {
            return travelSeconds + dwellSeconds;
        }
    }

    private record TrainPosition(
        UUID currentStationId,
        UUID nextStationId,
        double progressPercent,
        double latitude,
        double longitude,
        double speedKmh,
        String status
    ) {
    }
}
