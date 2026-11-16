export function EstadoCarga({ mensaje = 'Cargando datos...' }: { mensaje?: string }) {
  return (
    <div className="estado-panel estado-carga" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{mensaje}</span>
    </div>
  )
}
