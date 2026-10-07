package com.geosoa.backend.adapter.inei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
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
import com.geosoa.backend.persistence.DistritoEntity;
import com.geosoa.backend.persistence.DistritoRepository;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

class IneiAdapterTests {

	private static final String URL_BASE = "https://inei.test/geoserver/Interoperabilidad";

	private static final String DISTRITO_CON_LIMITES = """
			{"type": "FeatureCollection", "features": [{"type": "Feature", "id": "ig_pobtotal_dist.1290",
				"geometry": {"type": "MultiPolygon", "coordinates": [[[
					[-77.05, -12.07], [-77.0, -12.07], [-77.0, -12.02], [-77.05, -12.02], [-77.05, -12.07]
				]]]},
				"properties": {"ubigeo": "150101", "nombdep": "Lima", "nombprov": "Lima", "nombdist": "Lima",
					"pobtotal": 273142}}]}
			""";

	private MockRestServiceServer server;

	private DistritoRepository repository;

	private IneiAdapter adapter;

	@BeforeEach
	void configurar() {
		RestClient.Builder builder = RestClient.builder().baseUrl(URL_BASE);
		server = MockRestServiceServer.bindTo(builder).build();
		repository = mock(DistritoRepository.class);
		adapter = new IneiAdapter(builder.build(), repository);
	}

	@Test
	void obtieneElDistritoDeUnPunto() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("request", "GetFeature"))
			.andExpect(parametro("typeName", "Interoperabilidad:ig_pobtotal_dist"))
			.andExpect(parametro("propertyName", "ubigeo,nombdep,nombprov,nombdist,pobtotal,geom"))
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
	void obtieneLosDistritosDentroDeUnRadio() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andExpect(parametro("CQL_FILTER", "BBOX(geom,-72.9271,-16.3697,-72.5529,-16.0103)"
					+ " AND DWITHIN(geom,POINT(-72.74 -16.19),20.0,kilometers)"))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 3, "features": [
						{"geometry": null, "properties": {"ubigeo": "040401", "nombdep": "Arequipa",
							"nombprov": "Castilla", "nombdist": "Aplao", "pobtotal": 7956}},
						{"geometry": null, "properties": {"ubigeo": "040413", "nombdep": "Arequipa",
							"nombprov": "Castilla", "nombdist": "Uraca", "pobtotal": 7236.0}},
						{"geometry": null, "properties": {"ubigeo": "040206", "nombdep": "Arequipa",
							"nombprov": "Camaná", "nombdist": "Ocoña", "pobtotal": null}}
					]}
					""", MediaType.APPLICATION_JSON));

		List<DistritoPoblado> distritos = adapter.distritosEnRadio(-16.19, -72.74, 20);

		assertThat(distritos).containsExactly(
				new DistritoPoblado(new Distrito("040401", "Arequipa", "Castilla", "Aplao"),
						new Poblacion("040401", 7_956, null)),
				new DistritoPoblado(new Distrito("040413", "Arequipa", "Castilla", "Uraca"),
						new Poblacion("040413", 7_236, null)));
		server.verify();
	}

	@Test
	void devuelveListaVaciaSinDistritosEnElRadio() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess("""
					{"type": "FeatureCollection", "totalFeatures": 0, "features": []}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.distritosEnRadio(-12, -85, 10)).isEmpty();
	}

	@Test
	void rechazaRadioInvalido() {
		assertThatThrownBy(() -> adapter.distritosEnRadio(-16.19, -72.74, 0))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> adapter.distritosEnRadio(-16.19, -72.74, Double.NaN))
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
	@SuppressWarnings("unchecked")
	void guardaLosDistritosConsultadosComoRespaldo() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess(DISTRITO_CON_LIMITES, MediaType.APPLICATION_JSON));

		adapter.distrito(-12.05, -77.03);

		ArgumentCaptor<List<DistritoEntity>> guardados = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(guardados.capture());
		DistritoEntity guardado = guardados.getValue().getFirst();
		assertThat(guardado.toDistrito()).isEqualTo(new Distrito("150101", "Lima", "Lima", "Lima"));
		assertThat(guardado.toPoblacion()).hasValueSatisfying(p -> assertThat(p.total()).isEqualTo(273_142));
		assertThat(guardado.getLimites()).isEqualTo(
				"[[[[-77.05,-12.07],[-77.0,-12.07],[-77.0,-12.02],[-77.05,-12.02],[-77.05,-12.07]]]]");
	}

	@Test
	void noGuardaRespaldoSinLimites() {
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess("""
					{"features": [{"geometry": null, "properties": {"ubigeo": "150101", "nombdep": "Lima",
						"nombprov": "Lima", "nombdist": "Lima", "pobtotal": 273142}}]}
					""", MediaType.APPLICATION_JSON));

		adapter.distrito(-12.05, -77.03);

		verify(repository, never()).saveAll(anyList());
	}

	@Test
	void respondeAunqueFalleElRespaldo() {
		when(repository.saveAll(anyList())).thenThrow(new DataAccessResourceFailureException("MySQL caído"));
		server.expect(requestTo(startsWith(URL_BASE + "/ows")))
			.andRespond(withSuccess(DISTRITO_CON_LIMITES, MediaType.APPLICATION_JSON));

		assertThat(adapter.distrito(-12.05, -77.03)).contains(new Distrito("150101", "Lima", "Lima", "Lima"));
	}

	@Test
	void calculaLaEnvolventeDeLosLimites() {
		Multipoligono limites = new Multipoligono(List.of(
				List.of(List.of(List.of(-77.05, -12.07), List.of(-77.0, -12.02), List.of(-77.05, -12.07))),
				List.of(List.of(List.of(-77.1, -12.0), List.of(-77.08, -11.9), List.of(-77.1, -12.0)))));

		assertThat(limites.envolvente()).isEqualTo(new Multipoligono.Envolvente(-77.1, -12.07, -77.0, -11.9));
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
