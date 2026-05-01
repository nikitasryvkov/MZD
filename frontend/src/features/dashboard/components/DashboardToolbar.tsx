import {
  Filter,
  MapPinned,
  PanelLeftClose,
  Radar,
  RefreshCcw,
  Route,
  RotateCcw,
  TrainFront,
  Users,
} from 'lucide-react'
import type { DraftDashboardFilters } from '@/features/dashboard/model/dashboardFilters'
import { EVENT_STATUS_OPTIONS } from '@/features/dashboard/model/dashboardFilters'
import { formatStatusLabel } from '@/shared/lib/format'
import { Panel } from '@/shared/ui/Panel'
import styles from './DashboardToolbar.module.css'

interface DashboardToolbarProps {
  draftFilters: DraftDashboardFilters
  inlineErrors: Record<string, string>
  isRefreshing: boolean
  onApply: () => void
  onRefresh: () => void
  onReset: () => void
  onHide?: () => void
  onDraftFieldChange: (
    field:
      | 'timeRangeFrom'
      | 'timeRangeTo'
      | 'departmentCodesInput'
      | 'includeKpi'
      | 'includePersonnel',
    value: string | boolean,
  ) => void
  onToggleLayer: (field: keyof DraftDashboardFilters['layerFilter']) => void
  onToggleStatus: (status: (typeof EVENT_STATUS_OPTIONS)[number]) => void
}

const layerOptions = [
  { field: 'showStations', label: 'Станции', icon: MapPinned },
  { field: 'showSegments', label: 'Участки', icon: Route },
  { field: 'showTrains', label: 'Поезда', icon: TrainFront },
  { field: 'showEvents', label: 'События', icon: Radar },
] as const

export function DashboardToolbar({
  draftFilters,
  inlineErrors,
  isRefreshing,
  onApply,
  onRefresh,
  onReset,
  onHide,
  onDraftFieldChange,
  onToggleLayer,
  onToggleStatus,
}: DashboardToolbarProps) {
  return (
    <Panel
      title="Настройки отображения"
      accent="cool"
      className={styles.toolbar}
      actions={
        <div className={styles.headerActions}>
          {onHide ? (
            <button type="button" onClick={onHide}>
              <PanelLeftClose size={16} />
              <span>Скрыть</span>
            </button>
          ) : null}
          <button className={styles.primaryButton} type="button" onClick={onApply}>
            Применить
          </button>
          <button type="button" onClick={onRefresh}>
            <RefreshCcw size={16} />
            <span>{isRefreshing ? 'Обновляем...' : 'Обновить'}</span>
          </button>
          <button type="button" onClick={onReset}>
            <RotateCcw size={16} />
            <span>Сбросить</span>
          </button>
        </div>
      }
    >
      <div className={styles.grid}>
        <section className={styles.group}>
          <div className={styles.groupHeader}>
            <Filter size={16} />
            <h3>Отображение</h3>
          </div>
          <div className={styles.pillGrid}>
            {layerOptions.map((option) => {
              const Icon = option.icon
              return (
                <button
                  key={option.field}
                  className={styles.pill}
                  data-active={draftFilters.layerFilter[option.field]}
                  type="button"
                  onClick={() => onToggleLayer(option.field)}
                >
                  <Icon size={16} />
                  <span>{option.label}</span>
                </button>
              )
            })}
          </div>

          <div className={styles.toggleRow}>
            <button
              className={styles.pill}
              data-active={draftFilters.includeKpi}
              type="button"
              onClick={() =>
                onDraftFieldChange('includeKpi', !draftFilters.includeKpi)
              }
            >
              Показатели
            </button>
            <button
              className={styles.pill}
              data-active={draftFilters.includePersonnel}
              type="button"
              onClick={() =>
                onDraftFieldChange(
                  'includePersonnel',
                  !draftFilters.includePersonnel,
                )
              }
            >
              Персонал
            </button>
          </div>

          {inlineErrors.includePersonnel ? (
            <small className={styles.analyticsError}>
              {inlineErrors.includePersonnel}
            </small>
          ) : null}
        </section>

        <section className={styles.group}>
          <div className={styles.groupHeader}>
            <Radar size={16} />
            <h3>Статусы событий</h3>
          </div>
          <div className={styles.pillGrid}>
            {EVENT_STATUS_OPTIONS.map((status) => (
              <button
                key={status}
                className={styles.pill}
                data-active={draftFilters.eventStatuses.includes(status)}
                type="button"
                onClick={() => onToggleStatus(status)}
              >
                {formatStatusLabel(status)}
              </button>
            ))}
          </div>
        </section>

        <section className={[styles.group, styles.periodGroup].join(' ')}>
          <div className={styles.groupHeader}>
            <Users size={16} />
            <h3>Период и подразделения</h3>
          </div>
          <div className={styles.fieldGrid}>
            <label className={styles.field}>
              <span>Начало периода</span>
              <input
                type="datetime-local"
                value={draftFilters.timeRangeFrom}
                onChange={(event) =>
                  onDraftFieldChange('timeRangeFrom', event.target.value)
                }
              />
              {inlineErrors.timeRangeFrom ? (
                <small>{inlineErrors.timeRangeFrom}</small>
              ) : null}
            </label>

            <label className={styles.field}>
              <span>Конец периода</span>
              <input
                type="datetime-local"
                value={draftFilters.timeRangeTo}
                onChange={(event) =>
                  onDraftFieldChange('timeRangeTo', event.target.value)
                }
              />
              {inlineErrors.timeRangeTo ? (
                <small>{inlineErrors.timeRangeTo}</small>
              ) : null}
            </label>

            <label className={[styles.field, styles.fieldWide].join(' ')}>
              <span>Подразделения</span>
              <input
                type="text"
                placeholder="DCS-01, DCS-02"
                value={draftFilters.departmentCodesInput}
                onChange={(event) =>
                  onDraftFieldChange('departmentCodesInput', event.target.value)
                }
              />
              {inlineErrors.departmentCodesInput ? (
                <small>{inlineErrors.departmentCodesInput}</small>
              ) : (
                <small>
                  Можно перечислить несколько кодов через запятую или с новой строки.
                </small>
              )}
            </label>
          </div>
        </section>
      </div>
    </Panel>
  )
}
