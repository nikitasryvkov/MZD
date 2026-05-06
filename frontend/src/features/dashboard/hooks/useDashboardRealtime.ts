import { useEffect, useEffectEvent, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Client } from '@stomp/stompjs'
import { getAuthToken, getWebSocketUrl } from '@/shared/api/http'
import {
  type BoundingBox,
  type DashboardQueryResponse,
  type DashboardQueryRequest,
  type EventPreview,
  type EventUpdateMessage,
  type GeoJsonFeatureCollection,
  type OperationalEvent,
  type Train,
  type TrainUpdateMessage,
} from '@/features/dashboard/api/dashboardApi'
import { dashboardKeys } from '@/features/dashboard/api/dashboardKeys'
import {
  removeGeoJsonFeature,
  resolveDashboardGeoJsonSources,
  toOperationalEventGeoJsonFeature,
  toTrainGeoJsonFeature,
  upsertGeoJsonFeature,
} from '@/features/dashboard/model/dashboardGeoJson'

export type StreamingConnectionState =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'disconnected'

interface UseDashboardRealtimeOptions {
  enabled: boolean
  request: DashboardQueryRequest
  selectedEventId?: string
  forceSnapshotResync?: boolean
}

const SNAPSHOT_RESYNC_DEBOUNCE_MS = 750
const MAX_LAYER_ROWS = 1000
const MAX_EVENTS_PREVIEW_ROWS = 100

function severityRank(value: string) {
  switch (value.toUpperCase()) {
    case 'CRITICAL':
      return 0
    case 'HIGH':
      return 1
    case 'MEDIUM':
      return 2
    default:
      return 3
  }
}

function upsertEntity<T extends { id: string }>(items: T[], nextItem: T) {
  const existingIndex = items.findIndex((item) => item.id === nextItem.id)

  if (existingIndex === -1) {
    return [...items, nextItem]
  }

  return items.map((item) => (item.id === nextItem.id ? nextItem : item))
}

function sortTrains(items: Train[]) {
  return [...items].sort((left, right) =>
    left.trainNumber.localeCompare(right.trainNumber, 'ru'),
  )
}

function sortOperationalEvents(items: OperationalEvent[]) {
  return [...items].sort((left, right) => {
    const updatedAtDifference =
      toComparableInstant(right.updatedAt) - toComparableInstant(left.updatedAt)
    if (updatedAtDifference !== 0) {
      return updatedAtDifference
    }

    return left.id.localeCompare(right.id)
  })
}

function limitGeoJsonFeatures(
  collection: GeoJsonFeatureCollection,
  allowedIds: string[],
): GeoJsonFeatureCollection {
  const allowedIdSet = new Set(allowedIds)

  return {
    ...collection,
    features: collection.features.filter((feature) => allowedIdSet.has(feature.id)),
  }
}

function toComparableInstant(value?: string) {
  if (!value) {
    return Number.NEGATIVE_INFINITY
  }

  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? Number.NEGATIVE_INFINITY : parsed
}

function isInsideBoundingBox(
  latitude: number,
  longitude: number,
  bbox?: BoundingBox,
) {
  if (!bbox) {
    return true
  }

  return (
    latitude >= bbox.minLat &&
    latitude <= bbox.maxLat &&
    longitude >= bbox.minLon &&
    longitude <= bbox.maxLon
  )
}

function hasTrainRealtimeResyncRequirement(request: DashboardQueryRequest) {
  return Boolean(request.departmentCodes?.length)
}

function hasEventRealtimeResyncRequirement(request: DashboardQueryRequest) {
  return Boolean(request.departmentCodes?.length || request.timeRange)
}

function matchesTrainRequest(train: Train, request: DashboardQueryRequest) {
  return isInsideBoundingBox(train.latitude, train.longitude, request.bbox)
}

function matchesEventRequest(
  event: Pick<OperationalEvent, 'latitude' | 'longitude' | 'status'>,
  request: DashboardQueryRequest,
) {
  if (!isInsideBoundingBox(event.latitude, event.longitude, request.bbox)) {
    return false
  }

  if (request.eventStatuses?.length && !request.eventStatuses.includes(event.status)) {
    return false
  }

  return true
}

