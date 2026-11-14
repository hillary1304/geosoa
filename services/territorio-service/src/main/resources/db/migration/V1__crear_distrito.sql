CREATE TABLE distrito (
    ubigeo CHAR(6) NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    provincia VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    poblacion BIGINT,
    densidad_hab_km2 DOUBLE,
    limites LONGTEXT,
    min_longitud DOUBLE,
    min_latitud DOUBLE,
    max_longitud DOUBLE,
    max_latitud DOUBLE,
    actualizado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (ubigeo)
);
