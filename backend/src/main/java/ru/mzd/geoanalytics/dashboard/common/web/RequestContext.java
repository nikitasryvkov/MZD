package ru.mzd.geoanalytics.dashboard.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestContext {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String TRACE_ID_ATTRIBUTE = "traceId";

    private RequestContext() {
    }

    public static UUID currentRequestId() {
        return currentRequestIdString()
            .map(UUID::fromString)
            .orElseGet(UUID::randomUUID);
    }

    public static Optional<String> currentRequestIdString() {
        return currentRequestAttribute(REQUEST_ID_ATTRIBUTE);
    }

    public static Optional<String> currentTraceId() {
        return currentRequestAttribute(TRACE_ID_ATTRIBUTE);
    }

    public static UUID requestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return attribute instanceof String value ? UUID.fromString(value) : UUID.randomUUID();
    }

    public static String traceId(HttpServletRequest request) {
        Object attribute = request.getAttribute(TRACE_ID_ATTRIBUTE);
        return attribute instanceof String value ? value : null;
    }

    private static Optional<String> currentRequestAttribute(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            Object value = servletRequestAttributes.getRequest().getAttribute(name);
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return Optional.of(stringValue);
            }
        }
        return Optional.empty();
    }
}
