import { eventoSimulado, indicadoresSimulados, sismosSimulados } from '../mocks/datos'
import type { ListaSismos, Procedencia } from './tipos'

// Modo de demostración: permite mostrar el dashboard sin depender de la API.
let simulado = false

export function esModoSimulado() {
  return simulado
}

export function cambiarModoSimulado(valor: boolean) {
  simulado = valor
}

const procedenciaSimulada: Procedencia = { origen: 'live', actualizadoEn: null }

type Parametros = Record<string, string | number | boolean | undefined | null>

export function respuestaSimulada<T>(ruta: string, parametros?: Parametros): T {
  if (ruta === '/sismos') {
    return listaSimulada(parametros) as T
  }

  if (ruta === '/sismos/ultimo') {
    return { valor: sismosSimulados[0], procedencia: procedenciaSimulada } as T
  }

  if (ruta === '/indicadores') {
    return indicadoresSimulados as T
  }

  const evento = /^\/sismos\/([^/]+)\/evento$/.exec(ruta)
  if (evento) {
    const sismo = sismosSimulados.find((item) => item.codigo === evento[1]) ?? sismosSimulados[0]
    return eventoSimulado(sismo) as T
  }

  throw new Error(`Ruta simulada no disponible: ${ruta}`)
}

function listaSimulada(parametros?: Parametros): ListaSismos {
  const magnitudMinima = Number(parametros?.magnitudMinima ?? 0)
  const departamento = String(parametros?.departamento ?? '').toUpperCase()
  const desde = parametros?.desde ? String(parametros.desde) : null
  const pagina = Number(parametros?.pagina ?? 0)
  const tamano = Number(parametros?.tamano ?? 50)

  const filtrados = sismosSimulados
    .filter((sismo) => sismo.magnitud >= magnitudMinima)
    .filter((sismo) => !departamento || (sismo.departamento ?? '').toUpperCase() === departamento)
    .filter((sismo) => !desde || sismo.fechaHora.slice(0, 10) >= desde)
    .sort((a, b) => b.fechaHora.localeCompare(a.fechaHora))

  const inicio = pagina * tamano
  const items = filtrados.slice(inicio, inicio + tamano)

  return {
    items,
    pagina,
    tamano,
    hayMas: inicio + items.length < filtrados.length,
    total: filtrados.length,
    procedencia: procedenciaSimulada,
  }
}
