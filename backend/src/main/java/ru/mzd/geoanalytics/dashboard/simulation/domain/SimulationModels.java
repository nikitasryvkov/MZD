package ru.mzd.geoanalytics.dashboard.simulation.domain;

import java.util.List;
import java.util.UUID;

public final class SimulationModels {

    private SimulationModels() {
    }

    public record SimulationProfile(
        UUID id,
        String profileName,
        int tickIntervalSeconds,
        int trainCount,
        double eventGenerationIntensity
    ) {
    }

    public record RouteStop(
        UUID stationId,
        int sequenceNo,
        double latitude,
        double longitude
    ) {
    }

    public record SimulatedTrainState(
        UUID id,
        String trainNumber,
        UUID routeId,
        UUID currentStationId,
        UUID nextStationId,
        double progressPercent,
        double speedKmh,
        String status,
        double currentLatitude,
        double currentLongitude,
        double nextLatitude,
        double nextLongitude
    ) {
    }

    public record SimulatedTrainUpdate(
        UUID id,
        UUID currentStationId,
        UUID nextStationId,
        double progressPercent,
        double speedKmh,
        String status,
        double latitude,
        double longitude
    ) {
    }

    public record SimulatedEventState(
        UUID id,
        String status
    ) {
    }

    public record StationSeed(
        UUID id,
        String code,
        String name,
        String departmentCode,
        String stationType,
        int orderIndex,
        double latitude,
        double longitude
    ) {
    }

    public record CoordinatePoint(
        double latitude,
        double longitude
    ) {
    }

    public record RouteDefinition(
        UUID id,
        String code,
        String name,
        List<StationSeed> stations
    ) {
    }
}
