package ru.mzd.geoanalytics.generator.common.domain;

public final class GeoMath {

    private GeoMath() {
    }

    public static double interpolate(double start, double end, double factor) {
        return start + ((end - start) * factor);
    }

    public static double distanceKm(
        double fromLatitude,
        double fromLongitude,
        double toLatitude,
        double toLongitude
    ) {
        double latDiff = toRadians(toLatitude - fromLatitude);
        double lonDiff = toRadians(toLongitude - fromLongitude);
        double originLat = toRadians(fromLatitude);
        double destinationLat = toRadians(toLatitude);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
            + Math.cos(originLat) * Math.cos(destinationLat)
            * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private static double toRadians(double value) {
        return Math.toRadians(value);
    }
}
