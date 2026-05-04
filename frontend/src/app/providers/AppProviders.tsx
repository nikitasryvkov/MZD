import type { PropsWithChildren } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/app/auth/AuthProvider'
import { ApiError } from '@/shared/api/http'
import { ToastProvider } from '@/shared/ui/ToastProvider'

function shouldRetryQuery(failureCount: number, error: Error) {
  if (error instanceof ApiError && error.status > 0 && error.status < 500) {
    return false
  }

  return failureCount < 2
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: shouldRetryQuery,
      refetchOnWindowFocus: false,
    },
  },
})

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>{children}</AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}
