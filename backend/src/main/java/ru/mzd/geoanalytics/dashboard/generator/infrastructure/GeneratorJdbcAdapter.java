package ru.mzd.geoanalytics.dashboard.generator.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.common.exception.ConflictException;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;
import ru.mzd.geoanalytics.dashboard.generator.application.port.GeneratorPersistencePort;
import ru.mzd.geoanalytics.dashboard.generator.domain.GeneratorModels;

@Repository
public class GeneratorJdbcAdapter implements GeneratorPersistencePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    public GeneratorJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public GeneratorModels.ReferenceNetwork loadReferenceNetwork() {
        List<GeneratorModels.ReferenceStation> stations = jdbcTemplate.query("""
            SELECT
                s.id,
                s.code,
                io.display_name,
                io.department_code,
                s.station_type,
                ST_Y(s.location) AS latitude,
                ST_X(s.location) AS longitude
            FROM dashboard.station s
            JOIN dashboard.infrastructure_object io ON io.id = s.id
            ORDER BY COALESCE(s.order_index, 2147483647), io.display_name
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            new GeneratorModels.ReferenceStation(
                getUuid(rs, "id"),
                rs.getString("code"),
                rs.getString("display_name"),
                rs.getString("department_code"),
                rs.getString("station_type"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude")
            )
        );

        List<GeneratorModels.ReferenceRouteSegment> routeSegments = jdbcTemplate.query("""
            SELECT
                rs.id,
                rs.from_station_id,
                from_station.code AS from_station_code,
                rs.to_station_id,
                to_station.code AS to_station_code,
                io.department_code,
                rs.length_km,
                ST_AsText(rs.geometry) AS geometry_wkt,
                rs.status
            FROM dashboard.route_segment rs
            JOIN dashboard.infrastructure_object io ON io.id = rs.id
            JOIN dashboard.station from_station ON from_station.id = rs.from_station_id
            JOIN dashboard.station to_station ON to_station.id = rs.to_station_id
            ORDER BY rs.length_km DESC, rs.id
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            new GeneratorModels.ReferenceRouteSegment(
                getUuid(rs, "id"),
                getUuid(rs, "from_station_id"),
                rs.getString("from_station_code"),
                getUuid(rs, "to_station_id"),
                rs.getString("to_station_code"),
                rs.getString("department_code"),
                rs.getBigDecimal("length_km").doubleValue(),
                parseLineString(rs.getString("geometry_wkt")),
                rs.getString("status")
            )
        );

