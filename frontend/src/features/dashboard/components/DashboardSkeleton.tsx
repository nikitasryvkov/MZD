import { Skeleton } from '@/shared/ui/Skeleton'
import styles from './DashboardSkeleton.module.css'

export function DashboardSkeleton() {
  return (
    <div className={styles.shell} aria-hidden="true">
      <Skeleton className={styles.toolbar} />
      <div className={styles.layout}>
        <div className={styles.primaryColumn}>
          <Skeleton className={styles.map} />
          <Skeleton className={styles.feed} />
        </div>
        <div className={styles.secondaryColumn}>
          <Skeleton className={styles.card} />
          <Skeleton className={styles.card} />
        </div>
      </div>
    </div>
  )
}
