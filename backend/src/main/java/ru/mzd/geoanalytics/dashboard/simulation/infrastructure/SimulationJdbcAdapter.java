package ru.mzd.geoanalytics.dashboard.simulation.infrastructure;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.simulation.application.port.SimulationPersistencePort;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

@Repository
public class SimulationJdbcAdapter implements SimulationPersistencePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    public SimulationJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SimulationModels.SimulationProfile ensureActiveProfile(String defaultProfileName) {
        SimulationModels.SimulationProfile profile = jdbcTemplate.query("""
            SELECT
                id,
                profile_name,
                tick_interval_seconds,
                train_count,
                event_generation_intensity
            FROM dashboard.simulation_profile
            WHERE is_active
            ORDER BY is_default DESC, updated_at DESC
            LIMIT 1
            """, resultSet -> resultSet.next() ? mapProfile(resultSet) : null);

        if (profile != null) {
            return profile;
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO dashboard.simulation_profile (
                id,
                profile_name,
                tick_interval_seconds,
                train_count,
                event_generation_intensity,
                is_default,
                is_active
            )
            VALUES (
                :id,
                :profileName,
                :tickIntervalSeconds,
                :trainCount,
                :eventGenerationIntensity,
                true,
                true
            )
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("profileName", defaultProfileName)
            .addValue("tickIntervalSeconds", 5)
            .addValue("trainCount", 18)
            .addValue("eventGenerationIntensity", new BigDecimal("0.35")));

