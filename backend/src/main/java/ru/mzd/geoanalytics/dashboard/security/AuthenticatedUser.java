package ru.mzd.geoanalytics.dashboard.security;

import java.util.Set;

public record AuthenticatedUser(
    String principalId,
    Set<String> authorities
) {
}
