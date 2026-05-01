package ru.mzd.geoanalytics.dashboard.generator.infrastructure;

import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.dashboard.api.DashboardApiModels;
import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;
import ru.mzd.geoanalytics.dashboard.generator.application.port.TrainStreamingPort;
import ru.mzd.geoanalytics.dashboard.streaming.DashboardStreamingGateway;

@Component
public class TrainStreamingAdapter implements TrainStreamingPort {

    private final DashboardStreamingGateway dashboardStreamingGateway;

    public TrainStreamingAdapter(DashboardStreamingGateway dashboardStreamingGateway) {
        this.dashboardStreamingGateway = dashboardStreamingGateway;
    }

    @Override
    public void publishTrainUpsert(TrainProjection trainProjection) {
        dashboardStreamingGateway.publishTrainUpsert(new DashboardApiModels.TrainResponse(
            trainProjection.id(),
            trainProjection.trainNumber(),
            trainProjection.latitude(),
            trainProjection.longitude(),
            trainProjection.status(),
            trainProjection.currentStationId(),
            trainProjection.nextStationId(),
            trainProjection.progressPercent(),
            trainProjection.speedKmh(),
            trainProjection.lastUpdated()
        ));
    }
}
