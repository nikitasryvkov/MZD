import type { StreamingConnectionState } from '@/features/dashboard/hooks/useDashboardRealtime'
import styles from './StreamingStatusNotice.module.css'

interface StreamingStatusNoticeProps {
  state: StreamingConnectionState
  className?: string
}

function getStreamingStatusMessage(state: StreamingConnectionState) {
  switch (state) {
    case 'connecting':
      return 'Подключение к потоку данных...'
    case 'reconnecting':
      return 'Восстановление потока данных...'
    case 'disconnected':
      return 'Поток данных отключен.'
    case 'idle':
    case 'connected':
      return undefined
  }
}

export function StreamingStatusNotice({
  state,
  className,
}: StreamingStatusNoticeProps) {
  const message = getStreamingStatusMessage(state)

  if (!message) {
    return null
  }

  return (
    <div
      className={[styles.notice, className].filter(Boolean).join(' ')}
      data-state={state}
      role="status"
      aria-live="polite"
    >
      {message}
    </div>
  )
}
