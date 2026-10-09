# Plan de implementación – Frontend

Dashboard web responsivo en React que consume la API REST del backend y muestra los datos en mapas, indicadores y gráficos. Referencias: [endpoints del sistema](endpoints.md) y [servicios externos](servicios-externos.md).

## Tecnologías

| Uso | Librería |
|---|---|
| Base | React 19, TypeScript, Vite |
| Peticiones, caché y reintentos | TanStack Query |
| Estado en la URL (filtros y selección) | React Router (`useSearchParams`) |
| Mapas | Leaflet + React Leaflet, capas WMS del INEI |
| Gráficos | Recharts |
| Listado con orden y paginación | TanStack Table |
| Componentes accesibles (diálogo, selectores) | Radix UI |
| Fechas | date-fns (zona horaria `America/Lima`) |
| Íconos | lucide-react |
| Errores de renderizado | react-error-boundary |
| Datos simulados | MSW (Mock Service Worker) |
| Pruebas | Vitest + Testing Library |
| Calidad | ESLint (con `jsx-a11y`) + Prettier |

**Requisitos:** Node.js 22 LTS y npm.

## Estructura

```
frontend
├── index.html
├── package.json
├── vite.config.ts
├── .env.example
├── public
│   └── mockServiceWorker.js
└── src
    ├── main.tsx
    ├── App.tsx
    ├── styles           Variables CSS, tema claro/oscuro y estilos globales
    ├── api              Cliente HTTP, tipos y hooks de TanStack Query
    ├── mocks            Handlers y datos de MSW
    ├── hooks            Hooks compartidos (filtros en la URL, selección)
    ├── components       Componentes reutilizables (tarjetas, estados, insignias)
    └── features
        ├── layout       Encabezado, panel lateral y grilla del dashboard
        ├── mapa         Mapa base, capas WMS, sismos y radio de afectación
        ├── detalle      Detalle integrado del evento
        ├── filtros      Formulario de filtros
        ├── sismos       Listado tabular
        └── indicadores  Tarjetas y gráficos
```

## Contrato con el backend

Los tipos de `src/api` replican los modelos del backend (`Sismo`, `Distrito`, `Precipitacion`, `Poblacion`, `EventoIntegrado`, `Procedencia`) y el formato de error de [endpoints.md](endpoints.md). Las rutas `/api/...` se crean en la Etapa 8 del backend; hasta entonces el frontend trabaja con MSW y se ajusta a lo que quede registrado en `endpoints.md`.

| Recurso | Uso en el dashboard |
|---|---|
| Listado de sismos con filtros y paginación | Mapa, listado y gráficos |
| Último sismo | Resaltado y tarjeta de indicador |
| Detalle integrado de un sismo | Panel de detalle |
| Indicadores | Tarjetas y gráficos |
| `GET /actuator/health` | Estado de cada fuente (IGP, SENAMHI, INEI) |

En desarrollo, Vite redirige `/api` y `/actuator` a `http://localhost:8080` mediante su proxy, así que no hace falta CORS. Para otros entornos la URL base se define en `VITE_API_URL`.

## Verificación por etapa

`npm run lint`, `npm test` y `npm run build` sin errores, y revisión de la página con `npm run dev`.

---

## Etapa 1 – Proyecto base y diseño

**Objetivo:** tener el proyecto React configurado y el layout del dashboard.

- [x] Crear el proyecto en `frontend` con Vite (plantilla `react-ts`) e instalar las dependencias base.
- [x] Configurar ESLint, Prettier, Vitest y los scripts `dev`, `build`, `lint` y `test`.
- [x] Migrar las variables y estilos de `Frontend/css` a `src/styles`, con modo claro/oscuro.
- [x] Componentes del layout: encabezado, panel lateral de filtros, mapa, indicadores, gráficos y listado, sin datos.
- [x] Layout responsivo para móvil, tablet y escritorio.
- [x] Proxy de Vite hacia el backend y archivo `.env.example`.
- [x] Eliminar `Frontend/` e `iniciemos.js`, reemplazados por el proyecto de Vite.
- [x] Actualizar el README (tecnologías y cómo ejecutar el frontend).

**Entregable:** dashboard base responsivo en React, sin datos.

## Etapa 2 – Cliente de la API y estado

**Objetivo:** centralizar la comunicación con el backend y el estado compartido.

