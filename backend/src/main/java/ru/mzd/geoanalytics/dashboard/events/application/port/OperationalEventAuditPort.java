package ru.mzd.geoanalytics.dashboard.events.application.port;

import java.util.UUID;

public interface OperationalEventAuditPort {

    void recordStatusChange(UUID eventId, String principalId, String newStatus);
}
