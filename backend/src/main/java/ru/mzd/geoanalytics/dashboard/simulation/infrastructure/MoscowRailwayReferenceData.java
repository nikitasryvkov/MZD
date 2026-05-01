package ru.mzd.geoanalytics.dashboard.simulation.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

final class MoscowRailwayReferenceData {

    private MoscowRailwayReferenceData() {
    }

    static List<SimulationModels.StationSeed> stations() {
        return List.of(
            station("LNG-VOK", "LNG", "Ленинградский вокзал", "MZD-MSK", "TERMINAL", 1, 55.776261, 37.655328),
            station("YRS-VOK", "YRS", "Ярославский вокзал", "MZD-MSK", "TERMINAL", 2, 55.776389, 37.657222),
            station("KZN-VOK", "KZN", "Казанский вокзал", "MZD-MSK", "TERMINAL", 3, 55.773611, 37.655670),
            station("KUR-VOK", "KUV", "Курский вокзал", "MZD-MSK", "TERMINAL", 4, 55.757500, 37.660556),
            station("KIE-VOK", "KIV", "Киевский вокзал", "MZD-MSK", "TERMINAL", 5, 55.743200, 37.567400),
            station("BLR-VOK", "BLR", "Белорусский вокзал", "MZD-MSK", "TERMINAL", 6, 55.776375, 37.581757),
            station("SAV-VOK", "SAV", "Савёловский вокзал", "MZD-MSK", "TERMINAL", 7, 55.794167, 37.588056),
            station("RIG-VOK", "RIG", "Рижский вокзал", "MZD-MSK", "TERMINAL", 8, 55.792778, 37.632500),
            station("PVL-VOK", "PVL", "Павелецкий вокзал", "MZD-MSK", "TERMINAL", 9, 55.729830, 37.639590),
            station("MYTISHCHI", "MYT", "Мытищи", "MZD-YAR", "HUB", 10, 55.914490, 37.762230),
            station("LYUBERTSY", "LBR", "Люберцы I", "MZD-KZN", "HUB", 11, 55.681667, 37.896944),
            station("OREKHOVO-ZUEVO", "OZH", "Орехово-Зуево", "MZD-GOR", "HUB", 12, 55.795556, 38.976389),
            station("PODOLSK", "PDL", "Подольск", "MZD-KUR", "HUB", 13, 55.432290, 37.564570),
            station("VOSKRESENSK", "VSK", "Воскресенск", "MZD-KZN", "HUB", 14, 55.312778, 38.702778),
            station("TULA", "TUL", "Тула-1-Курская", "MZD-KUR", "HUB", 15, 54.199028, 37.577889),
            station("RYAZAN", "RZN", "Рязань I", "MZD-KZN", "HUB", 16, 54.633611, 39.713056),
            station("VLADIMIR", "VLD", "Владимир", "MZD-GOR", "HUB", 17, 56.129940, 40.420420),
            station("BRYANSK", "BRN", "Брянск-Орловский", "MZD-KIE", "HUB", 18, 53.263056, 34.405556),
            station("SMOLENSK", "SML", "Смоленск-Центральный", "MZD-BEL", "HUB", 19, 54.797778, 32.034444),
            station("KALUGA", "KLG", "Калуга I", "MZD-KIE", "HUB", 20, 54.533611, 36.278611),
            station("TVER", "TVR", "Тверь", "MZD-LEN", "HUB", 21, 56.835830, 35.891390),
            station("DMITROV", "DMT", "Дмитров", "MZD-SAV", "HUB", 22, 56.337510, 37.510810),
            station("VOLOKOLAMSK", "VLK", "Волоколамск", "MZD-RIG", "HUB", 23, 55.993533, 35.936183),
            station("KURSK", "KRS", "Курск", "MZD-KUR", "HUB", 24, 51.750020, 36.227897),
            station("PAVELETS", "PVT", "Павелец-Тульский", "MZD-PAV", "STATION", 25, 53.787000, 39.240000),
            station("ODINTSOVO", "ODN", "Одинцово", "MZD-BEL", "HUB", 26, 55.672331, 37.282531),
            station("MCC-DC", "MDC", "Москва-Сити МЦК", "MZD-MSK", "RING", 27, 55.747220, 37.532220),
            station("MCC-LUZH", "MLU", "Лужники МЦК", "MZD-MSK", "RING", 28, 55.720280, 37.563060),
            station("MCC-NIZH", "MNZ", "Нижегородская МЦК", "MZD-MSK", "RING", 29, 55.732220, 37.728250),
            station("MCC-ROST", "MRS", "Ростокино МЦК", "MZD-MSK", "RING", 30, 55.840859, 37.666685),
            station("MCC-OKRU", "MOK", "Окружная МЦК", "MZD-MSK", "RING", 31, 55.848976, 37.571211)
        );
    }