- [ ] Tipos TypeScript del modelo común y del formato de error.
- [ ] Cliente `fetch` con URL base configurable, timeout (`AbortSignal.timeout`) y errores uniformes (`ApiError`).
- [ ] Configurar `QueryClient` con reintentos y tiempos de caché acordes al backend.
- [ ] Hooks por recurso: `useSismos`, `useUltimoSismo`, `useEventoIntegrado`, `useIndicadores`, `useEstadoFuentes`.
- [ ] Handlers de MSW con datos simulados, incluidos errores `503` y datos con `origen` `cache` y `db`.
- [ ] Hook `useFiltros` que lee y escribe los filtros y el sismo seleccionado en la URL.
- [ ] Pruebas de los hooks contra MSW.

**Entregable:** hooks de datos probados con datos simulados.

## Etapa 3 – Mapa base

**Objetivo:** mostrar el mapa del Perú con las capas territoriales.

- [ ] `MapContainer` de React Leaflet centrado en el Perú.
- [ ] Capa base de OpenStreetMap.
- [ ] Capas `WMSTileLayer` del INEI: `ig_departamento`, `ig_provincia` e `ig_distrito`.
- [ ] `LayersControl` para activarlas y desactivarlas.
- [ ] Ajustar el tamaño del mapa al cambiar el layout.

**Entregable:** mapa interactivo con límites territoriales.

## Etapa 4 – Capa de sismos

**Objetivo:** representar los sismos en el mapa.

- [ ] `CircleMarker` con radio según magnitud y color según profundidad (variables del tema).
- [ ] `Popup` con magnitud, fecha y hora en `America/Lima`, profundidad y referencia.
- [ ] Resaltar el último sismo.
- [ ] Leyenda de magnitud y profundidad como control del mapa.
- [ ] Seleccionar un sismo actualiza la URL.

**Entregable:** sismos del backend visibles en el mapa.

## Etapa 5 – Detalle integrado del evento

**Objetivo:** mostrar la información combinada del sismo seleccionado.

- [ ] Panel de detalle (Radix `Dialog` en móvil, panel lateral en escritorio) que se abre con el sismo de la URL.
- [ ] Datos del distrito del epicentro (INEI).
- [ ] Precipitación de la zona (SENAMHI) con su rango en mm.
- [ ] Población, densidad y población expuesta; radio de afectación dibujado con `Circle`.
- [ ] Insignia de procedencia (`live`, `cache`, `db`) y fecha de actualización por fuente.
- [ ] Aviso de respuesta parcial cuando `fuentesFaltantes` no está vacío.

**Entregable:** vista integrada IGP + SENAMHI + INEI por evento.

## Etapa 6 – Filtros y listado

**Objetivo:** permitir consultas sobre los sismos.

- [ ] Formulario de filtros: rango de fechas, magnitud mínima, profundidad y departamento.
- [ ] Los filtros se guardan en la URL y se pueden compartir.
- [ ] Listado con TanStack Table: orden y paginación del lado del servidor.
- [ ] Sincronización con el mapa: elegir una fila centra el mapa y abre el detalle.

**Entregable:** consultas filtradas reflejadas en el mapa y en el listado.

## Etapa 7 – Indicadores y gráficos

**Objetivo:** resumir la información en el dashboard.

- [ ] Tarjetas: total de sismos, magnitud máxima, último sismo y población expuesta.
- [ ] Gráfico de sismos por día.
- [ ] Gráfico de sismos por departamento.
- [ ] Distribución por magnitud y por profundidad.
- [ ] Indicadores y gráficos se actualizan con los filtros.

**Entregable:** panel de indicadores actualizado según los filtros.

## Etapa 8 – Resiliencia, accesibilidad y cierre

**Objetivo:** dejar el dashboard robusto y listo para la entrega.

- [ ] Estados de carga, vacío y error en cada componente, y `ErrorBoundary` por sección.
- [ ] Indicador del estado de cada fuente a partir de `/actuator/health`, con actualización periódica.
- [ ] Aviso cuando los datos provienen de caché o de MySQL.
- [ ] Revisión de accesibilidad: contraste, navegación por teclado, foco y textos alternativos.
- [ ] Pruebas de componentes con Testing Library y revisión en móvil, tablet y escritorio.
- [ ] Integración con el backend real y ajuste de `geosoa.cors.allowed-origins` para el origen del frontend desplegado.
- [ ] Build de producción con `npm run build`.

**Entregable:** dashboard completo funcionando con el backend.
