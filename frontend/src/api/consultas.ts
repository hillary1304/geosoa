import { useQuery } from '@tanstack/react-query'
import { pedir } from './cliente'
import type { EventoIntegrado, FiltrosApi, Indicadores, ListaSismos } from './tipos'

// Límite que permite la API para traer el conjunto usado por mapa y gráficos.
export const TAMANO_DATASET = 2000

function aParametros(filtros: FiltrosApi) {
  return {
    desde: filtros.desde,
    hasta: filtros.hasta,
    magnitudMinima: filtros.magnitudMinima,
    departamento: filtros.departamento,
  }
}

export function useSismos(filtros: FiltrosApi, tamano = TAMANO_DATASET) {
  return useQuery({
    queryKey: ['sismos', filtros, tamano],
    queryFn: () => pedir<ListaSismos>('/sismos', { ...aParametros(filtros), pagina: 0, tamano }),
    staleTime: 60_000,
  })
}

export function useEvento(codigo: string | null) {
  return useQuery({
    queryKey: ['evento', codigo],
    queryFn: () => pedir<EventoIntegrado>(`/sismos/${codigo}/evento`),
    enabled: Boolean(codigo),
    staleTime: 5 * 60_000,
  })
}

export function useIndicadores() {
  return useQuery({
    queryKey: ['indicadores'],
    queryFn: () => pedir<Indicadores>('/indicadores'),
    staleTime: 5 * 60_000,
  })
}
