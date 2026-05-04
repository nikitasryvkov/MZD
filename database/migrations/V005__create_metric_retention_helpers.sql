SET search_path TO dashboard, public;

CREATE FUNCTION prune_metric(
    p_retain_for interval DEFAULT interval '30 days'
)
RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE
    deleted_rows bigint;
BEGIN
    DELETE FROM metric
    WHERE calculated_at < now() - p_retain_for;

    GET DIAGNOSTICS deleted_rows = ROW_COUNT;
    RETURN deleted_rows;
END;
$$;

CREATE INDEX idx_rolling_stock_position_recorded_at
    ON rolling_stock_position (recorded_at);

CREATE INDEX idx_metric_calculated_at
    ON metric (calculated_at);
