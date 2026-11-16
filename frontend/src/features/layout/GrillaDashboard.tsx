import { useMemo, useState } from 'react'
import { useEvento, useIndicadores, useSismos } from '../../api/consultas'
import { ErrorConsulta } from '../../api/cliente'
import { aFiltrosApi, filtrarSismos, FILTROS_INICIALES } from '../../lib/filtros'
import type { Filtros } from '../../lib/filtros'
import { EstadoError } from '../../components/EstadoError'
import { KpiGrid } from './KpiGrid'
import { PanelFiltros } from './PanelFiltros'
import { PanelMapa } from './PanelMapa'
import { PanelDetalleEvento } from '../detalle/PanelDetalleEvento'

function mensajeDeError(error: unknown) {
  return error instanceof ErrorConsulta ? error.message : 'No se pudieron cargar los datos.'
}

export function GrillaDashboard() {
  const [filtros, setFiltros] = useState<Filtros>(FILTROS_INICIALES)
  const [codigoSeleccionado, setCodigoSeleccionado] = useState<string | null>(null)
  const [detalleAbierto, setDetalleAbierto] = useState(false)

  // Los filtros de la API se recalculan solo cuando cambia la interfaz.
  const filtrosApi = useMemo(() => aFiltrosApi(filtros), [filtros])
  const consulta = useSismos(filtrosApi)
  const indicadores = useIndicadores()
  const evento = useEvento(codigoSeleccionado)

  // Texto y profundidad se filtran en el navegador.
  const sismos = useMemo(
    () => filtrarSismos(consulta.data?.items ?? [], filtros),
    [consulta.data, filtros],
  )

  const total = consulta.data?.total ?? sismos.length

  const cambiarFiltros = (cambios: Partial<Filtros>) => {
    setFiltros((actuales) => ({ ...actuales, ...cambios }))
  }

  // El sismo queda seleccionado aunque se cierre el panel, para conservar sus datos.
  const seleccionarSismo = (codigo: string) => {
    setCodigoSeleccionado(codigo)
    setDetalleAbierto(true)
  }

  const restablecer = () => {
    setFiltros(FILTROS_INICIALES)
    setCodigoSeleccionado(null)
    setDetalleAbierto(false)
  }

  return (
    <main className="main-content" role="main">
      <h2 className="sr-only">Panel de monitoreo sísmico</h2>

      {consulta.isError && (
        <EstadoError
          mensaje={mensajeDeError(consulta.error)}
          onReintentar={() => consulta.refetch()}
        />
      )}

      <KpiGrid
        sismos={sismos}
        total={total}
        cargando={consulta.isLoading}
        indicadores={indicadores.data}
        evento={evento.data ?? null}
      />

      <section className="workspace-grid" aria-label="Espacio de trabajo geográfico y filtros">
        <PanelFiltros filtros={filtros} onCambiar={cambiarFiltros} onRestablecer={restablecer} />
        <PanelMapa
          sismos={sismos}
          seleccionado={codigoSeleccionado}
          evento={evento.data ?? null}
          cargando={consulta.isLoading}
          onSeleccionar={seleccionarSismo}
        />
      </section>

      {/* Los gráficos analíticos y la tabla detallada se agregarán en la siguiente entrega. */}

      {codigoSeleccionado && detalleAbierto && (
        <PanelDetalleEvento
          codigo={codigoSeleccionado}
          evento={evento.data}
          cargando={evento.isLoading}
          error={evento.isError ? mensajeDeError(evento.error) : null}
          onCerrar={() => setDetalleAbierto(false)}
        />
      )}
    </main>
  )
}
