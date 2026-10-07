package com.geosoa.backend.adapter.inei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

class IneiAdapterTests {

	private static final String URL_BASE = "https://inei.test/geoserver/Interoperabilidad";

	private MockRestServiceServer server;

	private IneiAdapter adapter;

	@BeforeEach
	void configurar() {
		RestClient.Builder builder = RestClient.builder().baseUrl(URL_BASE);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new IneiAdapter(builder.build());
	}

	@Test
	void obtieneElDistritoDeUnPunto() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("request", "GetFeature"))
			.andExpect(parametro("typeName", "Interoperabilidad:ig_pobtotal_dist"))
			.andExpect(parametro("CQL_FILTER", "INTERSECTS(geom,POINT(-77.03 -12.05))"))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 1, "features": [
						{"type": "Feature", "id": "ig_pobtotal_dist.1290", "geometry": null, "properties": {
							"nombdep": "Lima", "nombprov": "Lima", "nombdist": "Lima", "ubigeo": "150101"}}
					], "crs": null}
					""", MediaType.APPLICATION_JSON));

		Distrito distrito = adapter.distrito(-12.05, -77.03).orElseThrow();

		assertThat(distrito).isEqualTo(new Distrito("150101", "Lima", "Lima", "Lima"));
		server.verify();
	}

	@Test
	void devuelveVacioFueraDeLosDistritos() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 0, "features": [], "crs": null}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.distrito(-12, -80)).isEmpty();
	}

	@Test
	void descartaDistritosConAtributosNulos() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess("""
					{"features": [{"properties": {"ubigeo": null, "nombdep": "Lima", "nombprov": "Lima", "nombdist": "Lima"}}]}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.distrito(-12.05, -77.03)).isEmpty();
	}

	@Test
	void obtieneLaPoblacionYDensidadDeUnUbigeo() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andExpect(parametro("propertyName", "ubigeo,pobtotal,geom"))
			.andExpect(parametro("CQL_FILTER", "ubigeo='150101'"))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "features": [{"type": "Feature", "id": "ig_pobtotal_dist.1290",
						"geometry": {"type": "MultiPolygon", "coordinates": [[[
							[-77.05, -12.07], [-77.0, -12.07], [-77.0, -12.02], [-77.05, -12.02], [-77.05, -12.07]
						]]]},
						"properties": {"ubigeo": "150101", "pobtotal": 273142}}]}
					""", MediaType.APPLICATION_JSON));

		Poblacion poblacion = adapter.poblacion("150101").orElseThrow();

		assertThat(poblacion.ubigeo()).isEqualTo("150101");
		assertThat(poblacion.total()).isEqualTo(273_142);
		assertThat(poblacion.densidadHabKm2()).isCloseTo(9_036, within(10.0));
		server.verify();
	}

	@Test
	void poblacionSinGeometriaNoTieneDensidad() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess("""
					{"features": [{"geometry": null, "properties": {"ubigeo": "040402", "pobtotal": 1102.0}}]}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.poblacion("040402")).contains(new Poblacion("040402", 1_102, null));
	}

	@Test
	void devuelveVacioSiElUbigeoNoExiste() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 0, "features": []}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.poblacion("999999")).isEmpty();
	}

	@Test
	void rechazaUbigeoInvalido() {
		assertThatThrownBy(() -> adapter.poblacion("15010' OR '1'='1"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.poblacion(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void calculaElAreaDescontandoLosHuecos() {
		Multipoligono cuadrado = new Multipoligono(List.of(List.of(
				List.of(List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(1.0, 1.0), List.of(0.0, 1.0), List.of(0.0, 0.0)))));
		Multipoligono conHueco = new Multipoligono(List.of(List.of(
				List.of(List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(1.0, 1.0), List.of(0.0, 1.0), List.of(0.0, 0.0)),
				List.of(List.of(0.0, 0.0), List.of(0.5, 0.0), List.of(0.5, 1.0), List.of(0.0, 1.0), List.of(0.0, 0.0)))));

		assertThat(cuadrado.areaKm2()).isCloseTo(12_363, within(5.0));
		assertThat(conHueco.areaKm2()).isCloseTo(cuadrado.areaKm2() / 2, within(1.0));
	}

	@Test
	void informaFuenteNoDisponibleAnteServicioCaido() {
		server.expect(requestTo(startsWith(URL_BASE)))
			.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("Server-unavailable!"));

		assertThatThrownBy(() -> adapter.distrito(-12.05, -77.03))
			.isInstanceOfSatisfying(FuenteNoDisponibleException.class,
					ex -> assertThat(ex.getFuente()).isEqualTo(Fuente.INEI));
	}

	@Test
	void informaFuenteNoDisponibleAnteErrorHttp() {
		server.expect(requestTo(startsWith(URL_BASE))).andRespond(withServerError());

		assertThatThrownBy(() -> adapter.distrito(-12.05, -77.03))
			.isInstanceOf(FuenteNoDisponibleException.class);
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

		assertThatThrownBy(() -> adapter.distrito(-12.05, -77.03))
			.isInstanceOf(FuenteNoDisponibleException.class);
	}

	@Test
	void rechazaCoordenadasFueraDeRango() {
		assertThatThrownBy(() -> adapter.distrito(-91, -77))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.distrito(Double.NaN, -77))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static RequestMatcher parametro(String nombre, String valor) {
		return request -> {
			String recibido = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(nombre);
			assertThat(recibido).isNotNull();
			assertThat(UriUtils.decode(recibido, StandardCharsets.UTF_8)).isEqualTo(valor);
		};
	}

}
