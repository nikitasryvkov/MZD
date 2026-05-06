package ru.mzd.geoanalytics.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;

class AuthenticatedUserProviderTest {

    private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider(new ApplicationProperties());

    @Test
    void monitoringUserCanReadEventsButCannotManageThem() {
        var authentication = authentication("ROLE_MONITORING_USER");

        assertThat(provider.hasEventAccess(authentication)).isTrue();
        assertThat(provider.hasAdminAccess(authentication)).isFalse();
    }

    @Test
    void adminCanManageEvents() {
        var authentication = authentication("ROLE_ADMIN");

        assertThat(provider.hasEventAccess(authentication)).isTrue();
        assertThat(provider.hasAdminAccess(authentication)).isTrue();
    }

    private UsernamePasswordAuthenticationToken authentication(String authority) {
        return new UsernamePasswordAuthenticationToken(
            "user",
            "N/A",
            List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
