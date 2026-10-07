package com.geosoa.backend.domain;

import java.time.ZonedDateTime;

public record Sismo(
		String codigo,
		ZonedDateTime fechaHora,
		double latitud,
		double longitud,
		double profundidadKm,
		double magnitud,
		String intensidad,
		String referencia,
		String departamento,
		boolean sentido) {
}
