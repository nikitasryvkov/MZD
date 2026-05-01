package ru.mzd.geoanalytics.dashboard.dashboard.application.port;

import java.util.List;
import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;
import ru.mzd.geoanalytics.dashboard.dashboard.domain.DashboardQuery;

public interface DashboardReadPort {

    DashboardViewModels.MapDataView loadMapData(DashboardQuery query);

    List<DashboardViewModels.EventPreviewView> loadEventsPreview(DashboardQuery query);

    DashboardViewModels.KpiSummaryView loadKpiSummary(DashboardQuery query);

    DashboardViewModels.PersonnelSummaryView loadPersonnelSummary(DashboardQuery query);
}
