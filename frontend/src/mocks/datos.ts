import type { EventoIntegrado, Indicadores, Sismo } from '../api/tipos'

// Datos de ejemplo para el botón "Modo Simulado".
const PLANTILLAS = [
  { departamento: 'LIMA', referencia: '12 km al O de Lima', latitud: -12.05, longitud: -77.08, magnitud: 5.4, profundidadKm: 38 },
  { departamento: 'AREQUIPA', referencia: '29 km al SO de Aplao, Castilla - Arequipa', latitud: -16.19, longitud: -72.74, magnitud: 4.2, profundidadKm: 120 },
  { departamento: 'TACNA', referencia: '28 km al SE de Tarata, Tarata - Tacna', latitud: -17.68, longitud: -69.88, magnitud: 4.8, profundidadKm: 25 },
  { departamento: 'PUNO', referencia: '19 km al SO de Santa Lucía, Lampa - Puno', latitud: -15.42, longitud: -70.65, magnitud: 3.9, profundidadKm: 210 },
  { departamento: 'ICA', referencia: '35 km al O de Pisco, Pisco - Ica', latitud: -13.71, longitud: -76.6, magnitud: 5.1, profundidadKm: 45 },
  { departamento: 'ANCASH', referencia: '81 km al SO de Huarmey, Huarmey - Áncash', latitud: -10.56, longitud: -78.9, magnitud: 4.5, profundidadKm: 95 },
  { departamento: 'CAJAMARCA', referencia: '15 km al N de Cajamarca', latitud: -7.05, longitud: -78.51, magnitud: 3.7, profundidadKm: 15 },
  { departamento: 'MOQUEGUA', referencia: '22 km al O de Moquegua', latitud: -17.19, longitud: -71.15, magnitud: 4.9, profundidadKm: 160 },
  { departamento: 'LORETO', referencia: '40 km al S de Iquitos', latitud: -4.15, longitud: -73.25, magnitud: 4.1, profundidadKm: 600 },
  { departamento: 'PIURA', referencia: '30 km al O de Piura', latitud: -5.19, longitud: -80.94, magnitud: 5.8, profundidadKm: 30 },
  { departamento: 'CUSCO', referencia: '18 km al NE de Cusco', latitud: -13.45, longitud: -71.85, magnitud: 4.4, profundidadKm: 80 },
  { departamento: 'JUNIN', referencia: '12 km al O de Huancayo', latitud: -12.06, longitud: -75.33, magnitud: 3.6, profundidadKm: 12 },
  { departamento: 'LA LIBERTAD', referencia: '25 km al S de Trujillo', latitud: -8.31, longitud: -79.03, magnitud: 4.7, profundidadKm: 55 },
  { departamento: 'HUANUCO', referencia: '20 km al E de Huánuco', latitud: -9.93, longitud: -75.9, magnitud: 4.0, profundidadKm: 130 },
  { departamento: 'AMAZONAS', referencia: '35 km al N de Chachapoyas', latitud: -5.95, longitud: -77.85, magnitud: 3.5, profundidadKm: 8 },
  { departamento: 'SAN MARTIN', referencia: '28 km al O de Tarapoto', latitud: -6.48, longitud: -76.62, magnitud: 4.3, profundidadKm: 105 },
  { departamento: 'OCEANO', referencia: '27 km al SO de Yauca, Caravelí - Arequipa', latitud: -15.81, longitud: -74.73, magnitud: 4.6, profundidadKm: 35 },
  { departamento: 'CALLAO', referencia: '10 km al O del Callao', latitud: -12.06, longitud: -77.25, magnitud: 5.0, profundidadKm: 60 },
]

const BASE = new Date('2026-11-15T20:00:00-05:00')

export const sismosSimulados: Sismo[] = PLANTILLAS.map((plantilla, indice) => ({
  codigo: `2026-${900 - indice}`,
  fechaHora: new Date(BASE.getTime() - indice * 6 * 60 * 60 * 1000).toISOString(),
  latitud: plantilla.latitud,
  longitud: plantilla.longitud,
  profundidadKm: plantilla.profundidadKm,
  magnitud: plantilla.magnitud,
  intensidad: indice % 3 === 0 ? 'II-III' : null,
  referencia: plantilla.referencia,
  departamento: plantilla.departamento,
  sentido: indice % 2 === 0,
}))

export const indicadoresSimulados: Indicadores = {
  totalAlmacenado: sismosSimulados.length,
  magnitudMaximaAlmacenada: Math.max(...sismosSimulados.map((sismo) => sismo.magnitud)),
  cobertura: 'datos de demostración del modo simulado',
}

export function eventoSimulado(sismo: Sismo): EventoIntegrado {
  return {
    sismo,
    distrito: {
      ubigeo: '150101',
      departamento: 'Lima',
      provincia: 'Lima',
      nombre: 'Lima',
    },
    precipitacion: { capa: 'ANUAL', minimoMm: 10, maximoMm: 15 },
    poblacion: { ubigeo: '150101', total: 273142, densidadHabKm2: 3.2 },
    radioAfectacionKm: 5,
    poblacionExpuesta: 42000,
    fuentesFaltantes: [],
    procedencias: {
      IGP: { origen: 'live', actualizadoEn: null },
      SENAMHI: { origen: 'cache', actualizadoEn: null },
      INEI: { origen: 'db', actualizadoEn: null },
    },
  }
}
