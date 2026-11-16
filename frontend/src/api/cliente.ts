import { esModoSimulado, respuestaSimulada } from './modo'
import type { ErrorApi } from './tipos'

const BASE = import.meta.env.VITE_API_URL ?? '/api'

// Error uniforme para los problemas devueltos por la API.
export class ErrorConsulta extends Error {
  readonly status: number

  readonly detalles: string[]

  constructor(status: number, mensaje: string, detalles: string[] = []) {
    super(mensaje)
    this.name = 'ErrorConsulta'
    this.status = status
    this.detalles = detalles
  }
}

type Parametros = Record<string, string | number | boolean | undefined | null>

function construirUrl(ruta: string, parametros?: Parametros) {
  const url = new URL(`${BASE}${ruta}`, window.location.origin)

  Object.entries(parametros ?? {}).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') {
      url.searchParams.set(clave, String(valor))
    }
  })

  return url.toString()
}

// Punto único de salida hacia consulta-service.
export async function pedir<T>(ruta: string, parametros?: Parametros): Promise<T> {
  if (esModoSimulado()) {
    return respuestaSimulada<T>(ruta, parametros)
  }

  const respuesta = await fetch(construirUrl(ruta, parametros), {
    signal: AbortSignal.timeout(15000),
  })

  const texto = await respuesta.text()
  const cuerpo = texto ? JSON.parse(texto) : null

  if (!respuesta.ok) {
    const error = cuerpo as ErrorApi | null
    throw new ErrorConsulta(
      respuesta.status,
      error?.mensaje ?? 'No se pudo consultar el servicio de consulta',
      error?.detalles ?? [],
    )
  }

  return cuerpo as T
}
