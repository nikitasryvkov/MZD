package ru.mzd.geoanalytics.dashboard.generator.infrastructure.messaging;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.generator.api.GeneratorApiModels;
import ru.mzd.geoanalytics.dashboard.generator.api.GeneratorApiMapper;
import ru.mzd.geoanalytics.dashboard.generator.application.GeneratorIngestionService;
import ru.mzd.geoanalytics.dashboard.generator.application.GeneratorReferenceQueryService;
import ru.mzd.geoanalytics.dashboard.generator.application.model.GeneratorIngestionModels;

@Component
public class GeneratorRabbitListener {

    private static final String REFERENCE_NETWORK_QUEUE =
        "${generator.messaging.rabbitmq.queues.reference-network-request:mzhd.generator.reference-network.queue}";
    private static final String ACTIVE_EVENTS_QUEUE =
        "${generator.messaging.rabbitmq.queues.active-events-request:mzhd.generator.active-events.queue}";
    private static final String TRAINS_SYNC_QUEUE =
        "${generator.messaging.rabbitmq.queues.trains-sync:mzhd.generator.trains-sync.queue}";
    private static final String EVENTS_SYNC_QUEUE =
        "${generator.messaging.rabbitmq.queues.events-sync:mzhd.generator.events-sync.queue}";
    private static final String PERSONNEL_SYNC_QUEUE =
        "${generator.messaging.rabbitmq.queues.personnel-snapshot-sync:mzhd.generator.personnel-sync.queue}";

    private final GeneratorReferenceQueryService generatorReferenceQueryService;
    private final GeneratorIngestionService generatorIngestionService;
    private final GeneratorApiMapper generatorApiMapper;
    private final Validator validator;

    public GeneratorRabbitListener(
        GeneratorReferenceQueryService generatorReferenceQueryService,
        GeneratorIngestionService generatorIngestionService,
        GeneratorApiMapper generatorApiMapper,
        Validator validator
    ) {
        this.generatorReferenceQueryService = generatorReferenceQueryService;
        this.generatorIngestionService = generatorIngestionService;
        this.generatorApiMapper = generatorApiMapper;
        this.validator = validator;
    }

    @RabbitListener(queues = REFERENCE_NETWORK_QUEUE)
    public GeneratorApiModels.ReferenceNetworkResponse loadReferenceNetwork(GeneratorApiModels.EmptyRequest request) {
        validate(request);
        return generatorApiMapper.toApiResponse(generatorReferenceQueryService.loadReferenceNetwork());
    }

    @RabbitListener(queues = ACTIVE_EVENTS_QUEUE)
    public List<GeneratorApiModels.ActiveEventResponse> loadActiveEvents(GeneratorApiModels.EmptyRequest request) {
        validate(request);
        return generatorApiMapper.toActiveEventResponses(generatorReferenceQueryService.loadActiveEvents());
    }

    @RabbitListener(queues = TRAINS_SYNC_QUEUE)
    public GeneratorApiModels.BatchIngestionResponse syncTrains(GeneratorApiModels.SyncTrainsRequest request) {
        validate(request);
        GeneratorIngestionModels.BatchIngestionView result = generatorIngestionService.syncTrains(
            generatorApiMapper.toCommand(request),
            "rabbitmq:" + request.sourceSystem()
        );
        return generatorApiMapper.toApiResponse(result);
    }

    @RabbitListener(queues = EVENTS_SYNC_QUEUE)
    public GeneratorApiModels.BatchIngestionResponse syncEvents(GeneratorApiModels.SyncEventsRequest request) {
        validate(request);
        GeneratorIngestionModels.BatchIngestionView result = generatorIngestionService.syncEvents(
            generatorApiMapper.toCommand(request),
            "rabbitmq:" + request.sourceSystem()
        );
        return generatorApiMapper.toApiResponse(result);
    }

    @RabbitListener(queues = PERSONNEL_SYNC_QUEUE)
    public GeneratorApiModels.BatchIngestionResponse syncPersonnelSnapshot(
        GeneratorApiModels.SyncPersonnelSnapshotRequest request
    ) {
        validate(request);
        GeneratorIngestionModels.BatchIngestionView result = generatorIngestionService.syncPersonnelSnapshot(
            generatorApiMapper.toCommand(request),
            "rabbitmq:" + request.sourceSystem()
        );
        return generatorApiMapper.toApiResponse(result);
    }

    private <T> void validate(T payload) {
        if (payload == null) {
            throw new IllegalArgumentException("RabbitMQ payload is required.");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
