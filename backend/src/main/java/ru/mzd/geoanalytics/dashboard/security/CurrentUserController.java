package ru.mzd.geoanalytics.dashboard.security;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ApplicationProperties applicationProperties;

    public CurrentUserController(
        AuthenticatedUserProvider authenticatedUserProvider,
        ApplicationProperties applicationProperties
    ) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.applicationProperties = applicationProperties;
    }

    @GetMapping
    public CurrentUserResponse currentUser(Authentication authentication) {
        AuthenticatedUser user = authenticatedUserProvider.getRequiredUser(authentication);

        return new CurrentUserResponse(
            user.principalId(),
            user.authorities().stream().sorted().toList(),
            new CurrentUserPermissions(
                authenticatedUserProvider.hasAnyAuthority(
                    authentication,
                    applicationProperties.getSecurity().getPersonnelAuthorities()
                ),
                authenticatedUserProvider.hasAnyAuthority(
                    authentication,
                    applicationProperties.getSecurity().getAdminAuthorities()
                )
            )
        );
    }

    public record CurrentUserResponse(
        String principalId,
        List<String> authorities,
        CurrentUserPermissions permissions
    ) {
    }

    public record CurrentUserPermissions(
        boolean canViewPersonnel,
        boolean canManageEvents
    ) {
    }
}
