SET search_path TO dashboard, public;

CREATE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE FUNCTION set_last_updated()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.last_updated := now();
    RETURN NEW;
END;
$$;

CREATE FUNCTION prevent_infrastructure_object_kind_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.object_kind <> NEW.object_kind THEN
        RAISE EXCEPTION USING
            ERRCODE = 'check_violation',
            CONSTRAINT = 'chk_infrastructure_object_kind_immutable',
            MESSAGE = 'infrastructure_object.object_kind cannot be changed after creation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION assert_infrastructure_object_kind()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    actual_kind infrastructure_object_kind;
    expected_kind infrastructure_object_kind := TG_ARGV[0]::infrastructure_object_kind;
BEGIN
    SELECT object_kind
    INTO actual_kind
    FROM infrastructure_object
    WHERE id = NEW.id;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING
            ERRCODE = 'foreign_key_violation',
            MESSAGE = format(
                'Missing infrastructure_object row for %s.id=%s',
                TG_TABLE_NAME,
                NEW.id
            );
    END IF;

    IF actual_kind <> expected_kind THEN
        RAISE EXCEPTION USING
            ERRCODE = 'check_violation',
            CONSTRAINT = 'chk_infrastructure_object_kind_matches_child',
            MESSAGE = format(
                'Expected infrastructure_object.kind=%s for %s but found %s',
                expected_kind,
                TG_TABLE_NAME,
                actual_kind
            );
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION enforce_operational_event_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        IF NOT EXISTS (
            SELECT 1
            FROM event_status_transition t
            WHERE t.from_status = OLD.status
              AND t.to_status = NEW.status
        ) THEN
            RAISE EXCEPTION USING
                ERRCODE = 'check_violation',
                CONSTRAINT = 'chk_operational_event_status_transition',
                MESSAGE = format(
                    'Illegal operational event status transition: %s -> %s',
                    OLD.status,
                    NEW.status
                );
        END IF;

        IF NEW.status IN ('RESOLVED', 'CANCELED') AND NEW.ended_at IS NULL THEN
            NEW.ended_at := now();
        END IF;
    END IF;

    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE FUNCTION prune_rolling_stock_position(
    p_retain_for interval DEFAULT interval '24 hours'
)
RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE
    deleted_rows bigint;
BEGIN
    DELETE FROM rolling_stock_position
    WHERE recorded_at < now() - p_retain_for;

    GET DIAGNOSTICS deleted_rows = ROW_COUNT;
    RETURN deleted_rows;
END;
$$;

CREATE FUNCTION prune_security_audit_log(
    p_retain_for interval DEFAULT interval '90 days'
)
RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE
    deleted_rows bigint;
BEGIN
    DELETE FROM security_audit_log
    WHERE occurred_at < now() - p_retain_for;

    GET DIAGNOSTICS deleted_rows = ROW_COUNT;
    RETURN deleted_rows;
END;
$$;

CREATE TRIGGER trg_route_set_updated_at
BEFORE UPDATE ON route
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_simulation_profile_set_updated_at
BEFORE UPDATE ON simulation_profile
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_rolling_stock_unit_set_last_updated
BEFORE UPDATE ON rolling_stock_unit
FOR EACH ROW
EXECUTE FUNCTION set_last_updated();

CREATE TRIGGER trg_infrastructure_object_prevent_kind_change
BEFORE UPDATE OF object_kind ON infrastructure_object
FOR EACH ROW
EXECUTE FUNCTION prevent_infrastructure_object_kind_change();

CREATE TRIGGER trg_station_assert_object_kind
BEFORE INSERT OR UPDATE ON station
FOR EACH ROW
EXECUTE FUNCTION assert_infrastructure_object_kind('STATION');

CREATE TRIGGER trg_route_segment_assert_object_kind
BEFORE INSERT OR UPDATE ON route_segment
FOR EACH ROW
EXECUTE FUNCTION assert_infrastructure_object_kind('ROUTE_SEGMENT');

CREATE TRIGGER trg_operational_event_enforce_update
BEFORE UPDATE ON operational_event
FOR EACH ROW
EXECUTE FUNCTION enforce_operational_event_update();

CREATE INDEX idx_infrastructure_object_department_kind
    ON infrastructure_object (department_code, object_kind)
    WHERE department_code IS NOT NULL;

CREATE INDEX idx_station_location_gist
    ON station
    USING gist (location);

CREATE INDEX idx_route_segment_from_station_id
    ON route_segment (from_station_id);

CREATE INDEX idx_route_segment_to_station_id
    ON route_segment (to_station_id);

CREATE INDEX idx_route_segment_geometry_gist
    ON route_segment
    USING gist (geometry);

CREATE INDEX idx_route_station_link_station_id
    ON route_station_link (station_id);

CREATE INDEX idx_rolling_stock_unit_route_status
    ON rolling_stock_unit (route_id, status);

CREATE INDEX idx_rolling_stock_unit_current_station_id
    ON rolling_stock_unit (current_station_id);

CREATE INDEX idx_rolling_stock_unit_next_station_id
    ON rolling_stock_unit (next_station_id);

CREATE INDEX idx_rolling_stock_unit_current_position_gist
    ON rolling_stock_unit
    USING gist (current_position)
    WHERE current_position IS NOT NULL;

CREATE UNIQUE INDEX ux_rolling_stock_position_stock_recorded_at_desc
    ON rolling_stock_position (rolling_stock_id, recorded_at DESC);

CREATE INDEX idx_rolling_stock_position_position_gist
    ON rolling_stock_position
    USING gist (position);

CREATE INDEX idx_operational_event_status_severity_started_at
    ON operational_event (status, severity, started_at DESC);

CREATE INDEX idx_operational_event_affected_infrastructure_object_id
    ON operational_event (affected_infrastructure_object_id);

CREATE INDEX idx_operational_event_department_status_started_at
    ON operational_event (department_code, status, started_at DESC)
    WHERE department_code IS NOT NULL;

CREATE INDEX idx_operational_event_location_gist
    ON operational_event
    USING gist (location);

CREATE INDEX idx_operational_event_zone_geometry_gist
    ON operational_event
    USING gist (zone_geometry)
    WHERE zone_geometry IS NOT NULL;

CREATE INDEX idx_operational_event_active_window_gist
    ON operational_event
    USING gist (
        tstzrange(started_at, COALESCE(ended_at, 'infinity'::timestamptz), '[]')
    );

CREATE INDEX idx_operational_event_status_history_event_changed_at_desc
    ON operational_event_status_history (event_id, changed_at DESC);

CREATE INDEX idx_security_audit_log_occurred_event_outcome
    ON security_audit_log (occurred_at DESC, event_type, outcome);

CREATE INDEX idx_security_audit_log_principal_occurred_at_desc
    ON security_audit_log (principal_id, occurred_at DESC)
    WHERE principal_id IS NOT NULL;

CREATE INDEX idx_metric_code_calculated_at_desc
    ON metric (code, calculated_at DESC);

CREATE INDEX idx_metric_scope_code_calculated_at_desc
    ON metric (scope_infrastructure_object_id, code, calculated_at DESC)
    WHERE scope_infrastructure_object_id IS NOT NULL;

CREATE INDEX idx_staff_aggregate_period_dimension
    ON staff_aggregate (period_month, dimension_type);

CREATE INDEX idx_staff_aggregate_scope_period_dimension
    ON staff_aggregate (scope_infrastructure_object_id, period_month, dimension_type)
    WHERE scope_infrastructure_object_id IS NOT NULL;

CREATE UNIQUE INDEX ux_simulation_profile_single_default
    ON simulation_profile (is_default)
    WHERE is_default;
