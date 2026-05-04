package ru.mzd.geoanalytics.dashboard.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private final Security security = new Security();
    private final Cors cors = new Cors();
    private final Simulation simulation = new Simulation();
    private final Streaming streaming = new Streaming();

    public Security getSecurity() {
        return security;
    }

    public Cors getCors() {
        return cors;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public Streaming getStreaming() {
        return streaming;
    }

    public static class Security {
        private boolean enabled = true;
        private String issuerUri;
        private String jwkSetUri;
        private List<String> expectedAudiences = new ArrayList<>();
        @NotEmpty
        private List<String> personnelAuthorities = new ArrayList<>(List.of("ROLE_ADMIN", "ROLE_PERSONNEL_VIEWER"));
        @NotEmpty
        private List<String> adminAuthorities = new ArrayList<>(List.of("ROLE_ADMIN"));
        @NotEmpty
        private List<String> observabilityAuthorities = new ArrayList<>(List.of("ROLE_ADMIN", "ROLE_OBSERVABILITY"));
        @NotEmpty
        private List<String> dashboardAuthorities = new ArrayList<>(List.of(
            "ROLE_ADMIN",
            "ROLE_MONITORING_USER",
            "ROLE_PERSONNEL_VIEWER"
        ));
        @NotEmpty
        private List<String> eventAuthorities = new ArrayList<>(List.of(
            "ROLE_ADMIN",
            "ROLE_MONITORING_USER"
        ));
        @NotEmpty
        private List<String> generatorAuthorities = new ArrayList<>(List.of(
            "ROLE_ADMIN",
            "ROLE_GENERATOR"
        ));
        private final LocalDevUser localDevUser = new LocalDevUser();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public List<String> getExpectedAudiences() {
            return expectedAudiences;
        }

        public void setExpectedAudiences(List<String> expectedAudiences) {
            this.expectedAudiences = expectedAudiences == null
                ? new ArrayList<>()
                : expectedAudiences.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        public List<String> getPersonnelAuthorities() {
            return personnelAuthorities;
        }

        public void setPersonnelAuthorities(List<String> personnelAuthorities) {
            this.personnelAuthorities = personnelAuthorities;
        }

        public List<String> getAdminAuthorities() {
            return adminAuthorities;
        }

        public void setAdminAuthorities(List<String> adminAuthorities) {
            this.adminAuthorities = adminAuthorities;
        }

        public List<String> getObservabilityAuthorities() {
            return observabilityAuthorities;
        }

        public void setObservabilityAuthorities(List<String> observabilityAuthorities) {
            this.observabilityAuthorities = observabilityAuthorities;
        }

        public List<String> getDashboardAuthorities() {
            return dashboardAuthorities;
        }

        public void setDashboardAuthorities(List<String> dashboardAuthorities) {
            this.dashboardAuthorities = dashboardAuthorities;
        }

        public List<String> getEventAuthorities() {
            return eventAuthorities;
        }

        public void setEventAuthorities(List<String> eventAuthorities) {
            this.eventAuthorities = eventAuthorities;
        }

        public List<String> getGeneratorAuthorities() {
            return generatorAuthorities;
        }

        public void setGeneratorAuthorities(List<String> generatorAuthorities) {
            this.generatorAuthorities = generatorAuthorities;
        }

        public LocalDevUser getLocalDevUser() {
            return localDevUser;
        }
    }

    public static class LocalDevUser {
        @NotBlank
        private String principalId = "local-operator";
        @NotEmpty
        private List<String> roles = new ArrayList<>(List.of(
            "ADMIN",
            "MONITORING_USER",
            "PERSONNEL_VIEWER",
            "OBSERVABILITY"
        ));

        public String getPrincipalId() {
            return principalId;
        }

        public void setPrincipalId(String principalId) {
            this.principalId = principalId;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    public static class Cors {
        @NotEmpty
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000", "http://localhost:5173"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Simulation {
        private boolean enabled = true;
        private boolean seedOnStartup = true;
        private boolean schedulerEnabled = true;
        private boolean operationalSeedEnabled = true;
        @NotBlank
        private String defaultProfileName = "mzhd-main";
        @Min(1)
        private int initialEventCount = 4;
        @Min(1)
        private int routeCount = 13;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSeedOnStartup() {
            return seedOnStartup;
        }

        public void setSeedOnStartup(boolean seedOnStartup) {
            this.seedOnStartup = seedOnStartup;
        }

        public boolean isSchedulerEnabled() {
            return schedulerEnabled;
        }

        public void setSchedulerEnabled(boolean schedulerEnabled) {
            this.schedulerEnabled = schedulerEnabled;
        }

        public boolean isOperationalSeedEnabled() {
            return operationalSeedEnabled;
        }

        public void setOperationalSeedEnabled(boolean operationalSeedEnabled) {
            this.operationalSeedEnabled = operationalSeedEnabled;
        }

        public String getDefaultProfileName() {
            return defaultProfileName;
        }

        public void setDefaultProfileName(String defaultProfileName) {
            this.defaultProfileName = defaultProfileName;
        }

        public int getInitialEventCount() {
            return initialEventCount;
        }

        public void setInitialEventCount(int initialEventCount) {
            this.initialEventCount = initialEventCount;
        }

        public int getRouteCount() {
            return routeCount;
        }

        public void setRouteCount(int routeCount) {
            this.routeCount = routeCount;
        }
    }

    public static class Streaming {
        private final BrokerRelay brokerRelay = new BrokerRelay();
        private final Outbox outbox = new Outbox();

        public BrokerRelay getBrokerRelay() {
            return brokerRelay;
        }

        public Outbox getOutbox() {
            return outbox;
        }
    }

    public static class BrokerRelay {
        private boolean enabled = false;
        @NotBlank
        private String host = "localhost";
        @Min(1)
        private int port = 61613;
        @NotBlank
        private String clientLogin = "dashboard";
        @NotBlank
        private String clientPasscode = "dashboard";
        @NotBlank
        private String systemLogin = "dashboard";
        @NotBlank
        private String systemPasscode = "dashboard";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getClientLogin() {
            return clientLogin;
        }

        public void setClientLogin(String clientLogin) {
            this.clientLogin = clientLogin;
        }

        public String getClientPasscode() {
            return clientPasscode;
        }

        public void setClientPasscode(String clientPasscode) {
            this.clientPasscode = clientPasscode;
        }

        public String getSystemLogin() {
            return systemLogin;
        }

        public void setSystemLogin(String systemLogin) {
            this.systemLogin = systemLogin;
        }

        public String getSystemPasscode() {
            return systemPasscode;
        }

        public void setSystemPasscode(String systemPasscode) {
            this.systemPasscode = systemPasscode;
        }
    }

    public static class Outbox {
        private boolean enabled = true;
        @Min(1)
        private int batchSize = 100;
        @Min(1)
        private long dispatchFixedDelayMs = 1_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getDispatchFixedDelayMs() {
            return dispatchFixedDelayMs;
        }

        public void setDispatchFixedDelayMs(long dispatchFixedDelayMs) {
            this.dispatchFixedDelayMs = dispatchFixedDelayMs;
        }
    }
}
