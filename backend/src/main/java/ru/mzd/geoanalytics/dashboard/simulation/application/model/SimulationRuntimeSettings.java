package ru.mzd.geoanalytics.dashboard.simulation.application.model;

public record SimulationRuntimeSettings(
    boolean enabled,
    boolean seedOnStartup,
    boolean schedulerEnabled,
    boolean operationalSeedEnabled,
    String defaultProfileName,
    int initialEventCount,
    int routeCount
) {
}
