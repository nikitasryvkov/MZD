package ru.mzd.geoanalytics.dashboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mzd.geoanalytics.dashboard.audit.SecurityAuditService;

@Component
public class SecurityAuditRequestFilter extends OncePerRequestFilter {

    private final SecurityAuditService securityAuditService;

    public SecurityAuditRequestFilter(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/") || path.equals("/actuator/prometheus"));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || response.getStatus() >= 400) {
            return;
        }

        securityAuditService.recordSecurityEvent(
            "AUTHENTICATION_ACCEPTED",
            "SUCCESS",
            authentication.getName(),
            request.getRequestURI(),
            request.getRemoteAddr(),
            Map.of(
                "method", request.getMethod(),
                "status", response.getStatus()
            )
        );
    }
}
