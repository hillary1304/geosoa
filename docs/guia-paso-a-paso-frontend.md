# Guía de Implementación Paso a Paso del Frontend – GeoSOA (React 19 + TypeScript)

Esta guía detalla el procedimiento exacto para construir el frontend de **GeoSOA** de forma incremental y profesional en el repositorio, alineada al plan oficial de arquitectura definido en [plan-frontend.md](plan-frontend.md) y los [endpoints del sistema](endpoints.md). Está estructurada en **8 etapas secuenciales** con comandos de Git, TypeScript, validaciones y justificaciones técnicas para sustentar un historial de ingeniería sólido ante la revisión docente.

---

## 📋 Resumen del Flujo de Commits en Git

Para registrar un avance ordenado y profesional en el repositorio, realizaremos un commit representativo al finalizar cada etapa:

| Etapa | Alcance y Archivos | Mensaje de Commit sugerido |
|---|---|---|
| **Etapa 1** | Proyecto Vite (`react-ts`), diseño CSS y layout del dashboard (`package.json`, `vite.config.ts`, `src/styles/`, `src/features/layout/`) | `feat(frontend): inicializar proyecto react vite, diseno responsive y layout base` |
| **Etapa 2** | Tipos TypeScript, cliente HTTP, TanStack Query, MSW y estado URL (`src/api/`, `src/mocks/`, `src/hooks/useFiltros.ts`) | `feat(api): cliente http, tipos typescript, react query, msw y estado url` |
| **Etapa 3** | Mapa interactivo con React Leaflet, teselas base y capas territoriales WMS INEI (`src/features/mapa/MapaBase.tsx`, `CapasTerritoriales.tsx`) | `feat(mapa): mapa react-leaflet, capas base y wms territorial inei` |
| **Etapa 4** | Simbología sísmica, marcadores por profundidad/magnitud, pulso y selección (`src/features/mapa/CapaSismos.tsx`, `MarcadorSismo.tsx`) | `feat(mapa): marcadores sismicos clasificados, popup y seleccion sincronizada` |
| **Etapa 5** | Panel de detalle tri-fuente SOA con Radix UI Dialog y radio de afectación (`src/features/detalle/`, cálculo de radio y badges) | `feat(soa): panel detalle integrado radix dialog, tri-fuente y radio de impacto` |
| **Etapa 6** | Formulario de filtros en URL y tabla interactiva con TanStack Table (`src/features/filtros/`, `src/features/sismos/TablaSismos.tsx`, CSV) | `feat(filtros): formulario sincronizado en url, tanstack table y exportacion csv` |
| **Etapa 7** | Tarjetas KPI de resumen y gráficos estadísticos con Recharts (`src/features/indicadores/TarjetasKpi.tsx`, `GraficosAnaliticos.tsx`) | `feat(analitica): tarjetas kpi y graficos analiticos interactivos con recharts` |
| **Etapa 8** | Resiliencia (`ErrorBoundary`), monitor Actuator Health, accesibilidad y cierre (`src/components/`, pruebas Vitest y build final) | `feat(frontend): resiliencia con error-boundary, health check, accesibilidad y cierre` |

---

## 🛠️ Etapa 1 – Proyecto Base, Diseño Visual y Layout del Dashboard

### 1.1 Objetivo
Inicializar la aplicación web con Vite, React 19 y TypeScript dentro del directorio `frontend/`. Configurar la grilla responsiva del dashboard, migrar las variables de diseño y colores institucionales (modo oscuro/claro) y configurar el proxy local hacia el backend Spring Boot (`http://localhost:8080`).

### 1.2 Archivos involucrados
- `frontend/package.json` y `frontend/vite.config.ts`
- `frontend/.env.example` y `frontend/index.html`
- `frontend/src/main.tsx` y `frontend/src/App.tsx`
- `frontend/src/styles/variables.css`, `frontend/src/styles/temas.css`, `frontend/src/styles/global.css`
- `frontend/src/features/layout/Encabezado.tsx`, `PanelFiltros.tsx`, `GrillaDashboard.tsx`

### 1.3 Pasos a realizar
1. Crear el proyecto React con Vite e instalar dependencias base:
   ```bash
   npm create vite@latest frontend -- --template react-ts
   cd frontend
   npm install lucide-react clsx
   npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom eslint prettier
   ```
