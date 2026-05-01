package ru.mzd.geoanalytics.dashboard.generator.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import ru.mzd.geoanalytics.dashboard.generator.application.model.GeneratorReferenceViewModels;
import ru.mzd.geoanalytics.dashboard.generator.application.port.GeneratorPersistencePort;
import ru.mzd.geoanalytics.dashboard.generator.domain.GeneratorModels;

@Service
public class GeneratorReferenceQueryService {

    private final GeneratorPersistencePort generatorPersistencePort;
    private final Clock clock;

    public GeneratorReferenceQueryService(
        GeneratorPersistencePort generatorPersistencePort,
        Clock clock
    ) {
        this.generatorPersistencePort = generatorPersistencePort;
        this.clock = clock;
    }

    public GeneratorReferenceViewModels.ReferenceNetworkView loadReferenceNetwork() {
        GeneratorModels.ReferenceNetwork network = generatorPersistencePort.loadReferenceNetwork();
        return new GeneratorReferenceViewModels.ReferenceNetworkView(
            network.generatedAt() != null ? network.generatedAt() : Instant.now(clock),
            network.stations().stream()
                .map(station -> new GeneratorReferenceViewModels.ReferenceStationView(
                    station.id(),
                    station.code(),
                    station.name(),
                    station.departmentCode(),
                    station.stationType(),
                    station.latitude(),
                    station.longitude()
                ))
                .toList(),
            network.routeSegments().stream()
                .map(segment -> new GeneratorReferenceViewModels.ReferenceRouteSegmentView(
                    segment.id(),
                    segment.fromStationId(),
                    segment.fromStationCode(),
                    segment.toStationId(),
                    segment.toStationCode(),
                    segment.departmentCode(),
                    segment.lengthKm(),
                    segment.shapePoints().stream()
                        .map(point -> new GeneratorReferenceViewModels.ReferencePointView(point.latitude(), point.longitude()))
                        .toList(),
                    segment.status()
                ))
                .toList(),
            network.routes().stream()
                .map(route -> new GeneratorReferenceViewModels.ReferenceRouteView(
                    route.id(),
                    route.code(),
                    route.name(),
                    route.stops().stream()
                        .map(stop -> new GeneratorReferenceViewModels.ReferenceRouteStopView(
                            stop.stationId(),
                            stop.stationCode(),
                            stop.sequenceNo()
                        ))
                        .toList()
                ))
                .toList()
        );
    }

    public java.util.List<GeneratorReferenceViewModels.ActiveEventView> loadActiveEvents() {
        return generatorPersistencePort.loadActiveEvents().stream()
            .map(event -> new GeneratorReferenceViewModels.ActiveEventView(
                event.id(),
                event.eventType(),
                event.title(),
                event.description(),
                event.severity(),
                event.status().name(),
                event.affectedObjectId(),
                event.departmentCode(),
                event.latitude(),
                event.longitude(),
                event.startedAt(),
                event.updatedAt(),
                event.lastChangedBy()
            ))
            .toList();
    }
}
