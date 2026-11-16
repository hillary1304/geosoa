package com.geosoa.consulta.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.geosoa.consulta.soporte.ServicioSimulado;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Con plazo total de 2 s y llamadas de hasta 1.5 s, las consultas paralelas solo esperan 0.5 s.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlazoEventoTests {

	private static final ServicioSimulado SISMOS = new ServicioSimulado();

	private static final ServicioSimulado METEOROLOGIA = new ServicioSimulado();

	private static final ServicioSimulado TERRITORIO = new ServicioSimulado();

	@DynamicPropertySource
	static void servicios(DynamicPropertyRegistry registro) {
		registro.add("geosoa.servicios.sismos-url", SISMOS::url);
		registro.add("geosoa.servicios.meteorologia-url", METEOROLOGIA::url);
		registro.add("geosoa.servicios.territorio-url", TERRITORIO::url);
		registro.add("geosoa.servicios.connect-timeout", () -> "500ms");
		registro.add("geosoa.servicios.read-timeout", () -> "1s");
		registro.add("geosoa.servicios.plazo-evento", () -> "2s");
	}

	@AfterAll
	static void cerrar() {
		SISMOS.close();
		METEOROLOGIA.close();
		TERRITORIO.close();
	}

	@LocalServerPort
	private int puerto;

	@Test
	void respondeParcialAlVencerElPlazoTotal() throws Exception {
		SISMOS.responder("/internal/v1/sismos/2026-0512", 200, """
				{"valor":{"codigo":"2026-0512","fechaHora":"2026-10-01T13:15:00Z","latitud":-12.05,
				"longitud":-77.03,"profundidadKm":30,"magnitud":6.0,"intensidad":"IV","referencia":"Lima",
				"departamento":"LIMA","sentido":true},
				"procedencia":{"origen":"live","actualizadoEn":"2026-10-07T15:00:00Z"}}""");
		String vacio = """
				{"valor":null,"procedencia":{"origen":"live","actualizadoEn":"2026-10-07T15:00:00Z"}}""";
		METEOROLOGIA.demorar("/internal/v1/precipitacion", 900, vacio);
		TERRITORIO.demorar("/internal/v1/distritos/por-punto", 900, vacio);
		TERRITORIO.demorar("/internal/v1/distritos/en-radio", 900, vacio);

		long inicio = System.nanoTime();
		HttpResponse<String> respuesta = HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/sismos/2026-0512/evento"))
				.build(), HttpResponse.BodyHandlers.ofString());
		long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;

		assertThat(respuesta.statusCode()).isEqualTo(200);
		JsonNode evento = JsonMapper.builder().build().readTree(respuesta.body());
		assertThat(evento.get("fuentesFaltantes").valueStream().map(JsonNode::asString))
			.containsExactlyInAnyOrder("SENAMHI", "INEI");
		assertThat(evento.at("/sismo/codigo").asString()).isEqualTo("2026-0512");
		assertThat(transcurridoMs).isLessThan(900);
	}

}
