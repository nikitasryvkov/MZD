package ru.mzd.geoanalytics.dashboard.events.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.analytics.application.port.KpiProjectionPort;
import ru.mzd.geoanalytics.dashboard.common.exception.ResourceNotFoundException;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateCommand;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateView;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventAuditPort;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventStreamingPort;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventAggregate;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusChange;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusUpdateResult;

@Service
public class UpdateOperationalEventStatusService {

    private final OperationalEventPersistencePort persistencePort;
    private final KpiProjectionPort kpiProjectionPort;
    private final OperationalEventStreamingPort operationalEventStreamingPort;
    private final OperationalEventAuditPort operationalEventAuditPort;

    public UpdateOperationalEventStatusService(
        OperationalEventPersistencePort persistencePort,
        KpiProjectionPort kpiProjectionPort,
        OperationalEventStreamingPort operationalEventStreamingPort,
        OperationalEventAuditPort operationalEventAuditPort
    ) {
        this.persistencePort = persistencePort;
        this.kpiProjectionPort = kpiProjectionPort;
        this.operationalEventStreamingPort = operationalEventStreamingPort;
        this.operationalEventAuditPort = operationalEventAuditPort;
    }

    @Transactional
    public OperationalEventStatusUpdateView updateStatus(
        UUID eventId,
        OperationalEventStatusUpdateCommand command
    ) {
        OperationalEventAggregate aggregate = persistencePort.findAggregateForUpdate(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("РћРїРµСЂР°С‚РёРІРЅРѕРµ СЃРѕР±С‹С‚РёРµ РЅРµ РЅР°Р№РґРµРЅРѕ."));

        OperationalEventStatusChange statusChange = aggregate.changeStatus(
            command.newStatus(),
            command.principalId(),
            command.comment()
        );

        OperationalEventStatusUpdateResult result = persistencePort.applyStatusChange(statusChange);
        kpiProjectionPort.recalculateGlobalSnapshot();
        operationalEventAuditPort.recordStatusChange(eventId, command.principalId(), result.status().name());

        persistencePort.findEventProjection(eventId).ifPresent(operationalEventStreamingPort::publishEventUpsert);

        return new OperationalEventStatusUpdateView(
            result.eventId(),
            result.status(),
            result.updatedAt(),
            result.affectedSection(),
            statusChange.summary(),
            List.copyOf(result.status().allowedTransitions())
        );
    }
}
