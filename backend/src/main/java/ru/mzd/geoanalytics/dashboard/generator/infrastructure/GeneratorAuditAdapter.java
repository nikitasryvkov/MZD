package ru.mzd.geoanalytics.dashboard.generator.infrastructure;

import java.util.Map;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;
import ru.mzd.geoanalytics.dashboard.generator.application.port.GeneratorAuditPort;

@Component
public class GeneratorAuditAdapter implements GeneratorAuditPort {

    private final SecurityAuditService securityAuditService;

    public GeneratorAuditAdapter(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @Override
    public void recordBatchAudit(
        String eventType,
        String principalId,
        String sourceSystem,
        int acceptedCount
    ) {
        securityAuditService.recordSecurityEvent(
            eventType,
            "SUCCESS",
            principalId,
            "/api/internal/v1/generator",
            null,
            Map.of(
                "sourceSystem", sourceSystem,
                "acceptedCount", acceptedCount
            )
        );
    }
}
