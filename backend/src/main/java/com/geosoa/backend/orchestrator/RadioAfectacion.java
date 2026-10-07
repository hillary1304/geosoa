package com.geosoa.backend.orchestrator;

final class RadioAfectacion {

	static final double MINIMO_KM = 5;

	static final double MAXIMO_KM = 300;

	private RadioAfectacion() {
	}

	// Distancia hipocentral aproximada hasta donde el sismo se percibe con claridad (10^(0.5M - 1) km),
	// proyectada a la superficie con Pitágoras para descontar la profundidad.
	static double calcularKm(double magnitud, double profundidadKm) {
		if (!Double.isFinite(magnitud) || !Double.isFinite(profundidadKm) || profundidadKm < 0) {
			throw new IllegalArgumentException("Magnitud o profundidad inválida: " + magnitud + ", " + profundidadKm);
		}
		double hipocentral = Math.pow(10, 0.5 * magnitud - 1);
		double superficial = Math.sqrt(Math.max(hipocentral * hipocentral - profundidadKm * profundidadKm, 0));
		double acotado = Math.clamp(superficial, MINIMO_KM, MAXIMO_KM);
		return Math.round(acotado * 10) / 10.0;
	}

}
