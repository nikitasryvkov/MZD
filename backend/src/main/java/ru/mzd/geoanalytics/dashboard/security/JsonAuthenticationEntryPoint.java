package ru.mzd.geoanalytics.dashboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;
import ru.mzd.geoanalytics.dashboard.common.web.ApiErrorResponse;
import ru.mzd.geoanalytics.dashboard.common.web.RequestContext;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final SecurityAuditService securityAuditService;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper, SecurityAuditService securityAuditService) {
        this.objectMapper = objectMapper;
        this.securityAuditService = securityAuditService;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        securityAuditService.recordSecurityEvent(
            "AUTHENTICATION_FAILED",
            "FAILURE",
            null,
            request.getRequestURI(),
            request.getRemoteAddr(),
            java.util.Map.of("reason", authException.getMessage())
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
            "authentication_error",
            "Требуется аутентификация.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            List.of(),
            null,
            null,
            null
        ));
    }
}
