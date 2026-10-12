import { KpiGrid } from './KpiGrid'
import { PanelFiltros } from './PanelFiltros'
import { PanelMapa } from './PanelMapa'
import { GraficosGrid } from './GraficosGrid'
import { TablaSismosPlaceholder } from './TablaSismosPlaceholder'

export function GrillaDashboard() {
  return (
    <main className="main-content" role="main">
      {/* 1. Indicadores Ejecutivos (Tarjetas KPI) */}
      <KpiGrid />

      {/* 2. Área Central de Trabajo: Filtros y Mapa */}
      <section className="workspace-grid" aria-label="Espacio de trabajo geográfico y filtros">
        <PanelFiltros />
        <PanelMapa />
      </section>

      {/* 3. Métricas y Gráficos Analíticos */}
      <GraficosGrid />

      {/* 4. Tabla Detallada con Paginación */}
      <TablaSismosPlaceholder />
    </main>
  )
}
