import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { PanelLeftOpen } from 'lucide-react'
import { startTransition, useCallback, useEffect, useReducer, useRef, useState } from 'react'
import { ApiError, formatApiErrorDescription } from '@/shared/api/http'
import { EmptyState } from '@/shared/ui/EmptyState'
import { ProgressBar } from '@/shared/ui/ProgressBar'
import { DashboardSkeleton } from '@/features/dashboard/components/DashboardSkeleton'
import { DashboardToolbar } from '@/features/dashboard/components/DashboardToolbar'
import { EventFeedPanel } from '@/features/dashboard/components/EventFeedPanel'
import { MapViewport } from '@/features/dashboard/components/MapViewport'
import { ObjectDetailsDrawer } from '@/features/dashboard/components/ObjectDetailsDrawer'
import { StreamingStatusNotice } from '@/features/dashboard/components/StreamingStatusNotice'
import { dashboardKeys } from '@/features/dashboard/api/dashboardKeys'
import { queryDashboardSnapshot, type BoundingBox } from '@/features/dashboard/api/dashboardApi'
import { useDashboardRealtime } from '@/features/dashboard/hooks/useDashboardRealtime'
import { createInitialDashboardState, dashboardReducer } from '@/features/dashboard/model/dashboardReducer'
import {
  normalizeFieldErrors,
  validateDashboardFilters,
} from '@/features/dashboard/model/dashboardFilters'
import { useToast } from '@/shared/ui/useToast'
import styles from './GeoAnalyticsDashboardPage.module.css'

const BOUNDS_SYNC_DEBOUNCE_MS = 300

