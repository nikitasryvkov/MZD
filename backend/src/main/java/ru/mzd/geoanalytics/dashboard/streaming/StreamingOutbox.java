package ru.mzd.geoanalytics.dashboard.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;

@Component
public class StreamingOutbox {

    private static final String TRAIN_SEQUENCE = "dashboard.streaming_train_sequence";
    private static final String EVENT_SEQUENCE = "dashboard.streaming_event_sequence";
    private static final String LEGACY_TRAINS_TOPIC = "/topic/v1/trains";
    private static final String LEGACY_EVENTS_TOPIC = "/topic/v1/events";
    private static final int MAX_RETRY_DELAY_SECONDS = 300;
    private static final int MAX_STORED_ERROR_LENGTH = 2_000;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final DashboardStreamingGateway dashboardStreamingGateway;
    private final ApplicationProperties applicationProperties;
    private final Clock clock;

    public StreamingOutbox(
        NamedParameterJdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        DashboardStreamingGateway dashboardStreamingGateway,
        ApplicationProperties applicationProperties,
        Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.dashboardStreamingGateway = dashboardStreamingGateway;
        this.applicationProperties = applicationProperties;
        this.clock = clock;
    }

    @Transactional
    public void enqueueTrainUpsert(StreamingMessages.TrainPayload train) {
        StreamingMessages.TrainUpdateMessage message =
            dashboardStreamingGateway.createTrainUpdateMessage(
                StreamingMessages.StreamOperation.UPSERT,
                train,
                UUID.randomUUID(),
                nextSequence(TRAIN_SEQUENCE),
                Instant.now(clock)
            );

        enqueueOrPublish(StreamingTopics.TRAINS, message);
    }

    @Transactional
    public void enqueueEventUpsert(StreamingMessages.EventPayload event) {
        StreamingMessages.EventUpdateMessage message =
            dashboardStreamingGateway.createEventUpdateMessage(
                StreamingMessages.StreamOperation.UPSERT,
                event,
                UUID.randomUUID(),
                nextSequence(EVENT_SEQUENCE),
                Instant.now(clock)
            );

        enqueueOrPublish(StreamingTopics.EVENTS, message);
    }

    @Scheduled(fixedDelayString = "${app.streaming.outbox.dispatch-fixed-delay-ms:1000}")
    @Transactional
    public void dispatchPending() {
        if (!applicationProperties.getStreaming().getOutbox().isEnabled()) {
            return;
        }

        List<OutboxRow> pendingRows = jdbcTemplate.query(
            """
            SELECT id, topic, CAST(payload AS text) AS payload, attempts
            FROM dashboard.streaming_outbox
            WHERE published_at IS NULL
              AND next_attempt_at <= now()
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """,
            new MapSqlParameterSource(
                "batchSize",
                applicationProperties.getStreaming().getOutbox().getBatchSize()
            ),
            (resultSet, rowNum) -> new OutboxRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("topic"),
                resultSet.getString("payload"),
                resultSet.getInt("attempts")
            )
        );

        for (OutboxRow row : pendingRows) {
            dispatch(row);
        }
    }

    private void enqueueOrPublish(String topic, Object message) {
        if (!applicationProperties.getStreaming().getOutbox().isEnabled()) {
            publish(topic, message);
            return;
        }

        enqueue(topic, message);
    }

    private void enqueue(String topic, Object message) {
        jdbcTemplate.update(
            """
            INSERT INTO dashboard.streaming_outbox (id, topic, payload)
            VALUES (:id, :topic, CAST(:payload AS jsonb))
            """,
            new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("topic", topic)
                .addValue("payload", serialize(message))
        );
    }

    private void dispatch(OutboxRow row) {
        try {
            publish(row.topic(), row.payload());
            markPublished(row.id());
        } catch (Exception exception) {
            markFailed(row, exception);
        }
    }

    private void publish(String topic, Object payload) {
        try {
            if (StreamingTopics.TRAINS.equals(topic) || LEGACY_TRAINS_TOPIC.equals(topic)) {
                StreamingMessages.TrainUpdateMessage message = payload instanceof String serializedPayload
                    ? objectMapper.readValue(serializedPayload, StreamingMessages.TrainUpdateMessage.class)
                    : (StreamingMessages.TrainUpdateMessage) payload;
                dashboardStreamingGateway.publishTrainUpdate(message);
                return;
            }

            if (StreamingTopics.EVENTS.equals(topic) || LEGACY_EVENTS_TOPIC.equals(topic)) {
                StreamingMessages.EventUpdateMessage message = payload instanceof String serializedPayload
                    ? objectMapper.readValue(serializedPayload, StreamingMessages.EventUpdateMessage.class)
                    : (StreamingMessages.EventUpdateMessage) payload;
                dashboardStreamingGateway.publishEventUpdate(message);
                return;
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid streaming outbox payload.", exception);
        }

        throw new IllegalArgumentException("Unsupported streaming topic: " + topic);
    }

    private void markPublished(UUID id) {
        jdbcTemplate.update(
            """
            UPDATE dashboard.streaming_outbox
            SET published_at = now(),
                last_error = NULL
            WHERE id = :id
            """,
            new MapSqlParameterSource("id", id)
        );
    }

    private void markFailed(OutboxRow row, Exception exception) {
        int nextAttempts = row.attempts() + 1;
        long retryDelaySeconds = Math.min(
            MAX_RETRY_DELAY_SECONDS,
            1L << Math.min(nextAttempts, 8)
        );
        Instant nextAttemptAt = Instant.now(clock).plusSeconds(retryDelaySeconds);

        jdbcTemplate.update(
            """
            UPDATE dashboard.streaming_outbox
            SET attempts = :attempts,
                next_attempt_at = :nextAttemptAt,
                last_error = :lastError
            WHERE id = :id
            """,
            new MapSqlParameterSource()
                .addValue("id", row.id())
                .addValue("attempts", nextAttempts)
                .addValue("nextAttemptAt", Timestamp.from(nextAttemptAt))
                .addValue("lastError", truncateError(exception))
        );
    }

    private long nextSequence(String sequenceName) {
        Long sequence = jdbcTemplate.getJdbcOperations().queryForObject(
            "SELECT nextval('" + sequenceName + "')",
            Long.class
        );

        if (sequence == null) {
            throw new IllegalStateException("Streaming sequence was not generated.");
        }

        return sequence;
    }

    private String serialize(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize streaming message.", exception);
        }
    }

    private String truncateError(Exception exception) {
        String message = exception.getMessage();
        String value = message == null ? exception.getClass().getName() : message;
        return value.length() <= MAX_STORED_ERROR_LENGTH
            ? value
            : value.substring(0, MAX_STORED_ERROR_LENGTH);
    }

    private record OutboxRow(
        UUID id,
        String topic,
        String payload,
        int attempts
    ) {
    }
}
