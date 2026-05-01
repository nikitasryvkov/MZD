import { Panel } from '@/shared/ui/Panel'
import styles from '@/pages/shared/SectionPage.module.css'

export function AboutPage() {
  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>Справка</p>
        <h1>Назначение системы</h1>
        <p>
          Система предназначена для контроля обстановки на сети МЖД,
          просмотра событий и сводных показателей.
        </p>
      </section>

      <div className={styles.contentGrid}>
        <Panel
          title="Основные разделы"
          eyebrow="Состав"
          subtitle="Краткое описание разделов системы."
          accent="cool"
        >
          <div className={styles.singleColumn}>
            <div>
              <strong>Карта</strong>
              <p>Показывает станции, участки, поезда и события на схеме сети.</p>
            </div>
            <div>
              <strong>События</strong>
              <p>Содержит список текущих событий и сведения по каждому из них.</p>
            </div>
            <div>
              <strong>Аналитика</strong>
              <p>Отображает сводные показатели по сети и персоналу.</p>
            </div>
          </div>
        </Panel>

        <Panel
          title="Назначение данных"
          eyebrow="Использование"
          subtitle="Что можно проверить в системе."
          accent="warm"
        >
          <div className={styles.singleColumn}>
            <div>
              <strong>Обстановка на карте</strong>
              <p>Позволяет быстро определить расположение объектов и проблемных участков.</p>
            </div>
            <div>
              <strong>События</strong>
              <p>Позволяют уточнить статус, время и место возникновения события.</p>
            </div>
            <div>
              <strong>Показатели</strong>
              <p>Позволяют оценить текущую нагрузку и общее состояние сети.</p>
            </div>
          </div>
        </Panel>
      </div>
    </div>
  )
}
