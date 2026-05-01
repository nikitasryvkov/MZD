const dateTimeFormatter = new Intl.DateTimeFormat('ru-RU', {
  day: '2-digit',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})

const fullDateTimeFormatter = new Intl.DateTimeFormat('ru-RU', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
})

const compactNumberFormatter = new Intl.NumberFormat('ru-RU', {
  notation: 'compact',
})

const percentFormatter = new Intl.NumberFormat('ru-RU', {
  maximumFractionDigits: 1,
})

const localizedLabels: Record<string, string> = {
  ACTIVE: 'Активно',
  AT_STATION: 'На станции',
  BIDIRECTIONAL: 'Двунаправленный',
  CANCELED: 'Отменено',
  CONNECTED: 'Подключено',
  CONNECTING: 'Подключение',
  CRITICAL: 'Критический',
  DELAY: 'Задержка',
  DISCONNECTED: 'Отключено',
  EVENT: 'Событие',
  HIGH: 'Высокий',
  HUB: 'Узел',
  IDLE: 'Ожидание',
  INCIDENT: 'Инцидент',
  IN_PROGRESS: 'В работе',
  INTERMEDIATE: 'Промежуточная',
  LOW: 'Низкий',
  MEDIUM: 'Средний',
  ON_ROUTE: 'В пути',
  ORIGIN: 'Начальная',
  OVERLOAD: 'Перегрузка',
  OVERLOADED: 'Перегружен',
  RECONNECTING: 'Переподключение',
  REGISTERED: 'Зарегистрировано',
  REPAIR: 'Ремонт',
  RESOLVED: 'Устранено',
  ROUTE_SEGMENT: 'Участок маршрута',
  SEGMENT: 'Участок',
  STATION: 'Станция',
  TERMINAL: 'Конечная',
  TRAIN: 'Поезд',
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return 'Нет отметки времени'
  }

  return dateTimeFormatter.format(new Date(value))
}

export function formatFullDateTime(value?: string | null) {
  if (!value) {
    return 'Нет отметки времени'
  }

  return fullDateTimeFormatter.format(new Date(value))
}

export function formatCompactNumber(value?: number | null) {
  if (value === undefined || value === null) {
    return '0'
  }

  return compactNumberFormatter.format(value)
}

export function formatPercent(value?: number | null) {
  if (value === undefined || value === null) {
    return 'н/д'
  }

  const prefix = value > 0 ? '+' : ''
  return `${prefix}${percentFormatter.format(value)}%`
}

export function formatDistanceKm(value?: number | null) {
  if (value === undefined || value === null) {
    return 'н/д'
  }

  return `${value.toFixed(1)} км`
}

export function formatSpeed(value?: number | null) {
  if (value === undefined || value === null) {
    return 'н/д'
  }

  return `${Math.round(value)} км/ч`
}

export function formatStatusLabel(value: string) {
  const normalizedValue = value.trim().toUpperCase()
  if (localizedLabels[normalizedValue]) {
    return localizedLabels[normalizedValue]
  }

  return value
    .toLowerCase()
    .split('_')
    .map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1))
    .join(' ')
}

export function toLocalInputValue(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}`
}
