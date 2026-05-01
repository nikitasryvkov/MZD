import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/app/auth/authContext'
import styles from './AppShell.module.css'

const navigationItems = [
  { to: '/', label: 'Карта' },
  { to: '/events', label: 'События' },
  { to: '/analytics', label: 'Аналитика' },
  { to: '/about', label: 'О сервисе' },
] as const

export function AppShell() {
  const { authEnabled, status, userName, signOut } = useAuth()
  const showAccount = authEnabled && status === 'authenticated'

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.brand}>
          <span className={styles.brandMark}>МЖД</span>
          <div>
            <strong>Оперативный мониторинг</strong>
            <span>Текущее состояние сети</span>
          </div>
        </div>

        <nav className={styles.nav} aria-label="Основная навигация">
          {navigationItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                [styles.navLink, isActive ? styles.navLinkActive : '']
                  .filter(Boolean)
                  .join(' ')
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className={styles.headerActions}>
          {showAccount ? (
            <>
              <span className={styles.userChip}>{userName ?? 'Сотрудник'}</span>
              <button type="button" className={styles.secondaryButton} onClick={() => void signOut()}>
                Выйти
              </button>
            </>
          ) : null}
        </div>
      </header>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
