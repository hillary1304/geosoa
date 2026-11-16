import { subDays } from 'date-fns'
import { formatInTimeZone } from 'date-fns-tz'
import type { FiltrosApi, Sismo } from '../api/tipos'
import { clasificarProfundidad } from './agregaciones'

export type Periodo = 'historico' | '24h' | '7d' | '30d'

export type ProfundidadFiltro = 'todas' | 'superficial' | 'intermedio' | 'profundo'

export interface Filtros {
  busqueda: string
  magnitudMinima: number
  profundidad: ProfundidadFiltro
  departamento: string
  periodo: Periodo
}

export const FILTROS_INICIALES: Filtros = {
  busqueda: '',
  magnitudMinima: 0,
  profundidad: 'todas',
  departamento: '',
  periodo: 'historico',
}

// Departamentos usados por el IGP; "OCEANO" agrupa los sismos frente a la costa.
export const DEPARTAMENTOS = [
  'AMAZONAS',
  'ANCASH',
  'APURIMAC',
  'AREQUIPA',
  'AYACUCHO',
  'CAJAMARCA',
  'CALLAO',
  'CUSCO',
  'HUANCAVELICA',
  'HUANUCO',
  'ICA',
  'JUNIN',
  'LA LIBERTAD',
  'LAMBAYEQUE',
  'LIMA',
  'LORETO',
  'MADRE DE DIOS',
  'MOQUEGUA',
  'OCEANO',
  'PASCO',
  'PIURA',
  'PUNO',
  'SAN MARTIN',
  'TACNA',
  'TUMBES',
  'UCAYALI',
]

export function calcularDesde(periodo: Periodo) {
  if (periodo === 'historico') {
    return undefined
  }

  const dias = periodo === '24h' ? 1 : periodo === '7d' ? 7 : 30
  return formatInTimeZone(subDays(new Date(), dias), 'America/Lima', 'yyyy-MM-dd')
}

// Convierte los filtros de la interfaz a los parámetros que acepta la API.
export function aFiltrosApi(filtros: Filtros): FiltrosApi {
  return {
    desde: calcularDesde(filtros.periodo),
    magnitudMinima: filtros.magnitudMinima > 0 ? filtros.magnitudMinima : undefined,
    departamento: filtros.departamento || undefined,
  }
}

// La API no filtra por profundidad ni por texto, así que se aplican en el navegador.
export function filtrarSismos(sismos: Sismo[], filtros: Filtros) {
  const texto = filtros.busqueda.trim().toLowerCase()

  return sismos.filter((sismo) => {
    if (
      texto &&
      !`${sismo.referencia ?? ''} ${sismo.codigo}`.toLowerCase().includes(texto)
    ) {
      return false
    }

    if (
      filtros.profundidad !== 'todas' &&
      clasificarProfundidad(sismo.profundidadKm) !== filtros.profundidad
    ) {
      return false
    }

    return true
  })
}
