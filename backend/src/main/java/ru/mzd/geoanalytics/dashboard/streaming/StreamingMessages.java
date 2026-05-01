package ru.mzd.geoanalytics.dashboard.streaming;

import java.time.Instant;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.dashboard.api.DashboardApiModels;

public final class StreamingMessages {

    private StreamingMessages() {
    }

    public enum StreamOperation {
        UPSERT,
        REMOVE
    }

    public record TrainUpdateMessage(
        UUID messageId,
        long sequence,
        Instant generatedAt,
        String operation,
        DashboardApiModels.TrainResponse train,
        DashboardApiModels.GeoJsonFeatureResponse feature
    ) {
    }

    public record EventUpdateMessage(
        UUID messageId,
        long sequence,
        Instant generatedAt,
        String operation,
        DashboardApiModels.OperationalEventResponse event,
        DashboardApiModels.GeoJsonFeatureResponse feature
    ) {
    }
}
