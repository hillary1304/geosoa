import { Activity, Award, Radio, Users } from 'lucide-react'
import type { EventoIntegrado, Indicadores, Sismo } from '../../api/tipos'
import { formatearFechaHora, formatearNumero } from '../../lib/formato'
import { magnitudMaxima } from '../../lib/agregaciones'

interface Props {
  sismos: Sismo[]
  total: number
  cargando: boolean
  indicadores?: Indicadores
  evento: EventoIntegrado | null
}

export function KpiGrid({ sismos, total, cargando, indicadores, evento }: Props) {
  const ultimo = sismos[0] ?? null
  const maxima = magnitudMaxima(sismos)

  return (
    <section className="kpi-grid" aria-label="Indicadores generales de monitoreo">
      {/* Total de sismos del conjunto filtrado. */}
      <article className="glass-panel kpi-card kpi-igp">
        <div className="kpi-header">
          <span className="kpi-title">Eventos Sísmicos</span>
          <div className="kpi-icon-wrapper">
            <Activity size={18} />
          </div>
        </div>
        <div className="kpi-value">{cargando ? '…' : formatearNumero(total)}</div>
        <div className="kpi-footer">
          <span className="pulse-dot"></span>
          <span>
            {indicadores?.totalAlmacenado
              ? `${formatearNumero(indicadores.totalAlmacenado)} en respaldo local`
              : 'Sismos analizados en el período'}
          </span>
        </div>
      </article>

      {/* Mayor magnitud dentro de los filtros actuales. */}
      <article className="glass-panel kpi-card kpi-max">
        <div className="kpi-header">
          <span className="kpi-title">Magnitud Máxima</span>
          <div className="kpi-icon-wrapper">
            <Award size={18} />
          </div>
        </div>
        <div className="kpi-value">{cargando ? '…' : maxima !== null ? maxima.toFixed(1) : '—'}</div>
        <div className="kpi-footer">
          <span>Registrado en territorio peruano</span>
        </div>
      </article>

      {/* Sismo más reciente del conjunto consultado. */}
      <article className="glass-panel kpi-card kpi-senamhi">
        <div className="kpi-header">
          <span className="kpi-title">Último Reporte IGP</span>
          <div className="kpi-icon-wrapper">
            <Radio size={18} />
          </div>
        </div>
        <div className="kpi-value" style={{ fontSize: '1.15rem' }}>
          {cargando ? '…' : ultimo ? `${ultimo.magnitud.toFixed(1)} · ${formatearFechaHora(ultimo.fechaHora)}` : '—'}
        </div>
        <div className="kpi-footer">
          <span className={`pulse-dot ${ultimo ? '' : 'danger'}`}></span>
          <span>{ultimo?.referencia ?? 'Esperando datos del servicio...'}</span>
        </div>
      </article>

      {/* Población del evento seleccionado; se calcula al abrir su detalle. */}
      <article className="glass-panel kpi-card kpi-inei">
        <div className="kpi-header">
          <span className="kpi-title">Población Expuesta</span>
          <div className="kpi-icon-wrapper">
            <Users size={18} />
          </div>
        </div>
        <div className="kpi-value">
          {evento ? formatearNumero(evento.poblacionExpuesta) : '—'}
        </div>
        <div className="kpi-footer">
          <span>
            {evento
              ? `${evento.distrito?.nombre ?? 'Sin distrito'} · radio ${evento.radioAfectacionKm} km`
              : 'Selecciona un sismo para calcularla (INEI)'}
          </span>
        </div>
      </article>
    </section>
  )
}
