package ru.mzd.geoanalytics.dashboard.events.application.model;

import java.time.Instant;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public record OperationalEventProjection(
    UUID id,
    String title,
    EventStatus status,
    String severity,
    double latitude,
    double longitude,
    UUID affectedObjectId,
    String affectedSection,
    Instant startedAt,
    Instant updatedAt
) {
}
