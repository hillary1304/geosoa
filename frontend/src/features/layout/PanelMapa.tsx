import { useEffect, useRef, useState } from 'react'
import { Layers, Compass } from 'lucide-react'
import { MapContainer, TileLayer, CircleMarker, Popup, Circle, WMSTileLayer } from 'react-leaflet'
import type { Map as MapaLeaflet } from 'leaflet'
import type { EventoIntegrado, Sismo } from '../../api/tipos'
import { colorProfundidad } from '../../lib/agregaciones'
import { formatearFechaHora } from '../../lib/formato'
import { EstadoCarga } from '../../components/EstadoCarga'
import { EstadoVacio } from '../../components/EstadoVacio'

interface Props {
  sismos: Sismo[]
  seleccionado: string | null
  evento: EventoIntegrado | null
  cargando: boolean
  onSeleccionar: (codigo: string) => void
}

export function PanelMapa({ sismos, seleccionado, evento, cargando, onSeleccionar }: Props) {
  const mapaRef = useRef<MapaLeaflet | null>(null)
  const [mostrarLimites, setMostrarLimites] = useState(false)

  // Centra el mapa cuando se elige un sismo desde la tabla o el detalle.
  useEffect(() => {
    const sismo = sismos.find((item) => item.codigo === seleccionado)
    const mapa = mapaRef.current
    if (sismo && mapa) {
      mapa.flyTo([sismo.latitud, sismo.longitud], Math.max(mapa.getZoom(), 6))
    }
  }, [seleccionado, sismos])

  const recentrar = () => {
    mapaRef.current?.setView([-9.19, -75.02], 5)
  }

  return (
    <div className="glass-panel map-panel" aria-label="Contenedor cartográfico de sismicidad">
      <div className="map-toolbar">
        <button
          className={`map-control-pill ${mostrarLimites ? 'active' : ''}`}
          type="button"
          title="Capa WMS de límites departamentales del INEI"
          onClick={() => setMostrarLimites((valor) => !valor)}
        >
          <Layers size={14} />
          <span>Límites INEI (WMS)</span>
        </button>
        <button
          className="map-control-pill"
          type="button"
          title="Recentrar vista nacional"
          onClick={recentrar}
        >
          <Compass size={14} />
          <span>Recentrar</span>
        </button>
      </div>

      <div className="map-canvas-area" id="map-view">
        {cargando && <EstadoCarga mensaje="Cargando sismos en el mapa..." />}
        {!cargando && sismos.length === 0 && (
          <EstadoVacio mensaje="No hay sismos para los filtros seleccionados." />
        )}
        {!cargando && sismos.length > 0 && (
          <MapContainer
            ref={mapaRef}
            center={[-9.19, -75.02]}
            zoom={5}
            scrollWheelZoom
            style={{ width: '100%', height: '100%' }}
          >
            <TileLayer
              attribution="&copy; OpenStreetMap"
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {mostrarLimites && (
              <WMSTileLayer
                url="https://geoespacial.inei.gob.pe/geoserver/Interoperabilidad/wms"
                layers="Interoperabilidad:ig_departamento"
                format="image/png"
                transparent
                version="1.1.0"
                opacity={0.45}
              />
            )}

            {sismos.map((sismo) => {
              const color = colorProfundidad(sismo.profundidadKm)
              const activo = sismo.codigo === seleccionado

              return (
                <CircleMarker
                  key={sismo.codigo}
                  center={[sismo.latitud, sismo.longitud]}
                  radius={4 + sismo.magnitud * 1.6}
                  pathOptions={{
                    color: activo ? '#f8fafc' : color,
                    fillColor: color,
                    fillOpacity: 0.65,
                    weight: activo ? 3 : 1.5,
                  }}
                  eventHandlers={{ click: () => onSeleccionar(sismo.codigo) }}
                >
                  <Popup>
                    <strong>{sismo.codigo}</strong>
                    <br />
                    {formatearFechaHora(sismo.fechaHora)}
                    <br />
                    Magnitud {sismo.magnitud.toFixed(1)} · Prof. {sismo.profundidadKm} km
                    <br />
                    {sismo.referencia}
                  </Popup>
                </CircleMarker>
              )
            })}

            {evento && (
              <Circle
                center={[evento.sismo.latitud, evento.sismo.longitud]}
                radius={evento.radioAfectacionKm * 1000}
                pathOptions={{
                  color: '#38bdf8',
                  fillColor: '#38bdf8',
                  fillOpacity: 0.08,
                  weight: 1.5,
                }}
              />
            )}
          </MapContainer>
        )}
      </div>

      {!cargando && sismos.length > 0 && (
        <p className="map-hint">Haz clic en un sismo del mapa para ver su detalle.</p>
      )}

      <div className="map-legend">
        <span className="legend-title">Profundidad del Epicentro</span>
        <div className="legend-row">
          <span className="legend-dot" style={{ background: 'var(--depth-shallow)' }}></span>
          <span>Superficial (&lt; 60 km)</span>
        </div>
        <div className="legend-row">
          <span className="legend-dot" style={{ background: 'var(--depth-intermediate)' }}></span>
          <span>Intermedio (60 - 300 km)</span>
        </div>
        <div className="legend-row">
          <span className="legend-dot" style={{ background: 'var(--depth-deep)' }}></span>
          <span>Profundo (&gt; 300 km)</span>
        </div>
      </div>
    </div>
  )
}
