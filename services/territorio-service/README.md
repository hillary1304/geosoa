# Territorio

Servicio local para las consultas INEI de distrito, población y distritos dentro de un radio. Se ejecuta con `mvn spring-boot:run` en el puerto 8083 y usa la instancia MySQL `mysql-territorio`, publicada localmente en 3308. No requiere Dockerfile; iniciar la base con `docker compose up -d mysql-territorio`.

La primera ejecución crea el esquema mediante Flyway. Antes de preparar esta migración se respaldó la tabla anterior y se compararon los conteos: `geosoa.distrito` tenía 0 filas y `geosoa_territorio.distrito` también tiene 0. Por eso no se copiaron filas; la base original permanece intacta. Las consultas nuevas al INEI irán poblando el respaldo territorial.

Endpoints internos:

- `GET /internal/v1/distritos/por-punto?latitud=-12.05&longitud=-77.03`
- `GET /internal/v1/distritos/en-radio?latitud=-12.05&longitud=-77.03&radioKm=20`
- `GET /internal/v1/distritos/{ubigeo}/poblacion`
- `GET /actuator/health`

Ejecutar `mvn verify` para compilar y correr las pruebas.
