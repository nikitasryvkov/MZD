package ru.mzd.geoanalytics.dashboard.generator.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GeneratorReferenceViewModels {

    private GeneratorReferenceViewModels() {
    }

    public record ReferenceNetworkView(
        Instant generatedAt,
        List<ReferenceStationView> stations,
        List<ReferenceRouteSegmentView> routeSegments,
        List<ReferenceRouteView> routes
    ) {
    }

    public record ReferenceStationView(
        UUID id,
        String code,
        String name,
        String departmentCode,
        String stationType,
        double latitude,
        double longitude
    ) {
    }

    public record ReferenceRouteSegmentView(
        UUID id,
        UUID fromStationId,
        String fromStationCode,
        UUID toStationId,
        String toStationCode,
        String departmentCode,
        double lengthKm,
        List<ReferencePointView> shapePoints,
        String status
    ) {
    }

    public record ReferencePointView(
        double latitude,
        double longitude
    ) {
    }

    public record ReferenceRouteView(
        UUID id,
        String code,
        String name,
        List<ReferenceRouteStopView> stops
    ) {
    }

    public record ReferenceRouteStopView(
        UUID stationId,
        String stationCode,
        int sequenceNo
    ) {
    }

    public record ActiveEventView(
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
        String lastChangedBy
    ) {
    }
}
