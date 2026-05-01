package ru.mzd.geoanalytics.dashboard.events.infrastructure;

import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.dashboard.api.DashboardApiModels;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventStreamingPort;
import ru.mzd.geoanalytics.dashboard.streaming.DashboardStreamingGateway;

@Component
public class OperationalEventStreamingAdapter implements OperationalEventStreamingPort {

    private final DashboardStreamingGateway dashboardStreamingGateway;

    public OperationalEventStreamingAdapter(DashboardStreamingGateway dashboardStreamingGateway) {
        this.dashboardStreamingGateway = dashboardStreamingGateway;
    }

    @Override
    public void publishEventUpsert(OperationalEventProjection eventProjection) {
        dashboardStreamingGateway.publishEventUpsert(new DashboardApiModels.OperationalEventResponse(
            eventProjection.id(),
            eventProjection.title(),
            eventProjection.status().name(),
            eventProjection.severity(),
            eventProjection.latitude(),
            eventProjection.longitude(),
            eventProjection.affectedObjectId(),
            eventProjection.affectedSection(),
            eventProjection.startedAt(),
            eventProjection.updatedAt()
        ));
    }
}
