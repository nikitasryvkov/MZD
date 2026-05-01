package ru.mzd.geoanalytics.dashboard.events.domain;

import java.util.List;
import java.util.Map;

public enum EventStatus {
    REGISTERED,
    IN_PROGRESS,
    RESOLVED,
    CANCELED;

    private static final Map<EventStatus, List<EventStatus>> ALLOWED_TRANSITIONS = Map.of(
        REGISTERED, List.of(IN_PROGRESS, CANCELED),
        IN_PROGRESS, List.of(RESOLVED, CANCELED),
        RESOLVED, List.of(),
        CANCELED, List.of()
    );

    public List<EventStatus> allowedTransitions() {
        return ALLOWED_TRANSITIONS.get(this);
    }

    public boolean canTransitionTo(EventStatus targetStatus) {
        return allowedTransitions().contains(targetStatus);
    }
}
