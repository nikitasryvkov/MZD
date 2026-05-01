import type { DashboardQueryRequest } from './dashboardApi'

export const dashboardKeys = {
  snapshotRoot: ['dashboard', 'snapshot'] as const,
  snapshot: (request: DashboardQueryRequest) =>
    ['dashboard', 'snapshot', request] as const,
  event: (eventId: string) => ['dashboard', 'event', eventId] as const,
}
