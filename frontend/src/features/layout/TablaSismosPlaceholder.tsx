import { Download, Table } from 'lucide-react'

export function TablaSismosPlaceholder() {
  return (
    <section className="glass-panel table-panel" aria-label="Listado tabular de eventos sísmicos">
      <div className="table-header-bar">
        <div>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 700 }}>
            Registro Detallado de Eventos Sísmicos
          </h3>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Listado tabular sincronizado con el mapa y ordenamiento por columnas.
          </p>
        </div>
        <div className="table-actions">
          <button
            className="btn-secondary"
            type="button"
            title="Exportar registros filtrados a formato CSV"
            disabled
          >
            <Download size={14} />
            <span>Exportar CSV</span>
          </button>
        </div>
      </div>

      <div className="table-responsive">
        <table className="data-table">
          <thead>
            <tr>
              <th>Magnitud</th>
              <th>Fecha y Hora</th>
              <th>Profundidad</th>
              <th>Referencia Geográfica</th>
              <th>Departamento</th>
              <th>Epicentro</th>
              <th style={{ textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={7} className="table-placeholder-row">
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '0.5rem',
                  }}
                >
                  <Table size={24} style={{ opacity: 0.5 }} />
                  <span>
                    Estructura tabular lista. Se poblará en la Etapa 6 con TanStack Table.
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className="pagination-bar">
        <span>Mostrando 0 de 0 registros</span>
        <div className="pagination-controls">
          <button className="page-btn" type="button" disabled>
            Anterior
          </button>
          <button className="page-btn" type="button" disabled>
            Siguiente
          </button>
        </div>
      </div>
    </section>
  )
}
