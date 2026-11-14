# Endpoints del sistema

URL base local: `http://localhost:8080`

Durante la migración, `meteorologia-service` escucha en `http://localhost:8082` y `consulta-service` usa `http://localhost:8084` mientras conviva con el backend monolítico en 8080. La distribución completa está en [puertos-locales.md](puertos-locales.md).

## Meteorología (`meteorologia-service`)

### `GET http://localhost:8082/internal/v1/precipitacion?latitud=-12.05&longitud=-77.04&capa=ANUAL`

Consulta la capa de precipitación del SENAMHI para un punto. `capa` es opcional y por defecto toma `ANUAL`; admite las capas definidas por `CapaPrecipitacion`.

**Respuesta `200 OK`:**

```json
{
  "valor": { "capa": "ANUAL", "minimoMm": 10.0, "maximoMm": 15.0 },
  "procedencia": { "origen": "live", "actualizadoEn": "2026-11-14T05:00:00Z" }
}
```

Si el punto no tiene cobertura, `valor` será `null`; eso no representa una caída del proveedor. Coordenadas inválidas responden `400`; SENAMHI indisponible sin resultado en caché responde `503`. La caché vive en memoria y se pierde al reiniciar el servicio.

Healthcheck: `GET http://localhost:8082/actuator/health`.

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
