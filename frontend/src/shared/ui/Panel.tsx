import type { PropsWithChildren, ReactNode } from 'react'
import styles from './Panel.module.css'

type PanelAccent = 'cool' | 'warm' | 'neutral'

interface PanelProps extends PropsWithChildren {
  title: string
  subtitle?: string
  eyebrow?: string
  actions?: ReactNode
  accent?: PanelAccent
  className?: string
}

export function Panel({
  title,
  subtitle,
  eyebrow,
  actions,
  accent = 'neutral',
  className,
  children,
}: PanelProps) {
  return (
    <section
      className={[styles.panel, className].filter(Boolean).join(' ')}
      data-accent={accent}
    >
      <header className={styles.header}>
        <div>
          {eyebrow ? <p className={styles.eyebrow}>{eyebrow}</p> : null}
          <h2>{title}</h2>
          {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
        </div>
        {actions ? <div className={styles.actions}>{actions}</div> : null}
      </header>
      <div className={styles.content}>{children}</div>
    </section>
  )
}
