package ru.mzd.geoanalytics.dashboard.generator.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.analytics.application.port.KpiProjectionPort;
import ru.mzd.geoanalytics.dashboard.common.exception.ResourceNotFoundException;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventStreamingPort;
import ru.mzd.geoanalytics.dashboard.generator.application.model.GeneratorIngestionModels;
import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;
import ru.mzd.geoanalytics.dashboard.generator.application.port.GeneratorAuditPort;
import ru.mzd.geoanalytics.dashboard.generator.application.port.GeneratorPersistencePort;
import ru.mzd.geoanalytics.dashboard.generator.application.port.TrainStreamingPort;
import ru.mzd.geoanalytics.dashboard.generator.domain.GeneratorModels;

@Service
public class GeneratorIngestionService {

    private final GeneratorPersistencePort generatorPersistencePort;
    private final OperationalEventPersistencePort operationalEventPersistencePort;
    private final TrainStreamingPort trainStreamingPort;
    private final OperationalEventStreamingPort operationalEventStreamingPort;
    private final KpiProjectionPort kpiProjectionPort;
    private final GeneratorAuditPort generatorAuditPort;
    private final Clock clock;

    public GeneratorIngestionService(
        GeneratorPersistencePort generatorPersistencePort,
        OperationalEventPersistencePort operationalEventPersistencePort,
        TrainStreamingPort trainStreamingPort,
        OperationalEventStreamingPort operationalEventStreamingPort,
        KpiProjectionPort kpiProjectionPort,
        GeneratorAuditPort generatorAuditPort,
        Clock clock
    ) {
        this.generatorPersistencePort = generatorPersistencePort;
        this.operationalEventPersistencePort = operationalEventPersistencePort;
        this.trainStreamingPort = trainStreamingPort;
        this.operationalEventStreamingPort = operationalEventStreamingPort;
        this.kpiProjectionPort = kpiProjectionPort;
        this.generatorAuditPort = generatorAuditPort;
        this.clock = clock;
    }

    @Transactional
    public GeneratorIngestionModels.BatchIngestionView syncTrains(
        GeneratorIngestionModels.SyncTrainsCommand request,
        String principalId
    ) {

        for (GeneratorIngestionModels.TrainUpsertItem train : request.trains()) {
            TrainProjection projection = generatorPersistencePort.upsertTrain(
                new GeneratorModels.TrainUpsertCommand(
                    train.id(),
                    train.trainNumber(),
                    train.routeId(),
                    train.currentStationId(),
                    train.nextStationId(),
                    train.progressPercent(),
                    train.latitude(),
                    train.longitude(),
                    train.speedKmh(),
                    train.status(),
                    request.generatedAt()
                )
            );
            trainStreamingPort.publishTrainUpsert(projection);
        }

        kpiProjectionPort.recalculateGlobalSnapshot();
        generatorAuditPort.recordBatchAudit("GENERATOR_TRAINS_SYNC", principalId, request.sourceSystem(), request.trains().size());
        return new GeneratorIngestionModels.BatchIngestionView(
            request.trains().size(),
            Instant.now(clock),
            request.sourceSystem()
        );
    }

    @Transactional
    public GeneratorIngestionModels.BatchIngestionView syncEvents(
        GeneratorIngestionModels.SyncEventsCommand request,
        String principalId
    ) {

        for (GeneratorIngestionModels.EventUpsertItem event : request.events()) {
            generatorPersistencePort.upsertEvent(
                new GeneratorModels.EventUpsertCommand(
                    event.id(),
                    event.eventType(),
                    event.title(),
                    event.description(),
                    event.severity(),
                    event.status(),
                    event.affectedObjectId(),
                    event.departmentCode(),
                    event.latitude(),
                    event.longitude(),
                    event.startedAt(),
                    event.endedAt(),
                    event.comment(),
                    request.sourceSystem()
                )
            );

            OperationalEventProjection projection = operationalEventPersistencePort.findEventProjection(event.id())
                .orElseThrow(() -> new ResourceNotFoundException("Operational event projection is missing after ingestion: " + event.id()));
            operationalEventStreamingPort.publishEventUpsert(projection);
        }

        kpiProjectionPort.recalculateGlobalSnapshot();
        generatorAuditPort.recordBatchAudit("GENERATOR_EVENTS_SYNC", principalId, request.sourceSystem(), request.events().size());
        return new GeneratorIngestionModels.BatchIngestionView(
            request.events().size(),
            Instant.now(clock),
            request.sourceSystem()
        );
    }

    @Transactional
    public GeneratorIngestionModels.BatchIngestionView syncPersonnelSnapshot(
        GeneratorIngestionModels.SyncPersonnelSnapshotCommand request,
        String principalId
    ) {

        generatorPersistencePort.replacePersonnelSnapshot(new GeneratorModels.PersonnelSnapshotCommand(
            request.periodMonth().withDayOfMonth(1),
            "DEPARTMENT_CODE",
            request.items().stream()
                .map(item -> new GeneratorModels.PersonnelSnapshotItem(
                    item.dimensionKey(),
                    item.headcount(),
                    item.changePercent()
                ))
                .toList()
        ));

        generatorAuditPort.recordBatchAudit("GENERATOR_PERSONNEL_SYNC", principalId, request.sourceSystem(), request.items().size());
        return new GeneratorIngestionModels.BatchIngestionView(
            request.items().size(),
            Instant.now(clock),
            request.sourceSystem()
        );
    }
}