2. Configurar el proxy de desarrollo en `vite.config.ts` para redirigir `/api` y `/actuator` a `http://localhost:8080` (evitando problemas de CORS en desarrollo).
3. Migrar la paleta de colores corporativa e institucional a variables CSS (`src/styles/`):
   - Sismos / IGP: `#ef4444` (Rojo)
   - Lluvias / SENAMHI: `#3b82f6` (Azul)
   - Población / INEI: `#10b981` (Verde)
   - Superficie Dark/Light: Variables HSL con efectos de elevación y glassmorphism.
4. Construir el esqueleto estructural en `src/features/layout/` con soporte responsivo para móviles, tablets y escritorios.
5. Reemplazar y retirar progresivamente la versión previa de `Frontend/` e `iniciemos.js`.

### 1.4 Validación de la Etapa 1
- Ejecutar: `npm run dev` en `frontend/` y abrir `http://localhost:5173`.
- Comprobar que el layout carga con su fondo estilizado, encabezado y contenedores vacíos sin errores en la consola (`F12`).
- Ejecutar: `npm run build` para asegurar compilación limpia en TypeScript.

### 1.5 Registro en Git
```bash
git add frontend/
git commit -m "feat(frontend): inicializar proyecto react vite, diseno responsive y layout base"
```

---

## 📡 Etapa 2 – Cliente API, Tipos TypeScript, TanStack Query y MSW

### 2.1 Objetivo
Construir la capa de acceso a datos fuertemente tipada con TypeScript, garantizando total independencia del backend gracias a Mock Service Worker (MSW), con caché inteligente y sincronización del estado en la URL.

### 2.2 Archivos involucrados
- `frontend/src/api/tipos.ts` (modelos `Sismo`, `Distrito`, `Precipitacion`, `Poblacion`, `EventoIntegrado`, `Procedencia`, `ApiError`).
- `frontend/src/api/cliente.ts` (wrapper de `fetch` con `AbortSignal.timeout` y mapeo de errores HTTP).
- `frontend/src/api/queries.ts` (hooks `useSismos`, `useUltimoSismo`, `useEventoIntegrado`, `useIndicadores`, `useEstadoFuentes`).
- `frontend/src/mocks/handlers.ts`, `browser.ts`, `datos.ts` (MSW con simulación de errores 503 y datos procedentes de `cache`/`db`).
- `frontend/src/hooks/useFiltros.ts` (lectura y escritura de filtros y sismo seleccionado en `useSearchParams`).
- `frontend/public/mockServiceWorker.js`.

### 2.3 Aspectos clave del código
1. **Tipado Estricto:** Replicar con fidelidad los contratos de [endpoints.md](endpoints.md).
2. **TanStack Query (`QueryClient`):** Configurar `staleTime: 60_000` para sismos y reintentos exponenciales con `retry: 2`.
3. **Mock Service Worker:** Permite el desarrollo y pruebas automatizadas idénticas a producción interceptando peticiones de red a nivel de navegador.
4. **URL como Fuente de Verdad (`useFiltros`):** Permite compartir enlaces con filtros específicos o un sismo seleccionado sin requerir estado global complejo como Redux.

### 2.4 Validación de la Etapa 2
- Inicializar el worker: `npx msw init public/`
- Probar consultas con MSW activado en desarrollo.
- Ejecutar pruebas automatizadas: `npm test` verificando que los hooks devuelven datos válidos.

### 2.5 Registro en Git
```bash
git add frontend/src/api/ frontend/src/mocks/ frontend/src/hooks/ frontend/public/
git commit -m "feat(api): cliente http, tipos typescript, react query, msw y estado url"
```

---

## 🗺️ Etapa 3 – Mapa Base Interactivo con React Leaflet y Capas WMS

### 3.1 Objetivo
Integrar el visor cartográfico centrado en el territorio peruano (`lat: -9.1899, lon: -75.0151`, zoom inicial `5.5`) mediante `react-leaflet`, conectando las capas OGC WMS oficiales del INEI.

### 3.2 Archivos involucrados
- `frontend/src/features/mapa/MapaBase.tsx` (`MapContainer`, eventos de resize y centrado).
- `frontend/src/features/mapa/CapasTerritoriales.tsx` (`WMSTileLayer` y `TileLayer` base).
- `frontend/src/features/mapa/ControlesMapa.tsx` (`LayersControl`, control de escala y selector de capas base).

