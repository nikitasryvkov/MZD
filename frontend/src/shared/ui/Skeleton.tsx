import styles from './Skeleton.module.css'

interface SkeletonProps {
  className?: string
}

export function Skeleton({ className }: SkeletonProps) {
  return <div className={[styles.skeleton, className].filter(Boolean).join(' ')} />
}
