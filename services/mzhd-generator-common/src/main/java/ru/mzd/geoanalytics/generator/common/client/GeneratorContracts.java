package ru.mzd.geoanalytics.generator.common.client;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class GeneratorContracts {

    private GeneratorContracts() {
    }

    public record ReferenceNetworkResponse(
        Instant generatedAt,
        List<ReferenceStationResponse> stations,
        List<ReferenceRouteSegmentResponse> routeSegments,
        List<ReferenceRouteResponse> routes
    ) {
    }

    public record EmptyRequest(
        Instant requestedAt
    ) {
    }

    public record ReferenceStationResponse(
        UUID id,
        String code,
        String name,
        String departmentCode,
        String stationType,
        double latitude,
        double longitude
    ) {
    }

    public record ReferenceRouteSegmentResponse(
        UUID id,
        UUID fromStationId,
        String fromStationCode,
        UUID toStationId,
        String toStationCode,
        String departmentCode,
        double lengthKm,
        List<ReferencePointResponse> shapePoints,
        String status
    ) {
    }

    public record ReferencePointResponse(
        double latitude,
        double longitude
    ) {
    }

    public record ReferenceRouteResponse(
        UUID id,
        String code,
        String name,
        List<ReferenceRouteStopResponse> stops
    ) {
    }

    public record ReferenceRouteStopResponse(
        UUID stationId,
        String stationCode,
        int sequenceNo
    ) {
    }

    public record ActiveEventResponse(
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

    public record SyncTrainsRequest(
        Instant generatedAt,
        String sourceSystem,
        List<TrainUpsertRequest> trains
    ) {
    }

    public record TrainUpsertRequest(
        UUID id,
        String trainNumber,
        UUID routeId,
        UUID currentStationId,
        UUID nextStationId,
        double progressPercent,
        double latitude,
        double longitude,
        double speedKmh,
        String status
    ) {
    }

    public record SyncEventsRequest(
        Instant generatedAt,
        String sourceSystem,
        List<EventUpsertRequest> events
    ) {
    }

    public record EventUpsertRequest(
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
        Instant endedAt,
        String comment
    ) {
    }

    public record SyncPersonnelSnapshotRequest(
        LocalDate periodMonth,
        String sourceSystem,
        List<PersonnelSnapshotItemRequest> items
    ) {
    }

    public record PersonnelSnapshotItemRequest(
        String dimensionKey,
        int headcount,
        Double changePercent
    ) {
    }

    public record BatchIngestionResponse(
        int acceptedCount,
        Instant appliedAt,
        String sourceSystem
    ) {
    }
}
