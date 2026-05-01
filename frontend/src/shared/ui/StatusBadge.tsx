import { formatStatusLabel } from '@/shared/lib/format'
import styles from './StatusBadge.module.css'

type StatusKind = 'status' | 'severity' | 'connection'

interface StatusBadgeProps {
  value: string
  kind?: StatusKind
}

function resolveTone(kind: StatusKind, value: string) {
  const normalizedValue = value.toUpperCase()

  if (kind === 'severity') {
    if (normalizedValue === 'CRITICAL') return 'danger'
    if (normalizedValue === 'HIGH') return 'warning'
    if (normalizedValue === 'MEDIUM') return 'info'
    return 'neutral'
  }

  if (kind === 'connection') {
    if (normalizedValue === 'CONNECTED') return 'success'
    if (normalizedValue === 'RECONNECTING') return 'warning'
    if (normalizedValue === 'DISCONNECTED') return 'danger'
    return 'info'
  }

  if (normalizedValue === 'RESOLVED') return 'success'
  if (normalizedValue === 'CANCELED') return 'danger'
  if (normalizedValue === 'IN_PROGRESS') return 'warning'
  if (normalizedValue === 'ON_ROUTE') return 'info'
  return 'neutral'
}

export function StatusBadge({ value, kind = 'status' }: StatusBadgeProps) {
  return (
    <span className={styles.badge} data-tone={resolveTone(kind, value)}>
      {formatStatusLabel(value)}
    </span>
  )
}
