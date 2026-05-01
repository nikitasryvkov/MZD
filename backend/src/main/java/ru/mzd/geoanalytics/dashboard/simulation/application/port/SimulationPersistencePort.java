package ru.mzd.geoanalytics.dashboard.simulation.application.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

public interface SimulationPersistencePort {

    SimulationModels.SimulationProfile ensureActiveProfile(String defaultProfileName);

    boolean hasReferenceData();

    void seedReferenceData(
        SimulationModels.SimulationProfile profile,
        int routeCount,
        int initialEventCount,
        boolean includeOperationalSeed
    );

    List<SimulationModels.SimulatedTrainState> loadTrainStates();

    Map<UUID, List<SimulationModels.RouteStop>> loadRouteStops();

    void saveTrainUpdate(SimulationModels.SimulatedTrainUpdate trainUpdate);

    List<SimulationModels.SimulatedEventState> loadActiveEvents();

    UUID createSimulatedEvent();
}
