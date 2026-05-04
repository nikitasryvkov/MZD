import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Clock3,
  MapPin,
  Route,
  TrainFront,
  UserRound,
  Warehouse,
  X,
} from 'lucide-react'
import { ApiError, formatApiErrorDescription } from '@/shared/api/http'
import {
  formatDateTime,
  formatDistanceKm,
  formatFullDateTime,
  formatSpeed,
  formatStatusLabel,
} from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Skeleton } from '@/shared/ui/Skeleton'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useToast } from '@/shared/ui/useToast'
import {
  dashboardKeys,
} from '@/features/dashboard/api/dashboardKeys'
import {
  getOperationalEventDetails,
  patchOperationalEventStatus,
  type DashboardQueryResponse,
  type EventStatus,
  type GeoJsonFeature,
} from '@/features/dashboard/api/dashboardApi'
import { resolveDashboardGeoJsonSources } from '@/features/dashboard/model/dashboardGeoJson'
import { normalizeFieldErrors } from '@/features/dashboard/model/dashboardFilters'
import type { DashboardSelection } from '@/features/dashboard/model/dashboardReducer'
import styles from './ObjectDetailsDrawer.module.css'

interface ObjectDetailsDrawerProps {
  selection: DashboardSelection | null
  snapshot?: DashboardQueryResponse
  onClose: () => void
}

function EventStatusForm({
  eventId,
  allowedTransitions,
}: {
  eventId: string
  allowedTransitions: EventStatus[]
}) {
  const queryClient = useQueryClient()
  const { pushToast } = useToast()
  const [nextStatus, setNextStatus] = useState<EventStatus | ''>(
    allowedTransitions[0] ?? '',
  )
  const [comment, setComment] = useState('')
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})
  const [conflictMessage, setConflictMessage] = useState<string>()

  const updateStatusMutation = useMutation({
    mutationFn: (payload: { eventId: string; newStatus: EventStatus; comment: string }) =>
      patchOperationalEventStatus(payload.eventId, {
        newStatus: payload.newStatus,
        comment: payload.comment || undefined,
      }),
    onSuccess: async (response) => {
      setComment('')
      setFormErrors({})
      setConflictMessage(undefined)
      setNextStatus(response.allowedTransitions[0] ?? '')

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: dashboardKeys.snapshotRoot }),
        queryClient.invalidateQueries({
          queryKey: dashboardKeys.event(response.eventId),
        }),
      ])

      pushToast({
        tone: 'success',
        title: 'Изменения сохранены',
        description: response.summary,
      })
    },
    onError: async (error) => {
      if (!(error instanceof ApiError)) {
        pushToast({
          tone: 'danger',
          title: 'Не удалось сохранить изменения',
          description: 'Изменение статуса не сохранено.',
        })
        return
      }

      if (error.status === 400) {
        setFormErrors(normalizeFieldErrors(error.payload?.fieldErrors))
        return
      }

      if (error.status === 409) {
        setConflictMessage(formatApiErrorDescription(error))
        setFormErrors({})
        await queryClient.invalidateQueries({
          queryKey: dashboardKeys.event(eventId),
        })
        return
      }

      pushToast({
        tone: error.status >= 500 || error.status === 0 ? 'danger' : 'warning',
        title:
          error.status === 0
            ? 'Соединение с сервером недоступно'
            : `Ошибка обновления события (${error.status})`,
        description: formatApiErrorDescription(error),
      })
    },
  })

  if (!allowedTransitions.length) {
    return (
      <EmptyState
        title="Дальнейшее изменение недоступно"
        description="Для этого события больше нет доступных статусов."
      />
    )
  }

  return (
    <form
      className={styles.form}
      onSubmit={(formEvent) => {
        formEvent.preventDefault()

        if (!nextStatus) {
          setFormErrors({
            newStatus: 'Сначала выберите следующее состояние.',
          })
          return
        }

        setFormErrors({})
        setConflictMessage(undefined)
        updateStatusMutation.mutate({
          eventId,
          newStatus: nextStatus,
          comment,
        })
      }}
    >
      <label className={styles.field}>
        <span>Новый статус</span>
        <select
          value={nextStatus}
          onChange={(event) => setNextStatus(event.target.value as EventStatus)}
        >
          <option value="" disabled>
            Выберите следующий статус
          </option>
          {allowedTransitions.map((status) => (
            <option key={status} value={status}>
              {formatStatusLabel(status)}
            </option>
          ))}
        </select>
        {formErrors.newStatus ? <small>{formErrors.newStatus}</small> : null}
      </label>

      <label className={styles.field}>
        <span>Комментарий</span>
        <textarea
          value={comment}
          placeholder="Комментарий"
          onChange={(event) => setComment(event.target.value)}
        />
        {formErrors.comment ? <small>{formErrors.comment}</small> : null}
      </label>

      {conflictMessage ? (
        <div className={styles.inlineAlert}>{conflictMessage}</div>
      ) : null}

      <button type="submit" disabled={updateStatusMutation.isPending}>
        {updateStatusMutation.isPending ? 'Сохранение...' : 'Сохранить'}
      </button>
    </form>
  )
}

