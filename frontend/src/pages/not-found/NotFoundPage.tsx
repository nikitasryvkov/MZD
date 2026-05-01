import { Link } from 'react-router-dom'
import styles from './NotFoundPage.module.css'

export function NotFoundPage() {
  return (
    <main className={styles.page}>
      <section className={styles.card}>
        <p className={styles.eyebrow}>Страница не найдена</p>
        <h1>Такого раздела в сервисе нет</h1>
        <p>
          Проверьте адрес страницы или вернитесь на карту через основное меню.
        </p>
        <Link to="/" className={styles.link}>
          Вернуться на главную
        </Link>
      </section>
    </main>
  )
}
