package ru.mzd.geoanalytics.dashboard.events.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.mzd.geoanalytics.dashboard.common.exception.ConflictException;

class OperationalEventAggregateTest {

    @Test
    void shouldCreateStatusChangeForAllowedTransition() {
        OperationalEventAggregate aggregate = new OperationalEventAggregate(
            UUID.randomUUID(),
            "Signal disruption",
            EventStatus.REGISTERED,
            Instant.parse("2026-04-21T10:00:00Z"),
            "Moscow Passenger",
            null
        );

        OperationalEventStatusChange change = aggregate.changeStatus(
            EventStatus.IN_PROGRESS,
            "operator-1",
            "Dispatcher took ownership"
        );

        assertThat(change.fromStatus()).isEqualTo(EventStatus.REGISTERED);
        assertThat(change.toStatus()).isEqualTo(EventStatus.IN_PROGRESS);
        assertThat(change.principalId()).isEqualTo("operator-1");
        assertThat(change.comment()).isEqualTo("Dispatcher took ownership");
    }

    @Test
    void shouldRejectIllegalTransition() {
        OperationalEventAggregate aggregate = new OperationalEventAggregate(
            UUID.randomUUID(),
            "Signal disruption",
            EventStatus.REGISTERED,
            Instant.parse("2026-04-21T10:00:00Z"),
            "Moscow Passenger",
            null
        );

        assertThatThrownBy(() -> aggregate.changeStatus(EventStatus.RESOLVED, "operator-1", null))
            .isInstanceOf(ConflictException.class)
            .satisfies(throwable -> {
                ConflictException conflictException = (ConflictException) throwable;
                assertThat(conflictException.getCurrentStatus()).isEqualTo("REGISTERED");
                assertThat(conflictException.getRequestedStatus()).isEqualTo("RESOLVED");
                assertThat(conflictException.getAllowedTransitions()).containsExactly("IN_PROGRESS", "CANCELED");
            });
    }
}