        List<RouteStopRow> routeStopRows = jdbcTemplate.query("""
            SELECT
                r.id AS route_id,
                r.code AS route_code,
                r.name AS route_name,
                rsl.sequence_no,
                s.id AS station_id,
                s.code AS station_code
            FROM dashboard.route r
            JOIN dashboard.route_station_link rsl ON rsl.route_id = r.id
            JOIN dashboard.station s ON s.id = rsl.station_id
            WHERE r.is_active
            ORDER BY r.code, rsl.sequence_no
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            new RouteStopRow(
                getUuid(rs, "route_id"),
                rs.getString("route_code"),
                rs.getString("route_name"),
                rs.getInt("sequence_no"),
                getUuid(rs, "station_id"),
                rs.getString("station_code")
            )
        );

        Map<UUID, RouteAccumulator> routesById = new LinkedHashMap<>();
        for (RouteStopRow row : routeStopRows) {
            routesById.computeIfAbsent(row.routeId(), ignored ->
                new RouteAccumulator(row.routeId(), row.routeCode(), row.routeName(), new ArrayList<>())
            ).stops().add(new GeneratorModels.ReferenceRouteStop(
                row.stationId(),
                row.stationCode(),
                row.sequenceNo()
            ));
        }

        List<GeneratorModels.ReferenceRoute> routes = routesById.values().stream()
            .map(route -> new GeneratorModels.ReferenceRoute(route.routeId(), route.routeCode(), route.routeName(), List.copyOf(route.stops())))
            .toList();

        return new GeneratorModels.ReferenceNetwork(Instant.now(clock), stations, routeSegments, routes);
    }

    @Override
    public List<GeneratorModels.ActiveEvent> loadActiveEvents() {
        return jdbcTemplate.query("""
            SELECT
                id,
                event_type,
                title,
                description,
                severity,
                status::text AS status,
                affected_infrastructure_object_id,
                department_code,
                ST_Y(location) AS latitude,
                ST_X(location) AS longitude,
                started_at,
                updated_at,
                last_changed_by
            FROM dashboard.operational_event
            WHERE is_active
            ORDER BY updated_at DESC
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            new GeneratorModels.ActiveEvent(
                getUuid(rs, "id"),
                rs.getString("event_type"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("severity"),
                EventStatus.valueOf(rs.getString("status")),
                getUuid(rs, "affected_infrastructure_object_id"),
                rs.getString("department_code"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
                rs.getTimestamp("updated_at").toInstant(),
                rs.getString("last_changed_by")
            )
        );
    }

    @Override
    @Transactional
    public TrainProjection upsertTrain(GeneratorModels.TrainUpsertCommand command) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("trainNumber", command.trainNumber())
            .addValue("routeId", command.routeId())
            .addValue("currentStationId", command.currentStationId())
            .addValue("nextStationId", command.nextStationId())
            .addValue("progressPercent", command.progressPercent())
            .addValue("speedKmh", command.speedKmh())
            .addValue("status", command.status())
            .addValue("longitude", command.longitude())
            .addValue("latitude", command.latitude())
            .addValue("lastUpdated", toTimestamp(command.lastUpdated()));

        jdbcTemplate.update("""
            INSERT INTO dashboard.rolling_stock_unit (
                id,
                train_number,
                route_id,
                current_station_id,
                next_station_id,
                current_position,
                progress_percent,
                speed_kmh,
                status,
                last_updated
            )
            VALUES (
                :id,
                :trainNumber,
                :routeId,
                :currentStationId,
                :nextStationId,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                :progressPercent,
                :speedKmh,
                :status,
                :lastUpdated
            )
            ON CONFLICT (id) DO UPDATE
            SET train_number = EXCLUDED.train_number,
                route_id = EXCLUDED.route_id,
                current_station_id = EXCLUDED.current_station_id,
                next_station_id = EXCLUDED.next_station_id,
                current_position = EXCLUDED.current_position,
                progress_percent = EXCLUDED.progress_percent,
                speed_kmh = EXCLUDED.speed_kmh,
                status = EXCLUDED.status,
                last_updated = EXCLUDED.last_updated
            """, parameters);

        jdbcTemplate.update("""
            INSERT INTO dashboard.rolling_stock_position (
                rolling_stock_id,
                recorded_at,
                position,
                speed_kmh
            )
            VALUES (
                :id,
                :lastUpdated,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                :speedKmh
            )
            """, parameters);

        return jdbcTemplate.query("""
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
            WHERE rsu.id = :id
            """, new MapSqlParameterSource("id", command.id()), resultSet -> {
            if (!resultSet.next()) {
                throw new IllegalStateException("Train projection is missing after ingestion: " + command.id());
            }

            return new TrainProjection(
                getUuid(resultSet, "id"),
                resultSet.getString("train_number"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                resultSet.getString("status"),
                getUuid(resultSet, "current_station_id"),
                getUuid(resultSet, "next_station_id"),
                resultSet.getBigDecimal("progress_percent") != null ? resultSet.getBigDecimal("progress_percent").doubleValue() : null,
                resultSet.getBigDecimal("speed_kmh") != null ? resultSet.getBigDecimal("speed_kmh").doubleValue() : null,
                resultSet.getTimestamp("last_updated").toInstant()
            );
        });
    }

    @Override
    @Transactional
    public void upsertEvent(GeneratorModels.EventUpsertCommand command) {
        ExistingEvent existingEvent = jdbcTemplate.query("""
            SELECT id, status::text AS status
            FROM dashboard.operational_event
            WHERE id = :id
            """, new MapSqlParameterSource("id", command.id()), resultSet ->
            resultSet.next()
                ? new ExistingEvent(getUuid(resultSet, "id"), EventStatus.valueOf(resultSet.getString("status")))
                : null
        );

        if (existingEvent != null && existingEvent.status() != command.status()) {
            if (!existingEvent.status().canTransitionTo(command.status())) {
                throw new ConflictException(
                    "Illegal operational event status transition for generator ingestion.",
                    existingEvent.status().name(),
                    command.status().name(),
                    existingEvent.status().allowedTransitions().stream().map(Enum::name).toList()
                );
            }
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("eventType", command.eventType())
            .addValue("title", command.title())
            .addValue("description", blankToNull(command.description()))
            .addValue("severity", command.severity())
            .addValue("status", command.status().name())
            .addValue("affectedObjectId", command.affectedObjectId())
            .addValue("departmentCode", blankToNull(command.departmentCode()))
            .addValue("longitude", command.longitude())
            .addValue("latitude", command.latitude())
            .addValue("startedAt", toTimestamp(command.startedAt()))
            .addValue("endedAt", toTimestamp(command.endedAt()))
            .addValue("updatedAt", Timestamp.from(Instant.now(clock)))
            .addValue("lastChangedBy", command.sourceSystem());

        if (existingEvent == null) {
            jdbcTemplate.update("""
                INSERT INTO dashboard.operational_event (
                    id,
                    event_type,
                    title,
                    description,
                    severity,
                    status,
                    affected_infrastructure_object_id,
                    department_code,
                    location,
                    started_at,
                    ended_at,
                    updated_at,
                    last_changed_by
                )
                VALUES (
                    :id,
                    :eventType,
                    :title,
                    :description,
                    :severity,
                    CAST(:status AS dashboard.event_status),
                    :affectedObjectId,
                    :departmentCode,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                    :startedAt,
                    :endedAt,
                    :updatedAt,
                    :lastChangedBy
                )
                """, parameters);

            insertStatusHistory(command.id(), null, command.status(), command.comment(), command.sourceSystem());
            return;
        }

        jdbcTemplate.update("""
            UPDATE dashboard.operational_event
            SET event_type = :eventType,
                title = :title,
                description = :description,
                severity = :severity,
                status = CAST(:status AS dashboard.event_status),
                affected_infrastructure_object_id = :affectedObjectId,
                department_code = :departmentCode,
                location = ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                started_at = :startedAt,
                ended_at = :endedAt,
                last_changed_by = :lastChangedBy
            WHERE id = :id
            """, parameters);

        if (existingEvent.status() != command.status()) {
            insertStatusHistory(command.id(), existingEvent.status(), command.status(), command.comment(), command.sourceSystem());
        }
    }

    @Override
    @Transactional
    public void replacePersonnelSnapshot(GeneratorModels.PersonnelSnapshotCommand command) {
        jdbcTemplate.update("""
            DELETE FROM dashboard.staff_aggregate
            WHERE period_month = :periodMonth
              AND dimension_type = :dimensionType
              AND scope_infrastructure_object_id IS NULL
            """, new MapSqlParameterSource()
            .addValue("periodMonth", command.periodMonth())
            .addValue("dimensionType", command.dimensionType()));

        UUID aggregateId = UUID.randomUUID();
        int totalHeadcount = command.items().stream().mapToInt(GeneratorModels.PersonnelSnapshotItem::headcount).sum();
        jdbcTemplate.update("""
            INSERT INTO dashboard.staff_aggregate (
                id,
                period_month,
                dimension_type,
                total_headcount,
                calculated_at
            )
            VALUES (
                :id,
                :periodMonth,
                :dimensionType,
                :totalHeadcount,
                :calculatedAt
            )
            """, new MapSqlParameterSource()
            .addValue("id", aggregateId)
            .addValue("periodMonth", command.periodMonth())
            .addValue("dimensionType", command.dimensionType())
            .addValue("totalHeadcount", totalHeadcount)
            .addValue("calculatedAt", Timestamp.from(Instant.now(clock))));

        for (GeneratorModels.PersonnelSnapshotItem item : command.items()) {
            jdbcTemplate.update("""
                INSERT INTO dashboard.staff_aggregate_item (
                    staff_aggregate_id,
                    dimension_key,
                    headcount,
                    change_percent
                )
                VALUES (
                    :staffAggregateId,
                    :dimensionKey,
                    :headcount,
                    :changePercent
                )
                """, new MapSqlParameterSource()
                .addValue("staffAggregateId", aggregateId)
                .addValue("dimensionKey", item.dimensionKey())
                .addValue("headcount", item.headcount())
                .addValue("changePercent", item.changePercent()));
        }
    }

    private void insertStatusHistory(
        UUID eventId,
        EventStatus fromStatus,
        EventStatus toStatus,
        String comment,
        String changedBy
    ) {
        jdbcTemplate.update("""
            INSERT INTO dashboard.operational_event_status_history (
                event_id,
                from_status,
                to_status,
                comment,
                changed_at,
                changed_by
            )
            VALUES (
                :eventId,
                CAST(:fromStatus AS dashboard.event_status),
                CAST(:toStatus AS dashboard.event_status),
                :comment,
                :changedAt,
                :changedBy
            )
            """, new MapSqlParameterSource()
            .addValue("eventId", eventId)
            .addValue("fromStatus", fromStatus != null ? fromStatus.name() : null)
            .addValue("toStatus", toStatus.name())
            .addValue("comment", blankToNull(comment))
            .addValue("changedAt", Timestamp.from(Instant.now(clock)))
            .addValue("changedBy", changedBy));
    }

    private UUID getUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value == null ? null : (UUID) value;
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private List<GeneratorModels.ReferencePoint> parseLineString(String geometryWkt) {
        if (geometryWkt == null || !geometryWkt.startsWith("LINESTRING(") || !geometryWkt.endsWith(")")) {
            throw new IllegalStateException("Unexpected route segment geometry format: " + geometryWkt);
        }

        String coordinatesSection = geometryWkt.substring("LINESTRING(".length(), geometryWkt.length() - 1);
        String[] coordinatePairs = coordinatesSection.split(",");
        ArrayList<GeneratorModels.ReferencePoint> points = new ArrayList<>(coordinatePairs.length);
        for (String coordinatePair : coordinatePairs) {
            String[] values = coordinatePair.trim().split("\\s+");
            if (values.length != 2) {
                throw new IllegalStateException("Unexpected coordinate pair in geometry: " + coordinatePair);
            }
            points.add(new GeneratorModels.ReferencePoint(
                Double.parseDouble(values[1]),
                Double.parseDouble(values[0])
            ));
        }
        return List.copyOf(points);
    }

    private record RouteStopRow(
        UUID routeId,
        String routeCode,
        String routeName,
        int sequenceNo,
        UUID stationId,
        String stationCode
    ) {
    }

    private record RouteAccumulator(
        UUID routeId,
        String routeCode,
        String routeName,
        List<GeneratorModels.ReferenceRouteStop> stops
    ) {
    }

    private record ExistingEvent(
        UUID id,
        EventStatus status
    ) {
    }
}
