package com.geosoa.consulta.domain;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record Sismo(
		String codigo,
		OffsetDateTime fechaHora,
		double latitud,
		double longitud,
		double profundidadKm,
		double magnitud,
		String intensidad,
		String referencia,
		String departamento,
		boolean sentido) {

	public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

	public Sismo {
		fechaHora = fechaHora == null ? null : fechaHora.atZoneSameInstant(ZONA_LIMA).toOffsetDateTime();
	}

}
