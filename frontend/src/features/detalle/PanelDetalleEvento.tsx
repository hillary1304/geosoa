import { useEffect } from 'react'
import { X, MapPin, CloudRain, Users, Waves } from 'lucide-react'
import type { EventoIntegrado } from '../../api/tipos'
import { formatearFechaHora, formatearNumero } from '../../lib/formato'
import { BadgeProcedencia } from '../../components/BadgeProcedencia'
import { EstadoCarga } from '../../components/EstadoCarga'
import { EstadoError } from '../../components/EstadoError'

interface Props {
  codigo: string
  evento?: EventoIntegrado
  cargando: boolean
  error: string | null
  onCerrar: () => void
}

export function PanelDetalleEvento({ codigo, evento, cargando, error, onCerrar }: Props) {
  const densidad = evento?.poblacion?.densidadHabKm2

  // Permite cerrar el panel con la tecla Escape.
  useEffect(() => {
    const alPresionar = (tecla: KeyboardEvent) => {
      if (tecla.key === 'Escape') {
        onCerrar()
      }
    }
    window.addEventListener('keydown', alPresionar)
    return () => window.removeEventListener('keydown', alPresionar)
  }, [onCerrar])

  return (
    <div className="detalle-overlay" role="presentation" onClick={onCerrar}>
      <aside
        className="glass-panel detalle-panel"
        role="dialog"
        aria-modal="true"
        aria-label={`Detalle del evento ${codigo}`}
        onClick={(eventoClick) => eventoClick.stopPropagation()}
      >
        <div className="detalle-header">
          <div>
            <h3>Evento {codigo}</h3>
            <p>Información integrada de IGP, SENAMHI e INEI</p>
          </div>
          <button className="icon-btn" type="button" onClick={onCerrar} aria-label="Cerrar detalle">
            <X size={16} />
          </button>
        </div>

        {cargando && <EstadoCarga mensaje="Consultando el evento integrado..." />}
        {error && <EstadoError mensaje={error} />}

        {evento && !cargando && (
          <div className="detalle-cuerpo">
            <section className="detalle-bloque">
              <h4>
                <Waves size={15} /> Sismo
              </h4>
              <div className="detalle-grid">
                <div>
                  <span className="detalle-etiqueta">Fecha y hora</span>
                  <span className="detalle-valor">{formatearFechaHora(evento.sismo.fechaHora)}</span>
                </div>
                <div>
                  <span className="detalle-etiqueta">Magnitud</span>
                  <span className="detalle-valor">{evento.sismo.magnitud.toFixed(1)}</span>
                </div>
                <div>
                  <span className="detalle-etiqueta">Profundidad</span>
                  <span className="detalle-valor">{evento.sismo.profundidadKm} km</span>
                </div>
                <div>
                  <span className="detalle-etiqueta">Departamento</span>
                  <span className="detalle-valor">{evento.sismo.departamento ?? '—'}</span>
                </div>
              </div>
              <p className="detalle-referencia">{evento.sismo.referencia ?? 'Sin referencia'}</p>
            </section>

            <section className="detalle-bloque">
              <h4>
                <MapPin size={15} /> Distrito del epicentro
              </h4>
              {evento.distrito ? (
                <div className="detalle-grid">
                  <div>
                    <span className="detalle-etiqueta">Distrito</span>
                    <span className="detalle-valor">{evento.distrito.nombre}</span>
                  </div>
                  <div>
                    <span className="detalle-etiqueta">Provincia</span>
                    <span className="detalle-valor">{evento.distrito.provincia}</span>
                  </div>
                  <div>
                    <span className="detalle-etiqueta">Ubigeo</span>
                    <span className="detalle-valor">{evento.distrito.ubigeo}</span>
                  </div>
                </div>
              ) : (
                <p className="detalle-sin-dato">Sin cobertura territorial para este punto.</p>
              )}
            </section>

            <section className="detalle-bloque">
              <h4>
                <CloudRain size={15} /> Precipitación
              </h4>
              {evento.precipitacion ? (
                <p className="detalle-valor">
                  {evento.precipitacion.minimoMm} - {evento.precipitacion.maximoMm ?? '—'} mm
                  <span className="detalle-etiqueta"> · capa {evento.precipitacion.capa}</span>
                </p>
              ) : (
                <p className="detalle-sin-dato">Sin cobertura de precipitación.</p>
              )}
            </section>

            <section className="detalle-bloque">
              <h4>
                <Users size={15} /> Población
              </h4>
              <div className="detalle-grid">
                <div>
                  <span className="detalle-etiqueta">Población del distrito</span>
                  <span className="detalle-valor">{formatearNumero(evento.poblacion?.total)}</span>
                </div>
                <div>
                  <span className="detalle-etiqueta">Densidad</span>
                  <span className="detalle-valor">
                    {densidad !== null && densidad !== undefined ? `${densidad} hab/km²` : '—'}
                  </span>
                </div>
                <div>
                  <span className="detalle-etiqueta">Radio de afectación</span>
                  <span className="detalle-valor">{evento.radioAfectacionKm} km</span>
                </div>
                <div>
                  <span className="detalle-etiqueta">Población expuesta</span>
                  <span className="detalle-valor">{formatearNumero(evento.poblacionExpuesta)}</span>
                </div>
              </div>
            </section>

            <section className="detalle-bloque">
              <h4>Procedencia de los datos</h4>
              <div className="detalle-badges">
                {Object.entries(evento.procedencias).map(([fuente, procedencia]) =>
                  procedencia ? (
                    <BadgeProcedencia key={fuente} fuente={fuente} procedencia={procedencia} />
                  ) : null,
                )}
              </div>
              {evento.fuentesFaltantes.length > 0 && (
                <p className="detalle-alerta">
                  Fuentes que no respondieron: {evento.fuentesFaltantes.join(', ')}. El evento se
                  muestra con la información disponible.
                </p>
              )}
            </section>
          </div>
        )}
      </aside>
    </div>
  )
}
