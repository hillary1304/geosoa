package com.geosoa.consulta.ruta;

public class ServicioNoDisponibleException extends RuntimeException {

	private final Servicio servicio;

	public ServicioNoDisponibleException(Servicio servicio, Throwable causa) {
		super("El servicio de " + servicio.id() + " no está disponible", causa);
		this.servicio = servicio;
	}

	public Servicio servicio() {
		return servicio;
	}

}
