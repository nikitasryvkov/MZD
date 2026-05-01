package ru.mzd.geoanalytics.dashboard.events.domain;

import java.time.Instant;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.common.exception.ConflictException;

public class OperationalEventAggregate {

    private final UUID id;
    private final String title;
    private final EventStatus status;
    private final Instant updatedAt;
    private final String affectedSection;
    private final String lastChangedBy;

    public OperationalEventAggregate(
        UUID id,
        String title,
        EventStatus status,
        Instant updatedAt,
        String affectedSection,
        String lastChangedBy
    ) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.updatedAt = updatedAt;
        this.affectedSection = affectedSection;
        this.lastChangedBy = lastChangedBy;
    }

    public OperationalEventStatusChange changeStatus(EventStatus newStatus, String principalId, String comment) {
        if (!status.canTransitionTo(newStatus)) {
            throw new ConflictException(
                "Недопустимый переход статуса оперативного события.",
                status.name(),
                newStatus.name(),
                status.allowedTransitions().stream().map(Enum::name).toList()
            );
        }

        String normalizedComment = comment == null || comment.isBlank() ? null : comment.trim();
        String summary = "Статус события \"" + title + "\" изменён с "
            + localizeStatus(status)
            + " на "
            + localizeStatus(newStatus)
            + ".";

        return new OperationalEventStatusChange(
            id,
            status,
            newStatus,
            principalId,
            normalizedComment,
            summary
        );
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getAffectedSection() {
        return affectedSection;
    }

    public String getLastChangedBy() {
        return lastChangedBy;
    }

    private String localizeStatus(EventStatus eventStatus) {
        return switch (eventStatus) {
            case REGISTERED -> "зарегистрировано";
            case IN_PROGRESS -> "в работе";
            case RESOLVED -> "устранено";
            case CANCELED -> "отменено";
        };
    }
}
