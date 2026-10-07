# Endpoints del sistema

URL base local: `http://localhost:8080`

## Monitoreo

### `GET /actuator/health`

Estado de la aplicación y de sus dependencias (base de datos, disco).

**Respuesta `200 OK`:**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL" } },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

En el perfil `dev` se muestran los detalles de cada componente.

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
