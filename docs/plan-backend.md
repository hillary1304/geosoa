# Plan de implementación – Backend

API REST en Java 21 y Spring Boot 4.1 que integra los servicios del IGP, SENAMHI e INEI mediante adaptadores, un servicio orquestador y una capa de caché. Referencias: [servicios externos](servicios-externos.md) y [endpoints del sistema](endpoints.md).

## Estructura de paquetes

```
com.geosoa.backend
├── config        Configuración (clientes HTTP, caché, CORS)
├── adapter       Adaptadores de servicios externos
│   ├── igp
│   ├── senamhi
│   └── inei
├── domain        Modelo común del sistema
├── orchestrator  Integración y correlación de datos
├── persistence   Entidades JPA y repositorios
├── api           Controladores REST y DTOs
└── shared        Excepciones y utilidades
```

---

## Etapa 1 – Configuración base

**Objetivo:** dejar el proyecto listo para desarrollar y conectado a MySQL.

- [x] Agregar dependencias: `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, `mysql-connector-j`, `spring-boot-starter-cache`, `caffeine`, `spring-boot-starter-actuator`, `flyway-mysql`.
- [x] Configurar `application.properties` y perfiles `dev` y `test`.
- [x] Conectar con el MySQL de `compose.yaml` (`geosoa`, `soauser`).
- [x] Crear la estructura de paquetes.
- [x] Configurar CORS para el frontend.
- [x] Habilitar `/actuator/health`.

**Entregable:** la aplicación arranca, conecta a MySQL y responde en `/actuator/health`.

## Etapa 2 – Modelo de dominio y persistencia

**Objetivo:** definir el formato común de datos y su almacenamiento.

- [x] Modelos de dominio: `Sismo`, `Precipitacion`, `Distrito`, `Poblacion`, `EventoIntegrado`.
- [x] Entidades JPA y repositorios para sismos y distritos.
- [x] Migraciones Flyway iniciales.
- [x] Manejo global de errores con `@RestControllerAdvice` y formato de error uniforme.

**Entregable:** esquema creado automáticamente al arrancar y pruebas de repositorio en verde.

## Etapa 3 – Adaptador IGP

**Objetivo:** consumir y normalizar los sismos del IGP (ArcGIS REST).

- [x] Cliente HTTP con `RestClient`, URL base y timeouts configurables.
- [x] Consulta de `UltimoSismo` y de `SismosReportados` con filtros por fecha, magnitud y departamento, más paginación.
- [x] Conversión de `fechaevento` (epoch ms) a `America/Lima`.
- [x] Descartar registros con campos nulos.
- [x] Mapeo de la respuesta ArcGIS al modelo `Sismo`.
- [x] Pruebas con respuestas simuladas (`MockRestServiceServer`).

**Entregable:** `IgpAdapter` devuelve sismos normalizados.

## Etapa 4 – Adaptador SENAMHI

**Objetivo:** obtener la precipitación en una coordenada (OGC WFS).

- [ ] Cliente WFS `GetFeature` con `CQL_FILTER=INTERSECTS(geom,POINT(lon lat))` y `propertyName=rango`.
- [ ] Capa configurable (anual por defecto, opción mensual o estacional).
- [ ] Interpretar `rango` (`"10 - 15"`) como mínimo y máximo en mm.
- [ ] Manejar respuestas vacías (punto en el mar o sin cobertura).
- [ ] Pruebas con respuestas simuladas.

**Entregable:** `SenamhiAdapter` devuelve la precipitación para una latitud y longitud.

## Etapa 5 – Adaptador INEI

**Objetivo:** identificar el distrito y la población de una coordenada (OGC WFS).

- [ ] Verificar los atributos de `ig_distrito` e `ig_pobtotal_dist` con `DescribeFeatureType`.
- [ ] Consulta del distrito por punto (Ubigeo, departamento, provincia, distrito).
- [ ] Consulta de población y densidad por Ubigeo.
- [ ] Consulta de distritos dentro de un radio (bbox + filtro por distancia).
- [ ] Persistir los límites distritales en MySQL como respaldo, porque el servicio presenta caídas.
- [ ] Pruebas con respuestas simuladas.

**Entregable:** `IneiAdapter` devuelve el distrito y la población para una coordenada o radio.

## Etapa 6 – Servicio orquestador

**Objetivo:** combinar las tres fuentes en una vista integrada del evento.

- [ ] `OrquestadorService` que, dado un sismo, consulte en paralelo a SENAMHI e INEI.
- [ ] Calcular el radio de afectación según magnitud y profundidad.
- [ ] Estimar la población expuesta sumando los distritos dentro del radio.
- [ ] Construir `EventoIntegrado` con el sismo, el distrito, la precipitación y la población.
- [ ] Respuesta parcial cuando una fuente no responde, indicando qué fuente faltó.
- [ ] Pruebas unitarias del orquestador.

**Entregable:** un evento integrado completo a partir de un sismo del IGP.

## Etapa 7 – Resiliencia y caché

**Objetivo:** mantener el servicio disponible ante caídas de las APIs del Estado.

- [ ] Caché en memoria con Caffeine por fuente y TTL configurable (IGP corto; SENAMHI e INEI largo).
- [ ] Reintentos con espera y circuit breaker por adaptador (Resilience4j).
- [ ] Respaldo en MySQL: si la fuente falla, servir el último dato almacenado.
- [ ] Tarea programada (`@Scheduled`) para sincronizar sismos recientes.
- [ ] Incluir en las respuestas la procedencia del dato (`origen`: `live`, `cache` o `db`) y su fecha de actualización.
- [ ] Indicador de estado de cada fuente externa en Actuator.

**Entregable:** el sistema sigue respondiendo con datos almacenados con una fuente caída.

## Etapa 8 – API REST, documentación y pruebas finales

**Objetivo:** exponer la API para el dashboard y cerrar la calidad del backend.

- [ ] Controladores `GET`: sismos (listado y filtros), detalle integrado, último sismo, indicadores y estado de las fuentes.
- [ ] Validación de parámetros y paginación.
- [ ] Documentación OpenAPI con springdoc y registro en [endpoints.md](endpoints.md).
- [ ] Pruebas de integración de los controladores.
- [ ] Empaquetado `jar` y verificación con `./mvnw verify`.

**Entregable:** API documentada, probada y lista para el frontend.
