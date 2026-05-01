import { PanelRightClose } from 'lucide-react'
import type { EventPreview } from '@/features/dashboard/api/dashboardApi'
import { formatStatusLabel } from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Panel } from '@/shared/ui/Panel'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import styles from './EventFeedPanel.module.css'

interface EventFeedPanelProps {
  events: EventPreview[]
  selectedEventId?: string
  onSelectEvent: (eventId: string) => void
  onHide?: () => void
}

export function EventFeedPanel({
  events,
  selectedEventId,
  onSelectEvent,
  onHide,
}: EventFeedPanelProps) {
  return (
    <Panel
      title="События на контроле"
      eyebrow="Оперативная лента"
      subtitle="Выберите событие, чтобы посмотреть подробности."
      accent="warm"
      className={styles.panel}
      actions={
        onHide ? (
          <button className={styles.hideButton} type="button" onClick={onHide}>
            <PanelRightClose size={16} />
            <span>Скрыть</span>
          </button>
        ) : null
      }
    >
      {events.length ? (
        <div className={styles.feed}>
          {events.map((event) => (
            <button
              key={event.id}
              className={styles.eventRow}
              data-selected={selectedEventId === event.id}
              type="button"
              onClick={() => onSelectEvent(event.id)}
            >
              <div className={styles.eventHeader}>
                <strong>{event.title}</strong>
                <StatusBadge kind="severity" value={event.severity} />
              </div>
              <div className={styles.metaRow}>
                <StatusBadge value={event.status} />
                <span>{event.affectedSection ?? 'Участок не указан'}</span>
              </div>
              <small>Статус: {formatStatusLabel(event.status)}</small>
            </button>
          ))}
        </div>
      ) : (
        <EmptyState
          title="Событий пока нет"
          description="Попробуйте изменить фильтры или период, чтобы увидеть подходящие записи."
        />
      )}
    </Panel>
  )
}
