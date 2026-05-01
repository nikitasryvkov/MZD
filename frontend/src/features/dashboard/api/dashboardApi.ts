import { apiRequest } from '@/shared/api/http'

export type EventStatus =
  | 'REGISTERED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CANCELED'

export interface BoundingBox {
  minLat: number
  minLon: number
  maxLat: number
  maxLon: number
}

export interface LayerFilter {
  showStations: boolean
  showSegments: boolean
  showTrains: boolean
  showEvents: boolean
}

export interface TimeRange {
  from: string
  to: string
}

export interface DashboardQueryRequest {
  bbox?: BoundingBox
  layerFilter: LayerFilter
  timeRange?: TimeRange
  eventStatuses?: EventStatus[]
  departmentCodes?: string[]
  includeKpi: boolean
  includePersonnel: boolean
}

export interface KpiSummary {
  activeEventsCount: number
  trainsOnLineCount: number
  overloadedSectionsCount: number
  updatedAt: string
}

export interface PersonnelSummaryItem {
  dimensionKey: string
  headcount: number
  changePercent?: number
}

export interface PersonnelSummary {
  totalHeadcount: number
  items: PersonnelSummaryItem[]
}

export interface Station {
  id: string
  code: string
  name: string
  latitude: number
  longitude: number
  stationType: string
}

export interface RouteSegmentGeometry {
  type: 'LineString'
  coordinates: [number, number][]
}

export interface RouteSegment {
  id: string
  fromStationId: string
  toStationId: string
  geometry: RouteSegmentGeometry
  lengthKm?: number
  status?: string
}

export interface GeoJsonGeometry {
  type: string
  coordinates?: unknown
  geometries?: GeoJsonGeometry[]
}

export interface GeoJsonFeature {
  type: 'Feature'
  id: string
  geometry: GeoJsonGeometry
  properties: Record<string, unknown>
}

export interface GeoJsonFeatureCollection {
  type: 'FeatureCollection'
  features: GeoJsonFeature[]
}

export interface Train {
  id: string
  trainNumber: string
  latitude: number
  longitude: number
  status: string
  currentStationId?: string
  nextStationId?: string
  progressPercent?: number
  speedKmh?: number
  lastUpdated?: string
}

export interface OperationalEvent {
  id: string
  title: string
  status: EventStatus
  severity: string
  latitude: number
  longitude: number
  affectedObjectId?: string
  affectedSection?: string
  startedAt?: string
  updatedAt: string
}

export interface EventPreview {
  id: string
  title: string
  severity: string
  status: EventStatus
  affectedSection?: string
  startedAt?: string
  updatedAt: string
}

export interface DashboardQueryResponse {
  requestId: string
  generatedAt: string
  kpiSummary: KpiSummary | null
  personnelSummary: PersonnelSummary | null
  mapData: {
    stations: Station[]
    routeSegments: RouteSegment[]
    trains: Train[]
    operationalEvents: OperationalEvent[]
    geoJsonSources?: {
      stations: GeoJsonFeatureCollection
      routeSegments: GeoJsonFeatureCollection
      trains: GeoJsonFeatureCollection
      operationalEvents: GeoJsonFeatureCollection
    }
  }
  eventsPreview: EventPreview[]
}

export interface OperationalEventStatusHistoryItem {
  id: string
  fromStatus?: EventStatus
  toStatus: EventStatus
  comment?: string
  changedAt: string
  changedBy?: string
}

export interface OperationalEventDetailsResponse {
  id: string
  type: string
  title: string
  description?: string
  status: EventStatus
  severity: string
  latitude: number
  longitude: number
  affectedObjectId?: string
  affectedSection?: string
  startedAt?: string
  endedAt?: string
  updatedAt: string
  lastChangedBy?: string
  allowedTransitions: EventStatus[]
  statusHistory: OperationalEventStatusHistoryItem[]
}

export interface UpdateOperationalEventStatusRequest {
  newStatus: EventStatus
  comment?: string
}

export interface UpdateOperationalEventStatusResponse {
  eventId: string
  status: EventStatus
  updatedAt: string
  affectedSection?: string
  summary: string
  allowedTransitions: EventStatus[]
}

export interface TrainUpdateMessage {
  messageId: string
  sequence: number
  generatedAt: string
  operation: 'UPSERT' | 'REMOVE'
  train: Train
  feature?: GeoJsonFeature
}

export interface EventUpdateMessage {
  messageId: string
  sequence: number
  generatedAt: string
  operation: 'UPSERT' | 'REMOVE'
  event: OperationalEvent
  feature?: GeoJsonFeature
}

export async function queryDashboardSnapshot(
  request: DashboardQueryRequest,
  signal?: AbortSignal,
) {
  return apiRequest<DashboardQueryResponse>('/api/v1/dashboard/query', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  })
}

export async function getOperationalEventDetails(eventId: string, signal?: AbortSignal) {
  return apiRequest<OperationalEventDetailsResponse>(`/api/v1/events/${eventId}`, {
    method: 'GET',
    signal,
  })
}

export async function patchOperationalEventStatus(
  eventId: string,
  request: UpdateOperationalEventStatusRequest,
) {
  return apiRequest<UpdateOperationalEventStatusResponse>(
    `/api/v1/events/${eventId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify(request),
    },
  )
}
