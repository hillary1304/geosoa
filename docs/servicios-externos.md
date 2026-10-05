# Servicios externos

Endpoints y parámetros de los servicios públicos que consume GeoSOA. Cada servicio tiene un adaptador dedicado en el backend.

| Entidad | Tecnología | Uso en el sistema | Estado verificado (2026-10-05) |
|---|---|---|---|
| IGP | ArcGIS REST (MapServer) | Eventos sísmicos | Disponible |
| SENAMHI | OGC WFS (GeoServer) | Precipitación en la zona del evento | Disponible |
| INEI | OGC WFS / WMS (GeoServer) | Límites distritales y población | No disponible (HTTP 503) |

---

## 1. IGP – Instituto Geofísico del Perú

- **Portal:** https://www.igp.gob.pe/servicios/infraestructura-de-datos-espaciales/componentes/webservice
- **URL base:** `https://ide.igp.gob.pe/arcgis/rest/services/monitoreocensis`
- **Sistema de referencia:** EPSG:4326 (WGS84)
- **Máximo de registros por consulta:** 2000
- **Formatos (`f`):** `json`, `geojson`, `pbf`

Servicios disponibles en la carpeta `monitoreocensis`: `UltimoSismo`, `SismosReportados`, `Sismicidad`, `PeligroSismico`, `AceleracionGRD`, `Dorsal`.

### 1.1 Último sismo

Devuelve **un único registro** con el último sismo reportado.

```
GET /UltimoSismo/MapServer/0/query
```

| Parámetro | Obligatorio | Valor recomendado | Descripción |
|---|---|---|---|
| `where` | Sí | `1=1` | Filtro SQL sobre atributos |
| `outFields` | No | `*` | Campos a devolver, separados por coma |
| `returnGeometry` | No | `false` | La ubicación ya viene en `latitud`/`longitud` |
| `f` | Sí | `json` | Formato de respuesta |

**Ejemplo:**

```
https://ide.igp.gob.pe/arcgis/rest/services/monitoreocensis/UltimoSismo/MapServer/0/query?where=1%3D1&outFields=*&returnGeometry=false&f=json
```

**Campos:**

| Campo | Tipo | Descripción |
|---|---|---|
| `objectid` | OID | Identificador interno |
| `magnitud` | Double | Magnitud |
| `profundidad` | Double | Profundidad en km |
| `fecha_local` | String | Fecha local, formato `dd/MM/yyyy` |
| `hora_local` | String | Hora local, formato `HH:mm:ss` |
| `latitud` | Double | Latitud del epicentro |
| `longitud` | Double | Longitud del epicentro |
| `intensidad` | String | Intensidad (puede venir vacía) |
| `referencia` | String | Referencia geográfica textual |
| `epicentro` | String | `Continente` o `Mar` |
| `ubicacion` | String | `Continente` o `Mar` |

**Respuesta (extracto):**

```json
{
  "features": [
    {
      "attributes": {
        "objectid": 1,
        "magnitud": 4.2,
        "profundidad": 95.0,
        "fecha_local": "05/10/2026",
        "hora_local": "05:32:49",
        "latitud": -16.19,
        "longitud": -72.74,
        "intensidad": "",
        "referencia": "29 km al SO de Aplao, Castilla - Arequipa",
        "epicentro": "Continente",
        "ubicacion": "Continente"
      }
    }
  ]
}
```

### 1.2 Sismos reportados

Histórico de sismos reportados del año en curso (719 registros al 2026-10-05). Es la fuente principal para listados, filtros e indicadores.

```
GET /SismosReportados/MapServer/0/query
```

| Parámetro | Obligatorio | Ejemplo | Descripción |
|---|---|---|---|
| `where` | Sí | `fechaevento >= DATE '2026-10-01' AND magnitud >= 4` | Filtro SQL sobre atributos |
| `outFields` | No | `*` | Campos a devolver |
| `orderByFields` | No | `fechaevento DESC` | Orden |
| `resultOffset` | No | `0` | Desplazamiento para paginación |
| `resultRecordCount` | No | `100` | Tamaño de página (máx. 2000) |
| `returnCountOnly` | No | `true` | Devuelve solo `{"count": n}` |
| `returnGeometry` | No | `false` | Incluir geometría |
| `geometry` | No | `-77.2,-12.3,-76.8,-11.9` | Filtro espacial (xmin,ymin,xmax,ymax) |
| `geometryType` | No | `esriGeometryEnvelope` | Tipo de geometría del filtro |
| `inSR` / `outSR` | No | `4326` | Sistema de referencia de entrada/salida |
| `spatialRel` | No | `esriSpatialRelIntersects` | Relación espacial |
| `f` | Sí | `json` | Formato de respuesta |

**Ejemplos:**

