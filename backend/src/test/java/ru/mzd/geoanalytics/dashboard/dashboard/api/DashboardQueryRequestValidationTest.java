package ru.mzd.geoanalytics.dashboard.dashboard.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardQueryRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldValidateBoundingBoxAndTimeRange() {
        DashboardApiModels.DashboardQueryRequest request = new DashboardApiModels.DashboardQueryRequest(
            new DashboardApiModels.BoundingBoxRequest(56.0, 38.0, 55.0, 37.0),
            new DashboardApiModels.LayerFilterRequest(true, true, true, true),
            new DashboardApiModels.TimeRangeRequest(
                Instant.parse("2026-04-21T12:00:00Z"),
                Instant.parse("2026-04-21T11:00:00Z")
            ),
            List.of("REGISTERED"),
            List.of("DCS-01"),
            true,
            true
        );

        var violations = validator.validate(request);

        assertThat(violations)
            .extracting(violation -> violation.getMessage())
            .contains(
                "bbox.maxLat must be greater than bbox.minLat",
                "bbox.maxLon must be greater than bbox.minLon",
                "timeRange.to must be later than timeRange.from"
            );
    }

    @Test
    void shouldRejectUnsupportedEventStatus() {
        DashboardApiModels.DashboardQueryRequest request = new DashboardApiModels.DashboardQueryRequest(
            null,
            new DashboardApiModels.LayerFilterRequest(true, true, true, true),
            null,
            List.of("UNKNOWN_STATUS"),
            List.of("DCS-01"),
            true,
            false
        );

        var violations = validator.validate(request);

        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("eventStatuses[0].<list element>");
    }
}
