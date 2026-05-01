package ru.mzd.geoanalytics.dashboard.events.application.port;

import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;

public interface OperationalEventStreamingPort {

    void publishEventUpsert(OperationalEventProjection eventProjection);
}
