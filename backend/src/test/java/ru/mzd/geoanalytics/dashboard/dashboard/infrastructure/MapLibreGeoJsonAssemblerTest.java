package ru.mzd.geoanalytics.dashboard.dashboard.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.mzd.geoanalytics.dashboard.dashboard.api.DashboardApiModels;

class MapLibreGeoJsonAssemblerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapLibreGeoJsonAssembler assembler = new MapLibreGeoJsonAssembler(objectMapper);

    @Test
    void shouldBuildGeoJsonSourcesForMapLibre() throws Exception {
        UUID stationId = UUID.randomUUID();
        UUID nextStationId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        UUID trainId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        DashboardApiModels.MapDataResponse source = new DashboardApiModels.MapDataResponse(
            List.of(new DashboardApiModels.StationResponse(
                stationId,
                "MSK",
                "Moscow",
                55.7558,
                37.6173,
                "HUB"
            )),
            List.of(new DashboardApiModels.RouteSegmentResponse(
                segmentId,
                stationId,
                nextStationId,
                objectMapper.readTree("""
                    {
                      "type": "LineString",
                      "coordinates": [[37.6173, 55.7558], [37.7, 55.8]]
                    }
                    """),
                18.4,
                "NORMAL"
            )),
            List.of(new DashboardApiModels.TrainResponse(
                trainId,
                "7001",
                55.761,
                37.63,
                "ON_TIME",
                stationId,
                nextStationId,
                42.5,
                82.0,
                Instant.parse("2026-04-22T08:00:00Z")
            )),
            List.of(new DashboardApiModels.OperationalEventResponse(
                eventId,
                "Signal disruption",
                "IN_PROGRESS",
                "HIGH",
                55.76,
                37.64,
                segmentId,
                "Moscow - Khimki",
                Instant.parse("2026-04-22T07:30:00Z"),
                Instant.parse("2026-04-22T08:05:00Z")
            )),
            null
        );

        DashboardApiModels.MapDataResponse enriched = assembler.enrichMapData(source);

        assertThat(enriched.geoJsonSources()).isNotNull();
        assertThat(enriched.geoJsonSources().stations().type()).isEqualTo("FeatureCollection");
        assertThat(enriched.geoJsonSources().routeSegments().features()).hasSize(1);
        assertThat(enriched.geoJsonSources().trains().features()).hasSize(1);
        assertThat(enriched.geoJsonSources().operationalEvents().features()).hasSize(1);

        DashboardApiModels.GeoJsonFeatureResponse stationFeature =
            enriched.geoJsonSources().stations().features().get(0);
        DashboardApiModels.GeoJsonFeatureResponse trainFeature =
            enriched.geoJsonSources().trains().features().get(0);
        DashboardApiModels.GeoJsonFeatureResponse eventFeature =
            enriched.geoJsonSources().operationalEvents().features().get(0);

        assertThat(stationFeature.id()).isEqualTo(stationId.toString());
        assertThat(stationFeature.geometry().get("type").asText()).isEqualTo("Point");
        assertThat(stationFeature.geometry().get("coordinates").get(0).asDouble()).isEqualTo(37.6173);
        assertThat(stationFeature.geometry().get("coordinates").get(1).asDouble()).isEqualTo(55.7558);
        assertThat(stationFeature.properties()).containsEntry("kind", "station");

        assertThat(trainFeature.properties())
            .containsEntry("kind", "train")
            .containsEntry("trainNumber", "7001")
            .containsEntry("currentStationId", stationId.toString());

        assertThat(eventFeature.properties())
            .containsEntry("kind", "event")
            .containsEntry("affectedSection", "Moscow - Khimki")
            .containsEntry("status", "IN_PROGRESS");
    }
}
