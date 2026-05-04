package ru.mzd.geoanalytics.dashboard.dashboard.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;
import ru.mzd.geoanalytics.dashboard.dashboard.application.port.DashboardGeoJsonPort;
import ru.mzd.geoanalytics.dashboard.dashboard.application.port.DashboardReadPort;
import ru.mzd.geoanalytics.dashboard.dashboard.domain.DashboardQuery;

@Service
public class DashboardQueryService {

    private final DashboardReadPort dashboardReadPort;
    private final DashboardGeoJsonPort dashboardGeoJsonPort;

    public DashboardQueryService(
        DashboardReadPort dashboardReadPort,
        DashboardGeoJsonPort dashboardGeoJsonPort
    ) {
        this.dashboardReadPort = dashboardReadPort;
        this.dashboardGeoJsonPort = dashboardGeoJsonPort;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardViewModels.DashboardSnapshotView query(DashboardQuery query) {
        DashboardViewModels.MapDataView mapData =
            dashboardGeoJsonPort.enrichMapData(dashboardReadPort.loadMapData(query));

        DashboardViewModels.KpiSummaryView kpiSummary = query.includeKpi()
            ? dashboardReadPort.loadKpiSummary(query)
            : null;
        DashboardViewModels.PersonnelSummaryView personnelSummary = query.includePersonnel()
            ? dashboardReadPort.loadPersonnelSummary(query)
            : null;

        return new DashboardViewModels.DashboardSnapshotView(
            kpiSummary,
            personnelSummary,
            mapData,
            dashboardReadPort.loadEventsPreview(query)
        );
    }
}
