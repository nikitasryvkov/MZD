import { useQuery } from '@tanstack/react-query'
import { queryDashboardSnapshot } from '@/features/dashboard/api/dashboardApi'
import { dashboardKeys } from '@/features/dashboard/api/dashboardKeys'
import { KpiPanel } from '@/features/dashboard/components/KpiPanel'
import { PersonnelWidget } from '@/features/dashboard/components/PersonnelWidget'
import { createInitialDashboardQueryRequest } from '@/features/dashboard/model/dashboardFilters'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Panel } from '@/shared/ui/Panel'
import styles from '@/pages/shared/SectionPage.module.css'

const defaultRequest = createInitialDashboardQueryRequest()

export function AnalyticsPage() {
  const dashboardQuery = useQuery({
    queryKey: dashboardKeys.snapshot(defaultRequest),
    queryFn: ({ signal }) => queryDashboardSnapshot(defaultRequest, signal),
  })

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>Аналитика</p>
        <h1>Сводка по текущей обстановке</h1>
        <p>
          В разделе отображаются показатели по событиям, движению поездов и персоналу.
        </p>
      </section>

      {dashboardQuery.isLoading && !dashboardQuery.data ? (
        <section className={styles.empty}>
          <EmptyState
            title="Загрузка сводки"
            description="Подождите несколько секунд."
          />
        </section>
      ) : (
        <div className={styles.contentGrid}>
          <div className={styles.singleColumn}>
            <KpiPanel data={dashboardQuery.data?.kpiSummary ?? null} />
          </div>
          <div className={styles.singleColumn}>
            <PersonnelWidget data={dashboardQuery.data?.personnelSummary ?? null} />
            <Panel
              title="Основные показатели"
              eyebrow="Ориентиры"
              subtitle="На что стоит обратить внимание в первую очередь."
            >
              <div className={styles.singleColumn}>
                <div>
                  <strong>Активные события</strong>
                  <p>Показывают количество событий, которые требуют контроля.</p>
                </div>
                <div>
                  <strong>Поезда на линии</strong>
                  <p>Показывают текущее количество поездов в движении.</p>
                </div>
                <div>
                  <strong>Перегруженные участки</strong>
                  <p>Показывают участки с повышенной нагрузкой.</p>
                </div>
              </div>
            </Panel>
          </div>
        </div>
      )}
    </div>
  )
}
