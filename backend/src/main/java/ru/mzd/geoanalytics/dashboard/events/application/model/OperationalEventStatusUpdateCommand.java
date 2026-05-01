package ru.mzd.geoanalytics.dashboard.events.application.model;

import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public record OperationalEventStatusUpdateCommand(
    EventStatus newStatus,
    String comment,
    String principalId
) {
}
