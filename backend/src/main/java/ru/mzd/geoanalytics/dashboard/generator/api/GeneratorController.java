package ru.mzd.geoanalytics.dashboard.generator.api;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mzd.geoanalytics.dashboard.generator.application.GeneratorIngestionService;
import ru.mzd.geoanalytics.dashboard.generator.application.GeneratorReferenceQueryService;
import ru.mzd.geoanalytics.dashboard.generator.application.model.GeneratorIngestionModels;
import ru.mzd.geoanalytics.dashboard.security.AuthenticatedUserProvider;

@RestController
@RequestMapping("/api/internal/v1/generator")
public class GeneratorController {

    private final GeneratorReferenceQueryService generatorReferenceQueryService;
    private final GeneratorIngestionService generatorIngestionService;
    private final GeneratorApiMapper generatorApiMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public GeneratorController(
        GeneratorReferenceQueryService generatorReferenceQueryService,
        GeneratorIngestionService generatorIngestionService,
        GeneratorApiMapper generatorApiMapper,
        AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.generatorReferenceQueryService = generatorReferenceQueryService;
        this.generatorIngestionService = generatorIngestionService;
        this.generatorApiMapper = generatorApiMapper;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/reference-network")
    @PreAuthorize("@authenticatedUserProvider.hasGeneratorAccess(authentication)")
    public GeneratorApiModels.ReferenceNetworkResponse loadReferenceNetwork() {
        return generatorApiMapper.toApiResponse(generatorReferenceQueryService.loadReferenceNetwork());
    }

    @GetMapping("/active-events")
    @PreAuthorize("@authenticatedUserProvider.hasGeneratorAccess(authentication)")
    public java.util.List<GeneratorApiModels.ActiveEventResponse> loadActiveEvents() {
        return generatorApiMapper.toActiveEventResponses(generatorReferenceQueryService.loadActiveEvents());
    }

    @PostMapping("/trains/sync")
    @PreAuthorize("@authenticatedUserProvider.hasGeneratorAccess(authentication)")
    public GeneratorApiModels.BatchIngestionResponse syncTrains(
        @Valid @RequestBody GeneratorApiModels.SyncTrainsRequest request,
        Authentication authentication
    ) {
        GeneratorIngestionModels.BatchIngestionView result = generatorIngestionService.syncTrains(
            generatorApiMapper.toCommand(request),
            authenticatedUserProvider.getRequiredUser(authentication).principalId()
        );
        return generatorApiMapper.toApiResponse(result);
    }

    @PostMapping("/events/sync")
    @PreAuthorize("@authenticatedUserProvider.hasGeneratorAccess(authentication)")
    public GeneratorApiModels.BatchIngestionResponse syncEvents(
        @Valid @RequestBody GeneratorApiModels.SyncEventsRequest request,
        Authentication authentication
    ) {
        GeneratorIngestionModels.BatchIngestionView result = generatorIngestionService.syncEvents(
            generatorApiMapper.toCommand(request),
            authenticatedUserProvider.getRequiredUser(authentication).principalId()
        );
        return generatorApiMapper.toApiResponse(result);
    }

    @PostMapping("/personnel/snapshot")
    @PreAuthorize("@authenticatedUserProvider.hasGeneratorAccess(authentication)")
    public GeneratorApiModels.BatchIngestionResponse syncPersonnelSnapshot(
        @Valid @RequestBody GeneratorApiModels.SyncPersonnelSnapshotRequest request,
        Authentication authentication
    ) {
        GeneratorIngestionModels.BatchIngestionView result = generatorIngestionService.syncPersonnelSnapshot(
            generatorApiMapper.toCommand(request),
            authenticatedUserProvider.getRequiredUser(authentication).principalId()
        );
        return generatorApiMapper.toApiResponse(result);
    }
}
