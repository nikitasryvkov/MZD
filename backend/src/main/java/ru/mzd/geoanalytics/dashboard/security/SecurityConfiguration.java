package ru.mzd.geoanalytics.dashboard.security;

import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ApplicationProperties applicationProperties,
        JwtAuthorityExtractor jwtAuthorityExtractor,
        JsonAuthenticationEntryPoint authenticationEntryPoint,
        JsonAccessDeniedHandler accessDeniedHandler,
        LocalDevelopmentAuthenticationFilter localDevelopmentAuthenticationFilter,
        SecurityAuditRequestFilter securityAuditRequestFilter
    ) throws Exception {
        http
            // The application uses stateless bearer/local-dev authentication and does not rely on cookies.
            // Keeping CSRF enabled blocks legitimate POST/PATCH dashboard API calls from the SPA.
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/actuator/prometheus")
                .hasAnyAuthority(applicationProperties.getSecurity().getObservabilityAuthorities().toArray(String[]::new))
                .anyRequest().authenticated()
            );

        if (applicationProperties.getSecurity().isEnabled()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthorityExtractor)));
            http.addFilterAfter(securityAuditRequestFilter, BearerTokenAuthenticationFilter.class);
        } else {
            http.addFilterBefore(localDevelopmentAuthenticationFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(securityAuditRequestFilter, LocalDevelopmentAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true")
    public JwtDecoder jwtDecoder(ApplicationProperties applicationProperties) {
        String jwkSetUri = trimToNull(applicationProperties.getSecurity().getJwkSetUri());
        String issuerUri = trimToNull(applicationProperties.getSecurity().getIssuerUri());
        NimbusJwtDecoder jwtDecoder;
        OAuth2TokenValidator<Jwt> validator;

        if (jwkSetUri != null) {
            jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            validator = issuerUri != null
                ? JwtValidators.createDefaultWithIssuer(issuerUri)
                : JwtValidators.createDefault();
        } else if (issuerUri != null) {
            jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
            validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        } else {
            throw new IllegalStateException("Проверка JWT включена, но не настроен issuer URI или JWKS URI.");
        }

        if (!applicationProperties.getSecurity().getExpectedAudiences().isEmpty()) {
            validator = new DelegatingOAuth2TokenValidator<>(
                validator,
                new JwtAudienceValidator(Set.copyOf(applicationProperties.getSecurity().getExpectedAudiences()))
            );
        }

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