function applyTrainDelta(
  currentSnapshot: DashboardQueryResponse | undefined,
  message: TrainUpdateMessage,
  request: DashboardQueryRequest,
) {
  if (!currentSnapshot) {
    return currentSnapshot
  }

  if (!request.layerFilter.showTrains) {
    return currentSnapshot
  }

  const shouldKeepTrain =
    message.operation !== 'REMOVE' && matchesTrainRequest(message.train, request)
  const nextTrains =
    !shouldKeepTrain
      ? currentSnapshot.mapData.trains.filter((train) => train.id !== message.train.id)
      : sortTrains(upsertEntity<Train>(currentSnapshot.mapData.trains, message.train))
  const limitedTrains = nextTrains.slice(0, MAX_LAYER_ROWS)
  const geoJsonSources = resolveDashboardGeoJsonSources(currentSnapshot.mapData)
  const nextTrainFeature = message.feature ?? toTrainGeoJsonFeature(message.train)
  const nextTrainGeoJsonSource = !shouldKeepTrain
    ? removeGeoJsonFeature(geoJsonSources.trains, message.train.id)
    : upsertGeoJsonFeature(geoJsonSources.trains, nextTrainFeature)
  const nextGeoJsonSources = {
    ...geoJsonSources,
    trains: limitGeoJsonFeatures(
      nextTrainGeoJsonSource,
      limitedTrains.map((train) => train.id),
    ),
  }

  return {
    ...currentSnapshot,
    mapData: {
      ...currentSnapshot.mapData,
      trains: limitedTrains,
      geoJsonSources: nextGeoJsonSources,
    },
  }
}

function sortEventPreview(items: EventPreview[]) {
  return [...items].sort((left, right) => {
    const severityDifference = severityRank(left.severity) - severityRank(right.severity)
    if (severityDifference !== 0) {
      return severityDifference
    }

    const startedAtDifference =
      toComparableInstant(right.startedAt) - toComparableInstant(left.startedAt)
    if (startedAtDifference !== 0) {
      return startedAtDifference
    }

    const updatedAtDifference =
      toComparableInstant(right.updatedAt) - toComparableInstant(left.updatedAt)
    if (updatedAtDifference !== 0) {
      return updatedAtDifference
    }

    return left.title.localeCompare(right.title, 'ru')
  })
}

function toEventPreview(
  event: OperationalEvent,
  existingPreview?: EventPreview,
): EventPreview {
  return {
    id: event.id,
    title: event.title,
    severity: event.severity,
    status: event.status,
    affectedSection: event.affectedSection ?? existingPreview?.affectedSection,
    startedAt: event.startedAt ?? existingPreview?.startedAt,
    updatedAt: event.updatedAt,
  }
}

function applyEventDelta(
  currentSnapshot: DashboardQueryResponse | undefined,
  message: EventUpdateMessage,
  request: DashboardQueryRequest,
) {
  if (!currentSnapshot) {
    return currentSnapshot
  }

  const shouldKeepEvent = matchesEventRequest(message.event, request)
  const shouldKeepEventOnMap = request.layerFilter.showEvents && shouldKeepEvent
  const nextOperationalEvents =
    message.operation === 'REMOVE' || !shouldKeepEventOnMap
      ? sortOperationalEvents(
          currentSnapshot.mapData.operationalEvents.filter(
            (event) => event.id !== message.event.id,
          ),
        )
      : sortOperationalEvents(
          upsertEntity<OperationalEvent>(
            currentSnapshot.mapData.operationalEvents,
            message.event,
          ),
        )
  const limitedOperationalEvents = nextOperationalEvents.slice(0, MAX_LAYER_ROWS)
  const geoJsonSources = resolveDashboardGeoJsonSources(currentSnapshot.mapData)
  const nextEventFeature = message.feature ?? toOperationalEventGeoJsonFeature(message.event)
  const nextEventGeoJsonSource =
    message.operation === 'REMOVE' || !shouldKeepEventOnMap
      ? removeGeoJsonFeature(geoJsonSources.operationalEvents, message.event.id)
      : upsertGeoJsonFeature(geoJsonSources.operationalEvents, nextEventFeature)
  const nextGeoJsonSources = {
    ...geoJsonSources,
    operationalEvents: limitGeoJsonFeatures(
      nextEventGeoJsonSource,
      limitedOperationalEvents.map((event) => event.id),
    ),
  }

  const nextPreview =
    message.operation === 'REMOVE' || !shouldKeepEvent
      ? currentSnapshot.eventsPreview
          .filter((event) => event.id !== message.event.id)
          .slice(0, MAX_EVENTS_PREVIEW_ROWS)
      : sortEventPreview(
          upsertEntity<EventPreview>(
            currentSnapshot.eventsPreview,
            toEventPreview(
              message.event,
              currentSnapshot.eventsPreview.find((event) => event.id === message.event.id),
            ),
          ),
        ).slice(0, MAX_EVENTS_PREVIEW_ROWS)

  return {
    ...currentSnapshot,
    mapData: {
      ...currentSnapshot.mapData,
      operationalEvents: limitedOperationalEvents,
      geoJsonSources: nextGeoJsonSources,
    },
    eventsPreview: nextPreview,
  }
}

function parseRealtimeMessage<T>(body: string) {
  try {
    return JSON.parse(body) as T
  } catch {
    return undefined
  }
}

