package com.geosoa.backend.adapter.igp;

import java.time.LocalDate;

public record FiltroSismos(LocalDate desde, LocalDate hasta, Double magnitudMinima, String departamento) {

	public static FiltroSismos sinFiltros() {
		return new FiltroSismos(null, null, null, null);
	}

}
