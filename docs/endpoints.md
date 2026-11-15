# Endpoints del sistema

URL base local: `http://localhost:8080`

Durante la migración, `meteorologia-service` escucha en `http://localhost:8082` y `consulta-service` usa `http://localhost:8084` mientras conviva con el backend monolítico en 8080. La distribución completa está en [puertos-locales.md](puertos-locales.md).

## Consulta (`consulta-service`) – API pública

Base local: `http://localhost:8084`. Es la API que consume el dashboard; coordina por HTTP a sismos, meteorología y territorio. El contrato completo está en [contracts/consulta-service/openapi.yaml](../contracts/consulta-service/openapi.yaml) y sus ejemplos en [contracts/consulta-service/ejemplos](../contracts/consulta-service/ejemplos), validados por las pruebas del servicio.

- `GET /api/sismos?desde=2026-11-01&hasta=2026-11-14&magnitudMinima=4&departamento=ICA&pagina=0&tamano=50`
- `GET /api/sismos/ultimo`
- `GET /api/sismos/{codigo}/evento`
- `GET /api/indicadores`
- `GET /actuator/health`

La lista responde `items`, `pagina`, `tamano`, `hayMas`, `total` y `procedencia`; `total` es `null` si la dependencia no lo informa. El último sismo responde `valor` y `procedencia`, con `valor: null` si aún no hay sismos. Las fechas de los sismos se presentan con offset en hora de Lima.

El evento integrado obtiene el sismo y consulta en paralelo la precipitación del epicentro, su distrito y los distritos dentro del radio de afectación. Si falla meteorología o territorio, responde `200` con el evento parcial y la fuente en `fuentesFaltantes`; si `sismos-service` no responde, `503`; si el sismo no existe, `404`. `poblacionExpuesta` es una estimación del método actual, no un conteo de personas afectadas.

Cada respuesta incluye `X-Request-Id`, que se propaga a los servicios de dominio. Consulta no reintenta: cada servicio administra sus reintentos. Pendientes: `GET /api/fuentes/estado`, que espera `GET /internal/v1/estado-fuente` en cada servicio de dominio, y los filtros de indicadores, que el contrato interno de sismos aún no ofrece.

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

## Territorio (`territorio-service`)

Base local: `http://localhost:8083`; la aplicación usa MySQL `geosoa_territorio` por el puerto local 3308.

- `GET /internal/v1/distritos/por-punto?latitud=-12.05&longitud=-77.03`
- `GET /internal/v1/distritos/en-radio?latitud=-12.05&longitud=-77.03&radioKm=20`
- `GET /internal/v1/distritos/150101/poblacion`
- `GET /actuator/health`

Los resultados de datos siguen el sobre `valor` y `procedencia`. Un punto sin cobertura se distingue de una caída de INEI; los datos de respaldo quedan limitados a los distritos ya almacenados.

## Sismos (`sismos-service`)

Base local: `http://localhost:8081`; usa su instancia MySQL `geosoa_sismos` en el puerto 3307.

- `GET /internal/v1/sismos?desde=2026-11-01&hasta=2026-11-14&magnitudMinima=4&departamento=ICA&pagina=0&tamano=50`
- `GET /internal/v1/sismos/ultimo`
- `GET /internal/v1/sismos/{codigo}`
- `GET /internal/v1/sismos/indicadores`
- `GET /actuator/health`

La lista está paginada desde cero; `hasta` es inclusivo y `tamano` admite de 1 a 2000. La respuesta incluye `elementos`, `pagina`, `tamano`, `total` y `procedencia`. El detalle y el último sismo consultan el IGP y recurren al respaldo local cuando la fuente falla. Los indicadores identifican su cobertura como respaldo almacenado; no representan el total histórico del IGP.

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
