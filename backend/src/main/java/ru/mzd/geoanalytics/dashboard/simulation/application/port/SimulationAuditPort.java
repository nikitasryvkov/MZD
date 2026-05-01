package ru.mzd.geoanalytics.dashboard.simulation.application.port;

import java.util.Map;

public interface SimulationAuditPort {

    void recordLifecycle(String eventType, String outcome, Map<String, Object> details);
}
