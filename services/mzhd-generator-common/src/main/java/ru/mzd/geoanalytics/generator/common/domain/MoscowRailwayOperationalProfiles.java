package ru.mzd.geoanalytics.generator.common.domain;

import java.time.ZonedDateTime;
import java.util.Map;

public final class MoscowRailwayOperationalProfiles {

    private static final Map<String, RouteOperationalProfile> PROFILES = Map.ofEntries(
        Map.entry("MZD-LEN", new RouteOperationalProfile("MZD-LEN", 5, 82.0, 70, false, 0.55)),
        Map.entry("MZD-YAR", new RouteOperationalProfile("MZD-YAR", 5, 80.0, 70, false, 0.58)),
        Map.entry("MZD-KAZ", new RouteOperationalProfile("MZD-KAZ", 6, 78.0, 75, false, 0.78)),
        Map.entry("MZD-GOR", new RouteOperationalProfile("MZD-GOR", 4, 76.0, 80, false, 0.60)),
        Map.entry("MZD-KUR", new RouteOperationalProfile("MZD-KUR", 5, 79.0, 75, false, 0.74)),
        Map.entry("MZD-KIE", new RouteOperationalProfile("MZD-KIE", 4, 74.0, 85, false, 0.46)),
        Map.entry("MZD-BEL", new RouteOperationalProfile("MZD-BEL", 4, 77.0, 75, false, 0.52)),
        Map.entry("MZD-SAV", new RouteOperationalProfile("MZD-SAV", 3, 72.0, 90, false, 0.40)),
        Map.entry("MZD-RIG", new RouteOperationalProfile("MZD-RIG", 3, 70.0, 90, false, 0.38)),
        Map.entry("MZD-PAV", new RouteOperationalProfile("MZD-PAV", 4, 76.0, 90, false, 0.44)),
        Map.entry("MZD-MCC", new RouteOperationalProfile("MZD-MCC", 6, 68.0, 45, true, 0.82)),
        Map.entry("MZD-BMO", new RouteOperationalProfile("MZD-BMO", 2, 63.0, 95, true, 0.34)),
        Map.entry("MZD-MCD", new RouteOperationalProfile("MZD-MCD", 5, 72.0, 55, false, 0.79))
    );

    private MoscowRailwayOperationalProfiles() {
    }

    public static RouteOperationalProfile profileFor(String routeCode) {
        return PROFILES.getOrDefault(routeCode, new RouteOperationalProfile(routeCode, 3, 70.0, 80, false, 0.45));
    }

    public static double commuterLoadFactor(String routeCode, ZonedDateTime now) {
        RouteOperationalProfile profile = profileFor(routeCode);
        int hour = now.getHour();
        boolean peakHour = (hour >= 7 && hour < 10) || (hour >= 17 && hour < 20);
        boolean night = hour >= 0 && hour < 5;

        if (profile.loopRoute() && peakHour) {
            return 1.22;
        }
        if (routeCode.equals("MZD-BMO") && night) {
            return 0.92;
        }
        if (peakHour && !routeCode.equals("MZD-BMO")) {
            return 1.14;
        }
        if (night) {
            return 0.90;
        }
        return 1.0;
    }

    public static double eventRiskWeight(String routeCode, ZonedDateTime now) {
        double baseRisk = profileFor(routeCode).riskWeight();
        int hour = now.getHour();
        if ((hour >= 7 && hour < 10) || (hour >= 17 && hour < 20)) {
            return baseRisk * 1.25;
        }
        if (hour >= 1 && hour < 5) {
            return baseRisk * 0.85;
        }
        return baseRisk;
    }

    public record RouteOperationalProfile(
        String routeCode,
        int trainCount,
        double cruiseSpeedKmh,
        int dwellSeconds,
        boolean loopRoute,
        double riskWeight
    ) {
    }
}
