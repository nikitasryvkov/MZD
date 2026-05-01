package ru.mzd.geoanalytics.dashboard.events.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.mzd.geoanalytics.dashboard.common.exception.ResourceNotFoundException;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventDetailsView;
import ru.mzd.geoanalytics.dashboard.events.application.port.OperationalEventPersistencePort;

@Service
public class OperationalEventDetailsService {

    private final OperationalEventPersistencePort persistencePort;

    public OperationalEventDetailsService(OperationalEventPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public OperationalEventDetailsView getDetails(UUID eventId) {
        return persistencePort.findDetails(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("РћРїРµСЂР°С‚РёРІРЅРѕРµ СЃРѕР±С‹С‚РёРµ РЅРµ РЅР°Р№РґРµРЅРѕ."));
    }
}
