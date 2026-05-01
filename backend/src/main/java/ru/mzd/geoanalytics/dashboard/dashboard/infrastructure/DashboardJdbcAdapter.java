package ru.mzd.geoanalytics.dashboard.dashboard.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;
import ru.mzd.geoanalytics.dashboard.dashboard.application.port.DashboardReadPort;
import ru.mzd.geoanalytics.dashboard.dashboard.domain.DashboardQuery;

@Repository
public class DashboardJdbcAdapter implements DashboardReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DashboardJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardViewModels.MapDataView loadMapData(DashboardQuery query) {
        return new DashboardViewModels.MapDataView(
            query.layerFilter().showStations() ? loadStations(query) : List.of(),
            query.layerFilter().showSegments() ? loadRouteSegments(query) : List.of(),
            query.layerFilter().showTrains() ? loadTrains(query) : List.of(),
            query.layerFilter().showEvents() ? loadOperationalEvents(query) : List.of(),
            null
        );
    }

    @Override
    public List<DashboardViewModels.EventPreviewView> loadEventsPreview(DashboardQuery query) {
        MapSqlParameterSource parameters = baseParameters(query);
        StringBuilder sql = new StringBuilder("""
            SELECT
                oe.id,
                oe.title,
                oe.severity,
                oe.status::text AS status,
                io.display_name AS affected_section,
                oe.started_at,
                oe.updated_at
            FROM dashboard.operational_event oe
            LEFT JOIN dashboard.infrastructure_object io
                ON io.id = oe.affected_infrastructure_object_id
            WHERE 1 = 1
            """);
        appendEventFilters(sql, query, "oe");
        sql.append(
            " ORDER BY"
                + " CASE UPPER(oe.severity)"
                + " WHEN 'CRITICAL' THEN 1"
                + " WHEN 'HIGH' THEN 2"
                + " WHEN 'MEDIUM' THEN 3"
                + " WHEN 'LOW' THEN 4"
                + " ELSE 5"
                + " END,"
                + " oe.started_at DESC NULLS LAST,"
                + " oe.updated_at DESC"
        );

        return jdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) ->
            new DashboardViewModels.EventPreviewView(
                getUuid(rs, "id"),
                rs.getString("title"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("affected_section"),
                rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
            )
        );
    }

    @Override
    public DashboardViewModels.KpiSummaryView loadKpiSummary(DashboardQuery query) {
        MapSqlParameterSource eventParameters = baseParameters(query);
        StringBuilder activeEventsSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM dashboard.operational_event oe
            WHERE oe.is_active
            """);
        appendEventFilters(activeEventsSql, query, "oe");
        int activeEventsCount = jdbcTemplate.queryForObject(activeEventsSql.toString(), eventParameters, Integer.class);

        MapSqlParameterSource trainParameters = baseParameters(query);
        StringBuilder trainsSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM dashboard.rolling_stock_unit rsu
            LEFT JOIN dashboard.infrastructure_object current_io
                ON current_io.id = rsu.current_station_id
            LEFT JOIN dashboard.infrastructure_object next_io
                ON next_io.id = rsu.next_station_id
            WHERE rsu.current_position IS NOT NULL
            """);
        appendTrainFilters(trainsSql, query);
        int trainsOnLineCount = jdbcTemplate.queryForObject(trainsSql.toString(), trainParameters, Integer.class);

        MapSqlParameterSource segmentParameters = baseParameters(query);
        StringBuilder overloadedSql = new StringBuilder("""
            SELECT COUNT(*)
            FROM dashboard.route_segment rs
            JOIN dashboard.infrastructure_object io ON io.id = rs.id
            WHERE UPPER(rs.status) = 'OVERLOADED'
            """);
        appendDepartmentAndBoundingFilters(overloadedSql, query, "io", "rs.geometry");
        int overloadedSectionsCount = jdbcTemplate.queryForObject(overloadedSql.toString(), segmentParameters, Integer.class);

        return new DashboardViewModels.KpiSummaryView(
            activeEventsCount,
            trainsOnLineCount,
            overloadedSectionsCount,
            Instant.now()
        );
    }

    @Override
    public DashboardViewModels.PersonnelSummaryView loadPersonnelSummary(DashboardQuery query) {
        UUID aggregateId = jdbcTemplate.query("""
            SELECT sa.id
            FROM dashboard.staff_aggregate sa
            WHERE sa.scope_infrastructure_object_id IS NULL
              AND sa.dimension_type = 'DEPARTMENT_CODE'
            ORDER BY sa.period_month DESC, sa.calculated_at DESC
            LIMIT 1
            """, resultSet -> resultSet.next() ? getUuid(resultSet, "id") : null);

        if (aggregateId == null) {
            return new DashboardViewModels.PersonnelSummaryView(0, List.of());
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("aggregateId", aggregateId);
        StringBuilder sql = new StringBuilder("""
            SELECT dimension_key, headcount, change_percent
            FROM dashboard.staff_aggregate_item
            WHERE staff_aggregate_id = :aggregateId
            """);
        if (!query.departmentCodes().isEmpty()) {
            parameters.addValue("departmentCodes", query.departmentCodes());
            sql.append(" AND dimension_key IN (:departmentCodes)");
        }
        sql.append(" ORDER BY headcount DESC, dimension_key");

        List<DashboardViewModels.PersonnelSummaryItemView> items = jdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) ->
            new DashboardViewModels.PersonnelSummaryItemView(
                rs.getString("dimension_key"),
                rs.getInt("headcount"),
                rs.getBigDecimal("change_percent") != null ? rs.getBigDecimal("change_percent").doubleValue() : null
            )
        );

        int totalHeadcount = items.stream().mapToInt(DashboardViewModels.PersonnelSummaryItemView::headcount).sum();
        return new DashboardViewModels.PersonnelSummaryView(totalHeadcount, items);
    }

    private List<DashboardViewModels.StationView> loadStations(DashboardQuery query) {
        MapSqlParameterSource parameters = baseParameters(query);
        StringBuilder sql = new StringBuilder("""
            SELECT
                s.id,
                s.code,
                io.display_name AS name,
                ST_Y(s.location) AS latitude,
                ST_X(s.location) AS longitude,
                s.station_type
            FROM dashboard.station s
            JOIN dashboard.infrastructure_object io ON io.id = s.id
            WHERE 1 = 1
            """);
        appendDepartmentAndBoundingFilters(sql, query, "io", "s.location");
        sql.append(" ORDER BY COALESCE(s.order_index, 2147483647), io.display_name");

        return jdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) ->
            new DashboardViewModels.StationView(
                getUuid(rs, "id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getString("station_type")
            )
        );
    }

    private List<DashboardViewModels.RouteSegmentView> loadRouteSegments(DashboardQuery query) {
        MapSqlParameterSource parameters = baseParameters(query);
        StringBuilder sql = new StringBuilder("""
            SELECT
                rs.id,
                rs.from_station_id,
                rs.to_station_id,
                ST_AsGeoJSON(rs.geometry) AS geometry_json,
                rs.length_km,
                rs.status
            FROM dashboard.route_segment rs
            JOIN dashboard.infrastructure_object io ON io.id = rs.id
            WHERE 1 = 1
            """);
        appendDepartmentAndBoundingFilters(sql, query, "io", "rs.geometry");
        sql.append(" ORDER BY rs.length_km DESC, rs.id");

        return jdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) ->
            new DashboardViewModels.RouteSegmentView(
                getUuid(rs, "id"),
                getUuid(rs, "from_station_id"),
                getUuid(rs, "to_station_id"),
                parseJson(rs.getString("geometry_json")),
                rs.getBigDecimal("length_km") != null ? rs.getBigDecimal("length_km").doubleValue() : null,
                rs.getString("status")
            )
        );
    }

    private List<DashboardViewModels.TrainView> loadTrains(DashboardQuery query) {
        MapSqlParameterSource parameters = baseParameters(query);
        StringBuilder sql = new StringBuilder("""
            SELECT
                rsu.id,
                rsu.train_number,
                ST_Y(rsu.current_position) AS latitude,
                ST_X(rsu.current_position) AS longitude,
                rsu.status,
                rsu.current_station_id,
                rsu.next_station_id,
                rsu.progress_percent,
                rsu.speed_kmh,
                rsu.last_updated
            FROM dashboard.rolling_stock_unit rsu
            LEFT JOIN dashboard.infrastructure_object current_io ON current_io.id = rsu.current_station_id
            LEFT JOIN dashboard.infrastructure_object next_io ON next_io.id = rsu.next_station_id
            WHERE rsu.current_position IS NOT NULL
            """);
        appendTrainFilters(sql, query);
        sql.append(" ORDER BY rsu.train_number");

        return jdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) ->
            new DashboardViewModels.TrainView(
                getUuid(rs, "id"),
                rs.getString("train_number"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getString("status"),
                getUuid(rs, "current_station_id"),
                getUuid(rs, "next_station_id"),
                rs.getBigDecimal("progress_percent") != null ? rs.getBigDecimal("progress_percent").doubleValue() : null,
                rs.getBigDecimal("speed_kmh") != null ? rs.getBigDecimal("speed_kmh").doubleValue() : null,
                rs.getTimestamp("last_updated") != null ? rs.getTimestamp("last_updated").toInstant() : null
            )
        );
    }

    private List<DashboardViewModels.OperationalEventView> loadOperationalEvents(DashboardQuery query) {
        MapSqlParameterSource parameters = baseParameters(query);
        StringBuilder sql = new StringBuilder("""
            SELECT
                oe.id,
                oe.title,
                oe.status::text AS status,
                oe.severity,
                ST_Y(oe.location) AS latitude,
                ST_X(oe.location) AS longitude,
                oe.affected_infrastructure_object_id,
                io.display_name AS affected_section,
                oe.started_at,
                oe.updated_at
            FROM dashboard.operational_event oe
            LEFT JOIN dashboard.infrastructure_object io
                ON io.id = oe.affected_infrastructure_object_id
            WHERE 1 = 1
            """);
        appendEventFilters(sql, query, "oe");
        sql.append(" ORDER BY oe.updated_at DESC, oe.id");

        return jdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) ->
            new DashboardViewModels.OperationalEventView(
                getUuid(rs, "id"),
                rs.getString("title"),
                rs.getString("status"),
                rs.getString("severity"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                getUuid(rs, "affected_infrastructure_object_id"),
                rs.getString("affected_section"),
                rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
            )
        );
    }

    private MapSqlParameterSource baseParameters(DashboardQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.boundingBox() != null) {
            parameters.addValue("minLat", query.boundingBox().minLat());
            parameters.addValue("minLon", query.boundingBox().minLon());
            parameters.addValue("maxLat", query.boundingBox().maxLat());
            parameters.addValue("maxLon", query.boundingBox().maxLon());
        }
        if (query.timeRange() != null) {
            parameters.addValue("timeFrom", toTimestamp(query.timeRange().from()));
            parameters.addValue("timeTo", toTimestamp(query.timeRange().to()));
        }
        if (!query.departmentCodes().isEmpty()) {
            parameters.addValue("departmentCodes", query.departmentCodes());
        }
        if (!query.eventStatuses().isEmpty()) {
            parameters.addValue("eventStatuses", query.eventStatuses().stream().map(Enum::name).toList());
        }
        return parameters;
    }

    private void appendEventFilters(StringBuilder sql, DashboardQuery query, String eventAlias) {
        if (query.boundingBox() != null) {
            sql.append(" AND ").append(eventAlias).append(".location && ")
                .append("ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)");
        }
        if (!query.eventStatuses().isEmpty()) {
            sql.append(" AND ").append(eventAlias).append(".status::text IN (:eventStatuses)");
        }
        if (!query.departmentCodes().isEmpty()) {
            sql.append(" AND ").append(eventAlias).append(".department_code IN (:departmentCodes)");
        }
        if (query.timeRange() != null) {
            sql.append(" AND COALESCE(").append(eventAlias).append(".ended_at, 'infinity'::timestamptz) >= :timeFrom");
            sql.append(" AND COALESCE(").append(eventAlias).append(".started_at, ").append(eventAlias).append(".updated_at) <= :timeTo");
        }
    }

    private void appendTrainFilters(StringBuilder sql, DashboardQuery query) {
        if (query.boundingBox() != null) {
            sql.append(" AND rsu.current_position && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)");
        }
        if (!query.departmentCodes().isEmpty()) {
            sql.append(" AND (current_io.department_code IN (:departmentCodes) OR next_io.department_code IN (:departmentCodes))");
        }
    }

    private void appendDepartmentAndBoundingFilters(
        StringBuilder sql,
        DashboardQuery query,
        String departmentAlias,
        String geometryExpression
    ) {
        if (!query.departmentCodes().isEmpty()) {
            sql.append(" AND ").append(departmentAlias).append(".department_code IN (:departmentCodes)");
        }
        if (query.boundingBox() != null) {
            sql.append(" AND ").append(geometryExpression)
                .append(" && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)");
        }
    }

    private UUID getUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value == null ? null : (UUID) value;
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось разобрать GeoJSON-представление.", exception);
        }
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
