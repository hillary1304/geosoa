# Puertos para ejecución local

Las aplicaciones Spring Boot se ejecutan en el host y usan puertos distintos durante la migración:

| Proceso | Puerto local |
|---|---:|
| Backend monolítico durante transición | 8080 |
| `sismos-service` | 8081 |
| `meteorologia-service` | 8082 |
| `territorio-service` | 8083 |
| `consulta-service` durante convivencia con el monolito | 8084 |
| `consulta-service` tras retirar el monolito | 8080 |

Los procesos Java aceptan `SERVER_PORT` para cambiar su puerto al arrancar. Las bases MySQL también se ejecutan en instancias y redes internas separadas: `mysql-sismos` publica su puerto interno 3306 como 3307 en el host y `mysql-territorio` publica 3306 como 3308. Cada aplicación apunta a la instancia que posee sus datos; meteorología y consulta no usan MySQL.

Durante el desarrollo local no se requieren Dockerfiles. Compose podrá iniciar solo las bases necesarias o el sistema completo cuando se prepare la integración final.
