import { Layers, Compass, MapPin } from 'lucide-react'

export function PanelMapa() {
  return (
    <div className="glass-panel map-panel" aria-label="Contenedor cartográfico de sismicidad">
      {/* Barra de herramientas superior */}
      <div className="map-toolbar">
        <button
          className="map-control-pill"
          type="button"
          title="Capa WMS de Límites Distritales INEI"
          disabled
        >
          <Layers size={14} />
          <span>Límites INEI (WMS)</span>
        </button>
        <button
          className="map-control-pill"
          type="button"
          title="Recentrar vista nacional"
          disabled
        >
          <Compass size={14} />
          <span>Recentrar</span>
        </button>
      </div>

      {/* Área del mapa (Placeholder para el montaje de React-Leaflet en Etapa 3) */}
      <div className="map-canvas-area" id="map-view">
        <div className="map-placeholder-info">
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: '50%',
              background: 'rgba(56, 189, 248, 0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-primary)',
            }}
          >
            <MapPin size={28} />
          </div>
          <h4>Visor Georreferenciado del Perú</h4>
          <p>
            Contenedor base preparado para el renderizado del mapa con React Leaflet,
            teselas de alta resolución y capas territoriales WMS del INEI.
          </p>
        </div>
      </div>

      {/* Leyenda sismológica */}
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
