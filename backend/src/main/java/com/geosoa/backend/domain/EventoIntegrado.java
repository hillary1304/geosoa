package com.geosoa.backend.domain;

import java.util.Map;
import java.util.Set;

public record EventoIntegrado(
		Sismo sismo,
		Distrito distrito,
		Precipitacion precipitacion,
		Poblacion poblacion,
		double radioAfectacionKm,
		Long poblacionExpuesta,
		Set<Fuente> fuentesFaltantes,
		Map<Fuente, Procedencia> procedencias) {

	public EventoIntegrado {
		fuentesFaltantes = fuentesFaltantes == null ? Set.of() : Set.copyOf(fuentesFaltantes);
		procedencias = procedencias == null ? Map.of() : Map.copyOf(procedencias);
	}

	public boolean completo() {
		return fuentesFaltantes.isEmpty();
	}

}
