package ru.mzd.geoanalytics.dashboard.simulation.infrastructure;

import java.util.Map;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationAuditPort;

@Component
public class SimulationAuditAdapter implements SimulationAuditPort {

    private final SecurityAuditService securityAuditService;

    public SimulationAuditAdapter(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @Override
    public void recordLifecycle(String eventType, String outcome, Map<String, Object> details) {
        securityAuditService.recordSimulationLifecycle(eventType, outcome, details);
    }
}
