import { useEffect, useEffectEvent, useRef } from 'react'
import type { FeatureCollection, Geometry, GeoJsonProperties } from 'geojson'
import maplibregl, {
  type ExpressionSpecification,
  type GeoJSONSource,
  type LngLatBounds,
  type Map as MapLibreMap,
  type MapGeoJSONFeature,
  type StyleSpecification,
} from 'maplibre-gl'
import type { BoundingBox, DashboardQueryResponse } from '@/features/dashboard/api/dashboardApi'
import type { DashboardSelection } from '@/features/dashboard/model/dashboardReducer'
import {
  resolveDashboardGeoJsonSources,
  toMapLibreFeatureCollection,
} from '@/features/dashboard/model/dashboardGeoJson'
import { EmptyState } from '@/shared/ui/EmptyState'
import styles from './MapViewport.module.css'

interface MapViewportProps {
  data: DashboardQueryResponse['mapData']
  selection: DashboardSelection | null
  onSelectObject: (selection: DashboardSelection) => void
  onBoundsChange: (bbox: BoundingBox) => void
  className?: string
}

const MAP_SOURCE_IDS = {
  stations: 'dashboard-stations',
  routeSegments: 'dashboard-route-segments',
  trains: 'dashboard-trains',
  operationalEvents: 'dashboard-operational-events',
} as const

const MAP_LAYER_IDS = {
  routeSegments: 'dashboard-route-segments',
  routeSegmentsHitArea: 'dashboard-route-segments-hit-area',
  stations: 'dashboard-stations',
  trains: 'dashboard-trains',
  operationalEvents: 'dashboard-operational-events',
} as const

const INTERACTIVE_LAYER_IDS = [
  MAP_LAYER_IDS.operationalEvents,
  MAP_LAYER_IDS.trains,
  MAP_LAYER_IDS.stations,
  MAP_LAYER_IDS.routeSegmentsHitArea,
] as const

const EMPTY_FEATURE_COLLECTION: FeatureCollection<Geometry, GeoJsonProperties> = {
  type: 'FeatureCollection',
  features: [],
}

const BASE_MAP_STYLE = {
  version: 8,
  sources: {
    cartoBase: {
      type: 'raster',
      tiles: [
        'https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
        'https://b.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
        'https://c.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
        'https://d.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
      ],
      tileSize: 256,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    },
  },
  layers: [
    {
      id: 'carto-base',
      type: 'raster',
      source: 'cartoBase',
      minzoom: 0,
      maxzoom: 22,
    },
  ],
} satisfies StyleSpecification

export function MapViewport({
  data,
  selection,
  onSelectObject,
  onBoundsChange,
  className,
}: MapViewportProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<MapLibreMap | null>(null)

  const hasVisibleLayers =
    data.stations.length ||
    data.routeSegments.length ||
    data.trains.length ||
    data.operationalEvents.length

  const handleSelectObject = useEffectEvent((nextSelection: DashboardSelection) => {
    onSelectObject(nextSelection)
  })

  const syncBounds = useEffectEvent(() => {
    const map = mapRef.current
    if (!map) {
      return
    }

    onBoundsChange(toBoundingBox(map.getBounds()))
  })

  const syncMapData = useEffectEvent(() => {
    const map = mapRef.current
    if (!map || !map.isStyleLoaded()) {
      return
    }

    const sources = resolveDashboardGeoJsonSources(data)
    setGeoJsonSourceData(map, MAP_SOURCE_IDS.stations, toMapLibreFeatureCollection(sources.stations))
    setGeoJsonSourceData(
      map,
      MAP_SOURCE_IDS.routeSegments,
      toMapLibreFeatureCollection(sources.routeSegments),
    )
    setGeoJsonSourceData(map, MAP_SOURCE_IDS.trains, toMapLibreFeatureCollection(sources.trains))
    setGeoJsonSourceData(
      map,
      MAP_SOURCE_IDS.operationalEvents,
      toMapLibreFeatureCollection(sources.operationalEvents),
    )
  })

  const syncSelectionStyles = useEffectEvent(() => {
    const map = mapRef.current
    if (!map || !map.isStyleLoaded()) {
      return
    }

    updatePaintProperty(
      map,
      MAP_LAYER_IDS.routeSegments,
      'line-color',
      buildRouteSegmentColorExpression(selection),
    )
    updatePaintProperty(
      map,
      MAP_LAYER_IDS.routeSegments,
      'line-width',
      buildRouteSegmentWidthExpression(selection),
    )
    updatePaintProperty(
      map,
      MAP_LAYER_IDS.stations,
      'circle-radius',
      buildCircleRadiusExpression(selection, 'station', 6, 9),
    )
    updatePaintProperty(
      map,
      MAP_LAYER_IDS.trains,
      'circle-radius',
      buildCircleRadiusExpression(selection, 'train', 7, 9),
    )
    updatePaintProperty(
      map,
      MAP_LAYER_IDS.operationalEvents,
      'circle-radius',
      buildCircleRadiusExpression(selection, 'event', 8, 10),
    )
  })

  useEffect(() => {
    const container = mapContainerRef.current
    if (!container || mapRef.current) {
      return
    }

    const map = new maplibregl.Map({
      container,
      style: BASE_MAP_STYLE,
      center: [37.6173, 55.7558],
      zoom: 8,
      minZoom: 6,
    })

    mapRef.current = map
    map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right')

    const initialResizeFrame = window.requestAnimationFrame(() => {
      map.resize()
    })

    const resizeObserver =
      typeof ResizeObserver === 'undefined'
        ? null
        : new ResizeObserver((entries) => {
            const entry = entries[0]
            if (!entry || entry.contentRect.width < 2 || entry.contentRect.height < 2) {
              return
            }

            map.resize()
          })

    resizeObserver?.observe(container)

    map.on('load', () => {
      addDashboardSources(map)
      addDashboardLayers(map)
      bindDashboardInteractions(map, handleSelectObject)
      syncMapData()
      syncSelectionStyles()
      syncBounds()
    })

    map.on('moveend', syncBounds)

    return () => {
      window.cancelAnimationFrame(initialResizeFrame)
      resizeObserver?.disconnect()
      map.remove()
      mapRef.current = null
    }
  }, [])

  useEffect(() => {
    syncMapData()
  }, [data])

  useEffect(() => {
    syncSelectionStyles()
  }, [selection])

  return (
    <section className={[styles.viewport, className].filter(Boolean).join(' ')}>
      <div className={styles.wrapper}>
        <div ref={mapContainerRef} className={styles.map} />

        {!hasVisibleLayers ? (
          <div className={styles.emptyOverlay}>
            <EmptyState
              title="На карте пока нет данных"
              description="Измените фильтры или переместите карту, чтобы увидеть объекты."
            />
          </div>
        ) : null}
      </div>
    </section>
  )
}

