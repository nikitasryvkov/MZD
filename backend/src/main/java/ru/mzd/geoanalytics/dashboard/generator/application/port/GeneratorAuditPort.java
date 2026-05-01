package ru.mzd.geoanalytics.dashboard.generator.application.port;

public interface GeneratorAuditPort {

    void recordBatchAudit(
        String eventType,
        String principalId,
        String sourceSystem,
        int acceptedCount
    );
}
