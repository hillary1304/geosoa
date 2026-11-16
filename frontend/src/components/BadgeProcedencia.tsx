import type { Procedencia } from '../api/tipos'
import { textoOrigen } from '../lib/formato'

interface Props {
  fuente: string
  procedencia: Procedencia
}

export function BadgeProcedencia({ fuente, procedencia }: Props) {
  return (
    <span className={`badge-procedencia badge-${procedencia.origen}`} title={`Dato de ${fuente}`}>
      {fuente}: {textoOrigen(procedencia.origen)}
    </span>
  )
}
