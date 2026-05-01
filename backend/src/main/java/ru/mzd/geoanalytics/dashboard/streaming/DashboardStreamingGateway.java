package ru.mzd.geoanalytics.dashboard.streaming;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.dashboard.api.DashboardApiModels;
import ru.mzd.geoanalytics.dashboard.dashboard.infrastructure.MapLibreGeoJsonAssembler;

@Component
public class DashboardStreamingGateway {

    private final SimpMessagingTemplate messagingTemplate;
    private final MapLibreGeoJsonAssembler mapLibreGeoJsonAssembler;
    private final Clock clock;
    private final AtomicLong trainSequence = new AtomicLong();
    private final AtomicLong eventSequence = new AtomicLong();

    public DashboardStreamingGateway(
        SimpMessagingTemplate messagingTemplate,
        MapLibreGeoJsonAssembler mapLibreGeoJsonAssembler,
        Clock clock
    ) {
        this.messagingTemplate = messagingTemplate;
        this.mapLibreGeoJsonAssembler = mapLibreGeoJsonAssembler;
        this.clock = clock;
    }

    public void publishTrainUpsert(DashboardApiModels.TrainResponse train) {
        messagingTemplate.convertAndSend(StreamingTopics.TRAINS, new StreamingMessages.TrainUpdateMessage(
            UUID.randomUUID(),
            trainSequence.incrementAndGet(),
            Instant.now(clock),
            StreamingMessages.StreamOperation.UPSERT.name(),
            train,
            mapLibreGeoJsonAssembler.toTrainFeature(train)
        ));
    }

    public void publishTrainRemove(DashboardApiModels.TrainResponse train) {
        messagingTemplate.convertAndSend(StreamingTopics.TRAINS, new StreamingMessages.TrainUpdateMessage(
            UUID.randomUUID(),
            trainSequence.incrementAndGet(),
            Instant.now(clock),
            StreamingMessages.StreamOperation.REMOVE.name(),
            train,
            mapLibreGeoJsonAssembler.toTrainFeature(train)
        ));
    }

    public void publishEventUpsert(DashboardApiModels.OperationalEventResponse event) {
        messagingTemplate.convertAndSend(StreamingTopics.EVENTS, new StreamingMessages.EventUpdateMessage(
            UUID.randomUUID(),
            eventSequence.incrementAndGet(),
            Instant.now(clock),
            StreamingMessages.StreamOperation.UPSERT.name(),
            event,
            mapLibreGeoJsonAssembler.toOperationalEventFeature(event)
        ));
    }

    public void publishEventRemove(DashboardApiModels.OperationalEventResponse event) {
        messagingTemplate.convertAndSend(StreamingTopics.EVENTS, new StreamingMessages.EventUpdateMessage(
            UUID.randomUUID(),
            eventSequence.incrementAndGet(),
            Instant.now(clock),
            StreamingMessages.StreamOperation.REMOVE.name(),
            event,
            mapLibreGeoJsonAssembler.toOperationalEventFeature(event)
        ));
    }
}
