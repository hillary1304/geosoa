# Plan de implementación – Frontend

Dashboard web responsivo en HTML, CSS y JavaScript que consume la API REST del backend y muestra los datos en mapas e indicadores. Referencia: [endpoints del sistema](endpoints.md).

## Tecnologías

- HTML5, CSS3 y JavaScript (módulos ES)
- [Leaflet](https://leafletjs.com/) para mapas
- [Chart.js](https://www.chartjs.org/) para gráficos
- Capas WMS del INEI para los límites territoriales

## Estructura

```
frontend
├── index.html
├── css
│   ├── base.css
│   └── components.css
└── js
    ├── main.js
    ├── api.js
    ├── state.js
    ├── map
    └── components
```

---

## Etapa 1 – Estructura y diseño base

**Objetivo:** tener la estructura del proyecto y el layout del dashboard.

- [ ] Crear la carpeta `frontend` con la estructura definida.
- [ ] Definir variables CSS (colores, tipografía, espaciado) y modo claro/oscuro.
- [ ] Layout responsivo: encabezado, panel lateral de filtros, mapa y zona de indicadores.
- [ ] Servir la aplicación localmente con un servidor estático.

**Entregable:** página base responsiva sin datos.

## Etapa 2 – Cliente de la API

**Objetivo:** centralizar la comunicación con el backend.

- [ ] Módulo `api.js` con `fetch`, URL base configurable y timeouts.
- [ ] Manejo uniforme de errores HTTP y de red.
- [ ] Datos simulados para desarrollar sin backend.
- [ ] Estado global simple (`state.js`) para filtros y selección actual.

**Entregable:** funciones de acceso a la API probadas con datos simulados.

## Etapa 3 – Mapa base

**Objetivo:** mostrar el mapa del Perú con las capas territoriales.

- [ ] Inicializar Leaflet centrado en el Perú.
- [ ] Capa base de OpenStreetMap.
- [ ] Capas WMS del INEI: departamentos, provincias y distritos.
- [ ] Control de capas para activarlas y desactivarlas.

**Entregable:** mapa interactivo con límites territoriales.

## Etapa 4 – Capa de sismos

**Objetivo:** representar los sismos en el mapa.

- [ ] Marcadores circulares con tamaño según magnitud y color según profundidad.
- [ ] Ventana emergente con magnitud, fecha, hora, profundidad y referencia.
- [ ] Resaltar el último sismo.
- [ ] Leyenda de magnitud y profundidad.

**Entregable:** sismos del backend visibles en el mapa.

## Etapa 5 – Detalle integrado del evento

**Objetivo:** mostrar la información combinada de un sismo seleccionado.

- [ ] Panel de detalle al seleccionar un sismo.
- [ ] Datos del distrito del epicentro (INEI).
- [ ] Precipitación en la zona (SENAMHI).
- [ ] Población expuesta y radio de afectación dibujado en el mapa.
- [ ] Mostrar la fuente y la fecha de actualización de cada dato.

**Entregable:** vista integrada IGP + SENAMHI + INEI por evento.

## Etapa 6 – Filtros y consultas

**Objetivo:** permitir consultas sobre los sismos.

- [ ] Filtros por rango de fechas, magnitud mínima, profundidad y departamento.
- [ ] Listado tabular de sismos sincronizado con el mapa.
- [ ] Paginación y orden del listado.
- [ ] Conservar los filtros en la URL para compartir consultas.

**Entregable:** consultas filtradas reflejadas en el mapa y en el listado.

## Etapa 7 – Indicadores y gráficos

**Objetivo:** resumir la información en el dashboard.

- [ ] Tarjetas de indicadores: total de sismos, magnitud máxima, último sismo y población expuesta.
- [ ] Gráfico de sismos por día.
- [ ] Gráfico de sismos por departamento.
- [ ] Distribución por magnitud.

**Entregable:** panel de indicadores actualizado según los filtros.

## Etapa 8 – Resiliencia, accesibilidad y cierre

**Objetivo:** dejar el dashboard robusto y listo para la entrega.

- [ ] Estados de carga, vacío y error en cada componente.
- [ ] Aviso cuando los datos provienen de caché o una fuente no está disponible.
- [ ] Revisión de accesibilidad (contraste, teclado, textos alternativos).
- [ ] Pruebas en móvil, tablet y escritorio.
- [ ] Integración final con el backend real.

**Entregable:** dashboard completo funcionando con el backend.
