package ru.mzd.geoanalytics.generator.common.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "generator.client")
public class GeneratorServiceClientProperties {

    @NotBlank
    private String exchange = "mzhd.generator.exchange";

    @Valid
    private RoutingKeys routingKeys = new RoutingKeys();

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
}
