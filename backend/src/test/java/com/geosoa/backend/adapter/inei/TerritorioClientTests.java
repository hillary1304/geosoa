package com.geosoa.backend.adapter.inei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Origen;

class TerritorioClientTests {

	@Test
	void consultaElServicioPorPuntoYDeserializaSuContrato() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://territorio.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TerritorioClient client = new TerritorioClient(builder.build());
		server.expect(requestTo("http://territorio.test/internal/v1/distritos/por-punto?latitud=-12.05&longitud=-77.03"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("latitud", "-12.05"))
			.andRespond(withSuccess("""
				{"valor":{"ubigeo":"150101","departamento":"Lima","provincia":"Lima","nombre":"Lima"},
				 "procedencia":{"origen":"live","actualizadoEn":"2026-11-14T06:00:00Z"}}
				""", MediaType.APPLICATION_JSON));

		Dato<Optional<Distrito>> respuesta = client.distrito(-12.05, -77.03);

		assertThat(respuesta.valor()).contains(new Distrito("150101", "Lima", "Lima", "Lima"));
		assertThat(respuesta.procedencia().origen()).isEqualTo(Origen.LIVE);
		assertThat(respuesta.procedencia().actualizadoEn()).isEqualTo(Instant.parse("2026-11-14T06:00:00Z"));
		server.verify();
	}

}
