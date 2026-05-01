package ru.mzd.geoanalytics.dashboard.dashboard.application.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DashboardViewModels {

    private DashboardViewModels() {
    }

    public record DashboardSnapshotView(
        KpiSummaryView kpiSummary,
        PersonnelSummaryView personnelSummary,
        MapDataView mapData,
        List<EventPreviewView> eventsPreview
    ) {
    }

    public record KpiSummaryView(
        int activeEventsCount,
        int trainsOnLineCount,
        int overloadedSectionsCount,
        Instant updatedAt
    ) {
    }

    public record PersonnelSummaryView(
        int totalHeadcount,
        List<PersonnelSummaryItemView> items
    ) {
    }

    public record PersonnelSummaryItemView(
        String dimensionKey,
        int headcount,
        Double changePercent
    ) {
    }

    public record MapDataView(
        List<StationView> stations,
        List<RouteSegmentView> routeSegments,
        List<TrainView> trains,
        List<OperationalEventView> operationalEvents,
        GeoJsonSourcesView geoJsonSources
    ) {
    }

    public record GeoJsonSourcesView(
        GeoJsonFeatureCollectionView stations,
        GeoJsonFeatureCollectionView routeSegments,
        GeoJsonFeatureCollectionView trains,
        GeoJsonFeatureCollectionView operationalEvents
    ) {
    }

    public record GeoJsonFeatureCollectionView(
        String type,
        List<GeoJsonFeatureView> features
    ) {
    }

    public record GeoJsonFeatureView(
        String type,
        String id,
        JsonNode geometry,
        Map<String, Object> properties
    ) {
    }

    public record StationView(
        UUID id,
        String code,
        String name,
        double latitude,
        double longitude,
        String stationType
    ) {
    }

    public record RouteSegmentView(
        UUID id,
        UUID fromStationId,
        UUID toStationId,
        JsonNode geometry,
        Double lengthKm,
        String status
    ) {
    }

    public record TrainView(
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

    public record OperationalEventView(
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

    public record EventPreviewView(
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
