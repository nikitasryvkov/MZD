package ru.mzd.geoanalytics.dashboard.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String type,
    String message,
    UUID requestId,
    String traceId,
    List<FieldValidationError> fieldErrors,
    String currentStatus,
    String requestedStatus,
    List<String> allowedTransitions
) {
}
