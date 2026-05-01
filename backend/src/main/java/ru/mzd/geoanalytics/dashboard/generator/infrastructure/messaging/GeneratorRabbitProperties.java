package ru.mzd.geoanalytics.dashboard.generator.infrastructure.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "generator.messaging.rabbitmq")
public class GeneratorRabbitProperties {

    @NotBlank
    private String exchange = "mzhd.generator.exchange";

    @Valid
    private RoutingKeys routingKeys = new RoutingKeys();

    @Valid
    private Queues queues = new Queues();

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public RoutingKeys getRoutingKeys() {
        return routingKeys;
    }

    public void setRoutingKeys(RoutingKeys routingKeys) {
        this.routingKeys = routingKeys;
    }

    public Queues getQueues() {
        return queues;
    }

    public void setQueues(Queues queues) {
        this.queues = queues;
    }

    public static class RoutingKeys {

        @NotBlank
        private String referenceNetworkRequest = "generator.reference-network.request";

        @NotBlank
        private String activeEventsRequest = "generator.active-events.request";

        @NotBlank
        private String trainsSync = "generator.trains.sync";

        @NotBlank
        private String eventsSync = "generator.events.sync";

        @NotBlank
        private String personnelSnapshotSync = "generator.personnel.snapshot.sync";

        public String getReferenceNetworkRequest() {
            return referenceNetworkRequest;
        }

        public void setReferenceNetworkRequest(String referenceNetworkRequest) {
            this.referenceNetworkRequest = referenceNetworkRequest;
        }

        public String getActiveEventsRequest() {
            return activeEventsRequest;
        }

        public void setActiveEventsRequest(String activeEventsRequest) {
            this.activeEventsRequest = activeEventsRequest;
        }

        public String getTrainsSync() {
            return trainsSync;
        }

        public void setTrainsSync(String trainsSync) {
            this.trainsSync = trainsSync;
        }

        public String getEventsSync() {
            return eventsSync;
        }

        public void setEventsSync(String eventsSync) {
            this.eventsSync = eventsSync;
        }

        public String getPersonnelSnapshotSync() {
            return personnelSnapshotSync;
        }

        public void setPersonnelSnapshotSync(String personnelSnapshotSync) {
            this.personnelSnapshotSync = personnelSnapshotSync;
        }
    }

    public static class Queues {

        @NotBlank
        private String referenceNetworkRequest = "mzhd.generator.reference-network.queue";

        @NotBlank
        private String activeEventsRequest = "mzhd.generator.active-events.queue";

        @NotBlank
        private String trainsSync = "mzhd.generator.trains-sync.queue";

        @NotBlank
        private String eventsSync = "mzhd.generator.events-sync.queue";

        @NotBlank
        private String personnelSnapshotSync = "mzhd.generator.personnel-sync.queue";

        public String getReferenceNetworkRequest() {
            return referenceNetworkRequest;
        }

        public void setReferenceNetworkRequest(String referenceNetworkRequest) {
            this.referenceNetworkRequest = referenceNetworkRequest;
        }

        public String getActiveEventsRequest() {
            return activeEventsRequest;
        }

        public void setActiveEventsRequest(String activeEventsRequest) {
            this.activeEventsRequest = activeEventsRequest;
        }

        public String getTrainsSync() {
            return trainsSync;
        }

        public void setTrainsSync(String trainsSync) {
            this.trainsSync = trainsSync;
        }

        public String getEventsSync() {
            return eventsSync;
        }

        public void setEventsSync(String eventsSync) {
            this.eventsSync = eventsSync;
        }

        public String getPersonnelSnapshotSync() {
            return personnelSnapshotSync;
        }

        public void setPersonnelSnapshotSync(String personnelSnapshotSync) {
            this.personnelSnapshotSync = personnelSnapshotSync;
        }
    }
}
