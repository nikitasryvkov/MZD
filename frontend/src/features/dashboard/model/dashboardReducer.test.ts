import { describe, expect, it } from 'vitest'
import {
  createInitialDashboardState,
  dashboardReducer,
  type DashboardState,
} from './dashboardReducer'

const bbox = {
  minLat: 55,
  minLon: 37,
  maxLat: 56,
  maxLon: 38,
}

describe('dashboardReducer', () => {
  it('keeps bbox when filters are reset', () => {
    const state = dashboardReducer(createInitialDashboardState(), {
      type: 'sync-bbox',
      bbox,
    })

    const nextState = dashboardReducer(state, { type: 'reset-filters' })

    expect(nextState.draftFilters.bbox).toEqual(bbox)
    expect(nextState.appliedFilters.bbox).toEqual(bbox)
  })

  it('does not recreate state when bbox changes are inside the comparison epsilon', () => {
    const state = dashboardReducer(createInitialDashboardState(), {
      type: 'sync-bbox',
      bbox,
    })
    const closeBbox = {
      minLat: bbox.minLat + 0.0001,
      minLon: bbox.minLon,
      maxLat: bbox.maxLat,
      maxLon: bbox.maxLon,
    }

    const nextState = dashboardReducer(state, {
      type: 'sync-bbox',
      bbox: closeBbox,
    })

    expect(nextState).toBe(state)
  })

  it('clears selected objects when filters are applied', () => {
    const state: DashboardState = {
      ...createInitialDashboardState(),
      selectedObject: {
        kind: 'event',
        id: 'event-1',
      },
    }

    const nextState = dashboardReducer(state, { type: 'apply-filters' })

    expect(nextState.selectedObject).toBeNull()
  })
})
