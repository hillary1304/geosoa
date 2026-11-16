// Tipos que reflejan el contrato público de consulta-service (/api).

export type OrigenDato = 'live' | 'cache' | 'db'

export type Fuente = 'IGP' | 'SENAMHI' | 'INEI'

export interface Procedencia {
  origen: OrigenDato
  actualizadoEn: string | null
}

export interface Sismo {
  codigo: string
  fechaHora: string
  latitud: number
  longitud: number
  profundidadKm: number
  magnitud: number
  intensidad: string | null
  referencia: string | null
  departamento: string | null
  sentido: boolean
}

export interface ListaSismos {
  items: Sismo[]
  pagina: number
  tamano: number
  hayMas: boolean
  total: number | null
  procedencia: Procedencia
}

export interface DatoSismo {
  valor: Sismo | null
  procedencia: Procedencia
}

export interface Distrito {
  ubigeo: string
  departamento: string
  provincia: string
  nombre: string
}

export interface Poblacion {
  ubigeo: string
  total: number
  densidadHabKm2: number | null
}

export interface Precipitacion {
  capa: string
  minimoMm: number
  maximoMm: number | null
}

export interface EventoIntegrado {
  sismo: Sismo
  distrito: Distrito | null
  precipitacion: Precipitacion | null
  poblacion: Poblacion | null
  radioAfectacionKm: number
  poblacionExpuesta: number | null
  fuentesFaltantes: Fuente[]
  procedencias: Partial<Record<Fuente, Procedencia>>
}

export interface Indicadores {
  totalAlmacenado: number | null
  magnitudMaximaAlmacenada: number | null
  cobertura: string
}

export interface ErrorApi {
  timestamp: string
  status: number
  error: string
  mensaje: string
  ruta: string
  detalles: string[]
}

// Filtros que entiende el endpoint /api/sismos.
export interface FiltrosApi {
  desde?: string
  hasta?: string
  magnitudMinima?: number
  departamento?: string
}
