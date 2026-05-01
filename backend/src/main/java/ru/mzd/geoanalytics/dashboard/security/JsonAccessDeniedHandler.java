package ru.mzd.geoanalytics.dashboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;
import ru.mzd.geoanalytics.dashboard.common.web.ApiErrorResponse;
import ru.mzd.geoanalytics.dashboard.common.web.RequestContext;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityAuditService securityAuditService;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper, SecurityAuditService securityAuditService) {
        this.objectMapper = objectMapper;
        this.securityAuditService = securityAuditService;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principalId = authentication != null ? authentication.getName() : null;

        securityAuditService.recordSecurityEvent(
            "ACCESS_DENIED",
            "FAILURE",
            principalId,
            request.getRequestURI(),
            request.getRemoteAddr(),
            java.util.Map.of("reason", accessDeniedException.getMessage())
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
            "forbidden",
            "У вас нет доступа к этому ресурсу.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            List.of(),
            null,
            null,
            null
        ));
    }
}
