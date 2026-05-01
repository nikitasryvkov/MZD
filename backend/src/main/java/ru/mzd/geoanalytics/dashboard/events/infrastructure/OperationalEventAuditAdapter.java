package ru.mzd.geoanalytics.dashboard.events.infrastructure;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventAuditPort;

@Component
public class OperationalEventAuditAdapter implements OperationalEventAuditPort {

    private final SecurityAuditService securityAuditService;

    public OperationalEventAuditAdapter(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @Override
    public void recordStatusChange(UUID eventId, String principalId, String newStatus) {
        securityAuditService.recordEventStatusChange(eventId, principalId, newStatus);
    }
}
