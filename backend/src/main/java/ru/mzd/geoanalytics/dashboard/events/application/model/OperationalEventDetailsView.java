package ru.mzd.geoanalytics.dashboard.events.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public record OperationalEventDetailsView(
    UUID id,
    String type,
    String title,
    String description,
    EventStatus status,
    String severity,
    double latitude,
    double longitude,
    UUID affectedObjectId,
    String affectedSection,
    Instant startedAt,
    Instant endedAt,
    Instant updatedAt,
    String lastChangedBy,
    List<EventStatus> allowedTransitions,
    List<OperationalEventStatusHistoryView> statusHistory
) {
}
