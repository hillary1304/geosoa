import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import '@testing-library/jest-dom'
import App from './App'

describe('App Layout Base (Etapa 1)', () => {
  it('renderiza el encabezado con el título de GeoSOA y las fuentes institucionales', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: /geosoa/i })).toBeInTheDocument()
    expect(screen.getByText('IGP:')).toBeInTheDocument()
    expect(screen.getByText('SENAMHI:')).toBeInTheDocument()
    expect(screen.getByText('INEI:')).toBeInTheDocument()
  })

  it('renderiza las cuatro tarjetas de indicadores clave (KPIs)', () => {
    render(<App />)
    expect(screen.getByText('Eventos Sísmicos')).toBeInTheDocument()
    expect(screen.getByText('Magnitud Máxima')).toBeInTheDocument()
    expect(screen.getByText('Último Reporte IGP')).toBeInTheDocument()
    expect(screen.getByText('Población Expuesta')).toBeInTheDocument()
  })

  it('renderiza el panel de filtros y el visor de mapa', () => {
    render(<App />)
    expect(screen.getByText('Filtros y Búsqueda')).toBeInTheDocument()
    expect(screen.getByText('Visor Georreferenciado del Perú')).toBeInTheDocument()
    expect(screen.getByText('Profundidad del Epicentro')).toBeInTheDocument()
  })

  it('renderiza la tabla de sismos y los contenedores de gráficos', () => {
    render(<App />)
    expect(screen.getByText('Registro Detallado de Eventos Sísmicos')).toBeInTheDocument()
    expect(screen.getByText('Frecuencia Sísmica en el Tiempo')).toBeInTheDocument()
    expect(screen.getByText('Sismos por Departamento')).toBeInTheDocument()
  })

  it('permite alternar el tema visual entre oscuro y claro', () => {
    render(<App />)
    const toggleThemeBtn = screen.getByLabelText(/cambiar tema de color/i)
    fireEvent.click(toggleThemeBtn)
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    fireEvent.click(toggleThemeBtn)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })
})
