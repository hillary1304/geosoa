package com.geosoa.backend.adapter.inei;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Component
public class IneiAdapter {

	static final String CAPA_DISTRITOS = "Interoperabilidad:ig_pobtotal_dist";

	private static final String ATRIBUTOS = "ubigeo,nombdep,nombprov,nombdist";

	private final RestClient restClient;

	public IneiAdapter(@Qualifier("ineiRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	// Vacío cuando el punto no pertenece a ningún distrito, por ejemplo en el mar.
	public Optional<Distrito> distrito(double latitud, double longitud) {
		validarCoordenadas(latitud, longitud);
		return distritos("INTERSECTS(geom," + punto(latitud, longitud) + ")").stream().findFirst();
	}

	private List<Distrito> distritos(String filtro) {
		return consultar(filtro).features().stream()
			.map(RespuestaWfs.Feature::properties)
			.filter(Objects::nonNull)
			.filter(RespuestaWfs.Propiedades::completas)
			.map(p -> new Distrito(p.ubigeo(), p.nombdep(), p.nombprov(), p.nombdist()))
			.toList();
	}

	private RespuestaWfs consultar(String filtro) {
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
					.queryParam("propertyName", ATRIBUTOS)
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

	private static String punto(double latitud, double longitud) {
		return "POINT(" + decimal(longitud) + " " + decimal(latitud) + ")";
	}

	private static String decimal(double valor) {
		return BigDecimal.valueOf(valor).toPlainString();
	}

}
