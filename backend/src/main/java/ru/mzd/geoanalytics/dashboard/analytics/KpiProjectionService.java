package ru.mzd.geoanalytics.dashboard.analytics;

import java.time.Clock;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.analytics.application.port.KpiProjectionPort;

@Service
public class KpiProjectionService implements KpiProjectionPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    public KpiProjectionService(NamedParameterJdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recalculateGlobalSnapshot() {
        int activeEventsCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dashboard.operational_event
            WHERE is_active
            """, new MapSqlParameterSource(), Integer.class);

        int trainsOnLineCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dashboard.rolling_stock_unit
            WHERE current_position IS NOT NULL
            """, new MapSqlParameterSource(), Integer.class);

        int overloadedSectionsCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dashboard.route_segment
            WHERE UPPER(status) = 'OVERLOADED'
            """, new MapSqlParameterSource(), Integer.class);

        Instant calculatedAt = Instant.now(clock);
        insertMetric("ACTIVE_EVENTS", "Активные события", activeEventsCount, calculatedAt);
        insertMetric("TRAINS_ON_LINE", "Поезда на линии", trainsOnLineCount, calculatedAt);
        insertMetric("OVERLOADED_SECTIONS", "Перегруженные участки", overloadedSectionsCount, calculatedAt);
    }

    private void insertMetric(String code, String name, Number value, Instant calculatedAt) {
        jdbcTemplate.update("""
            INSERT INTO dashboard.metric (
                code,
                name,
                value,
                calculated_at
            )
            VALUES (
                :code,
                :name,
                :value,
                :calculatedAt
            )
            """, new MapSqlParameterSource()
            .addValue("code", code)
            .addValue("name", name)
            .addValue("value", value)
            .addValue("calculatedAt", toTimestamp(calculatedAt)));
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