### 3.3 Aspectos clave del código
1. **Instalación:**
   ```bash
   npm install leaflet react-leaflet @types/leaflet
   ```
2. **Capas Base:** Configuración de teselas claras y oscuras (OpenStreetMap y Esri Canvas) sin marcas de agua ni restricciones de tokens.
3. **Capas WMS del INEI:**
   - URL GeoServer: `https://ide.inei.gob.pe/geoserver/wms`
   - Capas OGC: `ig_departamento`, `ig_provincia`, `ig_distrito`.
4. **Manejo de Ciclo de Vida:** Uso de `useMap()` para invalidar tamaño (`map.invalidateSize()`) cuando se expande o colapsa el layout.

### 3.4 Validación de la Etapa 3
- Levantar `npm run dev`.
- Verificar que el mapa cubre su contenedor sin áreas grises ni desfases en los tiles.
- Probar el control de capas para activar y desactivar los límites distritales del INEI.

### 3.5 Registro en Git
```bash
git add frontend/src/features/mapa/
git commit -m "feat(mapa): mapa react-leaflet, capas base y wms territorial inei"
```

---

## 🔴 Etapa 4 – Capa de Sismos, Marcadores Clasificados y Pulso Radar

### 4.1 Objetivo
Renderizar la sismicidad en tiempo real sobre el mapa utilizando simbología sismológica estandarizada y sincronización bidireccional con la URL.

### 4.2 Archivos involucrados
- `frontend/src/features/mapa/CapaSismos.tsx` (consumo de `useSismos` y mapeo de marcadores).
- `frontend/src/features/mapa/MarcadorSismo.tsx` (`CircleMarker` y `Popup`).
- `frontend/src/features/mapa/LeyendaMapa.tsx` (control flotante con clasificación de profundidad y magnitud).
- `frontend/src/features/mapa/utils.ts` (cálculo de colores y radios).

### 4.3 Aspectos clave del código
1. **Simbología por Profundidad:**
   - Superficial (`< 60 km`): Rojo (`#ef4444`)
   - Intermedio (`60 - 300 km`): Ámbar (`#f59e0b`)
   - Profundo (`> 300 km`): Cian (`#06b6d4`)
2. **Escala de Radio:** Escalamiento exponencial según magnitud de Richter ($r = \max(5, (M - 2) \cdot 3.5)$).
3. **Resaltado del Último Sismo:** Efecto CSS de pulso de radar animado sobre el evento más reciente.
4. **Sincronización:** Al hacer clic en un marcador, se ejecuta `seleccionarSismo(id)`, actualizando el parámetro `?sismoId=...` en la URL.
5. **Formateo de Fechas:** Uso de `date-fns` con zona horaria `America/Lima` (`formatInTimeZone`).

### 4.4 Validación de la Etapa 4
- Comprobar que los sismos se despliegan en el mapa según sus coordenadas.
- Verificar que el clic en un sismo abre el popup con sus datos oficiales y actualiza la URL.

### 4.5 Registro en Git
```bash
git add frontend/src/features/mapa/
git commit -m "feat(mapa): marcadores sismicos clasificados, popup y seleccion sincronizada"
```

---

## 🏛️ Etapa 5 – Panel de Detalle Integrado Tri-Fuente (SOA) y Radio de Impacto

### 5.1 Objetivo
Consolidar el núcleo arquitectónico de GeoSOA: la integración en tiempo real de **tres fuentes públicas independientes** (IGP + SENAMHI + INEI) a través de un diálogo accesible implementado con `@radix-ui/react-dialog`.

### 5.2 Archivos involucrados
- `frontend/src/features/detalle/PanelDetalleEvento.tsx` (Radix Dialog modal en móvil / drawer lateral en escritorio).
- `frontend/src/features/detalle/BadgeProcedencia.tsx` (insignia visual de origen: `live`, `cache`, `db`).
- `frontend/src/features/detalle/RadioImpactoCapa.tsx` (`Circle` de React Leaflet que grafica la zona de afectación).
- `frontend/src/features/detalle/calcularRadio.ts` (fórmula sismológica $R = 10^{(0.43 \cdot M - 0.27)}$).

### 5.3 Aspectos clave del código
1. **Instalación:**
   ```bash
   npm install @radix-ui/react-dialog
   ```
