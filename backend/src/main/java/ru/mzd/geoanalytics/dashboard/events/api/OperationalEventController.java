package ru.mzd.geoanalytics.dashboard.events.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mzd.geoanalytics.dashboard.events.application.OperationalEventDetailsService;
import ru.mzd.geoanalytics.dashboard.events.application.UpdateOperationalEventStatusService;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventDetailsView;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateCommand;
import ru.mzd.geoanalytics.dashboard.events.application.model.OperationalEventStatusUpdateView;
import ru.mzd.geoanalytics.dashboard.events.domain.EventStatus;
import ru.mzd.geoanalytics.dashboard.security.AuthenticatedUserProvider;

@RestController
@RequestMapping("/api/v1/events")
public class OperationalEventController {

    private final OperationalEventDetailsService operationalEventDetailsService;
    private final UpdateOperationalEventStatusService updateOperationalEventStatusService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public OperationalEventController(
        OperationalEventDetailsService operationalEventDetailsService,
        UpdateOperationalEventStatusService updateOperationalEventStatusService,
        AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.operationalEventDetailsService = operationalEventDetailsService;
        this.updateOperationalEventStatusService = updateOperationalEventStatusService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("@authenticatedUserProvider.hasEventAccess(authentication)")
    public OperationalEventApiModels.OperationalEventDetailsResponse getOperationalEventDetails(
        @PathVariable UUID eventId,
        Authentication authentication
    ) {
        return toApiResponse(operationalEventDetailsService.getDetails(eventId));
    }

    @PatchMapping("/{eventId}/status")
    @PreAuthorize("@authenticatedUserProvider.hasAdminAccess(authentication)")
    public OperationalEventApiModels.UpdateOperationalEventStatusResponse updateOperationalEventStatus(
        @PathVariable UUID eventId,
        @Valid @RequestBody OperationalEventApiModels.UpdateOperationalEventStatusRequest request,
        Authentication authentication
    ) {
        return toApiResponse(updateOperationalEventStatusService.updateStatus(
            eventId,
            new OperationalEventStatusUpdateCommand(
                EventStatus.valueOf(request.newStatus()),
                request.comment(),
                authenticatedUserProvider.getRequiredUser(authentication).principalId()
            )
        ));
    }

    private OperationalEventApiModels.OperationalEventDetailsResponse toApiResponse(OperationalEventDetailsView detailsView) {
        return new OperationalEventApiModels.OperationalEventDetailsResponse(
            detailsView.id(),
            detailsView.type(),
            detailsView.title(),
            detailsView.description(),
            detailsView.status().name(),
            detailsView.severity(),
            detailsView.latitude(),
            detailsView.longitude(),
            detailsView.affectedObjectId(),
            detailsView.affectedSection(),
            detailsView.startedAt(),
            detailsView.endedAt(),
            detailsView.updatedAt(),
            detailsView.lastChangedBy(),
            detailsView.allowedTransitions().stream().map(Enum::name).toList(),
            detailsView.statusHistory().stream()
                .map(item -> new OperationalEventApiModels.EventStatusHistoryItemResponse(
                    item.id(),
                    item.fromStatus() != null ? item.fromStatus().name() : null,
                    item.toStatus().name(),
                    item.comment(),
                    item.changedAt(),
                    item.changedBy()
                ))
                .toList()
        );
    }

    private OperationalEventApiModels.UpdateOperationalEventStatusResponse toApiResponse(
        OperationalEventStatusUpdateView updateView
    ) {
        return new OperationalEventApiModels.UpdateOperationalEventStatusResponse(
            updateView.eventId(),
            updateView.status().name(),
            updateView.updatedAt(),
            updateView.affectedSection(),
            updateView.summary(),
            updateView.allowedTransitions().stream().map(Enum::name).toList()
        );
    }
}
