package ru.mzd.geoanalytics.dashboard.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthorityExtractor implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        extractScopes(jwt).forEach(scope -> authorities.add(new SimpleGrantedAuthority(scope)));
        extractRoles(jwt).forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<String> extractScopes(Jwt jwt) {
        List<String> scopes = new ArrayList<>();

        Object scopeClaim = jwt.getClaims().get("scope");
        if (scopeClaim instanceof String scopeString && !scopeString.isBlank()) {
            for (String scope : scopeString.split(" ")) {
                if (!scope.isBlank()) {
                    scopes.add("SCOPE_" + scope.trim());
                }
            }
        }

        Object scpClaim = jwt.getClaims().get("scp");
        if (scpClaim instanceof Collection<?> collection) {
            collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(scope -> !scope.isBlank())
                .map(scope -> "SCOPE_" + scope)
                .forEach(scopes::add);
        }

        return scopes;
    }

    private Collection<String> extractRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        Object rolesClaim = jwt.getClaims().get("roles");
        addRoles(rolesClaim, roles);

        Object realmAccessClaim = jwt.getClaims().get("realm_access");
        if (realmAccessClaim instanceof Map<?, ?> realmAccess) {
            addRoles(realmAccess.get("roles"), roles);
        }

        Object resourceAccessClaim = jwt.getClaims().get("resource_access");
        if (resourceAccessClaim instanceof Map<?, ?> resourceAccess) {
            for (Object clientAccess : resourceAccess.values()) {
                if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                    addRoles(clientAccessMap.get("roles"), roles);
                }
            }
        }

        return roles;
    }

    private void addRoles(Object rolesClaim, Set<String> roles) {
        if (rolesClaim instanceof Collection<?> collection) {
            collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> !role.isBlank())
                .map(this::normalizeRole)
                .forEach(roles::add);
        }
    }

    private String normalizeRole(String rawRole) {
        String trimmedRole = rawRole.trim();
        return trimmedRole.startsWith("ROLE_") ? trimmedRole : "ROLE_" + trimmedRole;
    }
}
