package ru.mzd.geoanalytics.dashboard.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveOrGenerate(request.getHeader(REQUEST_ID_HEADER));
        String traceId = request.getHeader(TRACE_ID_HEADER);

        request.setAttribute(RequestContext.REQUEST_ID_ATTRIBUTE, requestId);
        if (traceId != null && !traceId.isBlank()) {
            request.setAttribute(RequestContext.TRACE_ID_ATTRIBUTE, traceId);
        }

        response.setHeader(REQUEST_ID_HEADER, requestId);

        MDC.put("requestId", requestId);
        if (traceId != null && !traceId.isBlank()) {
            MDC.put("traceId", traceId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private String resolveOrGenerate(String incomingValue) {
        if (incomingValue == null || incomingValue.isBlank()) {
            return UUID.randomUUID().toString();
        }

        try {
            return UUID.fromString(incomingValue).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID().toString();
        }
    }
}
