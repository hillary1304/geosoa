CREATE TABLE sismo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(20) NOT NULL,
    fecha_evento DATETIME(6) NOT NULL,
    latitud DOUBLE NOT NULL,
    longitud DOUBLE NOT NULL,
    profundidad_km DOUBLE NOT NULL,
    magnitud DOUBLE NOT NULL,
    intensidad VARCHAR(255),
    referencia VARCHAR(255),
    departamento VARCHAR(100),
    sentido BOOLEAN NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sismo_codigo UNIQUE (codigo)
);

CREATE INDEX idx_sismo_fecha_evento ON sismo (fecha_evento);

CREATE TABLE distrito (
    ubigeo CHAR(6) NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    provincia VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    poblacion BIGINT,
    densidad_hab_km2 DOUBLE,
    actualizado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (ubigeo)
);
