package ru.mzd.geoanalytics.dashboard.generator.infrastructure;

import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;
import ru.mzd.geoanalytics.dashboard.generator.application.port.TrainStreamingPort;
import ru.mzd.geoanalytics.dashboard.streaming.StreamingMessages;
import ru.mzd.geoanalytics.dashboard.streaming.StreamingOutbox;

@Component
public class TrainStreamingAdapter implements TrainStreamingPort {

    private final StreamingOutbox streamingOutbox;

    public TrainStreamingAdapter(StreamingOutbox streamingOutbox) {
        this.streamingOutbox = streamingOutbox;
    }

    @Override
    public void publishTrainUpsert(TrainProjection trainProjection) {
        StreamingMessages.TrainPayload payload = new StreamingMessages.TrainPayload(
            trainProjection.id(),
            trainProjection.trainNumber(),
            trainProjection.latitude(),
            trainProjection.longitude(),
            trainProjection.status(),
            trainProjection.currentStationId(),
            trainProjection.nextStationId(),
            trainProjection.progressPercent(),
            trainProjection.speedKmh(),
            trainProjection.lastUpdated()
        );
        streamingOutbox.enqueueTrainUpsert(payload);
    }
}
