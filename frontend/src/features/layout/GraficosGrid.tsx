import { TrendingUp, BarChart2, PieChart, Layers } from 'lucide-react'

export function GraficosGrid() {
  return (
    <section className="charts-grid" aria-label="Visualizaciones analíticas y estadísticas">
      {/* Gráfico 1: Serie Temporal */}
      <article className="glass-panel chart-card">
        <div className="chart-card-header">
          <h3 className="chart-card-title">Frecuencia Sísmica en el Tiempo</h3>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Eventos por Día
          </span>
        </div>
        <div className="chart-wrapper-placeholder">
          <TrendingUp size={32} style={{ opacity: 0.6 }} />
          <span>Área reservada para gráfico de línea temporal (Recharts)</span>
        </div>
      </article>

      {/* Gráfico 2: Por Departamento */}
      <article className="glass-panel chart-card">
        <div className="chart-card-header">
          <h3 className="chart-card-title">Sismos por Departamento</h3>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Top Regiones
          </span>
        </div>
        <div className="chart-wrapper-placeholder">
          <BarChart2 size={32} style={{ opacity: 0.6 }} />
          <span>Área reservada para gráfico de barras por región (Recharts)</span>
        </div>
      </article>

      {/* Gráfico 3: Distribución por Magnitud */}
      <article className="glass-panel chart-card">
        <div className="chart-card-header">
          <h3 className="chart-card-title">Distribución por Magnitud</h3>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Escala Richter
          </span>
        </div>
        <div className="chart-wrapper-placeholder">
          <PieChart size={32} style={{ opacity: 0.6 }} />
          <span>Área reservada para gráfico de donut por magnitud (Recharts)</span>
        </div>
      </article>

      {/* Gráfico 4: Estratificación por Profundidad */}
      <article className="glass-panel chart-card">
        <div className="chart-card-header">
          <h3 className="chart-card-title">Estratificación por Profundidad</h3>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Corte Litosférico
          </span>
        </div>
        <div className="chart-wrapper-placeholder">
          <Layers size={32} style={{ opacity: 0.6 }} />
          <span>Área reservada para distribución por profundidad (Recharts)</span>
        </div>
      </article>
    </section>
  )
}
