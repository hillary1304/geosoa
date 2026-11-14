package com.geosoa.backend.adapter.inei;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Component
public class TerritorioClient {

	private static final ParameterizedTypeReference<Dato<Optional<Distrito>>> DISTRITO =
			new ParameterizedTypeReference<>() {
			};

	private static final ParameterizedTypeReference<Dato<Optional<Poblacion>>> POBLACION =
			new ParameterizedTypeReference<>() {
			};

	private static final ParameterizedTypeReference<Dato<List<DistritoPoblado>>> DISTRITOS_EN_RADIO =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;

	@Autowired
	public TerritorioClient(@Value("${geosoa.territorio.url:http://localhost:8083}") String url,
			@Value("${geosoa.territorio.connect-timeout:500ms}") Duration connectTimeout,
			@Value("${geosoa.territorio.read-timeout:3s}") Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		this.restClient = RestClient.builder().baseUrl(url).requestFactory(factory).build();
	}

	TerritorioClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public Dato<Optional<Distrito>> distrito(double latitud, double longitud) {
		return get("/internal/v1/distritos/por-punto", DISTRITO, latitud, longitud);
	}

	public Dato<List<DistritoPoblado>> distritosEnRadio(double latitud, double longitud, double radioKm) {
		return get("/internal/v1/distritos/en-radio", DISTRITOS_EN_RADIO, latitud, longitud, radioKm);
	}

	public Dato<Optional<Poblacion>> poblacion(String ubigeo) {
		try {
			return restClient.get().uri("/internal/v1/distritos/{ubigeo}/poblacion", ubigeo).retrieve().body(POBLACION);
		}
		catch (RestClientException ex) {
			throw noDisponible(ex);
		}
	}

	private <T> T get(String path, ParameterizedTypeReference<T> tipo, double latitud, double longitud) {
		return get(path, tipo, latitud, longitud, null);
	}

	private <T> T get(String path, ParameterizedTypeReference<T> tipo, double latitud, double longitud,
			Double radioKm) {
		try {
			return restClient.get().uri(uri -> {
				var builder = uri.path(path).queryParam("latitud", latitud).queryParam("longitud", longitud);
				if (radioKm != null) {
					builder.queryParam("radioKm", radioKm);
				}
				return builder.build();
			}).retrieve().body(tipo);
		}
		catch (RestClientException ex) {
			throw noDisponible(ex);
		}
	}

	private static FuenteNoDisponibleException noDisponible(RestClientException ex) {
		return new FuenteNoDisponibleException(Fuente.INEI, "Territorio no disponible", ex);
	}

}
