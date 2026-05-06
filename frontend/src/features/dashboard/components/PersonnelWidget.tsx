import type { PersonnelSummary } from '@/features/dashboard/api/dashboardApi'
import { formatCompactNumber, formatPercent } from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Panel } from '@/shared/ui/Panel'
import styles from './PersonnelWidget.module.css'

interface PersonnelWidgetProps {
  data: PersonnelSummary | null
  unavailableDescription?: string
}

export function PersonnelWidget({ data, unavailableDescription }: PersonnelWidgetProps) {
  if (!data) {
    return (
      <Panel
        title="Персонал"
        eyebrow="Команды"
        subtitle={unavailableDescription ?? 'Сведения по персоналу недоступны.'}
      >
        <EmptyState
          title="Нет данных по персоналу"
          description={
            unavailableDescription ?? 'Попробуйте изменить фильтры или повторить запрос позже.'
          }
        />
      </Panel>
    )
  }

  const maxHeadcount = Math.max(...data.items.map((item) => item.headcount), 1)
  const sortedItems = [...data.items].sort((left, right) => right.headcount - left.headcount)

  return (
    <Panel
      title="Персонал"
      eyebrow="Команды"
      subtitle={`${formatCompactNumber(data.totalHeadcount)} сотрудников.`}
    >
      {sortedItems.length ? (
        <div className={styles.list}>
          {sortedItems.map((item) => (
            <article key={item.dimensionKey} className={styles.row}>
              <div className={styles.rowHeader}>
                <strong>{item.dimensionKey}</strong>
                <span>{formatCompactNumber(item.headcount)}</span>
              </div>
              <div className={styles.barTrack}>
                <div
                  className={styles.barFill}
                  style={{
                    width: `${(item.headcount / maxHeadcount) * 100}%`,
                  }}
                />
              </div>
              <small>{formatPercent(item.changePercent)}</small>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState
          title="Нет данных по персоналу"
          description="По текущим фильтрам раздел не содержит записей."
        />
      )}
    </Panel>
  )
}
