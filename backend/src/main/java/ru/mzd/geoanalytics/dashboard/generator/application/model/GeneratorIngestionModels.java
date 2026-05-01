package ru.mzd.geoanalytics.dashboard.generator.application.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public final class GeneratorIngestionModels {

    private GeneratorIngestionModels() {
    }

    public record BatchIngestionView(
        int acceptedCount,
        Instant appliedAt,
        String sourceSystem
    ) {
    }

    public record SyncTrainsCommand(
        Instant generatedAt,
        String sourceSystem,
        List<TrainUpsertItem> trains
    ) {
    }

    public record TrainUpsertItem(
        UUID id,
        String trainNumber,
        UUID routeId,
        UUID currentStationId,
        UUID nextStationId,
        Double progressPercent,
        Double latitude,
        Double longitude,
        Double speedKmh,
        String status
    ) {
    }

    public record SyncEventsCommand(
        Instant generatedAt,
        String sourceSystem,
        List<EventUpsertItem> events
    ) {
    }

    public record EventUpsertItem(
        UUID id,
        String eventType,
        String title,
        String description,
        String severity,
        EventStatus status,
        UUID affectedObjectId,
        String departmentCode,
        Double latitude,
        Double longitude,
        Instant startedAt,
        Instant endedAt,
        String comment
    ) {
    }

    public record SyncPersonnelSnapshotCommand(
        LocalDate periodMonth,
        String sourceSystem,
        List<PersonnelSnapshotItem> items
    ) {
    }

    public record PersonnelSnapshotItem(
        String dimensionKey,
        int headcount,
        Double changePercent
    ) {
    }
}
