package com.geosoa.backend.adapter.inei;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Component
public class IneiAdapter {

	static final String CAPA_DISTRITOS = "Interoperabilidad:ig_pobtotal_dist";

	private static final String ATRIBUTOS_DISTRITO = "ubigeo,nombdep,nombprov,nombdist";

	private static final String ATRIBUTOS_POBLACION = "ubigeo,pobtotal,geom";

	private static final String ATRIBUTOS_COMPLETOS = "ubigeo,nombdep,nombprov,nombdist,pobtotal,geom";

	private static final double KM_POR_GRADO = 111.32;

	private static final Pattern UBIGEO = Pattern.compile("\\d{6}");

	private final RestClient restClient;

	public IneiAdapter(@Qualifier("ineiRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	// Vacío cuando el punto no pertenece a ningún distrito, por ejemplo en el mar.
	public Optional<Distrito> distrito(double latitud, double longitud) {
		validarCoordenadas(latitud, longitud);
		return consultar("INTERSECTS(geom," + punto(latitud, longitud) + ")", ATRIBUTOS_DISTRITO).features().stream()
			.map(RespuestaWfs.Feature::properties)
			.filter(Objects::nonNull)
			.filter(RespuestaWfs.Propiedades::completas)
			.map(IneiAdapter::toDistrito)
			.findFirst();
	}

	public Optional<Poblacion> poblacion(String ubigeo) {
		if (ubigeo == null || !UBIGEO.matcher(ubigeo).matches()) {
			throw new IllegalArgumentException("Ubigeo inválido: " + ubigeo);
		}
		return consultar("ubigeo='" + ubigeo + "'", ATRIBUTOS_POBLACION).features().stream()
			.filter(f -> f.properties() != null && f.properties().pobtotal() != null)
			.map(IneiAdapter::toPoblacion)
			.findFirst();
	}

	// Distritos cuyo límite queda a menos de radioKm del punto, no solo los que contienen el punto.
	public List<DistritoPoblado> distritosEnRadio(double latitud, double longitud, double radioKm) {
		validarCoordenadas(latitud, longitud);
		if (!(radioKm > 0) || Double.isInfinite(radioKm)) {
			throw new IllegalArgumentException("Radio inválido: " + radioKm);
		}
		String filtro = bbox(latitud, longitud, radioKm) + " AND DWITHIN(geom," + punto(latitud, longitud) + ","
				+ decimal(radioKm) + ",kilometers)";
		return consultar(filtro, ATRIBUTOS_COMPLETOS).features().stream()
			.filter(f -> f.properties() != null && f.properties().completas() && f.properties().pobtotal() != null)
			.map(f -> new DistritoPoblado(toDistrito(f.properties()), toPoblacion(f)))
			.toList();
	}

	private static Distrito toDistrito(RespuestaWfs.Propiedades p) {
		return new Distrito(p.ubigeo(), p.nombdep(), p.nombprov(), p.nombdist());
	}

	static Poblacion toPoblacion(RespuestaWfs.Feature feature) {
		long total = Math.round(feature.properties().pobtotal());
		return new Poblacion(feature.properties().ubigeo(), total, densidad(total, feature.geometry()));
	}

	private static Double densidad(long total, Multipoligono geometria) {
		double area = geometria == null ? 0 : geometria.areaKm2();
		return area > 0 ? Math.round(total / area * 10) / 10.0 : null;
	}

	private RespuestaWfs consultar(String filtro, String atributos) {
		RespuestaWfs respuesta;
		try {
			// GeoServer informa los errores como XML con HTTP 200, lo que aquí falla al leerlos como JSON.
			respuesta = restClient.get()
				.uri(uri -> uri.path("/ows")
					.queryParam("service", "WFS")
					.queryParam("version", "1.0.0")
					.queryParam("request", "GetFeature")
					.queryParam("typeName", "{typeName}")
					.queryParam("outputFormat", "{formato}")
					.queryParam("propertyName", atributos)
					.queryParam("CQL_FILTER", "{filtro}")
					.build(CAPA_DISTRITOS, "application/json", filtro))
				.retrieve()
				.body(RespuestaWfs.class);
		}
		catch (RestClientException ex) {
			throw new FuenteNoDisponibleException(Fuente.INEI, "Error al consultar el INEI", ex);
		}
		if (respuesta == null) {
			throw new FuenteNoDisponibleException(Fuente.INEI, "El INEI devolvió una respuesta vacía", null);
		}
		return respuesta;
	}

	private static void validarCoordenadas(double latitud, double longitud) {
		if (!(latitud >= -90 && latitud <= 90) || !(longitud >= -180 && longitud <= 180)) {
			throw new IllegalArgumentException("Coordenadas fuera de rango: " + latitud + ", " + longitud);
		}
	}

	// Rectángulo que contiene el círculo del radio, para que GeoServer use su índice espacial.
	private static String bbox(double latitud, double longitud, double radioKm) {
		double deltaLatitud = radioKm / KM_POR_GRADO;
		double deltaLongitud = radioKm / (KM_POR_GRADO * Math.max(Math.cos(Math.toRadians(latitud)), 0.01));
		return "BBOX(geom," + decimal(redondear(longitud - deltaLongitud, false)) + ","
				+ decimal(redondear(latitud - deltaLatitud, false)) + ","
				+ decimal(redondear(longitud + deltaLongitud, true)) + ","
				+ decimal(redondear(latitud + deltaLatitud, true)) + ")";
	}

	private static double redondear(double valor, boolean haciaArriba) {
		return (haciaArriba ? Math.ceil(valor * 1e4) : Math.floor(valor * 1e4)) / 1e4;
	}

	private static String punto(double latitud, double longitud) {
		return "POINT(" + decimal(longitud) + " " + decimal(latitud) + ")";
	}

	private static String decimal(double valor) {
		return BigDecimal.valueOf(valor).toPlainString();
	}

}
