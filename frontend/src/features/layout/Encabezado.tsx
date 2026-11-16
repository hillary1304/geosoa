import { Activity, Database } from 'lucide-react'

interface EncabezadoProps {
  isMock: boolean
  onToggleMock: () => void
}

export function Encabezado({ isMock, onToggleMock }: EncabezadoProps) {
  return (
    <header className="navbar" role="banner">
      <div className="brand-section">
        <div className="brand-logo-icon" aria-hidden="true">
          <Activity size={24} color="#ffffff" />
        </div>
        <div className="brand-text">
          <h1>GeoSOA</h1>
          <span className="brand-subtitle">
            Plataforma de Riesgos Naturales del Perú
          </span>
        </div>
      </div>

      {/* Estado de Interoperabilidad con Fuentes Públicas SOA */}
      <div
        className="sources-status-bar"
        title="Estado de interoperabilidad con fuentes del Estado"
      >
        <div className="source-item">
          <span className="pulse-dot" id="status-igp"></span>
          <span className="label">IGP:</span>
          <span>Sismos</span>
        </div>
        <span className="source-separator">|</span>
        <div className="source-item">
          <span className="pulse-dot" id="status-senamhi"></span>
          <span className="label">SENAMHI:</span>
          <span>Lluvias</span>
        </div>
        <span className="source-separator">|</span>
        <div className="source-item">
          <span className="pulse-dot warning" id="status-inei"></span>
          <span className="label">INEI:</span>
          <span>Demografía (Caché)</span>
        </div>
      </div>

      {/* Acciones de Navegación y Configuración */}
      <div className="nav-actions">
        <button
          className={`mode-toggle-btn ${isMock ? 'active-mock' : ''}`}
          onClick={onToggleMock}
          title="Alternar entre API Spring Boot y datos simulados"
          type="button"
        >
          <Database size={14} />
          <span>{isMock ? 'Modo Simulado' : 'API en Vivo'}</span>
        </button>
      </div>
    </header>
  )
}