```
# Últimos 50 sismos
.../SismosReportados/MapServer/0/query?where=fechaevento%20IS%20NOT%20NULL&outFields=*&orderByFields=fechaevento%20DESC&resultRecordCount=50&returnGeometry=false&f=json

# Conteo de sismos de magnitud >= 4 desde una fecha
.../SismosReportados/MapServer/0/query?where=fechaevento%20%3E%3D%20DATE%20%272026-10-01%27%20AND%20magnitud%20%3E%3D%204&returnCountOnly=true&f=json
```

**Campos:**

| Campo | Tipo | Descripción |
|---|---|---|
| `objectid` | OID | Identificador interno |
| `code` | String | Código del reporte, p. ej. `2026-717` |
| `reporte` | SmallInteger | Número de reporte del año |
| `fechaevento` | Date | Fecha y hora del evento en epoch ms (UTC) |
| `fecha` | Date | Fecha local a medianoche en epoch ms |
| `hora` | String | Hora local `HH:mm:ss` |
| `lat` / `lon` | Double | Coordenadas del epicentro |
| `prof` | Integer | Profundidad en km |
| `negprof` | Integer | Profundidad negativa |
| `profundidad` | String | `Superficial`, `Intermedio` o `Profundo` |
| `magnitud` | Double | Magnitud |
| `mag` | String | Magnitud como texto |
| `int_` | String | Intensidad, p. ej. `II-III Oxapampa` |
| `ref` | String | Referencia geográfica textual |
| `departamento` | String | Departamento en mayúsculas |
| `sentido` | String | `1` si fue sentido, `0` si no |
| `ultimo` | String | `1` si es el último sismo |

**Respuesta (extracto):**

```json
{
  "attributes": {
    "objectid": 2374,
    "code": "2026-716",
    "reporte": 716,
    "fechaevento": 1791161419000,
    "fecha": 1791090000000,
    "hora": "19:50:19",
    "lat": -10.56,
    "lon": -75.39,
    "prof": 10,
    "profundidad": "Superficial",
    "magnitud": 3.5,
    "int_": "II-III Oxapampa",
    "ref": "2 km al NE de Oxapampa, Oxapampa - Pasco",
    "departamento": "PASCO",
    "sentido": "1",
    "ultimo": "0"
  }
}
```

**Consideraciones:**

- Existen registros con todos los campos nulos; filtrar siempre con `fechaevento IS NOT NULL`.
- Las fechas de tipo `Date` llegan en epoch ms; convertir a `America/Lima` para mostrarlas.
- Metadatos de la capa: `.../SismosReportados/MapServer/0?f=json`.

---

## 2. SENAMHI – Servicio Nacional de Meteorología e Hidrología

- **Catálogo:** https://idesep.senamhi.gob.pe/portalidesep/idesep_componente_catalogo_geoservicios_wfs.jsp
- **Catálogo en JSON:** `GET https://idesep.senamhi.gob.pe/servicios/list` (incluye `temaNomCorto`, `grupoNomCorto`, `objetoNombreCorto`, `urlWfs`)
- **Servidor:** GeoServer, OGC WFS 1.0.0
- **Sistema de referencia:** EPSG:4326

### 2.1 Precipitación anual (capa seleccionada)

```
GET https://idesep.senamhi.gob.pe/geoserver/g_05_02/ows
```

- **Capa (`typeName`):** `g_05_02:05_02_013_03_001_512_0000_00_00`
- **Geometría:** `geom` (MultiPolygon)
- **Atributos:** `rango` (String), rango de precipitación anual acumulada en mm, p. ej. `"10 - 15"`
- **Total de polígonos:** 57 680. No descargar la capa completa: consultar siempre por punto o por bbox.

| Parámetro | Obligatorio | Ejemplo | Descripción |
|---|---|---|---|
| `service` | Sí | `WFS` | Servicio OGC |
| `version` | Sí | `1.0.0` | Versión del estándar |
| `request` | Sí | `GetFeature` | También `GetCapabilities` y `DescribeFeatureType` |
| `typeName` | Sí | `g_05_02:05_02_013_03_001_512_0000_00_00` | Capa a consultar |
| `outputFormat` | No | `application/json` | También `GML2`, `SHAPE-ZIP` |
| `propertyName` | No | `rango` | Atributos a devolver; omitir `geom` evita traer geometrías |
| `CQL_FILTER` | No | `INTERSECTS(geom,POINT(-72.74 -16.19))` | Filtro CQL (orden `lon lat`) |
| `bbox` | No | `-72.75,-16.2,-72.73,-16.18` | Filtro por rectángulo `minx,miny,maxx,maxy` |
| `maxFeatures` | No | `10` | Límite de resultados |

**Ejemplo (precipitación en el epicentro de un sismo):**

```
https://idesep.senamhi.gob.pe/geoserver/g_05_02/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=g_05_02:05_02_013_03_001_512_0000_00_00&outputFormat=application/json&propertyName=rango&CQL_FILTER=INTERSECTS(geom,POINT(-77.03%20-12.05))
```

**Respuesta:**

