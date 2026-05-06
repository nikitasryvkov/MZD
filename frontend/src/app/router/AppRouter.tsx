import { lazy, Suspense, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/app/layout/AppShell'

const AnalyticsPage = lazy(() =>
  import('@/pages/analytics/AnalyticsPage').then((module) => ({
    default: module.AnalyticsPage,
  })),
)
const GeoAnalyticsDashboardPage = lazy(() =>
  import('@/pages/dashboard/GeoAnalyticsDashboardPage').then((module) => ({
    default: module.GeoAnalyticsDashboardPage,
  })),
)
const EventsPage = lazy(() =>
  import('@/pages/events/EventsPage').then((module) => ({
    default: module.EventsPage,
  })),
)
const NotFoundPage = lazy(() =>
  import('@/pages/not-found/NotFoundPage').then((module) => ({
    default: module.NotFoundPage,
  })),
)
function RouteFallback() {
  return (
    <div role="status" aria-live="polite">
      Загрузка...
    </div>
  )
}

function lazyRoute(element: ReactNode) {
  return <Suspense fallback={<RouteFallback />}>{element}</Suspense>
}

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={lazyRoute(<GeoAnalyticsDashboardPage />)} />
        <Route path="/events" element={lazyRoute(<EventsPage />)} />
        <Route path="/analytics" element={lazyRoute(<AnalyticsPage />)} />
        <Route path="/dashboard" element={<Navigate to="/" replace />} />
      </Route>
      <Route path="*" element={lazyRoute(<NotFoundPage />)} />
    </Routes>
  )
}
