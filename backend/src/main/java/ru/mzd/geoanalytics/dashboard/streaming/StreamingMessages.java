package ru.mzd.geoanalytics.dashboard.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
        TrainPayload train,
        GeoJsonFeature feature
    ) {
    }

    public record EventUpdateMessage(
        UUID messageId,
        long sequence,
        Instant generatedAt,
        String operation,
        EventPayload event,
        GeoJsonFeature feature
    ) {
    }

    public record TrainPayload(
        UUID id,
        String trainNumber,
        double latitude,
        double longitude,
        String status,
        UUID currentStationId,
        UUID nextStationId,
        Double progressPercent,
        Double speedKmh,
        Instant lastUpdated
    ) {
    }

    public record EventPayload(
        UUID id,
        String title,
        String status,
        String severity,
        double latitude,
        double longitude,
        UUID affectedObjectId,
        String affectedSection,
        Instant startedAt,
        Instant updatedAt
    ) {
    }

    public record GeoJsonFeature(
        String type,
        String id,
        JsonNode geometry,
        Map<String, Object> properties
    ) {
    }
}
