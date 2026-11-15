package com.geosoa.consulta.evento;

import com.geosoa.consulta.domain.Fuente;

// Cada consulta de contexto del evento y la fuente que queda faltante si no responde.
enum Rama {

	PRECIPITACION(Fuente.SENAMHI), DISTRITO(Fuente.INEI), EN_RADIO(Fuente.INEI), POBLACION(Fuente.INEI);

	private final Fuente fuente;

	Rama(Fuente fuente) {
		this.fuente = fuente;
	}

	Fuente fuente() {
		return fuente;
	}

}
