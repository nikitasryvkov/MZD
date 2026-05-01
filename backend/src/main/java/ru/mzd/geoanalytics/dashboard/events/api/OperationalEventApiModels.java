package ru.mzd.geoanalytics.dashboard.events.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.common.validation.ValueOfEnum;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public final class OperationalEventApiModels {

    private OperationalEventApiModels() {
    }

    public record UpdateOperationalEventStatusRequest(
        @NotBlank @ValueOfEnum(enumClass = EventStatus.class) String newStatus,
        @Size(max = 1000) String comment
    ) {
    }

    public record UpdateOperationalEventStatusResponse(
        UUID eventId,
        String status,
        Instant updatedAt,
        String affectedSection,
        String summary,
        List<String> allowedTransitions
    ) {
    }

    public record OperationalEventDetailsResponse(
        UUID id,
        String type,
        String title,
        String description,
        String status,
        String severity,
        double latitude,
        double longitude,
        UUID affectedObjectId,
        String affectedSection,
        Instant startedAt,
        Instant endedAt,
        Instant updatedAt,
        String lastChangedBy,
        List<String> allowedTransitions,
        List<EventStatusHistoryItemResponse> statusHistory
    ) {
    }

    public record EventStatusHistoryItemResponse(
        UUID id,
        String fromStatus,
        String toStatus,
        String comment,
        Instant changedAt,
        String changedBy
    ) {
    }
}
