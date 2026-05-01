package ru.mzd.geoanalytics.dashboard.dashboard.application.port;

import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;

public interface DashboardGeoJsonPort {

    DashboardViewModels.MapDataView enrichMapData(DashboardViewModels.MapDataView source);
}
