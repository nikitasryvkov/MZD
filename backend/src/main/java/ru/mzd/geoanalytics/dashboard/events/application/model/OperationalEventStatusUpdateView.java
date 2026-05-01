package ru.mzd.geoanalytics.dashboard.events.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public record OperationalEventStatusUpdateView(
    UUID eventId,
    EventStatus status,
    Instant updatedAt,
    String affectedSection,
    String summary,
    List<EventStatus> allowedTransitions
) {
}
