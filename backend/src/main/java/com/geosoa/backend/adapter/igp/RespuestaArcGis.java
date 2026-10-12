package com.geosoa.backend.adapter.igp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record RespuestaArcGis<T>(List<Feature<T>> features, Long count, Error error) {

	RespuestaArcGis {
		features = features == null ? List.of() : List.copyOf(features);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Feature<T>(T attributes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Error(Integer code, String message) {
	}

}
