package ru.mzd.geoanalytics.dashboard.common.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DataRetentionScheduler(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${app.retention.cleanup-cron:0 15 3 * * *}")
    public void pruneRetainedData() {
        prune("rolling_stock_position", "SELECT dashboard.prune_rolling_stock_position(CAST(:retainFor AS interval))", "24 hours");
        prune("security_audit_log", "SELECT dashboard.prune_security_audit_log(CAST(:retainFor AS interval))", "90 days");
        prune("metric", "SELECT dashboard.prune_metric(CAST(:retainFor AS interval))", "30 days");
    }

    private void prune(String tableName, String sql, String retainFor) {
        try {
            Long deletedRows = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("retainFor", retainFor),
                Long.class
            );
            if (deletedRows != null && deletedRows > 0) {
                log.info("Pruned {} rows from {}.", deletedRows, tableName);
            }
        } catch (Exception exception) {
            log.warn("Failed to prune {}.", tableName, exception);
        }
    }
}