```json
{
  "type": "FeatureCollection",
  "totalFeatures": 1,
  "features": [
    {
      "type": "Feature",
      "id": "05_02_013_03_001_512_0000_00_00.56092",
      "geometry": null,
      "properties": { "rango": "10 - 15" }
    }
  ]
}
```

### 2.2 Capas alternativas de precipitación

Mismo endpoint, parámetros y atributo `rango`; solo cambia `typeName`.

| Capa | Workspace | `typeName` |
|---|---|---|
| Mes de enero … diciembre | `g_05_02` | `g_05_02:05_02_001_03_001_512_0000_00_00` … `g_05_02:05_02_012_03_001_512_0000_00_00` |
| Verano / Otoño / Invierno / Primavera | `g_05_02` | `g_05_02:05_02_014_…` / `015` / `016` / `017` (sufijo `_03_001_512_0000_00_00`) |
| Anomalía de precipitación (monitoreo por década) | `g_04_02` | `g_04_02:04_02_005_03_002_512_0000_00_00`, `g_04_02:04_02_006_03_002_512_0000_00_00` |
| Pronóstico climático trimestral | `g_03_02` | `g_03_02:03_02_001_03_000_512_0000_00_00` |

Las capas de monitoreo y pronóstico se renuevan periódicamente; obtener sus `typeName` vigentes desde el catálogo JSON.

---

## 3. INEI – Instituto Nacional de Estadística e Informática

- **Portal:** https://ide.inei.gob.pe/
- **Servidor:** GeoServer, workspace `Interoperabilidad`
- **WFS:** `GET https://geoespacial.inei.gob.pe/geoserver/Interoperabilidad/ows`
- **WMS:** `GET https://geoespacial.inei.gob.pe/geoserver/Interoperabilidad/wms`
- **Sistema de referencia:** EPSG:4326

> El 2026-10-05 el servidor respondía `HTTP 503 Server-unavailable!`. Las capas se tomaron del catálogo oficial del portal; los atributos deben confirmarse con `DescribeFeatureType` cuando el servicio esté disponible.

### 3.1 Capas a utilizar

| Capa (`typeName`) | Contenido | Uso en el sistema |
|---|---|---|
| `Interoperabilidad:ig_distrito` | Límites distritales | Ubicar el distrito del epicentro |
| `Interoperabilidad:ig_provincia` | Límites provinciales | Agregación por provincia |
| `Interoperabilidad:ig_departamento` | Límites departamentales | Agregación por departamento |
| `Interoperabilidad:ig_pobtotal_dist` | Población total por distrito | Población y densidad expuesta |
| `Interoperabilidad:ig_pobtotal_prov` | Población total por provincia | Indicadores por provincia |
| `Interoperabilidad:ig_pob_total_dep` | Población total por departamento | Indicadores por departamento |
| `Interoperabilidad:ig_centropoblado` | Centros poblados | Poblados dentro del radio de afectación |

### 3.2 WFS GetFeature

| Parámetro | Obligatorio | Ejemplo | Descripción |
|---|---|---|---|
| `service` | Sí | `WFS` | Servicio OGC |
| `version` | Sí | `1.0.0` | Versión del estándar |
| `request` | Sí | `GetFeature` | También `GetCapabilities` y `DescribeFeatureType` |
| `typeName` | Sí | `Interoperabilidad:ig_distrito` | Capa a consultar |
| `outputFormat` | No | `application/json` | También GML |
| `maxFeatures` | No | `50` | Límite de resultados |
| `propertyName` | No | — | Atributos a devolver (pendiente de verificar) |
| `CQL_FILTER` | No | `INTERSECTS(<geom>,POINT(-72.74 -16.19))` | Filtro espacial; nombre de geometría pendiente de verificar |
| `bbox` | No | `-72.8,-16.3,-72.6,-16.1` | Filtro por rectángulo |

**Ejemplos:**

```
# Distritos (GeoJSON)
https://geoespacial.inei.gob.pe/geoserver/Interoperabilidad/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=Interoperabilidad%3Aig_distrito&maxFeatures=50&outputFormat=application%2Fjson

# Atributos de la capa
https://geoespacial.inei.gob.pe/geoserver/Interoperabilidad/ows?service=WFS&version=1.0.0&request=DescribeFeatureType&typeName=Interoperabilidad:ig_pobtotal_dist
```

**Atributos esperados según el informe** (pendientes de confirmar): código de Ubigeo, nombre de departamento, provincia y distrito, población total censada y densidad poblacional (hab/km²).

### 3.3 WMS GetMap

Para mostrar los límites directamente en el mapa del frontend sin pasar por el backend.

| Parámetro | Ejemplo |
|---|---|
| `service` | `WMS` |
| `version` | `1.1.0` |
| `request` | `GetMap` |
| `layers` | `Interoperabilidad:ig_distrito` |
| `bbox` | `-81.33,-18.35,-68.65,-0.04` |
| `width` / `height` | `531` / `768` |
| `srs` | `EPSG:4326` |
| `format` | `image/png` |
| `transparent` | `true` |
