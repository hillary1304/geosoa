import { render, screen } from '@testing-library/react'
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '@testing-library/jest-dom'
import App from './App'

// Cliente sin reintentos para que las pruebas no esperen a la API.
function crearCliente() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })
}

function renderizarApp() {
  return render(
    <QueryClientProvider client={crearCliente()}>
      <App />
    </QueryClientProvider>,
  )
}

describe('Panel principal de GeoSOA', () => {
  beforeEach(() => {
    // La API queda pendiente: las pruebas solo revisan la estructura inicial.
    vi.stubGlobal('fetch', vi.fn(() => new Promise<void>(() => undefined)))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renderiza el encabezado con el título y las fuentes institucionales', () => {
    renderizarApp()
    expect(screen.getByRole('heading', { name: /geosoa/i })).toBeInTheDocument()
    expect(screen.getByText('IGP:')).toBeInTheDocument()
    expect(screen.getByText('SENAMHI:')).toBeInTheDocument()
    expect(screen.getByText('INEI:')).toBeInTheDocument()
  })

  it('renderiza las cuatro tarjetas de indicadores clave', () => {
    renderizarApp()
    expect(screen.getByText('Eventos Sísmicos')).toBeInTheDocument()
    expect(screen.getByText('Magnitud Máxima')).toBeInTheDocument()
    expect(screen.getByText('Último Reporte IGP')).toBeInTheDocument()
    expect(screen.getByText('Población Expuesta')).toBeInTheDocument()
  })

  it('renderiza el panel de filtros y el mapa', () => {
    renderizarApp()
    expect(screen.getByText('Filtros y Búsqueda')).toBeInTheDocument()
    expect(screen.getByText('Profundidad del Epicentro')).toBeInTheDocument()
    expect(screen.getByText('Cargando sismos en el mapa...')).toBeInTheDocument()
  })

  it('no muestra las secciones pendientes de la siguiente entrega', () => {
    renderizarApp()
    expect(screen.queryByText('Registro Detallado de Eventos Sísmicos')).not.toBeInTheDocument()
    expect(screen.queryByText('Frecuencia Sísmica en el Tiempo')).not.toBeInTheDocument()
    expect(screen.queryByText('Sismos por Departamento')).not.toBeInTheDocument()
  })

  it('no muestra el botón de cambio de tema', () => {
    renderizarApp()
    expect(screen.queryByLabelText(/cambiar tema de color/i)).not.toBeInTheDocument()
  })
})
