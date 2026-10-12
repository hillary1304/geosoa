# Endpoints del sistema

URL base local: `http://localhost:8080`

## Monitoreo

### `GET /actuator/health`

Estado de la aplicación, de sus dependencias (base de datos, disco) y de cada fuente externa.

**Respuesta `200 OK`:**

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "igp": { "status": "UP", "details": { "state": "CLOSED", "failureRate": "-1.0%" } },
        "senamhi": { "status": "UP", "details": { "state": "CLOSED", "failureRate": "-1.0%" } },
        "inei": { "status": "CIRCUIT_OPEN", "details": { "state": "OPEN", "failureRate": "60.0%" } }
      }
    },
    "db": { "status": "UP", "details": { "database": "MySQL" } },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

En el perfil `dev` se muestran los detalles de cada componente.

`circuitBreakers` muestra el circuit breaker de cada fuente: `CLOSED` cuando responde con normalidad y `OPEN` (`CIRCUIT_OPEN`) cuando falló varias veces seguidas y el backend dejó de consultarla por un minuto, respondiendo desde caché o MySQL. Una fuente caída no cambia el `status` general, porque el sistema sigue respondiendo.

## Formato de error

Todos los errores de la API responden con el mismo cuerpo JSON.

| Estado | Caso |
|---|---|
| `400` | Parámetro faltante, con tipo inválido o fuera de rango |
| `404` | Recurso o ruta inexistente |
| `503` | Fuente externa (IGP, SENAMHI o INEI) no disponible |
| `500` | Error interno no controlado |

**Ejemplo `404 Not Found`:**

```json
{
  "timestamp": "2026-10-07T01:50:11.891Z",
  "status": 404,
  "error": "Not Found",
  "mensaje": "La ruta solicitada no existe",
  "ruta": "/api/inexistente",
  "detalles": []
}
```

`detalles` lista los parámetros inválidos en los errores `400` de validación.
