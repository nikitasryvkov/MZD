import type {
  ApiFieldError,
} from '@/shared/api/http'
import type {
  BoundingBox,
  DashboardQueryRequest,
  EventStatus,
  LayerFilter,
} from '@/features/dashboard/api/dashboardApi'

export interface DraftDashboardFilters {
  bbox: BoundingBox | null
  layerFilter: LayerFilter
  timeRangeFrom: string
  timeRangeTo: string
  eventStatuses: EventStatus[]
  departmentCodesInput: string
  includeKpi: boolean
}

export const EVENT_STATUS_OPTIONS: EventStatus[] = [
  'REGISTERED',
  'IN_PROGRESS',
  'RESOLVED',
  'CANCELED',
]

export function createInitialDashboardDraftFilters(): DraftDashboardFilters {
  return {
    bbox: null,
    layerFilter: {
      showStations: true,
      showSegments: true,
      showTrains: true,
      showEvents: true,
    },
    timeRangeFrom: '',
    timeRangeTo: '',
    eventStatuses: [],
    departmentCodesInput: '',
    includeKpi: true,
  }
}

export function parseDepartmentCodes(input: string) {
  return Array.from(
    new Set(
      input
        .split(/[\n,;]+/)
        .map((value) => value.trim())
        .filter(Boolean),
    ),
  )
}

export function buildDashboardQueryRequest(
  draftFilters: DraftDashboardFilters,
  options: { includePersonnel?: boolean } = {},
): DashboardQueryRequest {
  const departmentCodes = parseDepartmentCodes(draftFilters.departmentCodesInput)
  const hasTimeRange = draftFilters.timeRangeFrom && draftFilters.timeRangeTo

  return {
    bbox: draftFilters.bbox ?? undefined,
    layerFilter: draftFilters.layerFilter,
    timeRange: hasTimeRange
      ? {
          from: new Date(draftFilters.timeRangeFrom).toISOString(),
          to: new Date(draftFilters.timeRangeTo).toISOString(),
        }
      : undefined,
    eventStatuses: draftFilters.eventStatuses.length
      ? draftFilters.eventStatuses
      : undefined,
    departmentCodes: departmentCodes.length ? departmentCodes : undefined,
    includeKpi: draftFilters.includeKpi,
    includePersonnel: options.includePersonnel ?? false,
  }
}

export function createInitialDashboardQueryRequest(
  options: { includePersonnel?: boolean } = {},
): DashboardQueryRequest {
  return buildDashboardQueryRequest(createInitialDashboardDraftFilters(), options)
}

export function validateDashboardFilters(draftFilters: DraftDashboardFilters) {
  const errors: Record<string, string> = {}

  if (draftFilters.timeRangeFrom && !draftFilters.timeRangeTo) {
    errors.timeRangeTo = 'Укажите время окончания, если задано время начала.'
  }

  if (!draftFilters.timeRangeFrom && draftFilters.timeRangeTo) {
    errors.timeRangeFrom = 'Укажите время начала, если задано время окончания.'
  }

  if (draftFilters.timeRangeFrom && draftFilters.timeRangeTo) {
    const from = new Date(draftFilters.timeRangeFrom)
    const to = new Date(draftFilters.timeRangeTo)

    if (Number.isNaN(from.valueOf())) {
      errors.timeRangeFrom = 'Некорректное время начала.'
    }

    if (Number.isNaN(to.valueOf())) {
      errors.timeRangeTo = 'Некорректное время окончания.'
    }

    if (!errors.timeRangeFrom && !errors.timeRangeTo && from >= to) {
      errors.timeRangeTo = 'Время окончания должно быть позже времени начала.'
    }
  }

  if (parseDepartmentCodes(draftFilters.departmentCodesInput).length > 100) {
    errors.departmentCodesInput = 'Укажите не более 100 кодов подразделений.'
  }

  return errors
}

export function normalizeFieldErrors(fieldErrors: ApiFieldError[] = []) {
  return fieldErrors.reduce<Record<string, string>>((accumulator, fieldError) => {
    if (fieldError.field.startsWith('departmentCodes')) {
      accumulator.departmentCodesInput = fieldError.message
      return accumulator
    }

    if (fieldError.field === 'timeRange.from') {
      accumulator.timeRangeFrom = fieldError.message
      return accumulator
    }

    if (fieldError.field === 'timeRange.to') {
      accumulator.timeRangeTo = fieldError.message
      return accumulator
    }

    accumulator[fieldError.field] = fieldError.message
    return accumulator
  }, {})
}

export function areBoundingBoxesEqual(
  left: BoundingBox | null,
  right: BoundingBox | null,
) {
  const epsilon = 0.0005

  if (left === right) {
    return true
  }

  if (!left || !right) {
    return false
  }

  return (
    Math.abs(left.minLat - right.minLat) <= epsilon &&
    Math.abs(left.minLon - right.minLon) <= epsilon &&
    Math.abs(left.maxLat - right.maxLat) <= epsilon &&
    Math.abs(left.maxLon - right.maxLon) <= epsilon
  )
}
