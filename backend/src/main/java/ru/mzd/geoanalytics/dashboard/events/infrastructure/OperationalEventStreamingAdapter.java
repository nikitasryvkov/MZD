package ru.mzd.geoanalytics.dashboard.events.infrastructure;

import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventStreamingPort;
import ru.mzd.geoanalytics.dashboard.streaming.StreamingMessages;
import ru.mzd.geoanalytics.dashboard.streaming.StreamingOutbox;

@Component
public class OperationalEventStreamingAdapter implements OperationalEventStreamingPort {

    private final StreamingOutbox streamingOutbox;

    public OperationalEventStreamingAdapter(StreamingOutbox streamingOutbox) {
        this.streamingOutbox = streamingOutbox;
    }

    @Override
    public void publishEventUpsert(OperationalEventProjection eventProjection) {
        StreamingMessages.EventPayload payload = new StreamingMessages.EventPayload(
            eventProjection.id(),
            eventProjection.title(),
            eventProjection.status().name(),
            eventProjection.severity(),
            eventProjection.latitude(),
            eventProjection.longitude(),
            eventProjection.affectedObjectId(),
            eventProjection.affectedSection(),
            eventProjection.startedAt(),
            eventProjection.updatedAt()
        );
        streamingOutbox.enqueueEventUpsert(payload);
    }
}
