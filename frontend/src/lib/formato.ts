import { formatInTimeZone } from 'date-fns-tz'
import { es } from 'date-fns/locale'

const ZONA_LIMA = 'America/Lima'

export function formatearFechaHora(fecha: string) {
  return formatInTimeZone(new Date(fecha), ZONA_LIMA, 'dd/MM/yyyy HH:mm', { locale: es })
}

export function formatearFecha(fecha: string) {
  return formatInTimeZone(new Date(fecha), ZONA_LIMA, 'dd/MM/yyyy', { locale: es })
}

export function formatearHora(fecha: string) {
  return formatInTimeZone(new Date(fecha), ZONA_LIMA, 'HH:mm', { locale: es })
}

export function formatearNumero(valor: number | null | undefined) {
  if (valor === null || valor === undefined) {
    return '—'
  }
  return new Intl.NumberFormat('es-PE').format(valor)
}

export function textoOrigen(origen: string) {
  if (origen === 'live') {
    return 'En vivo'
  }
  if (origen === 'cache') {
    return 'Caché'
  }
  return 'Respaldo'
}
