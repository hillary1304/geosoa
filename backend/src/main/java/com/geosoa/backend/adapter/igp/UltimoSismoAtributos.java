package com.geosoa.backend.adapter.igp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record UltimoSismoAtributos(
		Double magnitud,
		Double profundidad,
		@JsonProperty("fecha_local") String fechaLocal,
		@JsonProperty("hora_local") String horaLocal,
		Double latitud,
		Double longitud,
		String intensidad,
		String referencia) {
}
