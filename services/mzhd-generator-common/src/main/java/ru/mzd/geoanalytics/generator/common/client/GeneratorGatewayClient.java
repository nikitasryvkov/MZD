package ru.mzd.geoanalytics.generator.common.client;

import java.time.Instant;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class GeneratorGatewayClient {

    private static final ParameterizedTypeReference<GeneratorContracts.ReferenceNetworkResponse> REFERENCE_NETWORK_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<List<GeneratorContracts.ActiveEventResponse>> ACTIVE_EVENTS_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<GeneratorContracts.BatchIngestionResponse> BATCH_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final RabbitTemplate rabbitTemplate;
    private final GeneratorServiceClientProperties properties;

    public GeneratorGatewayClient(
        RabbitTemplate rabbitTemplate,
        GeneratorServiceClientProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public GeneratorContracts.ReferenceNetworkResponse fetchReferenceNetwork() {
        return sendAndReceive(
            properties.getRoutingKeys().getReferenceNetworkRequest(),
            new GeneratorContracts.EmptyRequest(Instant.now()),
            REFERENCE_NETWORK_TYPE
        );
    }

    public List<GeneratorContracts.ActiveEventResponse> fetchActiveEvents() {
        return sendAndReceive(
            properties.getRoutingKeys().getActiveEventsRequest(),
            new GeneratorContracts.EmptyRequest(Instant.now()),
            ACTIVE_EVENTS_TYPE
        );
    }

    public GeneratorContracts.BatchIngestionResponse syncTrains(GeneratorContracts.SyncTrainsRequest request) {
        return sendAndReceive(properties.getRoutingKeys().getTrainsSync(), request, BATCH_RESPONSE_TYPE);
    }

    public GeneratorContracts.BatchIngestionResponse syncEvents(GeneratorContracts.SyncEventsRequest request) {
        return sendAndReceive(properties.getRoutingKeys().getEventsSync(), request, BATCH_RESPONSE_TYPE);
    }

    public GeneratorContracts.BatchIngestionResponse syncPersonnelSnapshot(
        GeneratorContracts.SyncPersonnelSnapshotRequest request
    ) {
        return sendAndReceive(properties.getRoutingKeys().getPersonnelSnapshotSync(), request, BATCH_RESPONSE_TYPE);
    }

    private <T> T sendAndReceive(
        String routingKey,
        Object request,
        ParameterizedTypeReference<T> responseType
    ) {
        T response = rabbitTemplate.convertSendAndReceiveAsType(
            properties.getExchange(),
            routingKey,
            request,
            responseType
        );

        if (response == null) {
            throw new IllegalStateException("No RabbitMQ reply received for routing key: " + routingKey);
        }

        return response;
    }
}