    static List<SimulationModels.RouteDefinition> routes(List<SimulationModels.StationSeed> stations) {
        Map<String, SimulationModels.StationSeed> byCode = stations.stream()
            .collect(Collectors.toMap(SimulationModels.StationSeed::code, Function.identity()));

        return List.of(
            route("ROUTE-LEN", "MZD-LEN", "Ленинградское направление", byCode, "LNG", "TVR"),
            route("ROUTE-YAR", "MZD-YAR", "Ярославское направление", byCode, "YRS", "MYT"),
            route("ROUTE-KAZ", "MZD-KAZ", "Казанское направление", byCode, "KZN", "LBR", "VSK", "RZN"),
            route("ROUTE-GOR", "MZD-GOR", "Горьковское направление", byCode, "KUV", "OZH", "VLD"),
            route("ROUTE-KUR", "MZD-KUR", "Курское направление", byCode, "KUV", "PDL", "TUL", "KRS"),
            route("ROUTE-KIE", "MZD-KIE", "Киевское направление", byCode, "KIV", "KLG", "BRN"),
            route("ROUTE-BEL", "MZD-BEL", "Смоленское / Белорусское направление", byCode, "BLR", "ODN", "SML"),
            route("ROUTE-SAV", "MZD-SAV", "Савёловское направление", byCode, "SAV", "DMT"),
            route("ROUTE-RIG", "MZD-RIG", "Рижское направление", byCode, "RIG", "VLK"),
            route("ROUTE-PAV", "MZD-PAV", "Павелецкое направление", byCode, "PVL", "PVT"),
            route("ROUTE-MCC", "MZD-MCC", "Московское центральное кольцо", byCode, "MDC", "MLU", "MNZ", "MRS", "MOK", "MDC"),
            route("ROUTE-BMO", "MZD-BMO", "Большое кольцо МЖД", byCode, "OZH", "VLD", "KLG", "VSK", "OZH"),
            route("ROUTE-MCD", "MZD-MCD", "Московские центральные диаметры", byCode, "ODN", "BLR", "KUV", "PDL")
        );
    }

    private static SimulationModels.StationSeed station(
        String seed,
        String code,
        String name,
        String departmentCode,
        String stationType,
        int orderIndex,
        double latitude,
        double longitude
    ) {
        return new SimulationModels.StationSeed(
            uuid(seed),
            code,
            name,
            departmentCode,
            stationType,
            orderIndex,
            latitude,
            longitude
        );
    }

    private static SimulationModels.RouteDefinition route(
        String seed,
        String code,
        String name,
        Map<String, SimulationModels.StationSeed> byCode,
        String... stationCodes
    ) {
        return new SimulationModels.RouteDefinition(
            uuid(seed),
            code,
            name,
            List.of(stationCodes).stream().map(stationCode -> lookup(byCode, stationCode)).toList()
        );
    }

    private static SimulationModels.StationSeed lookup(
        Map<String, SimulationModels.StationSeed> byCode,
        String stationCode
    ) {
        SimulationModels.StationSeed station = byCode.get(stationCode);
        if (station == null) {
            throw new IllegalStateException("Missing station definition for code: " + stationCode);
        }
        return station;
    }

    static List<SimulationModels.CoordinatePoint> segmentShape(
        SimulationModels.StationSeed fromStation,
        SimulationModels.StationSeed toStation
    ) {
        String key = fromStation.code() + "-" + toStation.code();
        List<SimulationModels.CoordinatePoint> shape = switch (key) {
            case "LNG-TVR" -> List.of(
                point(55.894569, 37.450744),
                point(55.979722, 37.173611)
            );
            case "KZN-LBR" -> List.of(
                point(55.716111, 37.816667),
                point(55.668889, 37.922500)
            );
            case "LBR-VSK" -> List.of(
                point(55.565154, 38.226217),
                point(55.498139, 38.364583)
            );
            case "VSK-RZN" -> List.of(
                point(55.099722, 38.780833)
            );
            case "KUV-OZH" -> List.of(
                point(55.750000, 38.016667)
            );
            case "OZH-VLD" -> List.of(
                point(55.925000, 39.463056)
            );
            case "KUV-PDL" -> List.of(
                point(55.618120, 37.668120),
                point(55.509806, 37.562028)
            );
            case "PDL-TUL" -> List.of(
                point(55.248583, 37.493750),
                point(54.934722, 37.451667)
            );
            case "TUL-KRS" -> List.of(
                point(52.978889, 36.111111)
            );
            case "KIV-KLG" -> List.of(
                point(55.429611, 36.841083),
                point(55.005830, 36.480528)
            );
            case "KLG-BRN" -> List.of(
                point(54.117778, 35.343056),
                point(52.821300, 34.495100)
            );
            case "BLR-ODN", "ODN-BLR" -> List.of(
                point(55.726944, 37.449167)
            );
            case "ODN-SML" -> List.of(
                point(55.578889, 36.689722),
                point(55.197610, 34.317226)
            );
            case "SAV-DMT" -> List.of(
                point(56.013333, 37.484917),
                point(56.171861, 37.510472)
            );
            case "RIG-VLK" -> List.of(
                point(55.903972, 36.856000)
            );
            case "PVL-PVT" -> List.of(
                point(54.544250, 38.620944)
            );
            case "VLD-KLG" -> List.of(
                point(56.398417, 38.704712),
                point(55.429611, 36.841083)
            );
            case "KLG-VSK" -> List.of(
                point(55.429611, 36.841083),
                point(55.121309, 37.959974)
            );
            case "VSK-OZH" -> List.of(
                point(55.573194, 38.922778)
            );
            case "BLR-KUV" -> List.of(
                point(55.786220, 37.645220)
            );
            default -> List.of();
        };

        return prependAndAppend(fromStation, toStation, shape);
    }

    private static List<SimulationModels.CoordinatePoint> prependAndAppend(
        SimulationModels.StationSeed fromStation,
        SimulationModels.StationSeed toStation,
        List<SimulationModels.CoordinatePoint> intermediatePoints
    ) {
        java.util.ArrayList<SimulationModels.CoordinatePoint> points = new java.util.ArrayList<>(intermediatePoints.size() + 2);
        points.add(point(fromStation.latitude(), fromStation.longitude()));
        points.addAll(intermediatePoints);
        points.add(point(toStation.latitude(), toStation.longitude()));
        return List.copyOf(points);
    }

    private static SimulationModels.CoordinatePoint point(double latitude, double longitude) {
        return new SimulationModels.CoordinatePoint(latitude, longitude);
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
