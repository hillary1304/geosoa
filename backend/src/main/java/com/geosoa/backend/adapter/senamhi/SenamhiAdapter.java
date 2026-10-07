package com.geosoa.backend.adapter.senamhi;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Component
public class SenamhiAdapter {

	private static final String CAPA_ANUAL = "g_05_02:05_02_013_03_001_512_0000_00_00";

	private static final Pattern RANGO_CERRADO = Pattern.compile("\\s*(\\d+(?:\\.\\d+)?)\\s*-\\s*(\\d+(?:\\.\\d+)?)\\s*");

	private static final Pattern RANGO_ABIERTO = Pattern.compile("\\s*>\\s*(\\d+(?:\\.\\d+)?)\\s*");

	private final RestClient restClient;

	public SenamhiAdapter(@Qualifier("senamhiRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	// Vacío cuando el punto no tiene cobertura, por ejemplo en el mar.
	public Optional<Precipitacion> precipitacion(double latitud, double longitud) {
		validarCoordenadas(latitud, longitud);
		RespuestaWfs respuesta = consultar(CAPA_ANUAL, latitud, longitud);
		return respuesta.features().stream()
			.map(RespuestaWfs.Feature::properties)
			.filter(Objects::nonNull)
			.map(RespuestaWfs.Propiedades::rango)
			.filter(Objects::nonNull)
			.findFirst()
			.map(rango -> interpretarRango("ANUAL", rango));
	}

	private RespuestaWfs consultar(String typeName, double latitud, double longitud) {
		String workspace = typeName.substring(0, typeName.indexOf(':'));
		String filtro = "INTERSECTS(geom,POINT(" + decimal(longitud) + " " + decimal(latitud) + "))";
		RespuestaWfs respuesta;
		try {
			// GeoServer informa los errores como XML con HTTP 200, lo que aquí falla al leerlos como JSON.
			respuesta = restClient.get()
				.uri(uri -> uri.path("/{workspace}/ows")
					.queryParam("service", "WFS")
					.queryParam("version", "1.0.0")
					.queryParam("request", "GetFeature")
					.queryParam("typeName", "{typeName}")
					.queryParam("outputFormat", "{formato}")
					.queryParam("propertyName", "rango")
					.queryParam("CQL_FILTER", "{filtro}")
					.build(workspace, typeName, "application/json", filtro))
				.retrieve()
				.body(RespuestaWfs.class);
		}
		catch (RestClientException ex) {
			throw new FuenteNoDisponibleException(Fuente.SENAMHI, "Error al consultar el SENAMHI", ex);
		}
		if (respuesta == null) {
			throw new FuenteNoDisponibleException(Fuente.SENAMHI, "El SENAMHI devolvió una respuesta vacía", null);
		}
		return respuesta;
	}

	static Precipitacion interpretarRango(String capa, String rango) {
		Matcher cerrado = RANGO_CERRADO.matcher(rango);
		if (cerrado.matches()) {
			return new Precipitacion(capa, Double.parseDouble(cerrado.group(1)), Double.parseDouble(cerrado.group(2)));
		}
		Matcher abierto = RANGO_ABIERTO.matcher(rango);
		if (abierto.matches()) {
			return new Precipitacion(capa, Double.parseDouble(abierto.group(1)), null);
		}
		throw new FuenteNoDisponibleException(Fuente.SENAMHI, "Rango de precipitación no reconocido: " + rango, null);
	}

	private static void validarCoordenadas(double latitud, double longitud) {
		if (!(latitud >= -90 && latitud <= 90) || !(longitud >= -180 && longitud <= 180)) {
			throw new IllegalArgumentException("Coordenadas fuera de rango: " + latitud + ", " + longitud);
		}
	}

	private static String decimal(double valor) {
		return BigDecimal.valueOf(valor).toPlainString();
	}

}
