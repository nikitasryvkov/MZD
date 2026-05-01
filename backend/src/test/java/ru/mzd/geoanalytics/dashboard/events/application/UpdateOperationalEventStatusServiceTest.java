package ru.mzd.geoanalytics.dashboard.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mzd.geoanalytics.dashboard.analytics.KpiProjectionService;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;
import ru.mzd.geoanalytics.dashboard.dashboard.api.DashboardApiModels;
import ru.mzd.geoanalytics.dashboard.events.api.OperationalEventApiModels;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventAggregate;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusUpdateResult;
import ru.mzd.geoanalytics.dashboard.security.AuthenticatedUser;
import ru.mzd.geoanalytics.dashboard.streaming.DashboardStreamingGateway;

@ExtendWith(MockitoExtension.class)
class UpdateOperationalEventStatusServiceTest {

    @Mock
    private OperationalEventPersistencePort persistencePort;
    @Mock
    private KpiProjectionService kpiProjectionService;
    @Mock
    private DashboardStreamingGateway dashboardStreamingGateway;
    @Mock
    private SecurityAuditService securityAuditService;

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
            new DashboardApiModels.OperationalEventResponse(
                eventId,
                "Signal disruption",
                "IN_PROGRESS",
                "HIGH",
                55.75,
                37.61,
                UUID.randomUUID(),
                "Moscow Passenger",
                Instant.parse("2026-04-21T10:45:00Z"),
                Instant.parse("2026-04-21T11:05:00Z")
            )
        ));

        OperationalEventApiModels.UpdateOperationalEventStatusResponse response = service.updateStatus(
            eventId,
            new OperationalEventApiModels.UpdateOperationalEventStatusRequest("IN_PROGRESS", "Dispatcher took ownership"),
            new AuthenticatedUser("operator-1", Set.of("ROLE_MONITORING_USER"))
        );

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.allowedTransitions()).containsExactly("RESOLVED", "CANCELED");
        verify(kpiProjectionService).recalculateGlobalSnapshot();
        verify(securityAuditService).recordEventStatusChange(eventId, "operator-1", "IN_PROGRESS");
        verify(dashboardStreamingGateway).publishEventUpsert(org.mockito.ArgumentMatchers.any());
    }
}
