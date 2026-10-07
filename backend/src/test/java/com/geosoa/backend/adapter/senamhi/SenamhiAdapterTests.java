package com.geosoa.backend.adapter.senamhi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

class SenamhiAdapterTests {

	private static final String URL_BASE = "https://senamhi.test/geoserver";

	private MockRestServiceServer server;

	private SenamhiAdapter adapter;

	@BeforeEach
	void configurar() {
		RestClient.Builder builder = RestClient.builder().baseUrl(URL_BASE);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new SenamhiAdapter(builder.build());
	}

	@Test
	void obtieneLaPrecipitacionAnualEnUnPunto() {
		server.expect(requestTo(startsWith(URL_BASE + "/g_05_02/ows")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("request", "GetFeature"))
			.andExpect(parametro("typeName", "g_05_02:05_02_013_03_001_512_0000_00_00"))
			.andExpect(queryParam("propertyName", "rango"))
			.andExpect(parametro("CQL_FILTER", "INTERSECTS(geom,POINT(-77.03 -12.05))"))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 1, "features": [
						{"type": "Feature", "id": "05_02_013_03_001_512_0000_00_00.39654", "geometry": null,
							"properties": {"rango": "10 - 15"}}
					], "crs": null}
					""", MediaType.APPLICATION_JSON));

		Precipitacion precipitacion = adapter.precipitacion(-12.05, -77.03).orElseThrow();

		assertThat(precipitacion).isEqualTo(new Precipitacion("ANUAL", 10, 15.0));
		server.verify();
	}

	@Test
	void devuelveVacioSinCobertura() {
		server.expect(requestTo(startsWith(URL_BASE + "/g_05_02/ows")))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 0, "features": [], "crs": null}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.precipitacion(-12, -80)).isEmpty();
	}

	@Test
	void informaFuenteNoDisponibleAnteErrorHttp() {
		server.expect(requestTo(startsWith(URL_BASE))).andRespond(withServerError());

		assertThatThrownBy(() -> adapter.precipitacion(-12.05, -77.03))
			.isInstanceOfSatisfying(FuenteNoDisponibleException.class,
					ex -> assertThat(ex.getFuente()).isEqualTo(Fuente.SENAMHI));
	}

	@Test
	void informaFuenteNoDisponibleAnteExcepcionXml() {
		server.expect(requestTo(startsWith(URL_BASE)))
			.andRespond(withSuccess("""
					<?xml version="1.0" ?>
					<ServiceExceptionReport version="1.2.0" xmlns="http://www.opengis.net/ogc">
						<ServiceException code="InvalidParameterValue" locator="typeName">Feature type unknown</ServiceException>
					</ServiceExceptionReport>
					""", MediaType.TEXT_XML));

		assertThatThrownBy(() -> adapter.precipitacion(-12.05, -77.03))
			.isInstanceOf(FuenteNoDisponibleException.class);
	}

	@Test
	void rechazaCoordenadasFueraDeRango() {
		assertThatThrownBy(() -> adapter.precipitacion(-91, -77))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.precipitacion(Double.NaN, -77))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void interpretaRangosCerradosYAbiertos() {
		assertThat(SenamhiAdapter.interpretarRango("ANUAL", "1500 - 1800"))
			.isEqualTo(new Precipitacion("ANUAL", 1500, 1800.0));
		assertThat(SenamhiAdapter.interpretarRango("ANUAL", "0 - 5"))
			.isEqualTo(new Precipitacion("ANUAL", 0, 5.0));
		assertThat(SenamhiAdapter.interpretarRango("ANUAL", "> 6000"))
			.isEqualTo(new Precipitacion("ANUAL", 6000, null));
	}

	@Test
	void informaRangoNoReconocido() {
		assertThatThrownBy(() -> SenamhiAdapter.interpretarRango("ANUAL", "sin datos"))
			.isInstanceOf(FuenteNoDisponibleException.class)
			.hasMessageContaining("sin datos");
	}

	private static RequestMatcher parametro(String nombre, String valor) {
		return request -> {
			String recibido = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(nombre);
			assertThat(recibido).isNotNull();
			assertThat(UriUtils.decode(recibido, StandardCharsets.UTF_8)).isEqualTo(valor);
		};
	}

}
