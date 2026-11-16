import type { Sismo } from '../api/tipos'

export type TipoProfundidad = 'superficial' | 'intermedio' | 'profundo'

export function clasificarProfundidad(km: number): TipoProfundidad {
  if (km < 60) {
    return 'superficial'
  }
  if (km <= 300) {
    return 'intermedio'
  }
  return 'profundo'
}

export function colorProfundidad(km: number) {
  if (km < 60) {
    return '#f87171'
  }
  if (km <= 300) {
    return '#fbbf24'
  }
  return '#22c55e'
}

export function magnitudMaxima(sismos: Sismo[]) {
  if (sismos.length === 0) {
    return null
  }
  return Math.max(...sismos.map((sismo) => sismo.magnitud))
}
