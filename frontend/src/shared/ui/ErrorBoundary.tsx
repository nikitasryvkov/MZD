import type { ErrorInfo, PropsWithChildren } from 'react'
import { Component } from 'react'
import styles from './ErrorBoundary.module.css'

interface ErrorBoundaryState {
  hasError: boolean
}

export class ErrorBoundary extends Component<PropsWithChildren, ErrorBoundaryState> {
  override state: ErrorBoundaryState = {
    hasError: false,
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  override componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Критическая ошибка рендера', error, errorInfo)
  }

  override render() {
    if (this.state.hasError) {
      return (
        <div className={styles.shell}>
          <div className={styles.card}>
            <p className={styles.eyebrow}>Ошибка</p>
            <h1>Не удалось открыть страницу</h1>
            <p>
              Обновите страницу и повторите попытку.
            </p>
            <button
              className={styles.button}
              type="button"
              onClick={() => window.location.reload()}
            >
              Обновить страницу
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
