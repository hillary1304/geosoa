import { useState, useEffect } from 'react'
import { Encabezado } from './features/layout/Encabezado'
import { GrillaDashboard } from './features/layout/GrillaDashboard'

export function App() {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    const saved = localStorage.getItem('geosoa-theme')
    return saved === 'light' ? 'light' : 'dark'
  })

  const [isMock, setIsMock] = useState<boolean>(true)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('geosoa-theme', theme)
  }, [theme])

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))
  }

  const toggleMock = () => {
    setIsMock((prev) => !prev)
  }

  return (
    <div className="app-container">
      <Encabezado
        theme={theme}
        onToggleTheme={toggleTheme}
        isMock={isMock}
        onToggleMock={toggleMock}
      />
      <GrillaDashboard />
    </div>
  )
}

export default App