2. **Consumo Tri-Fuente:** Llama al hook `useEventoIntegrado(sismoId)` cuando la URL contiene un ID.
3. **Indicadores de Procedencia:** Resalta con claridad si los datos provienen del servicio en vivo, de la caché Redis o de la base de datos de respaldo.
4. **Aviso de Respuesta Parcial:** Si la propiedad `fuentesFaltantes` no está vacía (por ejemplo, si el GeoServer de INEI está caído), muestra una alerta amistosa sin bloquear la consulta.
5. **Círculo de Afectación:** Dibuja el radio calculado en kilómetros centrado en el epicentro.

### 5.4 Validación de la Etapa 5
- Validar accesibilidad del diálogo: debe cerrarse al pulsar `Escape` y atrapar el foco correctamente.
- Comprobar que el radio de afectación se dibuja y desaparece al deseleccionar el sismo.

### 5.5 Registro en Git
```bash
git add frontend/src/features/detalle/
git commit -m "feat(soa): panel detalle integrado radix dialog, tri-fuente y radio de impacto"
```

---

## 🔍 Etapa 6 – Filtros Multicriterio y Listado con TanStack Table

### 6.1 Objetivo
Proporcionar un panel de búsqueda avanzada y un listado tabular sincronizado con ordenación, paginación del servidor y exportación a formato CSV.

### 6.2 Archivos involucrados
- `frontend/src/features/filtros/FormularioFiltros.tsx` (inputs de fecha, slider de magnitud, botones de profundidad).
- `frontend/src/features/filtros/SelectorDepartamento.tsx` (componente selector accesible con Radix UI).
- `frontend/src/features/sismos/TablaSismos.tsx` (tabla interactiva con TanStack Table).
- `frontend/src/features/sismos/columnas.tsx` (definición de columnas, badges y ordenamiento).
- `frontend/src/features/sismos/exportarCsv.ts` (utilidad de descarga CSV con BOM `\uFEFF`).

### 6.3 Aspectos clave del código
1. **Instalación:**
   ```bash
   npm install @tanstack/react-table @radix-ui/react-select
   ```
2. **Sincronización de Filtros:** Todos los cambios en los inputs modifican los parámetros de búsqueda (`searchParams`), permitiendo compartir búsquedas exactas mediante un enlace URL copiado.
3. **TanStack Table:** Maneja paginación (`pageSize: 10`), ordenamiento por magnitud o fecha, y selección de fila activa.
4. **Exportación CSV:** Incluye codificación UTF-8 con BOM para garantizar que los acentos y caracteres especiales de departamentos y distritos se abran correctamente en Excel.

### 6.4 Validación de la Etapa 6
- Probar un filtro de magnitud `>= 5.0` y verificar que la tabla y el mapa se filtren simultáneamente.
- Hacer clic en una fila de la tabla y confirmar que el mapa enfoca el evento y abre el detalle integrado.
- Descargar el archivo CSV y abrirlo en una hoja de cálculo.

### 6.5 Registro en Git
```bash
git add frontend/src/features/filtros/ frontend/src/features/sismos/
git commit -m "feat(filtros): formulario sincronizado en url, tanstack table y exportacion csv"
```

---

## 📊 Etapa 7 – Tarjetas KPI de Resumen y Gráficos Analíticos con Recharts

### 7.1 Objetivo
Sintetizar la información de los eventos sísmicos en indicadores ejecutivos y visualizaciones gráficas interactivas construidas con **Recharts**.

### 7.2 Archivos involucrados
- `frontend/src/features/indicadores/TarjetasKpi.tsx` (KPIs: Total sismos, Magnitud Máx, Último sismo, Población expuesta).
- `frontend/src/features/indicadores/GraficoTemporal.tsx` (`AreaChart` de frecuencia diaria).
- `frontend/src/features/indicadores/GraficoDepartamentos.tsx` (`BarChart` de distribución regional).
- `frontend/src/features/indicadores/GraficoMagnitudProfundidad.tsx` (`PieChart` de magnitudes y estratificación).

### 7.3 Aspectos clave del código
1. **Instalación:**
   ```bash
   npm install recharts
   ```
2. **Diseño Responsivo:** Envolver cada gráfico en `<ResponsiveContainer width="100%" height={260}>`.
3. **Reactividad Dinámica:** Los gráficos recalculan sus métricas automáticamente cada vez que los filtros en la URL cambian.
4. **Paleta Armonizada:** Los tooltips y ejes emplean las variables CSS del tema (modo claro y oscuro).

