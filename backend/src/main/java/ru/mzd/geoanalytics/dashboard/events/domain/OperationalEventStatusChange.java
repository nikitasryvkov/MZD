package ru.mzd.geoanalytics.dashboard.events.domain;

import java.util.UUID;

public record OperationalEventStatusChange(
    UUID eventId,
    EventStatus fromStatus,
    EventStatus toStatus,
    String principalId,
    String comment,
    String summary
) {
}
