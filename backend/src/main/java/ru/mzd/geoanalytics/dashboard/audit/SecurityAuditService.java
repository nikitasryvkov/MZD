package ru.mzd.geoanalytics.dashboard.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import ru.mzd.geoanalytics.dashboard.common.web.RequestContext;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SecurityAuditService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void recordSecurityEvent(
        String eventType,
        String outcome,
        String principalId,
        String targetResource,
        String sourceIp,
        Map<String, Object> details
    ) {
        try {
            jdbcTemplate.update("""
                INSERT INTO dashboard.security_audit_log (
                    principal_id,
                    event_type,
                    outcome,
                    source_ip,
                    target_resource,
                    request_id,
                    trace_id,
                    details_json
                )
                VALUES (
                    :principalId,
                    :eventType,
                    :outcome,
                    CAST(:sourceIp AS inet),
                    :targetResource,
                    :requestId,
                    :traceId,
                    CAST(:detailsJson AS jsonb)
                )
                """, new MapSqlParameterSource()
                .addValue("principalId", blankToNull(principalId))
                .addValue("eventType", eventType)
                .addValue("outcome", outcome)
                .addValue("sourceIp", blankToNull(sourceIp))
                .addValue("targetResource", targetResource)
                .addValue("requestId", RequestContext.currentRequestId())
                .addValue("traceId", RequestContext.currentTraceId().orElse(null))
                .addValue("detailsJson", toJson(details)));
        } catch (Exception exception) {
            log.warn("Не удалось записать запись аудита безопасности для eventType={}", eventType, exception);
        }
    }

    public void recordEventStatusChange(UUID eventId, String principalId, String newStatus) {
        recordSecurityEvent(
            "EVENT_STATUS_CHANGED",
            "SUCCESS",
            principalId,
            "/api/v1/events/" + eventId + "/status",
            null,
            Map.of(
                "eventId", eventId,
                "newStatus", newStatus
            )
        );
    }

    public void recordSimulationLifecycle(String eventType, String outcome, Map<String, Object> details) {
        recordSecurityEvent(eventType, outcome, "simulation-engine", "/internal/simulation", null, details);
    }

    private String toJson(Map<String, Object> details) throws JsonProcessingException {
        return objectMapper.writeValueAsString(details == null ? Map.of() : details);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
