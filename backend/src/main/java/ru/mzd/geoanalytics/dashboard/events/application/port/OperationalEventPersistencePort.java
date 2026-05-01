package ru.mzd.geoanalytics.dashboard.events.application.port;

import java.util.Optional;
import java.util.UUID;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventDetailsView;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventProjection;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventAggregate;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusChange;
import ru.mzd.geoanalytics.dashboard.events.domain.OperationalEventStatusUpdateResult;

public interface OperationalEventPersistencePort {

    Optional<OperationalEventAggregate> findAggregateForUpdate(UUID eventId);

    Optional<OperationalEventDetailsView> findDetails(UUID eventId);

    OperationalEventStatusUpdateResult applyStatusChange(OperationalEventStatusChange statusChange);

    Optional<OperationalEventProjection> findEventProjection(UUID eventId);
}
