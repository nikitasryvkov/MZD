package ru.mzd.geoanalytics.dashboard.dashboard.api;

import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;
import ru.mzd.geoanalytics.dashboard.common.web.RequestContext;
import ru.mzd.geoanalytics.dashboard.dashboard.application.DashboardQueryService;
import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;
import ru.mzd.geoanalytics.dashboard.dashboard.domain.DashboardQuery;
import ru.mzd.geoanalytics.dashboard.security.AuthenticatedUserProvider;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;
    private final DashboardApiMapper dashboardApiMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ApplicationProperties applicationProperties;
    private final Clock clock;

    public DashboardController(
        DashboardQueryService dashboardQueryService,
        DashboardApiMapper dashboardApiMapper,
        AuthenticatedUserProvider authenticatedUserProvider,
        ApplicationProperties applicationProperties,
        Clock clock
    ) {
        this.dashboardQueryService = dashboardQueryService;
        this.dashboardApiMapper = dashboardApiMapper;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.applicationProperties = applicationProperties;
        this.clock = clock;
    }

    @PostMapping("/query")
    @PreAuthorize("@authenticatedUserProvider.hasDashboardAccess(authentication)")
    public DashboardApiModels.DashboardQueryResponse queryDashboardSnapshot(
        @Valid @RequestBody DashboardApiModels.DashboardQueryRequest request,
        Authentication authentication
    ) {
        DashboardQuery query = dashboardApiMapper.toDomainQuery(request);
        validatePersonnelAccess(query, authentication);

        DashboardViewModels.DashboardSnapshotView snapshot = dashboardQueryService.query(query);
        return dashboardApiMapper.toApiResponse(
            RequestContext.currentRequestId(),
            Instant.now(clock),
            snapshot
        );
    }

    private void validatePersonnelAccess(DashboardQuery query, Authentication authentication) {
        if (query.includePersonnel()
            && !authenticatedUserProvider.hasAnyAuthority(authentication, applicationProperties.getSecurity().getPersonnelAuthorities())) {
            throw new AccessDeniedException("Для кадровой аналитики требуются дополнительные права.");
        }
    }
}
