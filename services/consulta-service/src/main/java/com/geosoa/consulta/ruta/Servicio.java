package com.geosoa.consulta.ruta;

import com.geosoa.consulta.domain.Fuente;

public enum Servicio {

	SISMOS("sismos", Fuente.IGP), METEOROLOGIA("meteorologia", Fuente.SENAMHI), TERRITORIO("territorio", Fuente.INEI);

	private final String id;

	private final Fuente fuente;

	Servicio(String id, Fuente fuente) {
		this.id = id;
		this.fuente = fuente;
	}

	public String id() {
		return id;
	}

	public Fuente fuente() {
		return fuente;
	}

	public String endpointLlamada() {
		return "direct:llamar-" + id;
	}

}
