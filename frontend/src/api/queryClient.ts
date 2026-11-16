import { QueryClient } from '@tanstack/react-query'
import { ErrorConsulta } from './cliente'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      // Los errores del cliente (400/404) no se reintentan.
      retry: (intentos, error) => {
        if (error instanceof ErrorConsulta && error.status < 500) {
          return false
        }
        return intentos < 2
      },
    },
  },
})
