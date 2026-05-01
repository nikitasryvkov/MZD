package ru.mzd.geoanalytics.dashboard.dashboard.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;

public record DashboardQuery(
    BoundingBox boundingBox,
    LayerFilter layerFilter,
    TimeRange timeRange,
    Set<EventStatus> eventStatuses,
    List<String> departmentCodes,
    boolean includeKpi,
    boolean includePersonnel
) {

    public record BoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
    }

    public record LayerFilter(boolean showStations, boolean showSegments, boolean showTrains, boolean showEvents) {
    }

    public record TimeRange(Instant from, Instant to) {
    }
}