export function GeoAnalyticsDashboardPage() {
  const { pushToast } = useToast()
  const [filtersVisible, setFiltersVisible] = useState(true)
  const [eventFeedVisible, setEventFeedVisible] = useState(true)
  const boundsSyncTimerRef = useRef<number | undefined>(undefined)
  const hasSkippedInitialBoundsSyncRef = useRef(false)
  const pendingBoundsRef = useRef<BoundingBox | null>(null)
  const [state, dispatch] = useReducer(
    dashboardReducer,
    undefined,
    createInitialDashboardState,
  )

  const dashboardQuery = useQuery({
    queryKey: dashboardKeys.snapshot(state.appliedFilters),
    queryFn: ({ signal }) =>
      queryDashboardSnapshot(state.appliedFilters, signal),
    placeholderData: keepPreviousData,
  })

  const selectedEventId =
    state.selectedObject?.kind === 'event' ? state.selectedObject.id : undefined

  const streamingConnectionState = useDashboardRealtime({
    enabled: Boolean(dashboardQuery.data),
    request: state.appliedFilters,
    selectedEventId,
  })

  const scheduleBoundsSync = useCallback((bbox: BoundingBox) => {
    if (!hasSkippedInitialBoundsSyncRef.current) {
      hasSkippedInitialBoundsSyncRef.current = true
      return
    }

    pendingBoundsRef.current = bbox

    if (boundsSyncTimerRef.current !== undefined) {
      return
    }

    boundsSyncTimerRef.current = window.setTimeout(() => {
      const nextBbox = pendingBoundsRef.current
      pendingBoundsRef.current = null
      boundsSyncTimerRef.current = undefined

      if (nextBbox) {
        startTransition(() => {
          dispatch({ type: 'sync-bbox', bbox: nextBbox })
        })
      }
    }, BOUNDS_SYNC_DEBOUNCE_MS)
  }, [])

  useEffect(() => {
    return () => {
      if (boundsSyncTimerRef.current !== undefined) {
        window.clearTimeout(boundsSyncTimerRef.current)
      }
    }
  }, [])

  useEffect(() => {
    if (dashboardQuery.data) {
      dispatch({ type: 'clear-inline-errors' })
    }
  }, [dashboardQuery.dataUpdatedAt, dashboardQuery.data])

  useEffect(() => {
    if (!(dashboardQuery.error instanceof ApiError)) {
      return
    }

    if (dashboardQuery.error.status === 400) {
      dispatch({
        type: 'set-inline-errors',
        errors: normalizeFieldErrors(dashboardQuery.error.payload?.fieldErrors),
      })
      return
    }

    pushToast({
      tone:
        dashboardQuery.error.status >= 500 || dashboardQuery.error.status === 0
          ? 'danger'
          : 'warning',
      title:
        dashboardQuery.error.status === 0
          ? 'Сервис временно недоступен'
          : `Не удалось обновить данные (${dashboardQuery.error.status})`,
      description: formatApiErrorDescription(dashboardQuery.error),
    })
  }, [
    dashboardQuery.error,
    dashboardQuery.errorUpdatedAt,
    pushToast,
  ])

  function handleApplyFilters() {
    const errors = validateDashboardFilters(state.draftFilters)

    if (Object.keys(errors).length) {
      dispatch({
        type: 'set-inline-errors',
        errors,
      })
      return
    }

    dispatch({ type: 'apply-filters' })
  }

  return (
    <div className={styles.page}>
      <ProgressBar active={dashboardQuery.isFetching && Boolean(dashboardQuery.data)} />

      {dashboardQuery.isLoading && !dashboardQuery.data ? (
        <div className={styles.loadingShell}>
          <DashboardSkeleton />
        </div>
      ) : dashboardQuery.data ? (
        <div className={styles.workspace}>
          <MapViewport
            className={styles.mapSurface}
            data={dashboardQuery.data.mapData}
            selection={state.selectedObject}
            onSelectObject={(selection) =>
              dispatch({ type: 'select-object', selection })
            }
            onBoundsChange={scheduleBoundsSync}
          />

          <StreamingStatusNotice
            state={streamingConnectionState}
            className={styles.streamStatus}
          />

          {!filtersVisible ? (
            <button
              className={styles.filtersToggle}
              type="button"
              onClick={() => setFiltersVisible(true)}
            >
              <PanelLeftOpen size={16} />
              <span>Показать фильтры</span>
            </button>
          ) : null}

          {filtersVisible ? (
            <aside className={styles.controlDock}>
              <DashboardToolbar
                draftFilters={state.draftFilters}
                inlineErrors={state.inlineErrors}
                isRefreshing={dashboardQuery.isFetching}
                onApply={handleApplyFilters}
                onRefresh={() => {
                  dispatch({ type: 'clear-inline-errors' })
                  void dashboardQuery.refetch()
                }}
                onReset={() => dispatch({ type: 'reset-filters' })}
                onHide={() => setFiltersVisible(false)}
                onDraftFieldChange={(field, value) =>
                  dispatch({ type: 'update-draft', field, value })
                }
                onToggleLayer={(field) => dispatch({ type: 'toggle-layer', field })}
                onToggleStatus={(status) =>
                  dispatch({ type: 'toggle-event-status', status })
                }
              />
            </aside>
          ) : null}

          {!eventFeedVisible ? (
            <button
              className={styles.feedToggle}
              type="button"
              onClick={() => setEventFeedVisible(true)}
            >
              <PanelLeftOpen size={16} />
              <span>Показать ленту</span>
            </button>
          ) : null}

          {eventFeedVisible ? (
            <aside className={styles.eventDock}>
              <EventFeedPanel
                events={dashboardQuery.data.eventsPreview}
                selectedEventId={selectedEventId}
                onHide={() => setEventFeedVisible(false)}
                onSelectEvent={(eventId) =>
                  dispatch({
                    type: 'select-object',
                    selection: {
                      kind: 'event',
                      id: eventId,
                    },
                  })
                }
              />
            </aside>
          ) : null}
        </div>
      ) : (
        <section className={styles.emptyShell}>
          <EmptyState
            title="Не удалось загрузить карту"
            description="Повторите попытку чуть позже или обновите страницу."
          />
          <button type="button" onClick={() => void dashboardQuery.refetch()}>
            Обновить данные
          </button>
        </section>
      )}

      <ObjectDetailsDrawer
        selection={state.selectedObject}
        snapshot={dashboardQuery.data}
        onClose={() => dispatch({ type: 'close-drawer' })}
      />
    </div>
  )
}
