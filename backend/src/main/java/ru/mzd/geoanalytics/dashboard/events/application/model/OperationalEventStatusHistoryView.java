package ru.mzd.geoanalytics.dashboard.events.application.model;

import java.time.Instant;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public record OperationalEventStatusHistoryView(
    UUID id,
    EventStatus fromStatus,
    EventStatus toStatus,
    String comment,
    Instant changedAt,
    String changedBy
) {
}
