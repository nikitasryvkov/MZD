import type { DashboardQueryRequest } from '@/features/dashboard/api/dashboardApi'
import {
  areBoundingBoxesEqual,
  buildDashboardQueryRequest,
  createInitialDashboardDraftFilters,
  type DraftDashboardFilters,
} from './dashboardFilters'

export type DashboardObjectKind = 'station' | 'segment' | 'train' | 'event'

export interface DashboardSelection {
  kind: DashboardObjectKind
  id: string
}

export interface DashboardState {
  draftFilters: DraftDashboardFilters
  appliedFilters: DashboardQueryRequest
  inlineErrors: Record<string, string>
  selectedObject: DashboardSelection | null
}

type DashboardAction =
  | {
      type: 'update-draft'
      field:
        | 'timeRangeFrom'
        | 'timeRangeTo'
        | 'departmentCodesInput'
        | 'includeKpi'
      value: string | boolean
    }
  | {
      type: 'toggle-layer'
      field: keyof DraftDashboardFilters['layerFilter']
    }
  | {
      type: 'toggle-event-status'
      status: NonNullable<DraftDashboardFilters['eventStatuses']>[number]
    }
  | {
      type: 'apply-filters'
    }
  | {
      type: 'reset-filters'
    }
  | {
      type: 'sync-bbox'
      bbox: DraftDashboardFilters['bbox']
    }
  | {
      type: 'set-inline-errors'
      errors: Record<string, string>
    }
  | {
      type: 'clear-inline-errors'
    }
  | {
      type: 'select-object'
      selection: DashboardSelection
    }
  | {
      type: 'close-drawer'
    }

export function createInitialDashboardState(): DashboardState {
  const draftFilters = createInitialDashboardDraftFilters()

  return {
    draftFilters,
    appliedFilters: buildDashboardQueryRequest(draftFilters),
    inlineErrors: {},
    selectedObject: null,
  }
}

export function dashboardReducer(
  state: DashboardState,
  action: DashboardAction,
): DashboardState {
  switch (action.type) {
    case 'update-draft': {
      return {
        ...state,
        draftFilters: {
          ...state.draftFilters,
          [action.field]: action.value,
        },
      }
    }

    case 'toggle-layer': {
      return {
        ...state,
        draftFilters: {
          ...state.draftFilters,
          layerFilter: {
            ...state.draftFilters.layerFilter,
            [action.field]: !state.draftFilters.layerFilter[action.field],
          },
        },
      }
    }

    case 'toggle-event-status': {
      const hasStatus = state.draftFilters.eventStatuses.includes(action.status)

      return {
        ...state,
        draftFilters: {
          ...state.draftFilters,
          eventStatuses: hasStatus
            ? state.draftFilters.eventStatuses.filter((status) => status !== action.status)
            : [...state.draftFilters.eventStatuses, action.status],
        },
      }
    }

    case 'apply-filters': {
      return {
        ...state,
        appliedFilters: buildDashboardQueryRequest(state.draftFilters),
        inlineErrors: {},
        selectedObject: null,
      }
    }

    case 'reset-filters': {
      const draftFilters = createInitialDashboardDraftFilters()
      draftFilters.bbox = state.draftFilters.bbox

      return {
        ...state,
        draftFilters,
        appliedFilters: buildDashboardQueryRequest(draftFilters),
        inlineErrors: {},
        selectedObject: null,
      }
    }

    case 'sync-bbox': {
      if (areBoundingBoxesEqual(state.draftFilters.bbox, action.bbox)) {
        return state
      }

      const draftFilters = {
        ...state.draftFilters,
        bbox: action.bbox,
      }

      return {
        ...state,
        draftFilters,
        appliedFilters: buildDashboardQueryRequest(draftFilters),
      }
    }

    case 'set-inline-errors': {
      return {
        ...state,
        inlineErrors: action.errors,
      }
    }

    case 'clear-inline-errors': {
      return {
        ...state,
        inlineErrors: {},
      }
    }

    case 'select-object': {
      return {
        ...state,
        selectedObject: action.selection,
      }
    }

    case 'close-drawer': {
      return {
        ...state,
        selectedObject: null,
      }
    }
  }
}
