package ru.mzd.geoanalytics.dashboard.simulation.application.port;

import ru.mzd.geoanalytics.dashboard.simulation.application.model.SimulationRuntimeSettings;

public interface SimulationSettingsPort {

    SimulationRuntimeSettings getSettings();
}
