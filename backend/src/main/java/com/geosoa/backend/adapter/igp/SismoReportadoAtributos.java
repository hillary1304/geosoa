package com.geosoa.backend.adapter.igp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record SismoReportadoAtributos(
		String code,
		Long fechaevento,
		Double lat,
		Double lon,
		Integer prof,
		Double magnitud,
		@JsonProperty("int_") String intensidad,
		String ref,
		String departamento,
		String sentido) {
}