function renderInfoRows(rows: Array<{ label: string; value: string }>) {
  return (
    <dl className={styles.infoGrid}>
      {rows.map((row) => (
        <div key={row.label} className={styles.infoCard}>
          <dt>{row.label}</dt>
          <dd>{row.value}</dd>
        </div>
      ))}
    </dl>
  )
}

function readString(value: unknown) {
  return typeof value === 'string' ? value : undefined
}

function readNumber(value: unknown) {
  return typeof value === 'number' ? value : undefined
}

function readPointCoordinates(feature?: GeoJsonFeature) {
  if (!feature || feature.geometry.type !== 'Point' || !Array.isArray(feature.geometry.coordinates)) {
    return null
  }

  const [longitude, latitude] = feature.geometry.coordinates
  return typeof latitude === 'number' && typeof longitude === 'number'
    ? { latitude, longitude }
    : null
}

function findGeoJsonFeature(
  snapshot: DashboardQueryResponse | undefined,
  selection: DashboardSelection | null,
) {
  if (!snapshot || !selection) {
    return undefined
  }

  const geoJsonSources = resolveDashboardGeoJsonSources(snapshot.mapData)

  switch (selection.kind) {
    case 'station':
      return geoJsonSources.stations.features.find((feature) => feature.id === selection.id)
    case 'segment':
      return geoJsonSources.routeSegments.features.find((feature) => feature.id === selection.id)
    case 'train':
      return geoJsonSources.trains.features.find((feature) => feature.id === selection.id)
    case 'event':
      return geoJsonSources.operationalEvents.features.find((feature) => feature.id === selection.id)
  }
}

