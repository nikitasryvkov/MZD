package ru.mzd.geoanalytics.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthorityExtractorTest {

    private final JwtAuthorityExtractor extractor = new JwtAuthorityExtractor();

    @Test
    void extractsKeycloakRealmAndClientRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claim("preferred_username", "viewer")
            .claim("realm_access", Map.of("roles", List.of("MONITORING_USER")))
            .claim("resource_access", Map.of(
                "mzd-dashboard-spa",
                Map.of("roles", List.of("ADMIN"))
            ))
            .claim("scope", "openid profile")
            .build();

        var authentication = extractor.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("viewer");
        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_ADMIN", "ROLE_MONITORING_USER", "SCOPE_openid", "SCOPE_profile");
    }
}