        return new SimulationModels.SimulationProfile(id, defaultProfileName, 5, 18, 0.35);
    }

    @Override
    public boolean hasReferenceData() {
        Integer stationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dashboard.station",
            new MapSqlParameterSource(),
            Integer.class
        );
        return stationCount != null && stationCount > 0;
    }

    @Override
    @Transactional
    public void seedReferenceData(
        SimulationModels.SimulationProfile profile,
        int routeCount,
        int initialEventCount,
        boolean includeOperationalSeed
    ) {
        List<SimulationModels.StationSeed> stations = MoscowRailwayReferenceData.stations();
        for (SimulationModels.StationSeed station : stations) {
            insertInfrastructureObject(station.id(), "STATION", station.name(), station.code(), station.departmentCode());
            insertStation(station);
        }

        List<SimulationModels.RouteDefinition> routes = MoscowRailwayReferenceData.routes(stations).stream()
            .limit(routeCount)
            .toList();
        for (SimulationModels.RouteDefinition route : routes) {
            insertRoute(route);
            for (int index = 0; index < route.stations().size(); index++) {
                SimulationModels.StationSeed station = route.stations().get(index);
                jdbcTemplate.update("""
                    INSERT INTO dashboard.route_station_link (
                        route_id,
                        station_id,
                        sequence_no,
                        stop_role
                    )
                    VALUES (
                        :routeId,
                        :stationId,
                        :sequenceNo,
                        :stopRole
                    )
                    """, new MapSqlParameterSource()
                    .addValue("routeId", route.id())
                    .addValue("stationId", station.id())
                    .addValue("sequenceNo", index + 1)
                    .addValue("stopRole", index == 0 ? "ORIGIN" : index == route.stations().size() - 1 ? "TERMINAL" : "INTERMEDIATE"));

                if (index < route.stations().size() - 1) {
                    insertRouteSegment(route.stations().get(index), route.stations().get(index + 1), index == 0 ? "OVERLOADED" : "ACTIVE");
                }
            }
        }

        if (includeOperationalSeed) {
            for (int index = 0; index < profile.trainCount(); index++) {
                SimulationModels.RouteDefinition route = routes.get(index % routes.size());
                SimulationModels.StationSeed currentStation = route.stations().get(index % (route.stations().size() - 1));
                SimulationModels.StationSeed nextStation = route.stations().get((index % (route.stations().size() - 1)) + 1);
                double progress = ThreadLocalRandom.current().nextDouble(5, 85);
                double latitude = interpolate(currentStation.latitude(), nextStation.latitude(), progress / 100.0);
                double longitude = interpolate(currentStation.longitude(), nextStation.longitude(), progress / 100.0);
                double speed = ThreadLocalRandom.current().nextDouble(35, 85);

                UUID trainId = UUID.randomUUID();
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
                        status
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
                        :status
                    )
                    """, new MapSqlParameterSource()
                    .addValue("id", trainId)
                    .addValue("trainNumber", String.format("MZD-%03d", index + 1))
                    .addValue("routeId", route.id())
                    .addValue("currentStationId", currentStation.id())
                    .addValue("nextStationId", nextStation.id())
                    .addValue("longitude", longitude)
                    .addValue("latitude", latitude)
                    .addValue("progressPercent", progress)
                    .addValue("speedKmh", speed)
                    .addValue("status", "ON_ROUTE"));

                jdbcTemplate.update("""
                    INSERT INTO dashboard.rolling_stock_position (
                        rolling_stock_id,
                        position,
                        speed_kmh
                    )
                    VALUES (
                        :rollingStockId,
                        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                        :speedKmh
                    )
                    """, new MapSqlParameterSource()
                    .addValue("rollingStockId", trainId)
                    .addValue("longitude", longitude)
                    .addValue("latitude", latitude)
                    .addValue("speedKmh", speed));
            }

            for (int index = 0; index < initialEventCount; index++) {
                createSimulatedEvent();
            }
        }

        seedPersonnelSnapshot(stations);
    }

    @Override
    public List<SimulationModels.SimulatedTrainState> loadTrainStates() {
        return jdbcTemplate.query("""
            SELECT
                rsu.id,
                rsu.train_number,
                rsu.route_id,
                rsu.current_station_id,
                rsu.next_station_id,
                COALESCE(rsu.progress_percent, 0) AS progress_percent,
                COALESCE(rsu.speed_kmh, 0) AS speed_kmh,
                rsu.status,
                ST_Y(current_station.location) AS current_latitude,
                ST_X(current_station.location) AS current_longitude,
                ST_Y(next_station.location) AS next_latitude,
                ST_X(next_station.location) AS next_longitude
            FROM dashboard.rolling_stock_unit rsu
            JOIN dashboard.station current_station ON current_station.id = rsu.current_station_id
            JOIN dashboard.station next_station ON next_station.id = rsu.next_station_id
            ORDER BY rsu.train_number
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            new SimulationModels.SimulatedTrainState(
                getUuid(rs, "id"),
                rs.getString("train_number"),
                getUuid(rs, "route_id"),
                getUuid(rs, "current_station_id"),
                getUuid(rs, "next_station_id"),
                rs.getBigDecimal("progress_percent").doubleValue(),
                rs.getBigDecimal("speed_kmh").doubleValue(),
                rs.getString("status"),
                rs.getDouble("current_latitude"),
                rs.getDouble("current_longitude"),
                rs.getDouble("next_latitude"),
                rs.getDouble("next_longitude")
            )
        );
    }

    @Override
    public Map<UUID, List<SimulationModels.RouteStop>> loadRouteStops() {
        List<Map.Entry<UUID, SimulationModels.RouteStop>> rows = jdbcTemplate.query("""
            SELECT
                rsl.route_id,
                rsl.station_id,
                rsl.sequence_no,
                ST_Y(s.location) AS latitude,
                ST_X(s.location) AS longitude
            FROM dashboard.route_station_link rsl
            JOIN dashboard.station s ON s.id = rsl.station_id
            ORDER BY rsl.route_id, rsl.sequence_no
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            Map.entry(
                getUuid(rs, "route_id"),
                new SimulationModels.RouteStop(
                    getUuid(rs, "station_id"),
                    rs.getInt("sequence_no"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
                )
            )
        );

        return rows.stream().collect(Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
        ));
    }

    @Override
    @Transactional
    public void saveTrainUpdate(SimulationModels.SimulatedTrainUpdate trainUpdate) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("id", trainUpdate.id())
            .addValue("currentStationId", trainUpdate.currentStationId())
            .addValue("nextStationId", trainUpdate.nextStationId())
            .addValue("progressPercent", trainUpdate.progressPercent())
            .addValue("speedKmh", trainUpdate.speedKmh())
            .addValue("status", trainUpdate.status())
            .addValue("longitude", trainUpdate.longitude())
            .addValue("latitude", trainUpdate.latitude());

        jdbcTemplate.update("""
            UPDATE dashboard.rolling_stock_unit
            SET current_station_id = :currentStationId,
                next_station_id = :nextStationId,
                current_position = ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                progress_percent = :progressPercent,
                speed_kmh = :speedKmh,
                status = :status
            WHERE id = :id
            """, parameters);

        jdbcTemplate.update("""
            INSERT INTO dashboard.rolling_stock_position (
                rolling_stock_id,
                position,
                speed_kmh
            )
            VALUES (
                :id,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                :speedKmh
            )
            """, parameters);
    }

    @Override
    public List<SimulationModels.SimulatedEventState> loadActiveEvents() {
        return jdbcTemplate.query("""
            SELECT id, status::text AS status
            FROM dashboard.operational_event
            WHERE is_active
            ORDER BY updated_at DESC
            """, new MapSqlParameterSource(), (rs, rowNum) ->
            new SimulationModels.SimulatedEventState(
                getUuid(rs, "id"),
                rs.getString("status")
            )
        );
    }

    @Override
    @Transactional
    public UUID createSimulatedEvent() {
        AffectedObjectSeed affectedObject = jdbcTemplate.query("""
            SELECT
                io.id,
                io.department_code,
                CASE
                    WHEN s.id IS NOT NULL THEN ST_Y(s.location)
                    ELSE ST_Y(ST_LineInterpolatePoint(rs.geometry, random()))
                END AS latitude,
                CASE
                    WHEN s.id IS NOT NULL THEN ST_X(s.location)
                    ELSE ST_X(ST_LineInterpolatePoint(rs.geometry, random()))
                END AS longitude
            FROM dashboard.infrastructure_object io
            LEFT JOIN dashboard.station s ON s.id = io.id
            LEFT JOIN dashboard.route_segment rs ON rs.id = io.id
            WHERE io.object_kind IN ('STATION', 'ROUTE_SEGMENT')
            ORDER BY random()
            LIMIT 1
            """, new MapSqlParameterSource(), resultSet -> resultSet.next()
            ? new AffectedObjectSeed(
                getUuid(resultSet, "id"),
                resultSet.getString("department_code"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude")
            )
            : null);

        EventSeed eventSeed = randomEventSeed(affectedObject);
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
                :updatedAt,
                :lastChangedBy
            )
            """, new MapSqlParameterSource()
            .addValue("id", eventSeed.id())
            .addValue("eventType", eventSeed.eventType())
            .addValue("title", eventSeed.title())
            .addValue("description", eventSeed.description())
            .addValue("severity", eventSeed.severity())
            .addValue("status", eventSeed.status())
            .addValue("affectedObjectId", eventSeed.affectedObjectId())
            .addValue("departmentCode", eventSeed.departmentCode())
            .addValue("longitude", eventSeed.longitude())
            .addValue("latitude", eventSeed.latitude())
            .addValue("startedAt", toTimestamp(eventSeed.startedAt()))
            .addValue("updatedAt", toTimestamp(eventSeed.updatedAt()))
            .addValue("lastChangedBy", eventSeed.lastChangedBy()));

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
                NULL,
                CAST(:toStatus AS dashboard.event_status),
                :comment,
                :changedAt,
                :changedBy
            )
            """, new MapSqlParameterSource()
            .addValue("eventId", eventSeed.id())
            .addValue("toStatus", eventSeed.status())
            .addValue("comment", "Инициализация симуляции МЖД")
            .addValue("changedAt", toTimestamp(eventSeed.updatedAt()))
            .addValue("changedBy", eventSeed.lastChangedBy()));

        return eventSeed.id();
    }

    private void insertInfrastructureObject(
        UUID id,
        String objectKind,
        String displayName,
        String shortCode,
        String departmentCode
    ) {
        jdbcTemplate.update("""
            INSERT INTO dashboard.infrastructure_object (
                id,
                object_kind,
                display_name,
                short_code,
                department_code
            )
            VALUES (
                :id,
                CAST(:objectKind AS dashboard.infrastructure_object_kind),
                :displayName,
                :shortCode,
                :departmentCode
            )
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("objectKind", objectKind)
            .addValue("displayName", displayName)
            .addValue("shortCode", shortCode)
            .addValue("departmentCode", departmentCode));
    }

    private void insertStation(SimulationModels.StationSeed station) {
        jdbcTemplate.update("""
            INSERT INTO dashboard.station (
                id,
                code,
                station_type,
                order_index,
                location
            )
            VALUES (
                :id,
                :code,
                :stationType,
                :orderIndex,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
            )
            """, new MapSqlParameterSource()
            .addValue("id", station.id())
            .addValue("code", station.code())
            .addValue("stationType", station.stationType())
            .addValue("orderIndex", station.orderIndex())
            .addValue("longitude", station.longitude())
            .addValue("latitude", station.latitude()));
    }

    private void insertRoute(SimulationModels.RouteDefinition route) {
        jdbcTemplate.update("""
            INSERT INTO dashboard.route (
                id,
                code,
                name,
                direction
            )
            VALUES (
                :id,
                :code,
                :name,
                :direction
            )
            """, new MapSqlParameterSource()
            .addValue("id", route.id())
            .addValue("code", route.code())
            .addValue("name", route.name())
            .addValue("direction", "BIDIRECTIONAL"));
    }

    private void insertRouteSegment(
        SimulationModels.StationSeed fromStation,
        SimulationModels.StationSeed toStation,
        String status
    ) {
        UUID segmentId = UUID.randomUUID();
        insertInfrastructureObject(
            segmentId,
            "ROUTE_SEGMENT",
            fromStation.name() + " - " + toStation.name(),
            fromStation.code() + "-" + toStation.code(),
            fromStation.departmentCode()
        );
        List<SimulationModels.CoordinatePoint> shapePoints = MoscowRailwayReferenceData.segmentShape(fromStation, toStation);

        jdbcTemplate.update("""
            INSERT INTO dashboard.route_segment (
                id,
                from_station_id,
                to_station_id,
                length_km,
                geometry,
                status
            )
            VALUES (
                :id,
                :fromStationId,
                :toStationId,
                :lengthKm,
                ST_GeomFromText(:geometryWkt, 4326),
                :status
            )
            """, new MapSqlParameterSource()
            .addValue("id", segmentId)
            .addValue("fromStationId", fromStation.id())
            .addValue("toStationId", toStation.id())
            .addValue("lengthKm", polylineDistanceKm(shapePoints))
            .addValue("geometryWkt", toLineStringWkt(shapePoints))
            .addValue("status", status));
    }

    private void seedPersonnelSnapshot(List<SimulationModels.StationSeed> stations) {
        UUID aggregateId = UUID.randomUUID();
        LocalDate periodMonth = LocalDate.now(clock).withDayOfMonth(1);
        Map<String, Integer> byDepartment = stations.stream()
            .collect(Collectors.toMap(
                SimulationModels.StationSeed::departmentCode,
                station -> 1200 + ThreadLocalRandom.current().nextInt(600),
                Integer::sum
            ));

        int totalHeadcount = byDepartment.values().stream().mapToInt(Integer::intValue).sum();
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
                'DEPARTMENT_CODE',
                :totalHeadcount,
                :calculatedAt
            )
            """, new MapSqlParameterSource()
            .addValue("id", aggregateId)
            .addValue("periodMonth", periodMonth)
            .addValue("totalHeadcount", totalHeadcount)
            .addValue("calculatedAt", toTimestamp(Instant.now(clock))));

        byDepartment.forEach((departmentCode, headcount) ->
            jdbcTemplate.update("""
                INSERT INTO dashboard.staff_aggregate_item (
                    staff_aggregate_id,
                    dimension_key,
                    headcount,
                    change_percent
                )
                VALUES (
                    :aggregateId,
                    :dimensionKey,
                    :headcount,
                    :changePercent
                )
                """, new MapSqlParameterSource()
                .addValue("aggregateId", aggregateId)
                .addValue("dimensionKey", departmentCode)
                .addValue("headcount", headcount)
                .addValue("changePercent", ThreadLocalRandom.current().nextDouble(-3.5, 4.2))));
    }

    private SimulationModels.SimulationProfile mapProfile(ResultSet resultSet) throws SQLException {
        return new SimulationModels.SimulationProfile(
            getUuid(resultSet, "id"),
            resultSet.getString("profile_name"),
            resultSet.getInt("tick_interval_seconds"),
            resultSet.getInt("train_count"),
            resultSet.getBigDecimal("event_generation_intensity").doubleValue()
        );
    }

    private EventSeed randomEventSeed(AffectedObjectSeed affectedObject) {
        Instant now = Instant.now(clock);
        String[] eventTypes = {"INCIDENT", "REPAIR", "DELAY", "OVERLOAD"};
        String[] severities = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
        String eventType = eventTypes[ThreadLocalRandom.current().nextInt(eventTypes.length)];
        String severity = severities[ThreadLocalRandom.current().nextInt(severities.length)];
        String departmentCode = affectedObject != null && affectedObject.departmentCode() != null
            ? affectedObject.departmentCode()
            : "MZD-MSK";
        double latitude = affectedObject != null
            ? affectedObject.latitude()
            : 55.75 + ThreadLocalRandom.current().nextDouble(-0.2, 0.2);
        double longitude = affectedObject != null
            ? affectedObject.longitude()
            : 37.62 + ThreadLocalRandom.current().nextDouble(-0.2, 0.2);

        return new EventSeed(
            UUID.randomUUID(),
            eventType,
            switch (eventType) {
                case "INCIDENT" -> "Сбой сигнализации на участке МЖД";
                case "REPAIR" -> "Плановое окно ремонта инфраструктуры";
                case "DELAY" -> "Задержка движения поездов";
                default -> "Перегрузка железнодорожного участка";
            },
            "Смоделированное оперативное событие на полигоне Московской железной дороги.",
            severity,
            "REGISTERED",
            affectedObject != null ? affectedObject.id() : null,
            departmentCode,
            latitude,
            longitude,
            now.minusSeconds(ThreadLocalRandom.current().nextInt(300, 3600)),
            now,
            "Симулятор МЖД"
        );
    }

    private double approximateDistanceKm(
        SimulationModels.CoordinatePoint fromPoint,
        SimulationModels.CoordinatePoint toPoint
    ) {
        double startLatitude = Math.toRadians(fromPoint.latitude());
        double endLatitude = Math.toRadians(toPoint.latitude());
        double latitudeDelta = endLatitude - startLatitude;
        double longitudeDelta = Math.toRadians(toPoint.longitude() - fromPoint.longitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2.0), 2)
            + (Math.cos(startLatitude) * Math.cos(endLatitude) * Math.pow(Math.sin(longitudeDelta / 2.0), 2));
        double earthRadiusKm = 6_371.0088;
        double distanceKm = earthRadiusKm * 2.0 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1.0 - haversine));
        return Math.max(5.0, distanceKm);
    }

    private double polylineDistanceKm(List<SimulationModels.CoordinatePoint> shapePoints) {
        double distanceKm = 0.0;
        for (int index = 0; index < shapePoints.size() - 1; index++) {
            distanceKm += approximateDistanceKm(shapePoints.get(index), shapePoints.get(index + 1));
        }
        return Math.max(5.0, distanceKm);
    }

    private String toLineStringWkt(List<SimulationModels.CoordinatePoint> shapePoints) {
        ArrayList<String> coordinates = new ArrayList<>(shapePoints.size());
        for (SimulationModels.CoordinatePoint shapePoint : shapePoints) {
            coordinates.add(shapePoint.longitude() + " " + shapePoint.latitude());
        }
        return "LINESTRING(" + String.join(", ", coordinates) + ")";
    }

    private double interpolate(double start, double end, double factor) {
        return start + ((end - start) * factor);
    }

    private UUID getUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value == null ? null : (UUID) value;
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private record AffectedObjectSeed(
        UUID id,
        String departmentCode,
        double latitude,
        double longitude
    ) {
    }

    private record EventSeed(
        UUID id,
        String eventType,
        String title,
        String description,
        String severity,
        String status,
        UUID affectedObjectId,
        String departmentCode,
        double latitude,
        double longitude,
        Instant startedAt,
        Instant updatedAt,
        String lastChangedBy
    ) {
    }
}
