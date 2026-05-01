package ru.mzd.geoanalytics.dashboard.security;

import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
        "invalid_token",
        "The required JWT audience is missing.",
        null
    );

    private final Set<String> expectedAudiences;

    public JwtAudienceValidator(Set<String> expectedAudiences) {
        this.expectedAudiences = expectedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (expectedAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }

        boolean matches = token.getAudience().stream().anyMatch(expectedAudiences::contains);
        return matches
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
