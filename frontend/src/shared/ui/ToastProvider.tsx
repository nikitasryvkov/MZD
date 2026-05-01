import {
  useEffect,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react'
import { X } from 'lucide-react'
import { ToastContext, type ToastInput } from './useToast'
import styles from './ToastProvider.module.css'

type ToastTone = 'info' | 'success' | 'warning' | 'danger'

interface ToastItem {
  id: string
  title: string
  description?: string
  tone: ToastTone
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const timersRef = useRef<Record<string, number>>({})

  function removeToast(id: string) {
    const timer = timersRef.current[id]
    if (timer) {
      window.clearTimeout(timer)
      delete timersRef.current[id]
    }

    setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== id))
  }

  useEffect(() => {
    const timers = timersRef.current

    return () => {
      for (const timer of Object.values(timers)) {
        window.clearTimeout(timer)
      }
    }
  }, [])

  function pushToast({ title, description, tone = 'info' }: ToastInput) {
    const id = crypto.randomUUID()
    setToasts((currentToasts) => [...currentToasts, { id, title, description, tone }])

    timersRef.current[id] = window.setTimeout(() => {
      removeToast(id)
    }, 6000)
  }

  return (
    <ToastContext.Provider value={{ pushToast, removeToast }}>
      {children}
      <div className={styles.viewport} aria-live="polite" aria-atomic="true">
        {toasts.map((toast) => (
          <article key={toast.id} className={styles.toast} data-tone={toast.tone}>
            <div>
              <strong>{toast.title}</strong>
              {toast.description ? <p>{toast.description}</p> : null}
            </div>
            <button
              className={styles.close}
              type="button"
              onClick={() => removeToast(toast.id)}
              aria-label="Закрыть уведомление"
            >
              <X size={16} />
            </button>
          </article>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
