import { Filter, RotateCcw, Calendar } from 'lucide-react'

export function PanelFiltros() {
  return (
    <aside className="glass-panel filters-sidebar" aria-label="Panel de filtros de consulta">
      <div className="sidebar-title">
        <span style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
          <Filter size={16} /> Filtros y Búsqueda
        </span>
        <button className="reset-filter-btn" type="button" title="Restablecer todos los filtros">
          <RotateCcw size={12} style={{ display: 'inline', marginRight: '3px' }} />
          Restablecer
        </button>
      </div>

      {/* Búsqueda por texto */}
      <div className="filter-group">
        <label className="filter-label" htmlFor="filter-search">
          Buscar Referencia o Código
        </label>
        <input
          type="text"
          id="filter-search"
          className="input-styled"
          placeholder="Ej. Oxapampa, Lima, 2026-716..."
          disabled
        />
      </div>

      {/* Magnitud Mínima */}
      <div className="filter-group">
        <div className="filter-label">
          <span>Magnitud Mínima</span>
          <span className="value-badge" style={{ color: 'var(--accent-primary)', fontWeight: 600 }}>
            ≥ 0.0
          </span>
        </div>
        <input
          type="range"
          id="filter-mag"
          className="slider-input"
          min="0"
          max="7"
          step="0.5"
          defaultValue="0"
          disabled
          style={{ width: '100%', accentColor: 'var(--accent-primary)', cursor: 'not-allowed' }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.68rem', color: 'var(--text-muted)' }}>
          <span>0 (Todos)</span>
          <span>4.0</span>
          <span>5.0</span>
          <span>7.0+</span>
        </div>
      </div>

      {/* Profundidad Epicentral */}
      <div className="filter-group">
        <label className="filter-label">Profundidad Epicentral</label>
        <div className="chips-group">
          <button className="chip-btn active" type="button" disabled>Todas</button>
          <button className="chip-btn" type="button" disabled style={{ color: 'var(--depth-shallow)' }}>
            Superficial (&lt;60km)
          </button>
          <button className="chip-btn" type="button" disabled style={{ color: 'var(--depth-intermediate)' }}>
            Intermedio
          </button>
          <button className="chip-btn" type="button" disabled style={{ color: 'var(--depth-deep)' }}>
            Profundo (&gt;300km)
          </button>
        </div>
      </div>

      {/* Departamento */}
      <div className="filter-group">
        <label className="filter-label" htmlFor="filter-dept">
          Departamento
        </label>
        <select id="filter-dept" className="select-styled" disabled>
          <option value="">Todos los departamentos</option>
        </select>
      </div>

      {/* Rango Temporal */}
      <div className="filter-group">
        <label className="filter-label">
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            <Calendar size={13} /> Período de Tiempo
          </span>
        </label>
        <div className="chips-group">
          <button className="chip-btn active" type="button" disabled>Histórico</button>
          <button className="chip-btn" type="button" disabled>24 Horas</button>
          <button className="chip-btn" type="button" disabled>7 Días</button>
          <button className="chip-btn" type="button" disabled>30 Días</button>
        </div>
      </div>

      {/* Pie del panel: Estado del Módulo */}
      <div
        style={{
          marginTop: 'auto',
          paddingTop: '1rem',
          borderTop: '1px solid var(--border-subtle)',
          fontSize: '0.72rem',
          color: 'var(--text-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.45rem',
        }}
      >
        <span className="pulse-dot warning"></span>
        <span>Módulo de filtros base (sin datos)</span>
      </div>
    </aside>
  )
}
