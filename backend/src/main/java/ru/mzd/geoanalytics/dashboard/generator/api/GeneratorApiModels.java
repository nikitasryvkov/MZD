package ru.mzd.geoanalytics.dashboard.generator.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.common.validation.ValueOfEnum;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public final class GeneratorApiModels {

    private GeneratorApiModels() {
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
        @NotNull Instant generatedAt,
        @NotBlank @Size(max = 128) String sourceSystem,
        @NotEmpty @Size(max = 256) List<@Valid TrainUpsertRequest> trains
    ) {
    }

    public record TrainUpsertRequest(
        @NotNull UUID id,
        @NotBlank @Size(max = 32) String trainNumber,
        @NotNull UUID routeId,
        @NotNull UUID currentStationId,
        @NotNull UUID nextStationId,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double progressPercent,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @DecimalMin("0.0") Double speedKmh,
        @NotBlank @Size(max = 32) String status
    ) {
    }

    public record SyncEventsRequest(
        @NotNull Instant generatedAt,
        @NotBlank @Size(max = 128) String sourceSystem,
        @NotEmpty @Size(max = 128) List<@Valid EventUpsertRequest> events
    ) {
    }

    public record EventUpsertRequest(
        @NotNull UUID id,
        @NotBlank @Size(max = 32) String eventType,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 16) String severity,
        @NotBlank @ValueOfEnum(enumClass = EventStatus.class) String status,
        UUID affectedObjectId,
        @Size(max = 64) String departmentCode,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Instant startedAt,
        Instant endedAt,
        @Size(max = 1000) String comment
    ) {
    }

    public record SyncPersonnelSnapshotRequest(
        @NotNull LocalDate periodMonth,
        @NotBlank @Size(max = 128) String sourceSystem,
        @NotEmpty @Size(max = 128) List<@Valid PersonnelSnapshotItemRequest> items
    ) {
    }

    public record PersonnelSnapshotItemRequest(
        @NotBlank @Size(max = 64) String dimensionKey,
        @Min(0) int headcount,
        @DecimalMin("-100.0") @DecimalMax("100.0") Double changePercent
    ) {
    }

    public record BatchIngestionResponse(
        int acceptedCount,
        Instant appliedAt,
        String sourceSystem
    ) {
    }
}
