package com.geosoa.backend.domain;

import java.time.Instant;
import java.util.Comparator;

public record Procedencia(Origen origen, Instant actualizadoEn) {

	public static Procedencia enVivo() {
		return new Procedencia(Origen.LIVE, Instant.now());
	}

	// Cuando un dato sale de varias consultas, se informa la procedencia menos fresca.
	public Procedencia combinar(Procedencia otra) {
		if (otra == null) {
			return this;
		}
		Origen peor = origen.compareTo(otra.origen) >= 0 ? origen : otra.origen;
		Instant masAntigua = Comparator.nullsFirst(Comparator.<Instant>naturalOrder())
			.compare(actualizadoEn, otra.actualizadoEn) <= 0 ? actualizadoEn : otra.actualizadoEn;
		return new Procedencia(peor, masAntigua);
	}

}
