package ru.mzd.geoanalytics.dashboard.simulation.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mzd.geoanalytics.dashboard.events.application.UpdateOperationalEventStatusService;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateCommand;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventStreamingPort;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;
import ru.mzd.geoanalytics.dashboard.generator.application.port.TrainStreamingPort;
import ru.mzd.geoanalytics.dashboard.security.AuthenticatedUser;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationPersistencePort;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

@Service
public class SimulationTickService {

    private static final Logger log = LoggerFactory.getLogger(SimulationTickService.class);
    private static final AuthenticatedUser SIMULATION_USER =
        new AuthenticatedUser("РЎРёРјСѓР»СЏС‚РѕСЂ", java.util.Set.of("ROLE_ADMIN"));

    private final SimulationPersistencePort simulationPersistencePort;
    private final UpdateOperationalEventStatusService updateOperationalEventStatusService;
    private final OperationalEventPersistencePort operationalEventPersistencePort;
    private final OperationalEventStreamingPort operationalEventStreamingPort;
    private final TrainStreamingPort trainStreamingPort;
    private final Clock clock;
    private final Random random = new Random();

    public SimulationTickService(
        SimulationPersistencePort simulationPersistencePort,
        UpdateOperationalEventStatusService updateOperationalEventStatusService,
        OperationalEventPersistencePort operationalEventPersistencePort,
        OperationalEventStreamingPort operationalEventStreamingPort,
        TrainStreamingPort trainStreamingPort,
        Clock clock
    ) {
        this.simulationPersistencePort = simulationPersistencePort;
        this.updateOperationalEventStatusService = updateOperationalEventStatusService;
        this.operationalEventPersistencePort = operationalEventPersistencePort;
        this.operationalEventStreamingPort = operationalEventStreamingPort;
        this.trainStreamingPort = trainStreamingPort;
        this.clock = clock;
    }

    public void tick(SimulationModels.SimulationProfile profile) {
        try {
            Map<UUID, List<SimulationModels.RouteStop>> routeStopsByRoute = simulationPersistencePort.loadRouteStops();
            for (SimulationModels.SimulatedTrainState trainState : simulationPersistencePort.loadTrainStates()) {
                updateTrain(profile, trainState, routeStopsByRoute.getOrDefault(trainState.routeId(), List.of()));
            }

            List<SimulationModels.SimulatedEventState> activeEvents = simulationPersistencePort.loadActiveEvents();
            for (SimulationModels.SimulatedEventState eventState : activeEvents) {
                maybeAdvanceEvent(eventState);
            }

            if (activeEvents.size() < 2 && random.nextDouble() < profile.eventGenerationIntensity()) {
                UUID newEventId = simulationPersistencePort.createSimulatedEvent();
                operationalEventPersistencePort.findEventProjection(newEventId)
                    .ifPresent(operationalEventStreamingPort::publishEventUpsert);
            }
        } catch (Exception exception) {
            log.error("РћС€РёР±РєР° РІРѕ РІСЂРµРјСЏ С‚РёРєР° СЃРёРјСѓР»СЏС†РёРё.", exception);
        }
    }

    private void updateTrain(
        SimulationModels.SimulationProfile profile,
        SimulationModels.SimulatedTrainState trainState,
        List<SimulationModels.RouteStop> routeStops
    ) {
        if (routeStops.size() < 2) {
            return;
        }

        List<SimulationModels.RouteStop> orderedStops = routeStops.stream()
            .sorted(Comparator.comparingInt(SimulationModels.RouteStop::sequenceNo))
            .toList();

        SimulationModels.RouteStop currentStop = findStop(orderedStops, trainState.currentStationId(), 0);
        SimulationModels.RouteStop nextStop = findStop(orderedStops, trainState.nextStationId(), 1);

        double progress = trainState.progressPercent() + 8 + random.nextDouble() * 18;
        while (progress >= 100) {
            progress -= 100;
            currentStop = nextStop;
            nextStop = nextStop(orderedStops, currentStop.stationId());
        }

        double latitude = interpolate(currentStop.latitude(), nextStop.latitude(), progress / 100.0);
        double longitude = interpolate(currentStop.longitude(), nextStop.longitude(), progress / 100.0);
        double speedKmh = 35 + random.nextDouble() * 55;
        String status = progress < 5 ? "AT_STATION" : "ON_ROUTE";

        SimulationModels.SimulatedTrainUpdate update = new SimulationModels.SimulatedTrainUpdate(
            trainState.id(),
            currentStop.stationId(),
            nextStop.stationId(),
            progress,
            speedKmh,
            status,
            latitude,
            longitude
        );
        simulationPersistencePort.saveTrainUpdate(update);

        trainStreamingPort.publishTrainUpsert(new TrainProjection(
            trainState.id(),
            trainState.trainNumber(),
            latitude,
            longitude,
            status,
            currentStop.stationId(),
            nextStop.stationId(),
            progress,
            speedKmh,
            Instant.now(clock)
        ));
    }

    private void maybeAdvanceEvent(SimulationModels.SimulatedEventState eventState) {
        EventStatus currentStatus = EventStatus.valueOf(eventState.status());
        if (currentStatus == EventStatus.REGISTERED && random.nextDouble() < 0.18) {
            updateOperationalEventStatusService.updateStatus(
                eventState.id(),
                new OperationalEventStatusUpdateCommand(
                    EventStatus.IN_PROGRESS,
                    "Р Р°Р·РІРёС‚РёРµ СЃРёРјСѓР»СЏС†РёРё",
                    SIMULATION_USER.principalId()
                )
            );
        } else if (currentStatus == EventStatus.IN_PROGRESS && random.nextDouble() < 0.12) {
            EventStatus terminalStatus = random.nextBoolean() ? EventStatus.RESOLVED : EventStatus.CANCELED;
            updateOperationalEventStatusService.updateStatus(
                eventState.id(),
                new OperationalEventStatusUpdateCommand(
                    terminalStatus,
                    "Р Р°Р·РІРёС‚РёРµ СЃРёРјСѓР»СЏС†РёРё",
                    SIMULATION_USER.principalId()
                )
            );
        }
    }

    private SimulationModels.RouteStop findStop(List<SimulationModels.RouteStop> orderedStops, UUID stationId, int fallbackIndex) {
        return orderedStops.stream()
            .filter(stop -> stop.stationId().equals(stationId))
            .findFirst()
            .orElse(orderedStops.get(Math.min(fallbackIndex, orderedStops.size() - 1)));
    }

    private SimulationModels.RouteStop nextStop(List<SimulationModels.RouteStop> orderedStops, UUID stationId) {
        for (int index = 0; index < orderedStops.size(); index++) {
            if (orderedStops.get(index).stationId().equals(stationId)) {
                return orderedStops.get((index + 1) % orderedStops.size());
            }
        }
        return orderedStops.get(0);
    }

    private double interpolate(double start, double end, double factor) {
        return start + ((end - start) * factor);
    }
}
