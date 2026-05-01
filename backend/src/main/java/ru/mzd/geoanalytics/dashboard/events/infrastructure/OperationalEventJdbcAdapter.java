package ru.mzd.geoanalytics.dashboard.events.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventDetailsView;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusHistoryView;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventAggregate;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusChange;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusUpdateResult;

@Repository
public class OperationalEventJdbcAdapter implements OperationalEventPersistencePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OperationalEventJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<OperationalEventAggregate> findAggregateForUpdate(UUID eventId) {
        return jdbcTemplate.query("""
            SELECT
                oe.id,
                oe.title,
                oe.status::text AS status,
                oe.updated_at,
                io.display_name AS affected_section,
                oe.last_changed_by
            FROM dashboard.operational_event oe
            LEFT JOIN dashboard.infrastructure_object io
                ON io.id = oe.affected_infrastructure_object_id
            WHERE oe.id = :eventId
            FOR UPDATE OF oe
            """, new MapSqlParameterSource("eventId", eventId), resultSet ->
            resultSet.next() ? Optional.of(new OperationalEventAggregate(
                getUuid(resultSet, "id"),
                resultSet.getString("title"),
                EventStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("affected_section"),
                resultSet.getString("last_changed_by")
            )) : Optional.empty()
        );
    }

    @Override
    public Optional<OperationalEventDetailsView> findDetails(UUID eventId) {
        OperationalEventDetailsView details = jdbcTemplate.query("""
            SELECT
                oe.id,
                oe.event_type,
                oe.title,
                oe.description,
                oe.status::text AS status,
                oe.severity,
                ST_Y(oe.location) AS latitude,
                ST_X(oe.location) AS longitude,
                oe.affected_infrastructure_object_id,
                io.display_name AS affected_section,
                oe.started_at,
                oe.ended_at,
                oe.updated_at,
                oe.last_changed_by
            FROM dashboard.operational_event oe
            LEFT JOIN dashboard.infrastructure_object io
                ON io.id = oe.affected_infrastructure_object_id
            WHERE oe.id = :eventId
            """, new MapSqlParameterSource("eventId", eventId), resultSet -> {
            if (!resultSet.next()) {
                return null;
            }

            EventStatus status = EventStatus.valueOf(resultSet.getString("status"));
            List<OperationalEventStatusHistoryView> history = loadStatusHistory(eventId);
            return new OperationalEventDetailsView(
                getUuid(resultSet, "id"),
                resultSet.getString("event_type"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                status,
                resultSet.getString("severity"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                getUuid(resultSet, "affected_infrastructure_object_id"),
                resultSet.getString("affected_section"),
                resultSet.getTimestamp("started_at") != null ? resultSet.getTimestamp("started_at").toInstant() : null,
                resultSet.getTimestamp("ended_at") != null ? resultSet.getTimestamp("ended_at").toInstant() : null,
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("last_changed_by"),
                status.allowedTransitions().stream().toList(),
                history
            );
        });

        return Optional.ofNullable(details);
    }

    @Override
    public OperationalEventStatusUpdateResult applyStatusChange(OperationalEventStatusChange statusChange) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("eventId", statusChange.eventId())
            .addValue("fromStatus", statusChange.fromStatus().name())
            .addValue("toStatus", statusChange.toStatus().name())
            .addValue("comment", statusChange.comment())
            .addValue("principalId", statusChange.principalId());

        jdbcTemplate.update("""
            UPDATE dashboard.operational_event
            SET status = CAST(:toStatus AS dashboard.event_status),
                last_changed_by = :principalId
            WHERE id = :eventId
            """, parameters);

        jdbcTemplate.update("""
            INSERT INTO dashboard.operational_event_status_history (
                event_id,
                from_status,
                to_status,
                comment,
                changed_by
            )
            VALUES (
                :eventId,
                CAST(:fromStatus AS dashboard.event_status),
                CAST(:toStatus AS dashboard.event_status),
                :comment,
                :principalId
            )
            """, parameters);

        return jdbcTemplate.query("""
            SELECT
                oe.id,
                oe.status::text AS status,
                oe.updated_at,
                io.display_name AS affected_section
            FROM dashboard.operational_event oe
            LEFT JOIN dashboard.infrastructure_object io
                ON io.id = oe.affected_infrastructure_object_id
            WHERE oe.id = :eventId
            """, new MapSqlParameterSource("eventId", statusChange.eventId()), resultSet -> {
            if (!resultSet.next()) {
                throw new IllegalStateException("РћР±РЅРѕРІР»С‘РЅРЅР°СЏ Р·Р°РїРёСЃСЊ РѕРїРµСЂР°С‚РёРІРЅРѕРіРѕ СЃРѕР±С‹С‚РёСЏ РѕС‚СЃСѓС‚СЃС‚РІСѓРµС‚.");
            }
            return new OperationalEventStatusUpdateResult(
                getUuid(resultSet, "id"),
                EventStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("affected_section")
            );
        });
    }

    @Override
    public Optional<OperationalEventProjection> findEventProjection(UUID eventId) {
        return jdbcTemplate.query("""
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
            WHERE oe.id = :eventId
            """, new MapSqlParameterSource("eventId", eventId), resultSet ->
            resultSet.next() ? Optional.of(new OperationalEventProjection(
                getUuid(resultSet, "id"),
                resultSet.getString("title"),
                EventStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("severity"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                getUuid(resultSet, "affected_infrastructure_object_id"),
                resultSet.getString("affected_section"),
                resultSet.getTimestamp("started_at") != null ? resultSet.getTimestamp("started_at").toInstant() : null,
                resultSet.getTimestamp("updated_at") != null ? resultSet.getTimestamp("updated_at").toInstant() : null
            )) : Optional.empty()
        );
    }

    private List<OperationalEventStatusHistoryView> loadStatusHistory(UUID eventId) {
        return jdbcTemplate.query("""
            SELECT
                id,
                from_status::text AS from_status,
                to_status::text AS to_status,
                comment,
                changed_at,
                changed_by
            FROM dashboard.operational_event_status_history
            WHERE event_id = :eventId
            ORDER BY changed_at DESC
            """, new MapSqlParameterSource("eventId", eventId), (rs, rowNum) ->
            new OperationalEventStatusHistoryView(
                getUuid(rs, "id"),
                rs.getString("from_status") != null ? EventStatus.valueOf(rs.getString("from_status")) : null,
                EventStatus.valueOf(rs.getString("to_status")),
                rs.getString("comment"),
                rs.getTimestamp("changed_at").toInstant(),
                rs.getString("changed_by")
            )
        );
    }

    private UUID getUuid(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value == null ? null : (UUID) value;
    }
}
