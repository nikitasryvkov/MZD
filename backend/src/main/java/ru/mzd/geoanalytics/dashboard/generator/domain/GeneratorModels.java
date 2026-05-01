package ru.mzd.geoanalytics.dashboard.generator.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public final class GeneratorModels {

    private GeneratorModels() {
    }

    public record ReferenceNetwork(
        Instant generatedAt,
        List<ReferenceStation> stations,
        List<ReferenceRouteSegment> routeSegments,
        List<ReferenceRoute> routes
    ) {
    }

    public record ReferenceStation(
        UUID id,
        String code,
        String name,
        String departmentCode,
        String stationType,
        double latitude,
        double longitude
    ) {
    }

    public record ReferenceRouteSegment(
        UUID id,
        UUID fromStationId,
        String fromStationCode,
        UUID toStationId,
        String toStationCode,
        String departmentCode,
        double lengthKm,
        List<ReferencePoint> shapePoints,
        String status
    ) {
    }

    public record ReferencePoint(
        double latitude,
        double longitude
    ) {
    }

    public record ReferenceRoute(
        UUID id,
        String code,
        String name,
        List<ReferenceRouteStop> stops
    ) {
    }

    public record ReferenceRouteStop(
        UUID stationId,
        String stationCode,
        int sequenceNo
    ) {
    }

    public record ActiveEvent(
        UUID id,
        String eventType,
        String title,
        String description,
        String severity,
        EventStatus status,
        UUID affectedObjectId,
        String departmentCode,
        double latitude,
        double longitude,
        Instant startedAt,
        Instant updatedAt,
        String lastChangedBy
    ) {
    }

    public record TrainUpsertCommand(
        UUID id,
        String trainNumber,
        UUID routeId,
        UUID currentStationId,
        UUID nextStationId,
        double progressPercent,
        double latitude,
        double longitude,
        double speedKmh,
        String status,
        Instant lastUpdated
    ) {
    }

    public record EventUpsertCommand(
        UUID id,
        String eventType,
        String title,
        String description,
        String severity,
        EventStatus status,
        UUID affectedObjectId,
        String departmentCode,
        double latitude,
        double longitude,
        Instant startedAt,
        Instant endedAt,
        String comment,
        String sourceSystem
    ) {
    }

    public record PersonnelSnapshotCommand(
        LocalDate periodMonth,
        String dimensionType,
        List<PersonnelSnapshotItem> items
    ) {
    }

    public record PersonnelSnapshotItem(
        String dimensionKey,
        int headcount,
        Double changePercent
    ) {
    }
}
