import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Encabezado } from './features/layout/Encabezado'
import { GrillaDashboard } from './features/layout/GrillaDashboard'
import { cambiarModoSimulado } from './api/modo'

export function App() {
  // Por defecto se consulta la API real; el botón permite usar datos de ejemplo.
  const [isMock, setIsMock] = useState(false)
  const queryClient = useQueryClient()

  const toggleMock = () => {
    const siguiente = !isMock
    cambiarModoSimulado(siguiente)
    setIsMock(siguiente)
    queryClient.invalidateQueries()
  }

  return (
    <div className="app-container">
      <Encabezado isMock={isMock} onToggleMock={toggleMock} />
      <GrillaDashboard />
    </div>
  )
}

export default App
