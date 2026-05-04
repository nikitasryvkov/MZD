package ru.mzd.geoanalytics.dashboard.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class DashboardStreamingGateway {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public DashboardStreamingGateway(
        SimpMessagingTemplate messagingTemplate,
        ObjectMapper objectMapper
    ) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public StreamingMessages.TrainUpdateMessage createTrainUpdateMessage(
        StreamingMessages.StreamOperation operation,
        StreamingMessages.TrainPayload train,
        UUID messageId,
        long sequence,
        Instant generatedAt
    ) {
        return new StreamingMessages.TrainUpdateMessage(
            messageId,
            sequence,
            generatedAt,
            operation.name(),
            train,
            toTrainFeature(train)
        );
    }

    public StreamingMessages.EventUpdateMessage createEventUpdateMessage(
        StreamingMessages.StreamOperation operation,
        StreamingMessages.EventPayload event,
        UUID messageId,
        long sequence,
        Instant generatedAt
    ) {
        return new StreamingMessages.EventUpdateMessage(
            messageId,
            sequence,
            generatedAt,
            operation.name(),
            event,
            toOperationalEventFeature(event)
        );
    }

    public void publishTrainUpdate(StreamingMessages.TrainUpdateMessage message) {
        messagingTemplate.convertAndSend(StreamingTopics.TRAINS, message);
    }

    public void publishEventUpdate(StreamingMessages.EventUpdateMessage message) {
        messagingTemplate.convertAndSend(StreamingTopics.EVENTS, message);
    }

    private StreamingMessages.GeoJsonFeature toTrainFeature(StreamingMessages.TrainPayload train) {
        return new StreamingMessages.GeoJsonFeature(
            "Feature",
            train.id().toString(),
            pointGeometry(train.longitude(), train.latitude()),
            compactProperties(
                "id", train.id().toString(),
                "kind", "train",
                "trainNumber", train.trainNumber(),
                "status", train.status(),
                "currentStationId", stringify(train.currentStationId()),
                "nextStationId", stringify(train.nextStationId()),
                "progressPercent", train.progressPercent(),
                "speedKmh", train.speedKmh(),
                "lastUpdated", stringify(train.lastUpdated())
            )
        );
    }

    private StreamingMessages.GeoJsonFeature toOperationalEventFeature(StreamingMessages.EventPayload event) {
        return new StreamingMessages.GeoJsonFeature(
            "Feature",
            event.id().toString(),
            pointGeometry(event.longitude(), event.latitude()),
            compactProperties(
                "id", event.id().toString(),
                "kind", "event",
                "title", event.title(),
                "status", event.status(),
                "severity", event.severity(),
                "affectedObjectId", stringify(event.affectedObjectId()),
                "affectedSection", event.affectedSection(),
                "startedAt", stringify(event.startedAt()),
                "updatedAt", stringify(event.updatedAt())
            )
        );
    }

    private JsonNode pointGeometry(double longitude, double latitude) {
        return objectMapper.valueToTree(Map.of(
            "type", "Point",
            "coordinates", List.of(longitude, latitude)
        ));
    }

    private Map<String, Object> compactProperties(Object... keyValuePairs) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int index = 0; index < keyValuePairs.length; index += 2) {
            Object value = keyValuePairs[index + 1];
            if (value != null) {
                properties.put(String.valueOf(keyValuePairs[index]), value);
            }
        }
        return properties;
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
