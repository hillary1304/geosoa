package com.geosoa.backend.adapter.inei;

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
	record Propiedades(String ubigeo, String nombdep, String nombprov, String nombdist) {

		boolean completas() {
			return ubigeo != null && nombdep != null && nombprov != null && nombdist != null;
		}

	}

}
