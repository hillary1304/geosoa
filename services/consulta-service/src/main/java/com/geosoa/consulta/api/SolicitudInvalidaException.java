package com.geosoa.consulta.api;

import java.util.List;

class SolicitudInvalidaException extends RuntimeException {

	private final List<String> detalles;

	SolicitudInvalidaException(List<String> detalles) {
		super("Parámetros inválidos");
		this.detalles = List.copyOf(detalles);
	}

	List<String> detalles() {
		return detalles;
	}

}
