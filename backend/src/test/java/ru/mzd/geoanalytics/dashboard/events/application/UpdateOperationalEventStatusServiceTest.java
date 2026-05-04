package ru.mzd.geoanalytics.dashboard.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mzd.geoanalytics.dashboard.analytics.application.port.KpiProjectionPort;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateCommand;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateView;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventAuditPort;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventStreamingPort;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventAggregate;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusUpdateResult;

@ExtendWith(MockitoExtension.class)
class UpdateOperationalEventStatusServiceTest {

    @Mock
    private OperationalEventPersistencePort persistencePort;
    @Mock
    private KpiProjectionPort kpiProjectionPort;
    @Mock
    private OperationalEventStreamingPort operationalEventStreamingPort;
    @Mock
    private OperationalEventAuditPort operationalEventAuditPort;

    @InjectMocks
    private UpdateOperationalEventStatusService service;

    @Test
    void shouldUpdateStatusAndPublishSideEffects() {
        UUID eventId = UUID.randomUUID();
        when(persistencePort.findAggregateForUpdate(eventId)).thenReturn(java.util.Optional.of(
            new OperationalEventAggregate(
                eventId,
                "Signal disruption",
                EventStatus.REGISTERED,
                Instant.parse("2026-04-21T11:00:00Z"),
                "Moscow Passenger",
                null
            )
        ));
        when(persistencePort.applyStatusChange(org.mockito.ArgumentMatchers.any())).thenReturn(
            new OperationalEventStatusUpdateResult(
                eventId,
                EventStatus.IN_PROGRESS,
                Instant.parse("2026-04-21T11:05:00Z"),
                "Moscow Passenger"
            )
        );
        when(persistencePort.findEventProjection(eventId)).thenReturn(java.util.Optional.of(
            new OperationalEventProjection(
                eventId,
                "Signal disruption",
                EventStatus.IN_PROGRESS,
                "HIGH",
                55.75,
                37.61,
                UUID.randomUUID(),
                "Moscow Passenger",
                Instant.parse("2026-04-21T10:45:00Z"),
                Instant.parse("2026-04-21T11:05:00Z")
            )
        ));

        OperationalEventStatusUpdateView response = service.updateStatus(
            eventId,
            new OperationalEventStatusUpdateCommand(
                EventStatus.IN_PROGRESS,
                "Dispatcher took ownership",
                "operator-1"
            )
        );

        assertThat(response.status()).isEqualTo(EventStatus.IN_PROGRESS);
        assertThat(response.allowedTransitions()).containsExactly(EventStatus.RESOLVED, EventStatus.CANCELED);
        verify(kpiProjectionPort).recalculateGlobalSnapshot();
        verify(operationalEventAuditPort).recordStatusChange(eventId, "operator-1", "IN_PROGRESS");
        verify(operationalEventStreamingPort).publishEventUpsert(org.mockito.ArgumentMatchers.any());
    }
}
