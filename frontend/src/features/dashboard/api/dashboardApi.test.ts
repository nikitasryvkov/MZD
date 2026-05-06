import { afterEach, describe, expect, it, vi } from 'vitest'
import { queryDashboardSnapshot } from './dashboardApi'
import { createInitialDashboardQueryRequest } from '../model/dashboardFilters'

function mockFetch(response: Response) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

describe('dashboardApi', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns a valid dashboard snapshot payload', async () => {
    const payload = {
      requestId: 'request-1',
      generatedAt: '2026-05-01T10:00:00Z',
      kpiSummary: null,
      personnelSummary: null,
      mapData: {
        stations: [],
        routeSegments: [],
        trains: [],
        operationalEvents: [],
      },
      eventsPreview: [],
    }
    mockFetch(
      new Response(JSON.stringify(payload), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    )

    await expect(
      queryDashboardSnapshot(createInitialDashboardQueryRequest()),
    ).resolves.toEqual(payload)
  })

  it('rejects malformed dashboard snapshot payloads before rendering', async () => {
    mockFetch(
      new Response(JSON.stringify({ requestId: 'request-1' }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    )

    await expect(
      queryDashboardSnapshot(createInitialDashboardQueryRequest()),
    ).rejects.toMatchObject({
      status: 0,
      message: 'Сервер вернул snapshot в неожиданном формате.',
    })
  })
})
