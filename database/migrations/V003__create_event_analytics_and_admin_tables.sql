SET search_path TO dashboard, public;

CREATE TABLE event_status_transition (
    from_status event_status NOT NULL,
    to_status event_status NOT NULL,
    CONSTRAINT pk_event_status_transition PRIMARY KEY (from_status, to_status),
    CONSTRAINT chk_event_status_transition_distinct
        CHECK (from_status <> to_status)
);

INSERT INTO event_status_transition (from_status, to_status)
VALUES
    ('REGISTERED', 'IN_PROGRESS'),
    ('REGISTERED', 'CANCELED'),
    ('IN_PROGRESS', 'RESOLVED'),
    ('IN_PROGRESS', 'CANCELED');

CREATE TABLE operational_event (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type varchar(32) NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    severity varchar(16) NOT NULL,
    status event_status NOT NULL DEFAULT 'REGISTERED',
    affected_infrastructure_object_id uuid
        REFERENCES infrastructure_object (id)
        ON DELETE SET NULL,
    department_code varchar(64),
    location geometry(Point, 4326) NOT NULL,
    zone_geometry geometry(Polygon, 4326),
    started_at timestamptz,
    ended_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    last_changed_by varchar(128),
    is_active boolean GENERATED ALWAYS AS (
        status IN ('REGISTERED', 'IN_PROGRESS')
    ) STORED,
    CONSTRAINT chk_operational_event_event_type_not_blank
        CHECK (btrim(event_type) <> ''),
    CONSTRAINT chk_operational_event_title_not_blank
        CHECK (btrim(title) <> ''),
    CONSTRAINT chk_operational_event_severity_not_blank
        CHECK (btrim(severity) <> ''),
    CONSTRAINT chk_operational_event_department_code_not_blank
        CHECK (department_code IS NULL OR btrim(department_code) <> ''),
    CONSTRAINT chk_operational_event_last_changed_by_not_blank
        CHECK (last_changed_by IS NULL OR btrim(last_changed_by) <> ''),
    CONSTRAINT chk_operational_event_time_window
        CHECK (started_at IS NULL OR ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT chk_operational_event_location_not_empty
        CHECK (NOT ST_IsEmpty(location)),
    CONSTRAINT chk_operational_event_location_longitude
        CHECK (ST_X(location) BETWEEN -180 AND 180),
    CONSTRAINT chk_operational_event_location_latitude
        CHECK (ST_Y(location) BETWEEN -90 AND 90),
    CONSTRAINT chk_operational_event_zone_geometry_not_empty
        CHECK (zone_geometry IS NULL OR NOT ST_IsEmpty(zone_geometry))
);

CREATE TABLE operational_event_status_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id uuid NOT NULL
        REFERENCES operational_event (id)
        ON DELETE CASCADE,
    from_status event_status,
    to_status event_status NOT NULL,
    comment varchar(1000),
    changed_at timestamptz NOT NULL DEFAULT now(),
    changed_by varchar(128),
    CONSTRAINT chk_operational_event_status_history_transition
        CHECK (from_status IS NULL OR from_status <> to_status),
    CONSTRAINT chk_operational_event_status_history_comment_not_blank
        CHECK (comment IS NULL OR btrim(comment) <> ''),
    CONSTRAINT chk_operational_event_status_history_changed_by_not_blank
        CHECK (changed_by IS NULL OR btrim(changed_by) <> '')
);

CREATE TABLE security_audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    principal_id varchar(128),
    event_type varchar(64) NOT NULL,
    outcome varchar(16) NOT NULL,
    source_ip inet,
    target_resource varchar(255),
    request_id uuid,
    trace_id varchar(64),
    details_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_security_audit_log_principal_id_not_blank
        CHECK (principal_id IS NULL OR btrim(principal_id) <> ''),
    CONSTRAINT chk_security_audit_log_event_type_not_blank
        CHECK (btrim(event_type) <> ''),
    CONSTRAINT chk_security_audit_log_outcome_not_blank
        CHECK (btrim(outcome) <> ''),
    CONSTRAINT chk_security_audit_log_target_resource_not_blank
        CHECK (target_resource IS NULL OR btrim(target_resource) <> ''),
    CONSTRAINT chk_security_audit_log_trace_id_not_blank
        CHECK (trace_id IS NULL OR btrim(trace_id) <> ''),
    CONSTRAINT chk_security_audit_log_details_json_object
        CHECK (jsonb_typeof(details_json) = 'object')
);

CREATE TABLE metric (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    value numeric(14, 2) NOT NULL,
    period_start timestamptz,
    period_end timestamptz,
    calculated_at timestamptz NOT NULL DEFAULT now(),
    scope_infrastructure_object_id uuid
        REFERENCES infrastructure_object (id)
        ON DELETE SET NULL,
    CONSTRAINT uq_metric_code_calculated_scope
        UNIQUE NULLS NOT DISTINCT (code, calculated_at, scope_infrastructure_object_id),
    CONSTRAINT chk_metric_code_not_blank
        CHECK (btrim(code) <> ''),
    CONSTRAINT chk_metric_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_metric_period
        CHECK (period_start IS NULL OR period_end IS NULL OR period_end >= period_start)
);

CREATE TABLE staff_aggregate (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    period_month date NOT NULL,
    dimension_type varchar(32) NOT NULL,
    total_headcount integer NOT NULL,
    scope_infrastructure_object_id uuid
        REFERENCES infrastructure_object (id)
        ON DELETE SET NULL,
    calculated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_staff_aggregate_period_dimension_scope
        UNIQUE NULLS NOT DISTINCT (
            period_month,
            dimension_type,
            scope_infrastructure_object_id
        ),
    CONSTRAINT chk_staff_aggregate_period_month
        CHECK (period_month = date_trunc('month', period_month)::date),
    CONSTRAINT chk_staff_aggregate_dimension_type_not_blank
        CHECK (btrim(dimension_type) <> ''),
    CONSTRAINT chk_staff_aggregate_total_headcount_non_negative
        CHECK (total_headcount >= 0)
);

CREATE TABLE staff_aggregate_item (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_aggregate_id uuid NOT NULL
        REFERENCES staff_aggregate (id)
        ON DELETE CASCADE,
    dimension_key varchar(64) NOT NULL,
    headcount integer NOT NULL,
    change_percent numeric(6, 2),
    CONSTRAINT uq_staff_aggregate_item_key
        UNIQUE (staff_aggregate_id, dimension_key),
    CONSTRAINT chk_staff_aggregate_item_dimension_key_not_blank
        CHECK (btrim(dimension_key) <> ''),
    CONSTRAINT chk_staff_aggregate_item_headcount_non_negative
        CHECK (headcount >= 0)
);

CREATE TABLE simulation_profile (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_name varchar(64) NOT NULL,
    tick_interval_seconds integer NOT NULL,
    train_count integer NOT NULL,
    event_generation_intensity numeric(8, 3) NOT NULL,
    is_default boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_simulation_profile_name UNIQUE (profile_name),
    CONSTRAINT chk_simulation_profile_name_not_blank
        CHECK (btrim(profile_name) <> ''),
    CONSTRAINT chk_simulation_profile_tick_interval_positive
        CHECK (tick_interval_seconds > 0),
    CONSTRAINT chk_simulation_profile_train_count_non_negative
        CHECK (train_count >= 0),
    CONSTRAINT chk_simulation_profile_event_generation_intensity_non_negative
        CHECK (event_generation_intensity >= 0)
);
