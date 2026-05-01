package ru.mzd.geoanalytics.dashboard.dashboard.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;
import ru.mzd.geoanalytics.dashboard.dashboard.domain.DashboardQuery;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

@Component
public class DashboardApiMapper {

    public DashboardQuery toDomainQuery(DashboardApiModels.DashboardQueryRequest request) {
        DashboardQuery.BoundingBox boundingBox = request.bbox() == null ? null : new DashboardQuery.BoundingBox(
            request.bbox().minLat(),
            request.bbox().minLon(),
            request.bbox().maxLat(),
            request.bbox().maxLon()
        );
        DashboardQuery.TimeRange timeRange = request.timeRange() == null ? null : new DashboardQuery.TimeRange(
            request.timeRange().from(),
            request.timeRange().to()
        );

        return new DashboardQuery(
            boundingBox,
            new DashboardQuery.LayerFilter(
                request.layerFilter().showStations(),
                request.layerFilter().showSegments(),
                request.layerFilter().showTrains(),
                request.layerFilter().showEvents()
            ),
            timeRange,
            request.eventStatuses() == null
                ? java.util.Set.of()
                : request.eventStatuses().stream().map(EventStatus::valueOf).collect(java.util.stream.Collectors.toSet()),
            request.departmentCodes() == null
                ? List.of()
                : request.departmentCodes().stream().map(String::trim).filter(value -> !value.isBlank()).toList(),
            request.includeKpi(),
            request.includePersonnel()
        );
    }

    public DashboardApiModels.DashboardQueryResponse toApiResponse(
        UUID requestId,
        Instant generatedAt,
        DashboardViewModels.DashboardSnapshotView snapshot
    ) {
        return new DashboardApiModels.DashboardQueryResponse(
            requestId,
            generatedAt,
            snapshot.kpiSummary() == null ? null : toApiResponse(snapshot.kpiSummary()),
            snapshot.personnelSummary() == null ? null : toApiResponse(snapshot.personnelSummary()),
            toApiResponse(snapshot.mapData()),
            snapshot.eventsPreview().stream().map(this::toApiResponse).toList()
        );
    }

    private DashboardApiModels.KpiSummaryResponse toApiResponse(DashboardViewModels.KpiSummaryView view) {
        return new DashboardApiModels.KpiSummaryResponse(
            view.activeEventsCount(),
            view.trainsOnLineCount(),
            view.overloadedSectionsCount(),
            view.updatedAt()
        );
    }

    private DashboardApiModels.PersonnelSummaryResponse toApiResponse(DashboardViewModels.PersonnelSummaryView view) {
        return new DashboardApiModels.PersonnelSummaryResponse(
            view.totalHeadcount(),
            view.items().stream().map(this::toApiResponse).toList()
        );
    }

    private DashboardApiModels.PersonnelSummaryItemResponse toApiResponse(
        DashboardViewModels.PersonnelSummaryItemView view
    ) {
        return new DashboardApiModels.PersonnelSummaryItemResponse(
            view.dimensionKey(),
            view.headcount(),
            view.changePercent()
        );
    }

    private DashboardApiModels.MapDataResponse toApiResponse(DashboardViewModels.MapDataView view) {
        return new DashboardApiModels.MapDataResponse(
            view.stations().stream().map(this::toApiResponse).toList(),
            view.routeSegments().stream().map(this::toApiResponse).toList(),
            view.trains().stream().map(this::toApiResponse).toList(),
            view.operationalEvents().stream().map(this::toApiResponse).toList(),
            view.geoJsonSources() == null ? null : toApiResponse(view.geoJsonSources())
        );
    }

    private DashboardApiModels.GeoJsonSourcesResponse toApiResponse(DashboardViewModels.GeoJsonSourcesView view) {
        return new DashboardApiModels.GeoJsonSourcesResponse(
            toApiResponse(view.stations()),
            toApiResponse(view.routeSegments()),
            toApiResponse(view.trains()),
            toApiResponse(view.operationalEvents())
        );
    }

    private DashboardApiModels.GeoJsonFeatureCollectionResponse toApiResponse(
        DashboardViewModels.GeoJsonFeatureCollectionView view
    ) {
        return new DashboardApiModels.GeoJsonFeatureCollectionResponse(
            view.type(),
            view.features().stream().map(this::toApiResponse).toList()
        );
    }

    private DashboardApiModels.GeoJsonFeatureResponse toApiResponse(DashboardViewModels.GeoJsonFeatureView view) {
        return new DashboardApiModels.GeoJsonFeatureResponse(
            view.type(),
            view.id(),
            view.geometry(),
            view.properties()
        );
    }

    private DashboardApiModels.StationResponse toApiResponse(DashboardViewModels.StationView view) {
        return new DashboardApiModels.StationResponse(
            view.id(),
            view.code(),
            view.name(),
            view.latitude(),
            view.longitude(),
            view.stationType()
        );
    }

    private DashboardApiModels.RouteSegmentResponse toApiResponse(DashboardViewModels.RouteSegmentView view) {
        return new DashboardApiModels.RouteSegmentResponse(
            view.id(),
            view.fromStationId(),
            view.toStationId(),
            view.geometry(),
            view.lengthKm(),
            view.status()
        );
    }

    private DashboardApiModels.TrainResponse toApiResponse(DashboardViewModels.TrainView view) {
        return new DashboardApiModels.TrainResponse(
            view.id(),
            view.trainNumber(),
            view.latitude(),
            view.longitude(),
            view.status(),
            view.currentStationId(),
            view.nextStationId(),
            view.progressPercent(),
            view.speedKmh(),
            view.lastUpdated()
        );
    }

    private DashboardApiModels.OperationalEventResponse toApiResponse(DashboardViewModels.OperationalEventView view) {
        return new DashboardApiModels.OperationalEventResponse(
            view.id(),
            view.title(),
            view.status(),
            view.severity(),
            view.latitude(),
            view.longitude(),
            view.affectedObjectId(),
            view.affectedSection(),
            view.startedAt(),
            view.updatedAt()
        );
    }

    private DashboardApiModels.EventPreviewResponse toApiResponse(DashboardViewModels.EventPreviewView view) {
        return new DashboardApiModels.EventPreviewResponse(
            view.id(),
            view.title(),
            view.severity(),
            view.status(),
            view.affectedSection(),
            view.startedAt(),
            view.updatedAt()
        );
    }
}