export function ObjectDetailsDrawer({
  selection,
  snapshot,
  onClose,
}: ObjectDetailsDrawerProps) {
  const { pushToast } = useToast()
  const selectedEventId = selection?.kind === 'event' ? selection.id : undefined
  const selectionOpenedAtRef = useRef(0)

  const eventDetailsQuery = useQuery({
    queryKey: dashboardKeys.event(selectedEventId ?? 'idle'),
    queryFn: ({ signal }) => getOperationalEventDetails(selectedEventId!, signal),
    enabled: Boolean(selectedEventId),
  })

  useEffect(() => {
    if (!(eventDetailsQuery.error instanceof ApiError)) {
      return
    }

    if (eventDetailsQuery.error.status === 404) {
      return
    }

    pushToast({
      tone:
        eventDetailsQuery.error.status >= 500 || eventDetailsQuery.error.status === 0
          ? 'danger'
          : 'warning',
      title:
        eventDetailsQuery.error.status === 0
          ? 'Детали события недоступны'
          : `Не удалось получить данные по событию (${eventDetailsQuery.error.status})`,
      description: formatApiErrorDescription(eventDetailsQuery.error),
    })
  }, [eventDetailsQuery.error, eventDetailsQuery.errorUpdatedAt, pushToast])

  useEffect(() => {
    if (selection) {
      selectionOpenedAtRef.current = window.performance.now()
    }
  }, [selection])

  const handleBackdropClick = () => {
    if (window.performance.now() - selectionOpenedAtRef.current < 120) {
      return
    }

    onClose()
  }

  const stations = snapshot?.mapData.stations ?? []
  const stationById = new Map(stations.map((station) => [station.id, station]))
  const selectedFeature = findGeoJsonFeature(snapshot, selection)

  let title = 'Детали объекта'
  let subtitle = 'Выберите объект на карте или событие в списке, чтобы посмотреть подробности.'
  let icon = Warehouse
  let content = (
    <EmptyState
      title="Ничего не выбрано"
      description="Нажмите на объект карты или событие в списке, чтобы открыть подробности."
    />
  )

  if (selection && snapshot) {
    if (selection.kind === 'event') {
      title = eventDetailsQuery.data?.title ?? 'Оперативное событие'
      subtitle = 'Сведения по событию и изменение статуса.'
      icon = Clock3

      if (eventDetailsQuery.isLoading) {
        content = (
          <div className={styles.loadingState}>
            <Skeleton className={styles.headerSkeleton} />
            <Skeleton className={styles.sectionSkeleton} />
            <Skeleton className={styles.sectionSkeleton} />
          </div>
        )
      } else if (eventDetailsQuery.error instanceof ApiError && eventDetailsQuery.error.status === 404) {
        content = (
          <EmptyState
            title="Событие не найдено"
            description="Событие больше недоступно."
          />
        )
      } else if (eventDetailsQuery.data) {
        const event = eventDetailsQuery.data
        content = (
          <div className={styles.sectionStack}>
            <section className={styles.section}>
              <div className={styles.badgeRow}>
                <StatusBadge value={event.status} />
                <StatusBadge kind="severity" value={event.severity} />
              </div>
              <p className={styles.description}>
                {event.description ?? 'Описание отсутствует.'}
              </p>
              {renderInfoRows([
                { label: 'Тип', value: formatStatusLabel(event.type) },
                { label: 'Затронутый участок', value: event.affectedSection ?? 'Неизвестно' },
                { label: 'Время начала', value: formatFullDateTime(event.startedAt) },
                { label: 'Время обновления', value: formatFullDateTime(event.updatedAt) },
                { label: 'Последний автор изменения', value: event.lastChangedBy ?? 'Неизвестно' },
                {
                  label: 'Координаты',
                  value: `${event.latitude.toFixed(4)}, ${event.longitude.toFixed(4)}`,
                },
              ])}
            </section>

            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <h3>История статусов</h3>
                <small>Сначала последние изменения</small>
              </div>
              {event.statusHistory.length ? (
                <div className={styles.historyList}>
                  {event.statusHistory.map((historyItem) => (
                    <article key={historyItem.id} className={styles.historyRow}>
                      <div>
                        <strong>{formatFullDateTime(historyItem.changedAt)}</strong>
                        <small>{historyItem.changedBy ?? 'Не указано'}</small>
                      </div>
                      <div className={styles.historyBadges}>
                        {historyItem.fromStatus ? (
                          <StatusBadge value={historyItem.fromStatus} />
                        ) : null}
                        <span className={styles.arrow}>-&gt;</span>
                        <StatusBadge value={historyItem.toStatus} />
                      </div>
                      <p>{historyItem.comment ?? 'Без комментария'}</p>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="История пуста"
                  description="Изменения по событию не зарегистрированы."
                />
              )}
            </section>

            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <h3>Изменить статус события</h3>
                <small>Выберите новый статус</small>
              </div>

              <EventStatusForm
                key={`${event.id}:${event.updatedAt}`}
                eventId={event.id}
                allowedTransitions={event.allowedTransitions}
              />
            </section>
          </div>
        )
      }
    }

    if (selection.kind === 'station') {
      const station = snapshot.mapData.stations.find((item) => item.id === selection.id)
      const stationProperties = selectedFeature?.properties ?? {}
      const stationCoordinates = readPointCoordinates(selectedFeature)
      title = station?.name ?? readString(stationProperties.name) ?? 'Станция'
      subtitle = 'Сведения по станции.'
      icon = Warehouse
      content = station || selectedFeature ? (
        <div className={styles.sectionStack}>
          <section className={styles.section}>
            <div className={styles.badgeRow}>
              <StatusBadge
                value={station?.stationType ?? readString(stationProperties.stationType) ?? 'UNKNOWN'}
              />
            </div>
            {renderInfoRows([
              {
                label: 'Код',
                value: station?.code ?? readString(stationProperties.code) ?? 'н/д',
              },
              {
                label: 'Тип',
                value: formatStatusLabel(
                  station?.stationType ?? readString(stationProperties.stationType) ?? 'UNKNOWN',
                ),
              },
              {
                label: 'Координаты',
                value: station
                  ? `${station.latitude.toFixed(4)}, ${station.longitude.toFixed(4)}`
                  : stationCoordinates
                    ? `${stationCoordinates.latitude.toFixed(4)}, ${stationCoordinates.longitude.toFixed(4)}`
                    : 'н/д',
              },
            ])}
          </section>
        </div>
      ) : (
        content
      )
    }

    if (selection.kind === 'segment') {
      const segment = snapshot.mapData.routeSegments.find((item) => item.id === selection.id)
      const segmentProperties = selectedFeature?.properties ?? {}
      const fromStationId =
        segment?.fromStationId ?? readString(segmentProperties.fromStationId) ?? ''
      const toStationId =
        segment?.toStationId ?? readString(segmentProperties.toStationId) ?? ''
      const fromStationLabel = stationById.get(fromStationId)?.name || fromStationId || 'Неизвестно'
      const toStationLabel = stationById.get(toStationId)?.name || toStationId || 'Неизвестно'
      title = segment
        ? `${stationById.get(segment.fromStationId)?.name ?? 'Неизвестно'} -> ${stationById.get(segment.toStationId)?.name ?? 'Неизвестно'}`
        : `${fromStationLabel} -> ${toStationLabel}`
      if (!title.includes('->')) {
        title = 'Участок маршрута'
      }
      subtitle = 'Сведения по участку.'
      icon = Route
      content = segment || selectedFeature ? (
        <div className={styles.sectionStack}>
          <section className={styles.section}>
            <div className={styles.badgeRow}>
              {segment?.status ?? readString(segmentProperties.status) ? (
                <StatusBadge value={segment?.status ?? readString(segmentProperties.status) ?? 'UNKNOWN'} />
              ) : null}
            </div>
            {renderInfoRows([
              {
                label: 'От станции',
                value: fromStationLabel,
              },
              {
                label: 'До станции',
                value: toStationLabel,
              },
              {
                label: 'Длина',
                value: formatDistanceKm(segment?.lengthKm ?? readNumber(segmentProperties.lengthKm)),
              },
              {
                label: 'Тип геометрии',
                value: segment?.geometry.type ?? selectedFeature?.geometry.type ?? 'н/д',
              },
            ])}
          </section>
        </div>
      ) : (
        content
      )
    }

    if (selection.kind === 'train') {
      const train = snapshot.mapData.trains.find((item) => item.id === selection.id)
      const trainProperties = selectedFeature?.properties ?? {}
      title = train?.trainNumber ?? readString(trainProperties.trainNumber) ?? 'Поезд'
      subtitle = 'Сведения по поезду.'
      icon = TrainFront
      content = train || selectedFeature ? (
        <div className={styles.sectionStack}>
          <section className={styles.section}>
            <div className={styles.badgeRow}>
              <StatusBadge value={train?.status ?? readString(trainProperties.status) ?? 'UNKNOWN'} />
            </div>
            {renderInfoRows([
              {
                label: 'Номер поезда',
                value: train?.trainNumber ?? readString(trainProperties.trainNumber) ?? 'н/д',
              },
              {
                label: 'Текущая станция',
                value:
                  stationById.get(
                    train?.currentStationId ?? readString(trainProperties.currentStationId) ?? '',
                  )?.name ?? 'Неизвестно',
              },
              {
                label: 'Следующая станция',
                value:
                  stationById.get(
                    train?.nextStationId ?? readString(trainProperties.nextStationId) ?? '',
                  )?.name ?? 'Неизвестно',
              },
              {
                label: 'Прогресс',
                value:
                  (train?.progressPercent ?? readNumber(trainProperties.progressPercent)) !== undefined
                    ? `${(train?.progressPercent ?? readNumber(trainProperties.progressPercent) ?? 0).toFixed(0)}%`
                    : 'н/д',
              },
              {
                label: 'Скорость',
                value: formatSpeed(train?.speedKmh ?? readNumber(trainProperties.speedKmh)),
              },
              {
                label: 'Последнее обновление',
                value: formatFullDateTime(
                  train?.lastUpdated ?? readString(trainProperties.lastUpdated),
                ),
              },
            ])}
          </section>
        </div>
      ) : (
        content
      )
    }
  }

  const Icon = icon

  return (
    <div className={styles.shell} data-open={Boolean(selection)}>
      {selection ? (
        <button
          type="button"
          className={styles.backdrop}
          aria-label="Закрыть панель деталей"
          onClick={handleBackdropClick}
        />
      ) : null}

      <aside className={styles.drawer} aria-hidden={!selection}>
        <header className={styles.header}>
          <div className={styles.headerContent}>
            <div className={styles.iconWrap}>
              <Icon size={18} />
            </div>
            <div>
              <p className={styles.eyebrow}>Детали объекта</p>
              <h2>{title}</h2>
              <p className={styles.subtitle}>{subtitle}</p>
            </div>
          </div>
          <button
            className={styles.close}
            type="button"
            aria-label="Закрыть панель"
            onClick={onClose}
          >
            <X size={18} />
          </button>
        </header>

        <div className={styles.summaryRow}>
          <div>
            <MapPin size={14} />
            <span>Выбранный объект</span>
          </div>
          <div>
            <UserRound size={14} />
            <span>{selection ? formatStatusLabel(selection.kind) : 'Ничего не выбрано'}</span>
          </div>
          <div>
            <Clock3 size={14} />
            <span>{snapshot ? formatDateTime(snapshot.generatedAt) : 'Нет данных'}</span>
          </div>
        </div>

        <div className={styles.content}>{content}</div>
      </aside>
    </div>
  )
}
