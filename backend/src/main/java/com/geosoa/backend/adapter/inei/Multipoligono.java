package com.geosoa.backend.adapter.inei;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
record Multipoligono(List<List<List<List<Double>>>> coordinates) {

	private static final double RADIO_TIERRA_KM = 6371.0088;

	private static final TypeReference<List<List<List<List<Double>>>>> COORDENADAS = new TypeReference<>() {
	};

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

	static Multipoligono desdeJson(String json) {
		return new Multipoligono(JsonMapper.shared().readValue(json, COORDENADAS));
	}

	// Dentro de algún anillo exterior y fuera de sus huecos.
	boolean contiene(double latitud, double longitud) {
		for (List<List<List<Double>>> poligono : coordinates) {
			if (!poligono.isEmpty() && dentroDe(poligono.getFirst(), latitud, longitud)
					&& poligono.stream().skip(1).noneMatch(hueco -> dentroDe(hueco, latitud, longitud))) {
				return true;
			}
		}
		return false;
	}

	// Se mide hasta los vértices, lo que basta porque los límites distritales tienen vértices densos.
	double distanciaKm(double latitud, double longitud) {
		if (contiene(latitud, longitud)) {
			return 0;
		}
		return coordinates.stream()
			.flatMap(List::stream)
			.flatMap(List::stream)
			.mapToDouble(p -> haversineKm(latitud, longitud, p.get(1), p.get(0)))
			.min()
			.orElse(Double.POSITIVE_INFINITY);
	}

	// Ray casting sobre puntos [lon, lat].
	private static boolean dentroDe(List<List<Double>> anillo, double latitud, double longitud) {
		boolean dentro = false;
		for (int i = 0, j = anillo.size() - 1; i < anillo.size(); j = i++) {
			double xi = anillo.get(i).get(0), yi = anillo.get(i).get(1);
			double xj = anillo.get(j).get(0), yj = anillo.get(j).get(1);
			if ((yi > latitud) != (yj > latitud) && longitud < (xj - xi) * (latitud - yi) / (yj - yi) + xi) {
				dentro = !dentro;
			}
		}
		return dentro;
	}

	private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
		return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
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
