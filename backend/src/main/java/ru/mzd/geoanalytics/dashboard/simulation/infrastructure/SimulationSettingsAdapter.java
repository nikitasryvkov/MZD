package ru.mzd.geoanalytics.dashboard.simulation.infrastructure;

import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;
import ru.mzd.geoanalytics.dashboard.simulation.application.model.SimulationRuntimeSettings;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationSettingsPort;

@Component
public class SimulationSettingsAdapter implements SimulationSettingsPort {

    private final ApplicationProperties applicationProperties;

    public SimulationSettingsAdapter(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public SimulationRuntimeSettings getSettings() {
        ApplicationProperties.Simulation simulation = applicationProperties.getSimulation();
        return new SimulationRuntimeSettings(
            simulation.isEnabled(),
            simulation.isSeedOnStartup(),
            simulation.isSchedulerEnabled(),
            simulation.isOperationalSeedEnabled(),
            simulation.getDefaultProfileName(),
            simulation.getInitialEventCount(),
            simulation.getRouteCount()
        );
    }
}