function addDashboardSources(map: MapLibreMap) {
  addGeoJsonSource(map, MAP_SOURCE_IDS.routeSegments)
  addGeoJsonSource(map, MAP_SOURCE_IDS.stations)
  addGeoJsonSource(map, MAP_SOURCE_IDS.trains)
  addGeoJsonSource(map, MAP_SOURCE_IDS.operationalEvents)
}

function addDashboardLayers(map: MapLibreMap) {
  if (!map.getLayer(MAP_LAYER_IDS.routeSegments)) {
    map.addLayer({
      id: MAP_LAYER_IDS.routeSegments,
      type: 'line',
      source: MAP_SOURCE_IDS.routeSegments,
      layout: {
        'line-cap': 'round',
        'line-join': 'round',
      },
      paint: {
        'line-color': '#394a58',
        'line-width': 4,
        'line-opacity': 0.85,
      },
    })
  }

  if (!map.getLayer(MAP_LAYER_IDS.routeSegmentsHitArea)) {
    map.addLayer({
      id: MAP_LAYER_IDS.routeSegmentsHitArea,
      type: 'line',
      source: MAP_SOURCE_IDS.routeSegments,
      layout: {
        'line-cap': 'round',
        'line-join': 'round',
      },
      paint: {
        'line-color': '#000000',
        'line-width': 14,
        'line-opacity': 0,
      },
    })
  }

  if (!map.getLayer(MAP_LAYER_IDS.stations)) {
    map.addLayer({
      id: MAP_LAYER_IDS.stations,
      type: 'circle',
      source: MAP_SOURCE_IDS.stations,
      paint: {
        'circle-radius': 6,
        'circle-color': '#e1e5e9',
        'circle-stroke-color': '#394a58',
        'circle-stroke-width': 2,
        'circle-opacity': 0.95,
      },
    })
  }

  if (!map.getLayer(MAP_LAYER_IDS.trains)) {
    map.addLayer({
      id: MAP_LAYER_IDS.trains,
      type: 'circle',
      source: MAP_SOURCE_IDS.trains,
      paint: {
        'circle-radius': 7,
        'circle-color': '#cd202c',
        'circle-stroke-color': '#9b1722',
        'circle-stroke-width': 2,
        'circle-opacity': 0.8,
      },
    })
  }

  if (!map.getLayer(MAP_LAYER_IDS.operationalEvents)) {
    map.addLayer({
      id: MAP_LAYER_IDS.operationalEvents,
      type: 'circle',
      source: MAP_SOURCE_IDS.operationalEvents,
      paint: {
        'circle-radius': 8,
        'circle-color': [
          'match',
          ['coalesce', ['get', 'severity'], ''],
          'CRITICAL',
          '#cd202c',
          'HIGH',
          '#9b1722',
          '#394a58',
        ],
        'circle-stroke-color': [
          'match',
          ['coalesce', ['get', 'severity'], ''],
          'CRITICAL',
          '#cd202c',
          'HIGH',
          '#9b1722',
          '#24313b',
        ],
        'circle-stroke-width': 2,
        'circle-opacity': 0.86,
      },
    })
  }
}

