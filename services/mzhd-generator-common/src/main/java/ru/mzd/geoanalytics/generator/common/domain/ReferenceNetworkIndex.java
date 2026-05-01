package ru.mzd.geoanalytics.generator.common.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.mzd.geoanalytics.generator.common.client.GeneratorContracts;

public final class ReferenceNetworkIndex {

    private final GeneratorContracts.ReferenceNetworkResponse network;
    private final Map<UUID, GeneratorContracts.ReferenceStationResponse> stationsById;
    private final Map<UUID, GeneratorContracts.ReferenceRouteResponse> routesById;
    private final Map<String, GeneratorContracts.ReferenceRouteSegmentResponse> segmentsByKey;

    private ReferenceNetworkIndex(
        GeneratorContracts.ReferenceNetworkResponse network,
        Map<UUID, GeneratorContracts.ReferenceStationResponse> stationsById,
        Map<UUID, GeneratorContracts.ReferenceRouteResponse> routesById,
        Map<String, GeneratorContracts.ReferenceRouteSegmentResponse> segmentsByKey
    ) {
        this.network = network;
        this.stationsById = stationsById;
        this.routesById = routesById;
        this.segmentsByKey = segmentsByKey;
    }

    public static ReferenceNetworkIndex of(GeneratorContracts.ReferenceNetworkResponse network) {
        Map<String, GeneratorContracts.ReferenceRouteSegmentResponse> segmentsByKey = new java.util.LinkedHashMap<>();
        for (GeneratorContracts.ReferenceRouteSegmentResponse segment : network.routeSegments()) {
            segmentsByKey.put(segmentKey(segment.fromStationId(), segment.toStationId()), segment);
            segmentsByKey.put(segmentKey(segment.toStationId(), segment.fromStationId()), reverseSegment(segment));
        }

        return new ReferenceNetworkIndex(
            network,
            network.stations().stream().collect(Collectors.toMap(GeneratorContracts.ReferenceStationResponse::id, Function.identity())),
            network.routes().stream().collect(Collectors.toMap(GeneratorContracts.ReferenceRouteResponse::id, Function.identity())),
            Map.copyOf(segmentsByKey)
        );
    }

    public List<GeneratorContracts.ReferenceStationResponse> stations() {
        return network.stations();
    }

    public List<GeneratorContracts.ReferenceRouteSegmentResponse> routeSegments() {
        return network.routeSegments();
    }

    public List<GeneratorContracts.ReferenceRouteResponse> routes() {
        return network.routes();
    }

    public GeneratorContracts.ReferenceStationResponse station(UUID stationId) {
        GeneratorContracts.ReferenceStationResponse station = stationsById.get(stationId);
        if (station == null) {
            throw new IllegalStateException("Unknown station id: " + stationId);
        }
        return station;
    }

    public GeneratorContracts.ReferenceRouteResponse route(UUID routeId) {
        GeneratorContracts.ReferenceRouteResponse route = routesById.get(routeId);
        if (route == null) {
            throw new IllegalStateException("Unknown route id: " + routeId);
        }
        return route;
    }

    public List<GeneratorContracts.ReferenceRouteStopResponse> orderedStops(UUID routeId) {
        return route(routeId).stops().stream()
            .sorted(Comparator.comparingInt(GeneratorContracts.ReferenceRouteStopResponse::sequenceNo))
            .toList();
    }

    public GeneratorContracts.ReferenceRouteSegmentResponse routeSegment(UUID fromStationId, UUID toStationId) {
        return segmentsByKey.get(segmentKey(fromStationId, toStationId));
    }

    private static String segmentKey(UUID fromStationId, UUID toStationId) {
        return fromStationId + "->" + toStationId;
    }

    private static GeneratorContracts.ReferenceRouteSegmentResponse reverseSegment(
        GeneratorContracts.ReferenceRouteSegmentResponse segment
    ) {
        java.util.ArrayList<GeneratorContracts.ReferencePointResponse> reversedShape = new java.util.ArrayList<>(segment.shapePoints());
        java.util.Collections.reverse(reversedShape);
        return new GeneratorContracts.ReferenceRouteSegmentResponse(
            segment.id(),
            segment.toStationId(),
            segment.toStationCode(),
            segment.fromStationId(),
            segment.fromStationCode(),
            segment.departmentCode(),
            segment.lengthKm(),
            List.copyOf(reversedShape),
            segment.status()
        );
    }
}
