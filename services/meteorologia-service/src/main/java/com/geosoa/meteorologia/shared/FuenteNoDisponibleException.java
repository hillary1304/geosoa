package com.geosoa.meteorologia.shared;

import com.geosoa.meteorologia.domain.Fuente;

public class FuenteNoDisponibleException extends RuntimeException {

	private final Fuente fuente;

	public FuenteNoDisponibleException(Fuente fuente, String mensaje, Throwable causa) {
		super(mensaje, causa);
		this.fuente = fuente;
	}

	public Fuente getFuente() {
		return fuente;
	}

}
