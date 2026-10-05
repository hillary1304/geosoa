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