### 7.4 Validación de la Etapa 7
- Verificar que los gráficos se adaptan correctamente al redimensionar la ventana (responsive).
- Pasar el cursor sobre las barras y áreas para verificar los Tooltips formateados en español.

### 7.5 Registro en Git
```bash
git add frontend/src/features/indicadores/
git commit -m "feat(analitica): tarjetas kpi y graficos analiticos interactivos con recharts"
```

---

## 🚀 Etapa 8 – Resiliencia, Accesibilidad, Pruebas y Cierre

### 8.1 Objetivo
Finalizar la aplicación garantizando calidad de nivel productivo: tolerancia a fallos con `ErrorBoundary`, monitoreo del estado de las fuentes externas mediante Spring Boot Actuator, accesibilidad WCAG y pruebas automatizadas.

### 8.2 Archivos involucrados
- `frontend/src/components/ErrorBoundary.tsx` (captura de errores de renderizado con `react-error-boundary`).
- `frontend/src/components/MonitorFuentesBadge.tsx` (consumo de `/actuator/health` con polling cada 30s).
- `frontend/src/components/Skeletons.tsx` (estados de carga tipo placeholder visual).
- `frontend/src/App.test.tsx` (pruebas de integración con Vitest y Testing Library).
- `frontend/README.md` (instrucciones de instalación, scripts y configuración).

### 8.3 Aspectos clave del código
1. **Instalación:**
   ```bash
   npm install react-error-boundary
   ```
2. **Resiliencia Local:** Si una sección falla, el `ErrorBoundary` muestra un mensaje de recuperación con botón de reintento sin congelar el resto del dashboard.
3. **Monitor de Fuentes Externas:** Consulta periódica a `/actuator/health` mostrando un semáforo (Verde/Amarillo/Rojo) con el estado de conectividad con IGP, SENAMHI e INEI.
4. **Accesibilidad:** Verificación de contraste, atributos `aria-*` y navegación por teclado en selectores y modales (`eslint-plugin-jsx-a11y`).

### 8.4 Validación de la Etapa 8
1. Ejecutar linters y formateo:
   ```bash
   npm run lint
   ```
2. Ejecutar la suite completa de pruebas:
   ```bash
   npm test
   ```
3. Generar el bundle de producción:
   ```bash
   npm run build
   ```
4. Probar la previsualización del build:
   ```bash
   npm run preview
   ```

### 8.5 Registro en Git
```bash
git add frontend/ docs/ README.md
git commit -m "feat(frontend): resiliencia con error-boundary, health check, accesibilidad y cierre"
```

---

## 🎓 Justificación Técnica para la Sustentación ante el Docente

Cuando el docente consulte sobre las decisiones de ingeniería de este frontend, puedes argumentar los siguientes fundamentos:

1. **¿Por qué React 19 + TypeScript en lugar de Vanilla JS?**
   - El dashboard maneja un estado multidimensional complejo (filtros cruzados, selección sincronizada en mapa, tabla paginada y gráficos analíticos). TypeScript proporciona tipado estático que previene errores de ejecución en tiempo de desarrollo al mapear las entidades del backend.
2. **¿Por qué TanStack Query para la capa de datos?**
   - Evita el uso manual de `useEffect` y variables de estado repetitivas. Proporciona caché en memoria, deduplicación de peticiones en curso, reintentos automáticos ante caídas de red y revalidación en segundo plano.
3. **¿Por qué la URL como fuente de verdad (`useSearchParams`)?**
   - Sigue las buenas prácticas de arquitectura web: cualquier usuario puede copiar el enlace de la barra de direcciones y compartir exactamente la misma vista, sismo seleccionado y filtros aplicados sin requerir almacenamiento en servidor.
4. **¿Cómo se demuestra la arquitectura SOA y la tolerancia a fallos?**
   - El panel de detalle integrado unifica en un solo contrato de datos tres servicios autónomos (IGP, SENAMHI, INEI). Si el GeoServer del INEI responde `503`, el backend y frontend devuelven datos de respaldo con la insignia de procedencia (`cache`/`db`) y el panel muestra una respuesta parcial sin interrumpir la operación del sistema.
5. **¿Por qué Mock Service Worker (MSW)?**
   - Permite el desacoplamiento total entre frontend y backend: el equipo de frontend puede desarrollar y validar todas las pantallas y pruebas unitarias con fidelidad total a los contratos de la API antes de que el backend esté desplegado.
