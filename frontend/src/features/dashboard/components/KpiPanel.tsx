import type { KpiSummary } from '@/features/dashboard/api/dashboardApi'
import { formatCompactNumber, formatDateTime } from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Panel } from '@/shared/ui/Panel'
import styles from './KpiPanel.module.css'

interface KpiPanelProps {
  data: KpiSummary | null
}

function MetricCard({
  label,
  value,
}: {
  label: string
  value: number
}) {
  return (
    <article className={styles.metricCard}>
      <span>{label}</span>
      <strong>{formatCompactNumber(value)}</strong>
    </article>
  )
}

export function KpiPanel({ data }: KpiPanelProps) {
  return (
    <Panel
      title="Ключевые показатели"
      eyebrow="Сводка"
      subtitle={
        data
          ? `Данные обновлены ${formatDateTime(data.updatedAt)}`
          : 'Показатели не отображаются.'
      }
      accent="warm"
    >
      {data ? (
        <div className={styles.metrics}>
          <MetricCard label="Активные события" value={data.activeEventsCount} />
          <MetricCard label="Поезда на линии" value={data.trainsOnLineCount} />
          <MetricCard label="Перегруженные участки" value={data.overloadedSectionsCount} />
        </div>
      ) : (
        <EmptyState
          title="Показатели недоступны"
          description="Включите показатели в фильтрах."
        />
      )}
    </Panel>
  )
}