export function useDashboardRealtime({
  enabled,
  request,
  selectedEventId,
  forceSnapshotResync = false,
}: UseDashboardRealtimeOptions) {
  const queryClient = useQueryClient()
  const resyncTimerRef = useRef<number | undefined>(undefined)
  const [connectionState, setConnectionState] =
    useState<StreamingConnectionState>('idle')

  const resyncSnapshotNow = useEffectEvent(() => {
    queryClient.invalidateQueries({ queryKey: dashboardKeys.snapshotRoot })

    if (selectedEventId) {
      queryClient.invalidateQueries({
        queryKey: dashboardKeys.event(selectedEventId),
      })
    }
  })

  const scheduleSnapshotResync = useEffectEvent(() => {
    if (resyncTimerRef.current !== undefined) {
      return
    }

    resyncTimerRef.current = window.setTimeout(() => {
      resyncTimerRef.current = undefined
      resyncSnapshotNow()
    }, SNAPSHOT_RESYNC_DEBOUNCE_MS)
  })

  const handleTrainMessage = useEffectEvent((message: TrainUpdateMessage) => {
    if (forceSnapshotResync || hasTrainRealtimeResyncRequirement(request)) {
      scheduleSnapshotResync()
      return
    }

    queryClient.setQueryData(
      dashboardKeys.snapshot(request),
      (currentSnapshot: DashboardQueryResponse | undefined) =>
        applyTrainDelta(currentSnapshot, message, request),
    )
  })

  const handleEventMessage = useEffectEvent((message: EventUpdateMessage) => {
    if (forceSnapshotResync || hasEventRealtimeResyncRequirement(request)) {
      scheduleSnapshotResync()
      return
    }

    queryClient.setQueryData(
      dashboardKeys.snapshot(request),
      (currentSnapshot: DashboardQueryResponse | undefined) =>
        applyEventDelta(currentSnapshot, message, request),
    )

    if (selectedEventId && selectedEventId === message.event.id) {
      queryClient.invalidateQueries({
        queryKey: dashboardKeys.event(selectedEventId),
      })
    }
  })

  useEffect(() => {
    return () => {
      if (resyncTimerRef.current !== undefined) {
        window.clearTimeout(resyncTimerRef.current)
      }
    }
  }, [])

  useEffect(() => {
    if (!enabled) {
      return
    }

    let isDisposed = false
    let reconnectTimer: number | undefined
    let client: Client | null = null
    let reconnectScheduled = false
    let reconnectAttempt = 0
    let hasConnectedOnce = false
    let pendingResync = false
    let trainSequence = 0
    let eventSequence = 0

    function scheduleReconnect() {
      if (isDisposed || reconnectScheduled) {
        return
      }

      reconnectScheduled = true
      const delay = Math.min(30000, 1000 * 2 ** reconnectAttempt)
      reconnectAttempt += 1
      setConnectionState('reconnecting')

      reconnectTimer = window.setTimeout(() => {
        reconnectScheduled = false
        connect()
      }, delay)
    }

    function connect() {
      if (isDisposed) {
        return
      }

      setConnectionState(hasConnectedOnce ? 'reconnecting' : 'connecting')

      const accessToken = getAuthToken()
      client = new Client({
        brokerURL: getWebSocketUrl(),
        reconnectDelay: 0,
        connectHeaders: accessToken
          ? {
              Authorization: `Bearer ${accessToken}`,
            }
          : {},
      })

      client.onConnect = () => {
        reconnectAttempt = 0
        trainSequence = 0
        eventSequence = 0
        setConnectionState('connected')

        if (hasConnectedOnce || pendingResync) {
          pendingResync = false
          resyncSnapshotNow()
        }

        hasConnectedOnce = true

        client?.subscribe('/topic/v1.trains', (frame) => {
          const message = parseRealtimeMessage<TrainUpdateMessage>(frame.body)
          if (!message) {
            pendingResync = true
            scheduleSnapshotResync()
            return
          }

          if (message.sequence <= trainSequence) {
            return
          }

          if (trainSequence !== 0 && message.sequence !== trainSequence + 1) {
            pendingResync = true
            scheduleSnapshotResync()
          }

          trainSequence = message.sequence
          handleTrainMessage(message)
        })

        client?.subscribe('/topic/v1.events', (frame) => {
          const message = parseRealtimeMessage<EventUpdateMessage>(frame.body)
          if (!message) {
            pendingResync = true
            scheduleSnapshotResync()
            return
          }

          if (message.sequence <= eventSequence) {
            return
          }

          if (eventSequence !== 0 && message.sequence !== eventSequence + 1) {
            pendingResync = true
            scheduleSnapshotResync()
          }

          eventSequence = message.sequence
          handleEventMessage(message)
        })
      }

      client.onStompError = () => {
        pendingResync = true
        scheduleReconnect()
      }

      client.onWebSocketError = () => {
        pendingResync = true
      }

      client.onWebSocketClose = () => {
        if (isDisposed) {
          return
        }

        pendingResync = pendingResync || hasConnectedOnce
        scheduleReconnect()
      }

      client.activate()
    }

    connect()

    return () => {
      isDisposed = true
      if (reconnectTimer) {
        window.clearTimeout(reconnectTimer)
      }
      setConnectionState('disconnected')
      void client?.deactivate()
    }
  }, [enabled, queryClient])

  return enabled ? connectionState : 'idle'
}
