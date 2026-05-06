import {
  CalendarRange,
  CircleHelp,
  Filter,
  MapPinned,
  PanelLeftClose,
  Radar,
  RefreshCcw,
  Route,
  RotateCcw,
  TrainFront,
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
      | 'includeKpi',
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

const departmentOptions = [
  'MZD-MSK',
  'MZD-BEL',
  'MZD-GOR',
  'MZD-KIE',
  'MZD-KUR',
  'MZD-KZN',
  'MZD-LEN',
  'MZD-PAV',
  'MZD-RIG',
  'MZD-SAV',
  'MZD-YAR',
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
                  aria-pressed={draftFilters.layerFilter[option.field]}
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
              aria-pressed={draftFilters.includeKpi}
              type="button"
              onClick={() =>
                onDraftFieldChange('includeKpi', !draftFilters.includeKpi)
              }
            >
              Показатели
            </button>
          </div>
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
                aria-pressed={draftFilters.eventStatuses.includes(status)}
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
            <CalendarRange size={16} />
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
              <span className={styles.fieldLabel}>
                Подразделения
                <span className={styles.helpWrap}>
                  <button
                    className={styles.helpButton}
                    type="button"
                    aria-label="Список подразделений"
                  >
                    <CircleHelp size={15} />
                  </button>
                  <span className={styles.tooltip} role="tooltip">
                    <span className={styles.tooltipList}>
                      {departmentOptions.map((departmentCode) => (
                        <span key={departmentCode}>{departmentCode}</span>
                      ))}
                    </span>
                  </span>
                </span>
              </span>
              <input
                type="text"
                list="department-code-options"
                placeholder="MZD-MSK, MZD-KUR, MZD-KZN"
                value={draftFilters.departmentCodesInput}
                onChange={(event) =>
                  onDraftFieldChange('departmentCodesInput', event.target.value)
                }
              />
              <datalist id="department-code-options">
                {departmentOptions.map((departmentCode) => (
                  <option key={departmentCode} value={departmentCode} />
                ))}
              </datalist>
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
