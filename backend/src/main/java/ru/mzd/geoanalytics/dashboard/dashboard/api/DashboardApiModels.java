package ru.mzd.geoanalytics.dashboard.dashboard.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.common.validation.ValueOfEnum;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public final class DashboardApiModels {

    private DashboardApiModels() {
    }

    public record DashboardQueryRequest(
        @Valid BoundingBoxRequest bbox,
        @NotNull @Valid LayerFilterRequest layerFilter,
        @Valid TimeRangeRequest timeRange,
        List<@ValueOfEnum(enumClass = EventStatus.class) String> eventStatuses,
        @Size(max = 100) List<@NotBlank @Size(max = 64) String> departmentCodes,
        @NotNull Boolean includeKpi,
        @NotNull Boolean includePersonnel
    ) {
    }

    public record BoundingBoxRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double minLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double minLon,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double maxLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double maxLon
    ) {
        @AssertTrue(message = "bbox.maxLat must be greater than bbox.minLat")
        @JsonIgnore
        public boolean isLatitudeRangeValid() {
            return maxLat > minLat;
        }

        @AssertTrue(message = "bbox.maxLon must be greater than bbox.minLon")
        @JsonIgnore
        public boolean isLongitudeRangeValid() {
            return maxLon > minLon;
        }
    }

    public record LayerFilterRequest(
        @NotNull Boolean showStations,
        @NotNull Boolean showSegments,
        @NotNull Boolean showTrains,
        @NotNull Boolean showEvents
    ) {
    }

    public record TimeRangeRequest(
        @NotNull Instant from,
        @NotNull Instant to
    ) {
        @AssertTrue(message = "timeRange.to must be later than timeRange.from")
        @JsonIgnore
        public boolean isRangeValid() {
            return to.isAfter(from);
        }
    }

    public record DashboardQueryResponse(
        UUID requestId,
        Instant generatedAt,
        KpiSummaryResponse kpiSummary,
        PersonnelSummaryResponse personnelSummary,
        MapDataResponse mapData,
        List<EventPreviewResponse> eventsPreview
    ) {
    }

    public record KpiSummaryResponse(
        int activeEventsCount,
        int trainsOnLineCount,
        int overloadedSectionsCount,
        Instant updatedAt
    ) {
    }

    public record PersonnelSummaryResponse(
        int totalHeadcount,
        List<PersonnelSummaryItemResponse> items
    ) {
    }

    public record PersonnelSummaryItemResponse(
        String dimensionKey,
        int headcount,
        Double changePercent
    ) {
    }

    public record MapDataResponse(
        List<StationResponse> stations,
        List<RouteSegmentResponse> routeSegments,
        List<TrainResponse> trains,
        List<OperationalEventResponse> operationalEvents,
        GeoJsonSourcesResponse geoJsonSources
    ) {
    }

    public record GeoJsonSourcesResponse(
        GeoJsonFeatureCollectionResponse stations,
        GeoJsonFeatureCollectionResponse routeSegments,
        GeoJsonFeatureCollectionResponse trains,
        GeoJsonFeatureCollectionResponse operationalEvents
    ) {
    }

    public record GeoJsonFeatureCollectionResponse(
        String type,
        List<GeoJsonFeatureResponse> features
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GeoJsonFeatureResponse(
        String type,
        String id,
        JsonNode geometry,
        Map<String, Object> properties
    ) {
    }

    public record StationResponse(
        UUID id,
        String code,
        String name,
        double latitude,
        double longitude,
        String stationType
    ) {
    }

    public record RouteSegmentResponse(
        UUID id,
        UUID fromStationId,
        UUID toStationId,
        JsonNode geometry,
        Double lengthKm,
        String status
    ) {
    }

    public record TrainResponse(
        UUID id,
        String trainNumber,
        double latitude,
        double longitude,
        String status,
        UUID currentStationId,
        UUID nextStationId,
        Double progressPercent,
        Double speedKmh,
        Instant lastUpdated
    ) {
    }

    public record OperationalEventResponse(
        UUID id,
        String title,
        String status,
        String severity,
        double latitude,
        double longitude,
        UUID affectedObjectId,
        String affectedSection,
        Instant startedAt,
        Instant updatedAt
    ) {
    }

    public record EventPreviewResponse(
        UUID id,
        String title,
        String severity,
        String status,
        String affectedSection,
        Instant startedAt,
        Instant updatedAt
    ) {
    }
}
