import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/app/layout/AppShell'
import { AnalyticsPage } from '@/pages/analytics/AnalyticsPage'
import { GeoAnalyticsDashboardPage } from '@/pages/dashboard/GeoAnalyticsDashboardPage'
import { EventsPage } from '@/pages/events/EventsPage'
import { NotFoundPage } from '@/pages/not-found/NotFoundPage'
import { AboutPage } from '@/pages/about/AboutPage'

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<GeoAnalyticsDashboardPage />} />
        <Route path="/events" element={<EventsPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/dashboard" element={<Navigate to="/" replace />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
