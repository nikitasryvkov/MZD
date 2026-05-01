package ru.mzd.geoanalytics.dashboard.common.exception;

import java.util.List;

public class ConflictException extends RuntimeException {

    private final String currentStatus;
    private final String requestedStatus;
    private final List<String> allowedTransitions;

    public ConflictException(
        String message,
        String currentStatus,
        String requestedStatus,
        List<String> allowedTransitions
    ) {
        super(message);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
        this.allowedTransitions = allowedTransitions;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getRequestedStatus() {
        return requestedStatus;
    }

    public List<String> getAllowedTransitions() {
        return allowedTransitions;
    }
}
