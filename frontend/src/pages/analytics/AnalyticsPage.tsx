import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/app/auth/authContext'
import { queryDashboardSnapshot } from '@/features/dashboard/api/dashboardApi'
import { dashboardKeys } from '@/features/dashboard/api/dashboardKeys'
import { KpiPanel } from '@/features/dashboard/components/KpiPanel'
import { PersonnelWidget } from '@/features/dashboard/components/PersonnelWidget'
import { StreamingStatusNotice } from '@/features/dashboard/components/StreamingStatusNotice'
import { useDashboardRealtime } from '@/features/dashboard/hooks/useDashboardRealtime'
import { createInitialDashboardQueryRequest } from '@/features/dashboard/model/dashboardFilters'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Panel } from '@/shared/ui/Panel'
import styles from '@/pages/shared/SectionPage.module.css'

const defaultRequest = createInitialDashboardQueryRequest()

export function AnalyticsPage() {
  const { authEnabled, permissions, permissionsLoaded } = useAuth()
  const canLoadAnalytics = !authEnabled || permissionsLoaded
  const canRequestPersonnel = !authEnabled || permissions.canViewPersonnel
  const analyticsRequest = useMemo(
    () => ({
      ...defaultRequest,
      includePersonnel: canRequestPersonnel,
    }),
    [canRequestPersonnel],
  )
  const dashboardQuery = useQuery({
    queryKey: dashboardKeys.snapshot(analyticsRequest),
    queryFn: ({ signal }) => queryDashboardSnapshot(analyticsRequest, signal),
    enabled: canLoadAnalytics,
  })
  const streamingConnectionState = useDashboardRealtime({
    enabled: canLoadAnalytics && Boolean(dashboardQuery.data),
    request: analyticsRequest,
    forceSnapshotResync: true,
  })
  const isInitialLoading =
    !canLoadAnalytics || (dashboardQuery.isLoading && !dashboardQuery.data)
  const isInitialError = dashboardQuery.isError && !dashboardQuery.data

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>Аналитика</p>
        <h1>Сводка по текущей обстановке</h1>
        <p>
          В разделе отображаются показатели по событиям, движению поездов и персоналу.
        </p>
      </section>

      <StreamingStatusNotice state={streamingConnectionState} />

      {isInitialLoading ? (
        <section className={styles.empty}>
          <EmptyState
            title="Загрузка сводки"
            description="Подождите несколько секунд."
          />
        </section>
      ) : isInitialError ? (
        <Panel
          title="Не удалось загрузить сводку"
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
        <div className={styles.contentGrid}>
          <div className={styles.singleColumn}>
            <KpiPanel data={dashboardQuery.data?.kpiSummary ?? null} />
          </div>
          <div className={styles.singleColumn}>
            <PersonnelWidget
              data={dashboardQuery.data?.personnelSummary ?? null}
              unavailableDescription={
                canRequestPersonnel
                  ? undefined
                  : 'Нет прав для запроса данных по персоналу.'
              }
            />
          </div>
          {dashboardQuery.isError ? (
            <Panel
              className={styles.fullWidth}
              title="Данные сводки не обновились"
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
        </div>
      )}
    </div>
  )
}
