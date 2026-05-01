package ru.mzd.geoanalytics.dashboard.generator.api;

import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.generator.application.model.GeneratorIngestionModels;
import ru.mzd.geoanalytics.dashboard.generator.application.model.GeneratorReferenceViewModels;

@Component
public class GeneratorApiMapper {

    public GeneratorIngestionModels.SyncTrainsCommand toCommand(GeneratorApiModels.SyncTrainsRequest request) {
        return new GeneratorIngestionModels.SyncTrainsCommand(
            request.generatedAt(),
            request.sourceSystem(),
            request.trains().stream()
                .map(train -> new GeneratorIngestionModels.TrainUpsertItem(
                    train.id(),
                    train.trainNumber(),
                    train.routeId(),
                    train.currentStationId(),
                    train.nextStationId(),
                    train.progressPercent(),
                    train.latitude(),
                    train.longitude(),
                    train.speedKmh(),
                    train.status()
                ))
                .toList()
        );
    }

    public GeneratorIngestionModels.SyncEventsCommand toCommand(GeneratorApiModels.SyncEventsRequest request) {
        return new GeneratorIngestionModels.SyncEventsCommand(
            request.generatedAt(),
            request.sourceSystem(),
            request.events().stream()
                .map(event -> new GeneratorIngestionModels.EventUpsertItem(
                    event.id(),
                    event.eventType(),
                    event.title(),
                    event.description(),
                    event.severity(),
                    EventStatus.valueOf(event.status()),
                    event.affectedObjectId(),
                    event.departmentCode(),
                    event.latitude(),
                    event.longitude(),
                    event.startedAt(),
                    event.endedAt(),
                    event.comment()
                ))
                .toList()
        );
    }

    public GeneratorIngestionModels.SyncPersonnelSnapshotCommand toCommand(
        GeneratorApiModels.SyncPersonnelSnapshotRequest request
    ) {
        return new GeneratorIngestionModels.SyncPersonnelSnapshotCommand(
            request.periodMonth(),
            request.sourceSystem(),
            request.items().stream()
                .map(item -> new GeneratorIngestionModels.PersonnelSnapshotItem(
                    item.dimensionKey(),
                    item.headcount(),
                    item.changePercent()
                ))
                .toList()
        );
    }

    public GeneratorApiModels.BatchIngestionResponse toApiResponse(GeneratorIngestionModels.BatchIngestionView view) {
        return new GeneratorApiModels.BatchIngestionResponse(
            view.acceptedCount(),
            view.appliedAt(),
            view.sourceSystem()
        );
    }

    public GeneratorApiModels.ReferenceNetworkResponse toApiResponse(
        GeneratorReferenceViewModels.ReferenceNetworkView view
    ) {
        return new GeneratorApiModels.ReferenceNetworkResponse(
            view.generatedAt(),
            view.stations().stream().map(this::toApiResponse).toList(),
            view.routeSegments().stream().map(this::toApiResponse).toList(),
            view.routes().stream().map(this::toApiResponse).toList()
        );
    }

    public java.util.List<GeneratorApiModels.ActiveEventResponse> toActiveEventResponses(
        java.util.List<GeneratorReferenceViewModels.ActiveEventView> views
    ) {
        return views.stream().map(this::toApiResponse).toList();
    }

    private GeneratorApiModels.ReferenceStationResponse toApiResponse(GeneratorReferenceViewModels.ReferenceStationView view) {
        return new GeneratorApiModels.ReferenceStationResponse(
            view.id(),
            view.code(),
            view.name(),
            view.departmentCode(),
            view.stationType(),
            view.latitude(),
            view.longitude()
        );
    }

    private GeneratorApiModels.ReferenceRouteSegmentResponse toApiResponse(
        GeneratorReferenceViewModels.ReferenceRouteSegmentView view
    ) {
        return new GeneratorApiModels.ReferenceRouteSegmentResponse(
            view.id(),
            view.fromStationId(),
            view.fromStationCode(),
            view.toStationId(),
            view.toStationCode(),
            view.departmentCode(),
            view.lengthKm(),
            view.shapePoints().stream().map(this::toApiResponse).toList(),
            view.status()
        );
    }

    private GeneratorApiModels.ReferencePointResponse toApiResponse(GeneratorReferenceViewModels.ReferencePointView view) {
        return new GeneratorApiModels.ReferencePointResponse(view.latitude(), view.longitude());
    }

    private GeneratorApiModels.ReferenceRouteResponse toApiResponse(GeneratorReferenceViewModels.ReferenceRouteView view) {
        return new GeneratorApiModels.ReferenceRouteResponse(
            view.id(),
            view.code(),
            view.name(),
            view.stops().stream().map(this::toApiResponse).toList()
        );
    }

    private GeneratorApiModels.ReferenceRouteStopResponse toApiResponse(
        GeneratorReferenceViewModels.ReferenceRouteStopView view
    ) {
        return new GeneratorApiModels.ReferenceRouteStopResponse(
            view.stationId(),
            view.stationCode(),
            view.sequenceNo()
        );
    }

    private GeneratorApiModels.ActiveEventResponse toApiResponse(GeneratorReferenceViewModels.ActiveEventView view) {
        return new GeneratorApiModels.ActiveEventResponse(
            view.id(),
            view.eventType(),
            view.title(),
            view.description(),
            view.severity(),
            view.status(),
            view.affectedObjectId(),
            view.departmentCode(),
            view.latitude(),
            view.longitude(),
            view.startedAt(),
            view.updatedAt(),
            view.lastChangedBy()
        );
    }
}
