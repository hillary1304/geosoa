# Meteorología

Servicio local para consultar y normalizar capas de precipitación del SENAMHI. No requiere MySQL ni Docker; conserva resultados en una caché en memoria con TTL de 12 horas.

Desde este directorio, iniciar con `mvn spring-boot:run`. El puerto por defecto es 8082 y se puede cambiar con `SERVER_PORT`. La URL, los timeouts y la capa por defecto se configuran con `SENAMHI_URL`, `SENAMHI_CONNECT_TIMEOUT`, `SENAMHI_READ_TIMEOUT` y `SENAMHI_CAPA`.

Endpoint: `GET /internal/v1/precipitacion?latitud=-12.05&longitud=-77.04&capa=ANUAL`. Healthcheck: `/actuator/health`. Ejecutar `mvn verify` para compilar y correr las pruebas.

El backend monolítico continúa en 8080 durante esta extracción; consulta usa 8084 para evitar el conflicto. Los puertos previstos de los servicios y de las dos instancias MySQL están en `docs/puertos-locales.md`.
