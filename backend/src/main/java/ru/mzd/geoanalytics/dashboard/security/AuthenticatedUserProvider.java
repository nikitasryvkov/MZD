package ru.mzd.geoanalytics.dashboard.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;

@Component
public class AuthenticatedUserProvider {

    private final ApplicationProperties applicationProperties;

    public AuthenticatedUserProvider(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public AuthenticatedUser getRequiredUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Требуется аутентифицированный пользователь.");
        }

        Set<String> authorities = authentication.getAuthorities()
            .stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .collect(Collectors.toSet());
        return new AuthenticatedUser(resolvePrincipalId(authentication), authorities);
    }

    public boolean hasAnyAuthority(Authentication authentication, Collection<String> requiredAuthorities) {
        if (authentication == null || requiredAuthorities == null || requiredAuthorities.isEmpty()) {
            return false;
        }

        Set<String> currentAuthorities = authentication.getAuthorities()
            .stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .collect(Collectors.toSet());

        return requiredAuthorities.stream().anyMatch(currentAuthorities::contains);
    }

    public boolean hasDashboardAccess(Authentication authentication) {
        return hasAnyAuthority(authentication, applicationProperties.getSecurity().getDashboardAuthorities());
    }

    public boolean hasEventAccess(Authentication authentication) {
        return hasAnyAuthority(authentication, applicationProperties.getSecurity().getEventAuthorities());
    }

    public boolean hasGeneratorAccess(Authentication authentication) {
        return hasAnyAuthority(authentication, applicationProperties.getSecurity().getGeneratorAuthorities());
    }

    private String resolvePrincipalId(Principal principal) {
        if (principal instanceof AbstractAuthenticationToken authenticationToken) {
            Object principalObject = authenticationToken.getPrincipal();
            if (principalObject instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                return jwt.getClaimAsString("preferred_username") != null
                    ? jwt.getClaimAsString("preferred_username")
                    : jwt.getSubject();
            }
        }

        return principal.getName();
    }
}
