import { Filter, RotateCcw, Calendar } from 'lucide-react'
import { DEPARTAMENTOS } from '../../lib/filtros'
import type { Filtros, Periodo, ProfundidadFiltro } from '../../lib/filtros'

interface Props {
  filtros: Filtros
  onCambiar: (cambios: Partial<Filtros>) => void
  onRestablecer: () => void
}

const PROFUNDIDADES: { valor: ProfundidadFiltro; etiqueta: string }[] = [
  { valor: 'todas', etiqueta: 'Todas' },
  { valor: 'superficial', etiqueta: 'Superficial (<60km)' },
  { valor: 'intermedio', etiqueta: 'Intermedio' },
  { valor: 'profundo', etiqueta: 'Profundo (>300km)' },
]

const PERIODOS: { valor: Periodo; etiqueta: string }[] = [
  { valor: 'historico', etiqueta: 'Histórico' },
  { valor: '24h', etiqueta: '24 Horas' },
  { valor: '7d', etiqueta: '7 Días' },
  { valor: '30d', etiqueta: '30 Días' },
]

export function PanelFiltros({ filtros, onCambiar, onRestablecer }: Props) {
  const activos = [
    filtros.busqueda.trim() !== '',
    filtros.magnitudMinima > 0,
    filtros.profundidad !== 'todas',
    filtros.departamento !== '',
    filtros.periodo !== 'historico',
  ].filter(Boolean).length

  return (
    <aside className="glass-panel filters-sidebar" aria-label="Panel de filtros de consulta">
      <div className="sidebar-title">
        <span style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
          <Filter size={16} /> Filtros y Búsqueda
        </span>
        <button
          className="reset-filter-btn"
          type="button"
          title="Restablecer todos los filtros"
          onClick={onRestablecer}
        >
          <RotateCcw size={12} style={{ display: 'inline', marginRight: '3px' }} />
          Restablecer
        </button>
      </div>

      {/* Búsqueda por referencia o código. Se aplica en el navegador. */}
      <div className="filter-group">
        <label className="filter-label" htmlFor="filter-search">
          Buscar Referencia o Código
        </label>
        <input
          type="text"
          id="filter-search"
          className="input-styled"
          value={filtros.busqueda}
          onChange={(evento) => onCambiar({ busqueda: evento.target.value })}
        />
      </div>

      {/* Magnitud mínima enviada a la API. */}
      <div className="filter-group">
        <label className="filter-label" htmlFor="filter-mag">
          <span>Magnitud Mínima</span>
          <span className="value-badge" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
            ≥ {filtros.magnitudMinima.toFixed(1)}
          </span>
        </label>
        <input
          type="range"
          id="filter-mag"
          className="slider-input"
          min="0"
          max="7"
          step="0.5"
          value={filtros.magnitudMinima}
          onChange={(evento) => onCambiar({ magnitudMinima: Number(evento.target.value) })}
          style={{ width: '100%', accentColor: 'var(--text-primary)' }}
        />
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontSize: '0.68rem',
            color: 'var(--text-muted)',
          }}
        >
          <span>0 (Todos)</span>
          <span>4.0</span>
          <span>5.0</span>
          <span>7.0+</span>
        </div>
      </div>

      {/* La API no filtra por profundidad; se filtra en el navegador. */}
      <div className="filter-group">
        <label className="filter-label">Profundidad Epicentral</label>
        <div className="chips-group">
          {PROFUNDIDADES.map((opcion) => (
            <button
              key={opcion.valor}
              className={`chip-btn profundidad-${opcion.valor} ${filtros.profundidad === opcion.valor ? 'active' : ''}`}
              type="button"
              onClick={() => onCambiar({ profundidad: opcion.valor })}
            >
              {opcion.etiqueta}
            </button>
          ))}
        </div>
      </div>

      {/* Departamento enviado a la API. */}
      <div className="filter-group">
        <label className="filter-label" htmlFor="filter-dept">
          Departamento
        </label>
        <select
          id="filter-dept"
          className="select-styled"
          value={filtros.departamento}
          onChange={(evento) => onCambiar({ departamento: evento.target.value })}
        >
          <option value="">Todos los departamentos</option>
          {DEPARTAMENTOS.map((departamento) => (
            <option key={departamento} value={departamento}>
              {departamento}
            </option>
          ))}
        </select>
      </div>

      {/* Período de tiempo convertido a la fecha "desde" de la API. */}
      <div className="filter-group">
        <label className="filter-label">
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            <Calendar size={13} /> Período de Tiempo
          </span>
        </label>
        <div className="chips-group">
          {PERIODOS.map((opcion) => (
            <button
              key={opcion.valor}
              className={`chip-btn ${filtros.periodo === opcion.valor ? 'active' : ''}`}
              type="button"
              onClick={() => onCambiar({ periodo: opcion.valor })}
            >
              {opcion.etiqueta}
            </button>
          ))}
        </div>
      </div>

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
        <span className={`pulse-dot ${activos > 0 ? 'warning' : ''}`}></span>
        <span>{activos > 0 ? `${activos} filtro(s) activo(s)` : 'Sin filtros activos'}</span>
      </div>
    </aside>
  )
}
