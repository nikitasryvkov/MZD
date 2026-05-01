package ru.mzd.geoanalytics.dashboard.events.domain;

import java.time.Instant;
import java.util.UUID;

public record OperationalEventStatusUpdateResult(
    UUID eventId,
    EventStatus status,
    Instant updatedAt,
    String affectedSection
) {
}
