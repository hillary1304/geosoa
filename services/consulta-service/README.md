# Consulta

API pública del dashboard. Coordina por HTTP a `sismos-service` (8081), `meteorologia-service` (8082) y `territorio-service` (8083) mediante rutas Apache Camel; no usa MySQL ni consulta IGP, SENAMHI o INEI directamente. Usa Java 21, Spring Boot 4.1.1 y Camel 4.22.0, administrado con su BOM.

Desde este directorio, iniciar con `mvn spring-boot:run`. Escucha en 8084 mientras el backend monolítico ocupa 8080; el puerto cambia con `SERVER_PORT`. Healthcheck: `GET /actuator/health`. Ejecutar `mvn verify` para compilar y correr las pruebas.

## Configuración

| Variable | Valor por defecto |
|---|---|
| `SISMOS_URL` | `http://localhost:8081` |
| `METEOROLOGIA_URL` | `http://localhost:8082` |
| `TERRITORIO_URL` | `http://localhost:8083` |
| `SERVICIOS_CONNECT_TIMEOUT` | `500ms` |
| `SERVICIOS_READ_TIMEOUT` | `3s` por llamada |
| `SERVICIOS_PLAZO_EVENTO` | `10s` para el evento integrado |

Consulta no reintenta; los reintentos automáticos del cliente HTTP están desactivados porque cada servicio de dominio ya administra los suyos.

## Rutas

Cada controlador entra por `ProducerTemplate` a una ruta `direct:`.

| Endpoint | Ruta Camel |
|---|---|
| `GET /api/sismos` | `listar-sismos` |
| `GET /api/sismos/ultimo` | `ultimo-sismo` |
| `GET /api/indicadores` | `indicadores` |
| `GET /api/sismos/{codigo}/evento` | `consultar-evento` |

Las llamadas HTTP pasan por `llamar-sismos`, `llamar-meteorologia` y `llamar-territorio`, que envían solo `X-Request-Id` y traducen los fallos: `404` y `400` se conservan; caída, error del servicio o timeout pasan a `503`.

`consultar-evento` obtiene el sismo, calcula el radio con `RadioAfectacion` y hace un `multicast` en paralelo a `evento-precipitacion`, `evento-distrito` y `evento-en-radio`. Cada rama atrapa su propio fallo, así que una caída de meteorología o territorio deja el evento parcial con `200`. Si el distrito del epicentro no vino poblado en el radio, `evento-poblacion` consulta su población solo si queda plazo para una llamada completa. Las consultas paralelas esperan como máximo el plazo total menos la peor llamada a sismos.

## Contrato

El OpenAPI y los ejemplos están en `contracts/consulta-service`. Las pruebas comparan la salida real con esos mismos ejemplos, así que un cambio en el JSON público obliga a actualizar el contrato.
