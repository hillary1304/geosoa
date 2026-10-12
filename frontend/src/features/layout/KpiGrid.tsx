import { Activity, Award, Radio, Users } from 'lucide-react'

export function KpiGrid() {
  return (
    <section className="kpi-grid" aria-label="Indicadores generales de monitoreo">
      {/* Tarjeta 1: Total Sismos */}
      <article className="glass-panel kpi-card kpi-igp">
        <div className="kpi-header">
          <span className="kpi-title">Eventos Sísmicos</span>
          <div className="kpi-icon-wrapper" style={{ color: 'var(--accent-igp)' }}>
            <Activity size={18} />
          </div>
        </div>
        <div className="kpi-value" id="stat-total-sismos">--</div>
        <div className="kpi-footer">
          <span>Sismos analizados en el período</span>
        </div>
      </article>

      {/* Tarjeta 2: Magnitud Máxima */}
      <article className="glass-panel kpi-card kpi-max">
        <div className="kpi-header">
          <span className="kpi-title">Magnitud Máxima</span>
          <div className="kpi-icon-wrapper" style={{ color: 'var(--accent-warning)' }}>
            <Award size={18} />
          </div>
        </div>
        <div className="kpi-value" id="stat-max-mag">--</div>
        <div className="kpi-footer">
          <span>Registrado en territorio peruano</span>
        </div>
      </article>

      {/* Tarjeta 3: Último Reporte IGP */}
      <article className="glass-panel kpi-card kpi-senamhi">
        <div className="kpi-header">
          <span className="kpi-title">Último Reporte IGP</span>
          <div className="kpi-icon-wrapper" style={{ color: 'var(--accent-primary)' }}>
            <Radio size={18} />
          </div>
        </div>
        <div className="kpi-value" style={{ fontSize: '1.35rem' }} id="stat-ultimo">--</div>
        <div className="kpi-footer">
          <span className="pulse-dot danger"></span>
          <span>Esperando datos del servicio...</span>
        </div>
      </article>

      {/* Tarjeta 4: Población Expuesta */}
      <article className="glass-panel kpi-card kpi-inei">
        <div className="kpi-header">
          <span className="kpi-title">Población Expuesta</span>
          <div className="kpi-icon-wrapper" style={{ color: 'var(--accent-inei)' }}>
            <Users size={18} />
          </div>
        </div>
        <div className="kpi-value" id="stat-poblacion-expuesta">--</div>
        <div className="kpi-footer">
          <span>En áreas de influencia directa (INEI)</span>
        </div>
      </article>
    </section>
  )
}
