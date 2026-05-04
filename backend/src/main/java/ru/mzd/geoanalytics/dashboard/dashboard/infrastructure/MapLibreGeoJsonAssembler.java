package ru.mzd.geoanalytics.dashboard.dashboard.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.dashboard.application.model.DashboardViewModels;
import ru.mzd.geoanalytics.dashboard.dashboard.application.port.DashboardGeoJsonPort;

@Component
public class MapLibreGeoJsonAssembler implements DashboardGeoJsonPort {

    private final ObjectMapper objectMapper;

    public MapLibreGeoJsonAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardViewModels.MapDataView enrichMapData(DashboardViewModels.MapDataView source) {
        return new DashboardViewModels.MapDataView(
            source.stations(),
            source.routeSegments(),
            source.trains(),
            source.operationalEvents(),
            buildSources(source)
        );
    }

    public DashboardViewModels.GeoJsonSourcesView buildSources(DashboardViewModels.MapDataView mapData) {
        return new DashboardViewModels.GeoJsonSourcesView(
            featureCollection(mapData.stations().stream().map(this::toStationFeature).toList()),
            featureCollection(mapData.routeSegments().stream().map(this::toRouteSegmentFeature).toList()),
            featureCollection(mapData.trains().stream().map(this::toTrainFeature).toList()),
            featureCollection(mapData.operationalEvents().stream().map(this::toOperationalEventFeature).toList())
        );
    }

    public DashboardViewModels.GeoJsonFeatureView toTrainFeature(DashboardViewModels.TrainView train) {
        return new DashboardViewModels.GeoJsonFeatureView(
            "Feature",
            train.id().toString(),
            pointGeometry(train.longitude(), train.latitude()),
            compactProperties(
                "id", train.id().toString(),
                "kind", "train",
                "trainNumber", train.trainNumber(),
                "status", train.status(),
                "currentStationId", stringify(train.currentStationId()),
                "nextStationId", stringify(train.nextStationId()),
                "progressPercent", train.progressPercent(),
                "speedKmh", train.speedKmh(),
                "lastUpdated", stringify(train.lastUpdated())
            )
        );
    }

    public DashboardViewModels.GeoJsonFeatureView toOperationalEventFeature(
        DashboardViewModels.OperationalEventView event
    ) {
        return new DashboardViewModels.GeoJsonFeatureView(
            "Feature",
            event.id().toString(),
            pointGeometry(event.longitude(), event.latitude()),
            compactProperties(
                "id", event.id().toString(),
                "kind", "event",
                "title", event.title(),
                "status", event.status(),
                "severity", event.severity(),
                "affectedObjectId", stringify(event.affectedObjectId()),
                "affectedSection", event.affectedSection(),
                "startedAt", stringify(event.startedAt()),
                "updatedAt", stringify(event.updatedAt())
            )
        );
    }

    private DashboardViewModels.GeoJsonFeatureView toStationFeature(DashboardViewModels.StationView station) {
        return new DashboardViewModels.GeoJsonFeatureView(
            "Feature",
            station.id().toString(),
            pointGeometry(station.longitude(), station.latitude()),
            compactProperties(
                "id", station.id().toString(),
                "kind", "station",
                "code", station.code(),
                "name", station.name(),
                "stationType", station.stationType()
            )
        );
    }

    private DashboardViewModels.GeoJsonFeatureView toRouteSegmentFeature(
        DashboardViewModels.RouteSegmentView routeSegment
    ) {
        return new DashboardViewModels.GeoJsonFeatureView(
            "Feature",
            routeSegment.id().toString(),
            routeSegment.geometry(),
            compactProperties(
                "id", routeSegment.id().toString(),
                "kind", "segment",
                "fromStationId", routeSegment.fromStationId().toString(),
                "toStationId", routeSegment.toStationId().toString(),
                "lengthKm", routeSegment.lengthKm(),
                "status", routeSegment.status()
            )
        );
    }

    private DashboardViewModels.GeoJsonFeatureCollectionView featureCollection(
        List<DashboardViewModels.GeoJsonFeatureView> features
    ) {
        return new DashboardViewModels.GeoJsonFeatureCollectionView("FeatureCollection", features);
    }

    private JsonNode pointGeometry(double longitude, double latitude) {
        return objectMapper.valueToTree(Map.of(
            "type", "Point",
            "coordinates", List.of(longitude, latitude)
        ));
    }

    private Map<String, Object> compactProperties(Object... keyValuePairs) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int index = 0; index < keyValuePairs.length; index += 2) {
            Object value = keyValuePairs[index + 1];
            if (value != null) {
                properties.put(String.valueOf(keyValuePairs[index]), value);
            }
        }
        return properties;
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
