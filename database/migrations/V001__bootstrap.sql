CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SCHEMA IF NOT EXISTS dashboard;

CREATE TYPE dashboard.infrastructure_object_kind AS ENUM (
    'STATION',
    'ROUTE_SEGMENT'
);

CREATE TYPE dashboard.event_status AS ENUM (
    'REGISTERED',
    'IN_PROGRESS',
    'RESOLVED',
    'CANCELED'
);
