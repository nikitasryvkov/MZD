import { describe, expect, it } from 'vitest'
import {
  buildDashboardQueryRequest,
  createInitialDashboardDraftFilters,
  parseDepartmentCodes,
  validateDashboardFilters,
} from './dashboardFilters'

describe('dashboardFilters', () => {
  it('deduplicates department codes from comma, semicolon and newline separated input', () => {
    expect(parseDepartmentCodes('DCS-01, DCS-02\nDCS-01; DCS-03')).toEqual([
      'DCS-01',
      'DCS-02',
      'DCS-03',
    ])
  })

  it('validates incomplete and inverted time ranges before request creation', () => {
    const missingTo = createInitialDashboardDraftFilters()
    missingTo.timeRangeFrom = '2026-05-01T10:00'

    expect(validateDashboardFilters(missingTo)).toHaveProperty('timeRangeTo')

    const invertedRange = createInitialDashboardDraftFilters()
    invertedRange.timeRangeFrom = '2026-05-01T11:00'
    invertedRange.timeRangeTo = '2026-05-01T10:00'

    expect(validateDashboardFilters(invertedRange)).toHaveProperty('timeRangeTo')
  })

  it('builds an API request with normalized optional filters', () => {
    const draftFilters = createInitialDashboardDraftFilters()
    draftFilters.timeRangeFrom = '2026-05-01T10:00'
    draftFilters.timeRangeTo = '2026-05-01T11:00'
    draftFilters.departmentCodesInput = 'DCS-01, DCS-01'
    draftFilters.eventStatuses = ['REGISTERED']

    const request = buildDashboardQueryRequest(draftFilters)

    expect(request.departmentCodes).toEqual(['DCS-01'])
    expect(request.eventStatuses).toEqual(['REGISTERED'])
    expect(new Date(request.timeRange?.from ?? '').getTime()).toBe(
      new Date('2026-05-01T10:00').getTime(),
    )
    expect(new Date(request.timeRange?.to ?? '').getTime()).toBe(
      new Date('2026-05-01T11:00').getTime(),
    )
  })

  it('keeps personnel data opt-in for non-analytics requests', () => {
    const draftFilters = createInitialDashboardDraftFilters()

    expect(buildDashboardQueryRequest(draftFilters).includePersonnel).toBe(false)
    expect(
      buildDashboardQueryRequest(draftFilters, { includePersonnel: true }).includePersonnel,
    ).toBe(true)
  })
})
