package ru.mzd.geoanalytics.dashboard.generator.application.model;

import java.time.Instant;
import java.util.UUID;

public record TrainProjection(
    UUID id,
    String trainNumber,
    double latitude,
    double longitude,
    String status,
    UUID currentStationId,
    UUID nextStationId,
    Double progressPercent,
    Double speedKmh,
    Instant lastUpdated
) {
}
