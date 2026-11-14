# Prueba de compatibilidad de consulta

Este módulo valida la base técnica de `consulta-service` antes de migrar la orquestación. Arranca con Java 21, Spring Boot 4.1.1 y Apache Camel 4.22.0; la versión de Camel se administra con su BOM. Camel 4.19 añadió soporte para Spring Boot 4 y 4.22 es la línea LTS indicada en la documentación oficial.

La prueba `ConsultaRouteTests` inicia el contexto Spring, verifica que Camel descubra `ConsultaRoute` y envía un mensaje a `direct:consulta-prueba`. Esta ruta solo confirma arranque e integración básica; todavía no implementa llamadas HTTP entre servicios ni el flujo de negocio.

Ejecutar desde este directorio:

```bash
mvn verify
```

Referencias: [Camel Spring Boot 4.22](https://camel.apache.org/camel-spring-boot/4.22.x/) y [guía de actualización Camel 4.18 → 4.19](https://camel.apache.org/manual/camel-4x-upgrade-guide-4_19.html).
