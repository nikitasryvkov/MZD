package ru.mzd.geoanalytics.dashboard.generator.application.port;

import ru.mzd.geoanalytics.dashboard.generator.application.model.TrainProjection;
import ru.mzd.geoanalytics.dashboard.generator.domain.GeneratorModels;

public interface GeneratorPersistencePort {

    GeneratorModels.ReferenceNetwork loadReferenceNetwork();

    java.util.List<GeneratorModels.ActiveEvent> loadActiveEvents();

    TrainProjection upsertTrain(GeneratorModels.TrainUpsertCommand command);

    void upsertEvent(GeneratorModels.EventUpsertCommand command);

    void replacePersonnelSnapshot(GeneratorModels.PersonnelSnapshotCommand command);
}
