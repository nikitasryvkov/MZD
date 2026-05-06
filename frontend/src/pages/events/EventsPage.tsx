import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  queryDashboardSnapshot,
} from '@/features/dashboard/api/dashboardApi'
import { dashboardKeys } from '@/features/dashboard/api/dashboardKeys'
import { EventFeedPanel } from '@/features/dashboard/components/EventFeedPanel'
import { ObjectDetailsDrawer } from '@/features/dashboard/components/ObjectDetailsDrawer'
import { StreamingStatusNotice } from '@/features/dashboard/components/StreamingStatusNotice'
import { useDashboardRealtime } from '@/features/dashboard/hooks/useDashboardRealtime'
import { createInitialDashboardQueryRequest } from '@/features/dashboard/model/dashboardFilters'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Panel } from '@/shared/ui/Panel'
import styles from '@/pages/shared/SectionPage.module.css'

const defaultRequest = createInitialDashboardQueryRequest()

export function EventsPage() {
  const [selectedEventId, setSelectedEventId] = useState<string>()
  const dashboardQuery = useQuery({
    queryKey: dashboardKeys.snapshot(defaultRequest),
    queryFn: ({ signal }) => queryDashboardSnapshot(defaultRequest, signal),
  })
  const streamingConnectionState = useDashboardRealtime({
    enabled: Boolean(dashboardQuery.data),
    request: defaultRequest,
    selectedEventId,
  })
  const isInitialLoading = dashboardQuery.isLoading && !dashboardQuery.data
  const isInitialError = dashboardQuery.isError && !dashboardQuery.data

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>События</p>
        <h1>Оперативная лента</h1>
        <p>
          В разделе отображаются текущие события по сети. Выберите запись,
          чтобы посмотреть подробности и статус.
        </p>
      </section>

      <StreamingStatusNotice state={streamingConnectionState} />

      {isInitialLoading ? (
        <section className={styles.empty}>
          <EmptyState
            title="Загрузка событий"
            description="Подождите несколько секунд."
          />
        </section>
      ) : isInitialError ? (
        <Panel
          title="Не удалось загрузить события"
          eyebrow="Внимание"
          accent="warm"
        >
          <div className={styles.singleColumn}>
            <p>Повторите попытку позже.</p>
            <button type="button" onClick={() => void dashboardQuery.refetch()}>
              Повторить
            </button>
          </div>
        </Panel>
      ) : (
        <>
          <div className={styles.contentGrid}>
            <EventFeedPanel
              events={dashboardQuery.data?.eventsPreview ?? []}
              selectedEventId={selectedEventId}
              onSelectEvent={setSelectedEventId}
            />
          </div>

          {dashboardQuery.isError ? (
            <Panel
              className={styles.fullWidth}
              title="Данные событий не обновились"
              eyebrow="Внимание"
              accent="warm"
            >
              <div className={styles.singleColumn}>
                <p>Показаны последние доступные данные.</p>
                <button type="button" onClick={() => void dashboardQuery.refetch()}>
                  Повторить
                </button>
              </div>
            </Panel>
          ) : null}
        </>
      )}

      <ObjectDetailsDrawer
        selection={selectedEventId ? { kind: 'event', id: selectedEventId } : null}
        snapshot={dashboardQuery.data}
        onClose={() => setSelectedEventId(undefined)}
      />
    </div>
  )
}
