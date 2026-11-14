package com.geosoa.backend.adapter.senamhi;

import java.util.Optional;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.geosoa.backend.domain.Fuente;

@Component
public class MeteorologiaClient {

	private static final ParameterizedTypeReference<Dato<Optional<Precipitacion>>> RESPUESTA =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;

	public MeteorologiaClient(@Value("${geosoa.meteorologia.url:http://localhost:8082}") String url,
			@Value("${geosoa.meteorologia.connect-timeout:500ms}") Duration connectTimeout,
			@Value("${geosoa.meteorologia.read-timeout:3s}") Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		this.restClient = RestClient.builder().baseUrl(url).requestFactory(factory).build();
	}

	public Dato<Optional<Precipitacion>> precipitacion(double latitud, double longitud, CapaPrecipitacion capa) {
		try {
			return restClient.get()
				.uri(uri -> uri.path("/internal/v1/precipitacion")
					.queryParam("latitud", latitud)
					.queryParam("longitud", longitud)
					.queryParam("capa", capa.name())
					.build())
			.retrieve()
			.body(RESPUESTA);
		}
		catch (RestClientException ex) {
			throw new FuenteNoDisponibleException(Fuente.SENAMHI, "Meteorología no disponible", ex);
		}
	}

}
