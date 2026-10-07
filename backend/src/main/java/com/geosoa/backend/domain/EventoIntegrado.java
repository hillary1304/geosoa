package com.geosoa.backend.domain;

import java.util.Set;

public record EventoIntegrado(
		Sismo sismo,
		Distrito distrito,
		Precipitacion precipitacion,
		Poblacion poblacion,
		Set<Fuente> fuentesFaltantes) {

	public EventoIntegrado {
		fuentesFaltantes = fuentesFaltantes == null ? Set.of() : Set.copyOf(fuentesFaltantes);
	}

	public boolean completo() {
		return fuentesFaltantes.isEmpty();
	}

}
