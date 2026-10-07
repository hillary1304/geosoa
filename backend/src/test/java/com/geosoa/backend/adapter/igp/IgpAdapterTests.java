package com.geosoa.backend.adapter.igp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.hamcrest.Matchers.startsWith;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

class IgpAdapterTests {

	private static final String URL_BASE = "https://igp.test/monitoreocensis";

	private MockRestServiceServer server;

	private IgpAdapter adapter;

	@BeforeEach
	void configurar() {
		RestClient.Builder builder = RestClient.builder().baseUrl(URL_BASE);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new IgpAdapter(builder.build());
	}

	@Test
	void obtieneElUltimoSismoEnHoraDeLima() {
		server.expect(requestTo(startsWith(URL_BASE + "/UltimoSismo/MapServer/0/query")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("where", "1%3D1"))
			.andExpect(queryParam("f", "json"))
			.andRespond(withSuccess("""
					{"features": [{"attributes": {
						"objectid": 1, "magnitud": 3.4, "profundidad": 15.0,
						"fecha_local": "07/10/2026", "hora_local": "15:51:30",
						"latitud": -11.49, "longitud": -74.59,
						"intensidad": "II San Martín de Pangoa",
						"referencia": "13 km al SO de San Martín de Pangoa, Satipo - Junín",
						"epicentro": "Continente", "ubicacion": "Continente"
					}}]}
					""", MediaType.APPLICATION_JSON));

		Sismo sismo = adapter.ultimoSismo().orElseThrow();

		assertThat(sismo.fechaHora()).isEqualTo(ZonedDateTime.parse("2026-10-07T15:51:30-05:00[America/Lima]"));
		assertThat(sismo.magnitud()).isEqualTo(3.4);
		assertThat(sismo.profundidadKm()).isEqualTo(15.0);
		assertThat(sismo.latitud()).isEqualTo(-11.49);
		assertThat(sismo.longitud()).isEqualTo(-74.59);
		assertThat(sismo.departamento()).isEqualTo("JUNÍN");
		assertThat(sismo.sentido()).isTrue();
		server.verify();
	}

	@Test
	void descartaElUltimoSismoConCamposNulos() {
		server.expect(requestTo(startsWith(URL_BASE + "/UltimoSismo/MapServer/0/query")))
			.andRespond(withSuccess("""
					{"features": [{"attributes": {"magnitud": null, "profundidad": 15.0,
						"fecha_local": "07/10/2026", "hora_local": "15:51:30",
						"latitud": -11.49, "longitud": -74.59}}]}
					""", MediaType.APPLICATION_JSON));

		assertThat(adapter.ultimoSismo()).isEmpty();
	}

	@Test
	void sinIntensidadElSismoNoFueSentido() {
		server.expect(requestTo(startsWith(URL_BASE + "/UltimoSismo/MapServer/0/query")))
			.andRespond(withSuccess("""
					{"features": [{"attributes": {"magnitud": 4.2, "profundidad": 95.0,
						"fecha_local": "05/10/2026", "hora_local": "05:32:49",
						"latitud": -16.19, "longitud": -72.74, "intensidad": "",
						"referencia": "29 km al SO de Aplao, Castilla - Arequipa"}}]}
					""", MediaType.APPLICATION_JSON));

		Sismo sismo = adapter.ultimoSismo().orElseThrow();

		assertThat(sismo.intensidad()).isNull();
		assertThat(sismo.sentido()).isFalse();
		assertThat(sismo.departamento()).isEqualTo("AREQUIPA");
	}

	@Test
	void informaFuenteNoDisponibleAnteErrorHttp() {
		server.expect(requestTo(startsWith(URL_BASE))).andRespond(withServerError());

		assertThatThrownBy(adapter::ultimoSismo)
			.isInstanceOfSatisfying(FuenteNoDisponibleException.class,
					ex -> assertThat(ex.getFuente()).isEqualTo(Fuente.IGP));
	}

	@Test
	void informaFuenteNoDisponibleAnteErrorEnElCuerpo() {
		server.expect(requestTo(startsWith(URL_BASE)))
			.andRespond(withSuccess("""
					{"error": {"code": 400, "message": "Unable to complete operation.", "details": []}}
					""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(adapter::ultimoSismo)
			.isInstanceOf(FuenteNoDisponibleException.class)
			.hasMessageContaining("Unable to complete operation.");
	}

	@Test
	void obtieneElDepartamentoDeLaReferencia() {
		assertThat(IgpAdapter.departamentoDeReferencia("28 km al S de Ica, Ica - Ica")).isEqualTo("ICA");
		assertThat(IgpAdapter.departamentoDeReferencia("120 km al O de Pisco")).isNull();
		assertThat(IgpAdapter.departamentoDeReferencia(null)).isNull();
	}

}
