import { AlertTriangle, RefreshCw } from 'lucide-react'

interface Props {
  mensaje: string
  onReintentar?: () => void
}

export function EstadoError({ mensaje, onReintentar }: Props) {
  return (
    <div className="estado-panel estado-error" role="alert">
      <AlertTriangle size={20} />
      <span>{mensaje}</span>
      {onReintentar && (
        <button className="btn-secondary" type="button" onClick={onReintentar}>
          <RefreshCw size={14} />
          <span>Reintentar</span>
        </button>
      )}
    </div>
  )
}
