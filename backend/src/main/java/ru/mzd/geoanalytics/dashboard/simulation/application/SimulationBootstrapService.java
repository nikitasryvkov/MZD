package ru.mzd.geoanalytics.dashboard.simulation.application;

import org.springframework.stereotype.Service;
import ru.mzd.geoanalytics.dashboard.analytics.application.port.KpiProjectionPort;
import ru.mzd.geoanalytics.dashboard.simulation.application.model.SimulationRuntimeSettings;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationPersistencePort;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationSettingsPort;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

@Service
public class SimulationBootstrapService {

    private final SimulationSettingsPort simulationSettingsPort;
    private final SimulationPersistencePort simulationPersistencePort;
    private final KpiProjectionPort kpiProjectionPort;

    public SimulationBootstrapService(
        SimulationSettingsPort simulationSettingsPort,
        SimulationPersistencePort simulationPersistencePort,
        KpiProjectionPort kpiProjectionPort
    ) {
        this.simulationSettingsPort = simulationSettingsPort;
        this.simulationPersistencePort = simulationPersistencePort;
        this.kpiProjectionPort = kpiProjectionPort;
    }

    public SimulationModels.SimulationProfile bootstrap() {
        SimulationRuntimeSettings settings = simulationSettingsPort.getSettings();
        if (!settings.enabled()) {
            return null;
        }

        SimulationModels.SimulationProfile profile = simulationPersistencePort.ensureActiveProfile(
            settings.defaultProfileName()
        );

        if (settings.seedOnStartup() && !simulationPersistencePort.hasReferenceData()) {
            simulationPersistencePort.seedReferenceData(
                profile,
                settings.routeCount(),
                settings.initialEventCount(),
                settings.operationalSeedEnabled()
            );
        }

        kpiProjectionPort.recalculateGlobalSnapshot();
        return profile;
    }
}
