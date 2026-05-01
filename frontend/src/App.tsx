import { AppRouter } from '@/app/router/AppRouter'
import { ErrorBoundary } from '@/shared/ui/ErrorBoundary'

export default function App() {
  return (
    <ErrorBoundary>
      <AppRouter />
    </ErrorBoundary>
  )
}
