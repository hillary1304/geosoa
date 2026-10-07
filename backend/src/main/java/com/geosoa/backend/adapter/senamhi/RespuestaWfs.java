package com.geosoa.backend.adapter.senamhi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record RespuestaWfs(List<Feature> features) {

	RespuestaWfs {
		features = features == null ? List.of() : List.copyOf(features);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Feature(Propiedades properties) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Propiedades(String rango) {
	}

}
