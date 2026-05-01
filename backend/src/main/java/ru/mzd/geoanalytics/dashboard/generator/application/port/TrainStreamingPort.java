package ru.mzd.geoanalytics.dashboard.generator.application.port;

import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;

public interface TrainStreamingPort {

    void publishTrainUpsert(TrainProjection trainProjection);
}