function bindDashboardInteractions(
  map: MapLibreMap,
  onSelectObject: (selection: DashboardSelection) => void,
) {
  bindPointerCursor(map, MAP_LAYER_IDS.routeSegmentsHitArea)
  bindPointerCursor(map, MAP_LAYER_IDS.stations)
  bindPointerCursor(map, MAP_LAYER_IDS.trains)
  bindPointerCursor(map, MAP_LAYER_IDS.operationalEvents)

  map.on('click', (event) => {
    const feature = map
      .queryRenderedFeatures(event.point, {
        layers: [...INTERACTIVE_LAYER_IDS],
      })
      .find((candidate) => Boolean(resolveSelection(candidate)))

    const selection = resolveSelection(feature)
    if (selection) {
      onSelectObject(selection)
    }
  })
}

function bindPointerCursor(map: MapLibreMap, layerId: string) {
  map.on('mouseenter', layerId, () => {
    map.getCanvas().style.cursor = 'pointer'
  })

  map.on('mouseleave', layerId, () => {
    map.getCanvas().style.cursor = ''
  })
}

function addGeoJsonSource(map: MapLibreMap, sourceId: string) {
  if (!map.getSource(sourceId)) {
    map.addSource(sourceId, {
      type: 'geojson',
      data: EMPTY_FEATURE_COLLECTION,
    })
  }
}

function setGeoJsonSourceData(
  map: MapLibreMap,
  sourceId: string,
  data: FeatureCollection<Geometry, GeoJsonProperties>,
) {
  const source = map.getSource(sourceId)
  if (source && 'setData' in source) {
    ;(source as GeoJSONSource).setData(data)
  }
}

function updatePaintProperty(
  map: MapLibreMap,
  layerId: string,
  property: string,
  value: ExpressionSpecification,
) {
  if (map.getLayer(layerId)) {
    map.setPaintProperty(layerId, property, value)
  }
}

function buildRouteSegmentColorExpression(
  selection: DashboardSelection | null,
): ExpressionSpecification {
  const selectedSegmentId = selection?.kind === 'segment' ? selection.id : ''

  return [
    'case',
    ['==', ['get', 'id'], selectedSegmentId],
    '#cd202c',
    ['==', ['coalesce', ['get', 'status'], ''], 'OVERLOADED'],
    '#9b1722',
    '#394a58',
  ]
}

function buildRouteSegmentWidthExpression(
  selection: DashboardSelection | null,
): ExpressionSpecification {
  const selectedSegmentId = selection?.kind === 'segment' ? selection.id : ''

  return ['case', ['==', ['get', 'id'], selectedSegmentId], 6, 4]
}

function buildCircleRadiusExpression(
  selection: DashboardSelection | null,
  kind: DashboardSelection['kind'],
  defaultRadius: number,
  selectedRadius: number,
): ExpressionSpecification {
  const selectedId = selection?.kind === kind ? selection.id : ''

  return ['case', ['==', ['get', 'id'], selectedId], selectedRadius, defaultRadius]
}

function getFeatureId(feature?: MapGeoJSONFeature): string | null {
  if (!feature) {
    return null
  }

  const propertyId = feature.properties?.id
  if (typeof propertyId === 'string') {
    return propertyId
  }

  if (typeof propertyId === 'number') {
    return String(propertyId)
  }

  if (typeof feature.id === 'string') {
    return feature.id
  }

  if (typeof feature.id === 'number') {
    return String(feature.id)
  }

  return null
}

function resolveSelection(
  feature?: MapGeoJSONFeature,
): DashboardSelection | null {
  const id = getFeatureId(feature)
  if (!id) {
    return null
  }

  const kind = feature?.properties?.kind
  if (kind === 'station' || kind === 'segment' || kind === 'train' || kind === 'event') {
    return { kind, id }
  }

  switch (feature?.layer?.id) {
    case MAP_LAYER_IDS.stations:
      return { kind: 'station', id }
    case MAP_LAYER_IDS.trains:
      return { kind: 'train', id }
    case MAP_LAYER_IDS.operationalEvents:
      return { kind: 'event', id }
    case MAP_LAYER_IDS.routeSegments:
    case MAP_LAYER_IDS.routeSegmentsHitArea:
      return { kind: 'segment', id }
    default:
      return null
  }
}

function toBoundingBox(bounds: LngLatBounds): BoundingBox {
  return {
    minLat: Number(bounds.getSouth().toFixed(6)),
    minLon: Number(bounds.getWest().toFixed(6)),
    maxLat: Number(bounds.getNorth().toFixed(6)),
    maxLon: Number(bounds.getEast().toFixed(6)),
  }
}
