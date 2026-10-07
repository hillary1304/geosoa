package com.geosoa.backend.adapter.inei;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.json.JsonMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
record Multipoligono(List<List<List<List<Double>>>> coordinates) {

	private static final double RADIO_TIERRA_KM = 6371.0088;

	Multipoligono {
		coordinates = coordinates == null ? List.of() : coordinates;
	}

	// Cada polígono es un anillo exterior seguido de sus huecos, con puntos [lon, lat].
	double areaKm2() {
		double area = 0;
		for (List<List<List<Double>>> poligono : coordinates) {
			for (int i = 0; i < poligono.size(); i++) {
				double anillo = areaAnillo(poligono.get(i));
				area += i == 0 ? anillo : -anillo;
			}
		}
		return area;
	}

	Envolvente envolvente() {
		List<List<Double>> puntos = coordinates.stream().flatMap(List::stream).flatMap(List::stream).toList();
		return new Envolvente(
				puntos.stream().mapToDouble(p -> p.get(0)).min().orElse(0),
				puntos.stream().mapToDouble(p -> p.get(1)).min().orElse(0),
				puntos.stream().mapToDouble(p -> p.get(0)).max().orElse(0),
				puntos.stream().mapToDouble(p -> p.get(1)).max().orElse(0));
	}

	String comoJson() {
		return JsonMapper.shared().writeValueAsString(coordinates);
	}

	// Aproximación esférica de Chamberlain y Duquette, suficiente para superficies distritales.
	private static double areaAnillo(List<List<Double>> anillo) {
		double suma = 0;
		for (int i = 0; i < anillo.size(); i++) {
			List<Double> a = anillo.get(i);
			List<Double> b = anillo.get((i + 1) % anillo.size());
			suma += Math.toRadians(b.get(0) - a.get(0))
					* (2 + Math.sin(Math.toRadians(a.get(1))) + Math.sin(Math.toRadians(b.get(1))));
		}
		return Math.abs(suma) * RADIO_TIERRA_KM * RADIO_TIERRA_KM / 2;
	}

	record Envolvente(double minLongitud, double minLatitud, double maxLongitud, double maxLatitud) {
	}

}
