package ru.mzd.geoanalytics.dashboard.simulation.infrastructure;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.simulation.application.SimulationBootstrapService;
import ru.mzd.geoanalytics.dashboard.simulation.application.SimulationTickService;
import ru.mzd.geoanalytics.dashboard.simulation.application.model.SimulationRuntimeSettings;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationAuditPort;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationSettingsPort;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

@Component
public class SimulationOrchestrator {

    private final SimulationSettingsPort simulationSettingsPort;
    private final SimulationBootstrapService simulationBootstrapService;
    private final SimulationTickService simulationTickService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final SimulationAuditPort simulationAuditPort;

    private ScheduledFuture<?> scheduledFuture;
    private SimulationModels.SimulationProfile activeProfile;

    public SimulationOrchestrator(
        SimulationSettingsPort simulationSettingsPort,
        SimulationBootstrapService simulationBootstrapService,
        SimulationTickService simulationTickService,
        ThreadPoolTaskScheduler taskScheduler,
        SimulationAuditPort simulationAuditPort
    ) {
        this.simulationSettingsPort = simulationSettingsPort;
        this.simulationBootstrapService = simulationBootstrapService;
        this.simulationTickService = simulationTickService;
        this.taskScheduler = taskScheduler;
        this.simulationAuditPort = simulationAuditPort;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        SimulationRuntimeSettings settings = simulationSettingsPort.getSettings();
        if (!settings.enabled()) {
            return;
        }

        activeProfile = simulationBootstrapService.bootstrap();
        if (activeProfile == null) {
            return;
        }

        if (!settings.schedulerEnabled()) {
            simulationAuditPort.recordLifecycle(
                "SIMULATION_SEED_ONLY",
                "SUCCESS",
                Map.of("profileName", activeProfile.profileName())
            );
            return;
        }

        scheduledFuture = taskScheduler.scheduleAtFixedRate(
            () -> simulationTickService.tick(activeProfile),
            Duration.ofSeconds(activeProfile.tickIntervalSeconds())
        );

        simulationAuditPort.recordLifecycle(
            "SIMULATION_START",
            "SUCCESS",
            Map.of(
                "profileName", activeProfile.profileName(),
                "tickIntervalSeconds", activeProfile.tickIntervalSeconds()
            )
        );
    }

    @PreDestroy
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        if (activeProfile != null) {
            simulationAuditPort.recordLifecycle(
                "SIMULATION_STOP",
                "SUCCESS",
                Map.of("profileName", activeProfile.profileName())
            );
        }
    }
}
