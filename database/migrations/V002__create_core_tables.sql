SET search_path TO dashboard, public;

CREATE TABLE infrastructure_object (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    object_kind infrastructure_object_kind NOT NULL,
    display_name varchar(255) NOT NULL,
    short_code varchar(64),
    department_code varchar(64),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_infrastructure_object_display_name_not_blank
        CHECK (btrim(display_name) <> ''),
    CONSTRAINT chk_infrastructure_object_short_code_not_blank
        CHECK (short_code IS NULL OR btrim(short_code) <> ''),
    CONSTRAINT chk_infrastructure_object_department_code_not_blank
        CHECK (department_code IS NULL OR btrim(department_code) <> '')
);

CREATE TABLE station (
    id uuid PRIMARY KEY
        REFERENCES infrastructure_object (id)
        ON DELETE CASCADE,
    code varchar(32) NOT NULL,
    station_type varchar(32) NOT NULL,
    order_index integer,
    location geometry(Point, 4326) NOT NULL,
    CONSTRAINT uq_station_code UNIQUE (code),
    CONSTRAINT chk_station_code_not_blank
        CHECK (btrim(code) <> ''),
    CONSTRAINT chk_station_type_not_blank
        CHECK (btrim(station_type) <> ''),
    CONSTRAINT chk_station_order_index_non_negative
        CHECK (order_index IS NULL OR order_index >= 0),
    CONSTRAINT chk_station_location_not_empty
        CHECK (NOT ST_IsEmpty(location)),
    CONSTRAINT chk_station_location_longitude
        CHECK (ST_X(location) BETWEEN -180 AND 180),
    CONSTRAINT chk_station_location_latitude
        CHECK (ST_Y(location) BETWEEN -90 AND 90)
);

CREATE TABLE route_segment (
    id uuid PRIMARY KEY
        REFERENCES infrastructure_object (id)
        ON DELETE CASCADE,
    from_station_id uuid NOT NULL
        REFERENCES station (id)
        ON DELETE RESTRICT,
    to_station_id uuid NOT NULL
        REFERENCES station (id)
        ON DELETE RESTRICT,
    length_km numeric(8, 3) NOT NULL,
    geometry geometry(LineString, 4326) NOT NULL,
    status varchar(32) NOT NULL,
    CONSTRAINT chk_route_segment_distinct_stations
        CHECK (from_station_id <> to_station_id),
    CONSTRAINT chk_route_segment_length_positive
        CHECK (length_km > 0),
    CONSTRAINT chk_route_segment_status_not_blank
        CHECK (btrim(status) <> ''),
    CONSTRAINT chk_route_segment_geometry_not_empty
        CHECK (NOT ST_IsEmpty(geometry)),
    CONSTRAINT chk_route_segment_geometry_min_points
        CHECK (ST_NPoints(geometry) >= 2)
);

CREATE TABLE route (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(32) NOT NULL,
    name varchar(255) NOT NULL,
    direction varchar(32),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_route_code UNIQUE (code),
    CONSTRAINT chk_route_code_not_blank
        CHECK (btrim(code) <> ''),
    CONSTRAINT chk_route_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_route_direction_not_blank
        CHECK (direction IS NULL OR btrim(direction) <> '')
);

CREATE TABLE route_station_link (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id uuid NOT NULL
        REFERENCES route (id)
        ON DELETE CASCADE,
    station_id uuid NOT NULL
        REFERENCES station (id)
        ON DELETE RESTRICT,
    sequence_no integer NOT NULL,
    stop_role varchar(32),
    added_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_route_station_link_route_sequence
        UNIQUE (route_id, sequence_no),
    CONSTRAINT chk_route_station_link_sequence_positive
        CHECK (sequence_no > 0),
    CONSTRAINT chk_route_station_link_stop_role_not_blank
        CHECK (stop_role IS NULL OR btrim(stop_role) <> '')
);

CREATE TABLE rolling_stock_unit (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    train_number varchar(32) NOT NULL,
    route_id uuid NOT NULL
        REFERENCES route (id)
        ON DELETE RESTRICT,
    current_station_id uuid
        REFERENCES station (id)
        ON DELETE RESTRICT,
    next_station_id uuid
        REFERENCES station (id)
        ON DELETE RESTRICT,
    current_position geometry(Point, 4326),
    progress_percent numeric(5, 2),
    speed_kmh numeric(6, 2),
    status varchar(32) NOT NULL,
    last_updated timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_rolling_stock_unit_train_number UNIQUE (train_number),
    CONSTRAINT chk_rolling_stock_unit_train_number_not_blank
        CHECK (btrim(train_number) <> ''),
    CONSTRAINT chk_rolling_stock_unit_progress_percent_range
        CHECK (
            progress_percent IS NULL
            OR (progress_percent >= 0 AND progress_percent <= 100)
        ),
    CONSTRAINT chk_rolling_stock_unit_speed_kmh_non_negative
        CHECK (speed_kmh IS NULL OR speed_kmh >= 0),
    CONSTRAINT chk_rolling_stock_unit_status_not_blank
        CHECK (btrim(status) <> ''),
    CONSTRAINT chk_rolling_stock_unit_distinct_stations
        CHECK (
            current_station_id IS NULL
            OR next_station_id IS NULL
            OR current_station_id <> next_station_id
        ),
    CONSTRAINT chk_rolling_stock_unit_current_position_not_empty
        CHECK (
            current_position IS NULL
            OR NOT ST_IsEmpty(current_position)
        ),
    CONSTRAINT chk_rolling_stock_unit_current_position_longitude
        CHECK (
            current_position IS NULL
            OR ST_X(current_position) BETWEEN -180 AND 180
        ),
    CONSTRAINT chk_rolling_stock_unit_current_position_latitude
        CHECK (
            current_position IS NULL
            OR ST_Y(current_position) BETWEEN -90 AND 90
        )
);

CREATE TABLE rolling_stock_position (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    rolling_stock_id uuid NOT NULL
        REFERENCES rolling_stock_unit (id)
        ON DELETE CASCADE,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    position geometry(Point, 4326) NOT NULL,
    speed_kmh numeric(6, 2),
    CONSTRAINT chk_rolling_stock_position_speed_kmh_non_negative
        CHECK (speed_kmh IS NULL OR speed_kmh >= 0),
    CONSTRAINT chk_rolling_stock_position_not_empty
        CHECK (NOT ST_IsEmpty(position)),
    CONSTRAINT chk_rolling_stock_position_longitude
        CHECK (ST_X(position) BETWEEN -180 AND 180),
    CONSTRAINT chk_rolling_stock_position_latitude
        CHECK (ST_Y(position) BETWEEN -90 AND 90)
);
