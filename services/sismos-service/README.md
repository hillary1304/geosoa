# Sismos

Servicio local que consulta el IGP y usa MySQL como respaldo. Primero inicia la base desde la raíz del repositorio con `docker compose up -d --wait mysql-sismos`; después, desde este directorio, ejecuta `mvn spring-boot:run`. La aplicación escucha en 8081 y sincroniza los registros recientes del IGP cada cinco minutos. La sincronización inicial consulta los últimos siete días, con un máximo de 2000 registros por ejecución.

Se puede cambiar el puerto con `SERVER_PORT`. La base usa `SISMOS_DB_URL`, `SISMOS_DB_USER` y `SISMOS_DB_PASSWORD`; la sincronización se controla con `SISMOS_SINCRONIZACION_HABILITADA`. El valor local por defecto es `true`; el backend monolítico la deja desactivada para evitar dos escritores.

Endpoints: `/internal/v1/sismos`, `/internal/v1/sismos/ultimo`, `/internal/v1/sismos/{codigo}`, `/internal/v1/sismos/indicadores` y `/actuator/health`. La cobertura de los indicadores es la de los datos almacenados, no el total histórico del IGP. Ejecuta `mvn verify` para compilar y correr las pruebas.
