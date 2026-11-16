import { Inbox } from 'lucide-react'

export function EstadoVacio({ mensaje = 'No hay datos para los filtros seleccionados.' }: { mensaje?: string }) {
  return (
    <div className="estado-panel estado-vacio">
      <Inbox size={20} />
      <span>{mensaje}</span>
    </div>
  )
}
