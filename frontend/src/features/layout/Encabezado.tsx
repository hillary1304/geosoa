import { Activity, Sun, Moon, Database } from 'lucide-react'

interface EncabezadoProps {
  theme: 'dark' | 'light'
  onToggleTheme: () => void
  isMock: boolean
  onToggleMock: () => void
}

export function Encabezado({
  theme,
  onToggleTheme,
  isMock,
  onToggleMock,
}: EncabezadoProps) {
  return (
    <header className="navbar" role="banner">
      <div className="brand-section">
        <div className="brand-logo-icon" aria-hidden="true">
          <Activity size={24} color="#ffffff" />
        </div>
        <div className="brand-text">
          <h1>
            GeoSOA <span className="brand-badge">SOA v1.0</span>
          </h1>
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

        <button
          className="icon-btn"
          onClick={onToggleTheme}
          title={`Cambiar a modo ${theme === 'dark' ? 'claro' : 'oscuro'}`}
          aria-label="Cambiar tema de color"
          type="button"
        >
          {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
        </button>
      </div>
    </header>
  )
}
