import type {
  DashboardQueryResponse,
  GeoJsonFeature,
  GeoJsonFeatureCollection,
  OperationalEvent,
  RouteSegment,
  Station,
  Train,
} from '@/features/dashboard/api/dashboardApi'
import type { FeatureCollection, Geometry, GeoJsonProperties, Point } from 'geojson'

export interface DashboardGeoJsonSources {
  stations: GeoJsonFeatureCollection
  routeSegments: GeoJsonFeatureCollection
  trains: GeoJsonFeatureCollection
  operationalEvents: GeoJsonFeatureCollection
}

type DashboardMapData = DashboardQueryResponse['mapData']

export function toStationGeoJsonFeature(station: Station): GeoJsonFeature {
  return {
    type: 'Feature',
    id: station.id,
    geometry: pointGeometry(station.longitude, station.latitude),
    properties: {
      id: station.id,
      kind: 'station',
      code: station.code,
      name: station.name,
      stationType: station.stationType,
    },
  }
}

export function toRouteSegmentGeoJsonFeature(segment: RouteSegment): GeoJsonFeature {
  return {
    type: 'Feature',
    id: segment.id,
    geometry: segment.geometry,
    properties: {
      id: segment.id,
      kind: 'segment',
      fromStationId: segment.fromStationId,
      toStationId: segment.toStationId,
      lengthKm: segment.lengthKm,
      status: segment.status,
    },
  }
}

export function toTrainGeoJsonFeature(train: Train): GeoJsonFeature {
  return {
    type: 'Feature',
    id: train.id,
    geometry: pointGeometry(train.longitude, train.latitude),
    properties: {
      id: train.id,
      kind: 'train',
      trainNumber: train.trainNumber,
      status: train.status,
      currentStationId: train.currentStationId,
      nextStationId: train.nextStationId,
      progressPercent: train.progressPercent,
      speedKmh: train.speedKmh,
      lastUpdated: train.lastUpdated,
    },
  }
}

export function toOperationalEventGeoJsonFeature(event: OperationalEvent): GeoJsonFeature {
  return {
    type: 'Feature',
    id: event.id,
    geometry: pointGeometry(event.longitude, event.latitude),
    properties: {
      id: event.id,
      kind: 'event',
      title: event.title,
      status: event.status,
      severity: event.severity,
      affectedObjectId: event.affectedObjectId,
      affectedSection: event.affectedSection,
      startedAt: event.startedAt,
      updatedAt: event.updatedAt,
    },
  }
}

export function createDashboardGeoJsonSources(mapData: DashboardMapData): DashboardGeoJsonSources {
  return {
    stations: createGeoJsonFeatureCollection(mapData.stations.map(toStationGeoJsonFeature)),
    routeSegments: createGeoJsonFeatureCollection(
      mapData.routeSegments.map(toRouteSegmentGeoJsonFeature),
    ),
    trains: createGeoJsonFeatureCollection(mapData.trains.map(toTrainGeoJsonFeature)),
    operationalEvents: createGeoJsonFeatureCollection(
      mapData.operationalEvents.map(toOperationalEventGeoJsonFeature),
    ),
  }
}

export function resolveDashboardGeoJsonSources(mapData: DashboardMapData): DashboardGeoJsonSources {
  return mapData.geoJsonSources ?? createDashboardGeoJsonSources(mapData)
}

export function upsertGeoJsonFeature(
  collection: GeoJsonFeatureCollection,
  nextFeature: GeoJsonFeature,
): GeoJsonFeatureCollection {
  const existingIndex = collection.features.findIndex((feature) => feature.id === nextFeature.id)

  if (existingIndex === -1) {
    return {
      ...collection,
      features: [...collection.features, nextFeature],
    }
  }

  return {
    ...collection,
    features: collection.features.map((feature) =>
      feature.id === nextFeature.id ? nextFeature : feature,
    ),
  }
}

export function removeGeoJsonFeature(
  collection: GeoJsonFeatureCollection,
  featureId: string,
): GeoJsonFeatureCollection {
  return {
    ...collection,
    features: collection.features.filter((feature) => feature.id !== featureId),
  }
}

export function toMapLibreFeatureCollection(
  collection: GeoJsonFeatureCollection,
): FeatureCollection<Geometry, GeoJsonProperties> {
  return {
    type: 'FeatureCollection',
    features: collection.features.map((feature) => ({
      type: 'Feature',
      id: feature.id,
      geometry: feature.geometry as Geometry,
      properties: {
        ...(feature.properties ?? {}),
        id: feature.id,
      },
    })),
  }
}

function createGeoJsonFeatureCollection(features: GeoJsonFeature[]): GeoJsonFeatureCollection {
  return {
    type: 'FeatureCollection',
    features,
  }
}

function pointGeometry(longitude: number, latitude: number): Point {
  return {
    type: 'Point',
    coordinates: [longitude, latitude],
  }
}
