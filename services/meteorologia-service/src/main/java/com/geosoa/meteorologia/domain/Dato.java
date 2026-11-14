package com.geosoa.meteorologia.domain;

public record Dato<T>(T valor, Procedencia procedencia) {

	public Dato<T> conOrigen(Origen origen) {
		return new Dato<>(valor, new Procedencia(origen, procedencia.actualizadoEn()));
	}

}
