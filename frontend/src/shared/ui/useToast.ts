import { createContext, useContext } from 'react'

type ToastTone = 'info' | 'success' | 'warning' | 'danger'

export interface ToastInput {
  title: string
  description?: string
  tone?: ToastTone
}

interface ToastContextValue {
  pushToast: (toast: ToastInput) => void
  removeToast: (id: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const context = useContext(ToastContext)

  if (!context) {
    throw new Error('useToast must be used inside ToastProvider')
  }

  return context
}
