import styles from './ProgressBar.module.css'

interface ProgressBarProps {
  active: boolean
}

export function ProgressBar({ active }: ProgressBarProps) {
  if (!active) {
    return <div className={styles.placeholder} aria-hidden="true" />
  }

  return (
    <div
      className={styles.progress}
      role="progressbar"
      aria-label="Обновление данных"
    />
  )
}
