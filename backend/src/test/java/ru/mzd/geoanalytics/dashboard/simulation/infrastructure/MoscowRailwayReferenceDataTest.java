package ru.mzd.geoanalytics.dashboard.simulation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import ru.mzd.geoanalytics.dashboard.simulation.domain.SimulationModels;

class MoscowRailwayReferenceDataTest {

    @Test
    void shouldExposeApprovedMoscowRailwayTopology() {
        List<SimulationModels.StationSeed> stations = MoscowRailwayReferenceData.stations();
        Map<String, SimulationModels.StationSeed> stationsByCode = stations.stream()
            .collect(Collectors.toMap(SimulationModels.StationSeed::code, Function.identity()));

        assertThat(stationsByCode).containsKeys(
            "LNG", "YRS", "KZN", "KUV", "KIV", "BLR", "SAV", "RIG", "PVL",
            "MYT", "LBR", "OZH", "PDL", "VSK", "TUL", "RZN", "VLD", "BRN", "SML",
            "MDC", "MLU", "MNZ", "MRS", "MOK"
        );

        List<SimulationModels.RouteDefinition> routes = MoscowRailwayReferenceData.routes(stations);
        Map<String, SimulationModels.RouteDefinition> routesByCode = routes.stream()
            .collect(Collectors.toMap(SimulationModels.RouteDefinition::code, Function.identity()));

        assertThat(routes).hasSize(13);
        assertThat(routesByCode).containsKeys(
            "MZD-LEN", "MZD-YAR", "MZD-KAZ", "MZD-GOR", "MZD-KUR", "MZD-KIE",
            "MZD-BEL", "MZD-SAV", "MZD-RIG", "MZD-PAV", "MZD-MCC", "MZD-BMO", "MZD-MCD"
        );
        assertThat(routesByCode.get("MZD-KAZ").stations())
            .extracting(SimulationModels.StationSeed::code)
            .containsExactly("KZN", "LBR", "VSK", "RZN");
        assertThat(routesByCode.get("MZD-MCC").stations())
            .extracting(SimulationModels.StationSeed::code)
            .containsExactly("MDC", "MLU", "MNZ", "MRS", "MOK", "MDC");
    }
}
