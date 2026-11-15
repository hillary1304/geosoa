package com.geosoa.consulta.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.geosoa.consulta.soporte.ServicioSimulado;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Controlador → ProducerTemplate → rutas Camel → sismos-service simulado por HTTP.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SismosApiTests {

	private static final ServicioSimulado SISMOS = new ServicioSimulado();

	private static final String SISMO = """
			{"codigo":"2026-0512","fechaHora":"2026-10-01T13:15:00Z","latitud":-12.05,"longitud":-77.03,
			"profundidadKm":30,"magnitud":6.0,"intensidad":"IV","referencia":"10 km al O de Lima",
			"departamento":"LIMA","sentido":true}""";

	private static final String PROCEDENCIA = """
			{"origen":"live","actualizadoEn":"2026-11-14T05:00:00Z"}""";

	@DynamicPropertySource
	static void servicios(DynamicPropertyRegistry registro) {
		registro.add("geosoa.servicios.sismos-url", SISMOS::url);
		registro.add("geosoa.servicios.read-timeout", () -> "400ms");
	}

	@AfterAll
	static void cerrar() {
		SISMOS.close();
	}

	@LocalServerPort
	private int puerto;

	private final HttpClient cliente = HttpClient.newHttpClient();

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limpiar() {
		SISMOS.reiniciar();
	}

	@Test
	void listaConFormaPublicaYPropagaFiltrosYRequestId() throws Exception {
		SISMOS.responder("/internal/v1/sismos", 200, """
				{"elementos":[%s],"pagina":0,"tamano":1,"total":132,"procedencia":%s}""".formatted(SISMO, PROCEDENCIA));

		HttpResponse<String> respuesta = get(
				"/api/sismos?desde=2026-11-01&hasta=2026-11-14&magnitudMinima=4&departamento=LA%20LIBERTAD&tamano=1",
				"prueba-123");

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(respuesta.headers().firstValue("X-Request-Id")).hasValue("prueba-123");
		JsonNode cuerpo = json.readTree(respuesta.body());
		assertThat(cuerpo.has("elementos")).isFalse();
		assertThat(cuerpo.get("items")).hasSize(1);
		assertThat(cuerpo.get("hayMas").asBoolean()).isTrue();
		assertThat(cuerpo.get("total").asLong()).isEqualTo(132);
		assertThat(cuerpo.at("/procedencia/origen").asString()).isEqualTo("live");
		assertThat(cuerpo.at("/items/0/fechaHora").asString()).isEqualTo("2026-10-01T08:15:00-05:00");

		ServicioSimulado.Solicitud recibida = SISMOS.recibidas().getFirst();
		assertThat(recibida.requestId()).isEqualTo("prueba-123");
		assertThat(recibida.query()).contains("desde=2026-11-01", "hasta=2026-11-14", "magnitudMinima=4.0",
				"departamento=LA+LIBERTAD", "pagina=0", "tamano=1");
	}

	@Test
	void enLaUltimaPaginaNoHayMas() throws Exception {
		SISMOS.responder("/internal/v1/sismos", 200, """
				{"elementos":[%s],"pagina":2,"tamano":50,"total":101,"procedencia":%s}""".formatted(SISMO, PROCEDENCIA));

		JsonNode cuerpo = json.readTree(get("/api/sismos?pagina=2", null).body());

		assertThat(cuerpo.get("hayMas").asBoolean()).isFalse();
	}

	@Test
	void generaUnRequestIdSiNoLlegaUnoValido() throws Exception {
		SISMOS.responder("/internal/v1/sismos/indicadores", 200, """
				{"totalAlmacenado":10,"magnitudMaximaAlmacenada":7.1,"cobertura":"respaldo local"}""");

		HttpResponse<String> respuesta = get("/api/indicadores", "no!valido");

		String generado = respuesta.headers().firstValue("X-Request-Id").orElseThrow();
		assertThat(generado).matches("[0-9a-f-]{36}");
		assertThat(SISMOS.recibidas().getFirst().requestId()).isEqualTo(generado);
	}

	@Test
	void devuelveElUltimoSismoEnHoraDeLima() throws Exception {
		SISMOS.responder("/internal/v1/sismos/ultimo", 200, """
				{"valor":%s,"procedencia":%s}""".formatted(SISMO, PROCEDENCIA));

		JsonNode cuerpo = json.readTree(get("/api/sismos/ultimo", null).body());

		assertThat(cuerpo.at("/valor/codigo").asString()).isEqualTo("2026-0512");
		assertThat(cuerpo.at("/valor/fechaHora").asString()).isEqualTo("2026-10-01T08:15:00-05:00");
	}

	@Test
	void sinSismosElUltimoEsNullYNoUnError() throws Exception {
		SISMOS.responder("/internal/v1/sismos/ultimo", 200, """
				{"valor":null,"procedencia":%s}""".formatted(PROCEDENCIA));

		HttpResponse<String> respuesta = get("/api/sismos/ultimo", null);

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(json.readTree(respuesta.body()).get("valor").isNull()).isTrue();
	}

	@Test
	void devuelveLosIndicadoresConSuCobertura() throws Exception {
		SISMOS.responder("/internal/v1/sismos/indicadores", 200, """
				{"totalAlmacenado":10,"magnitudMaximaAlmacenada":7.1,"cobertura":"respaldo local"}""");

		JsonNode cuerpo = json.readTree(get("/api/indicadores", null).body());

		assertThat(cuerpo.get("totalAlmacenado").asLong()).isEqualTo(10);
		assertThat(cuerpo.get("cobertura").asString()).isEqualTo("respaldo local");
	}

	@Test
	void rechazaParametrosInvalidosSinLlamarASismos() throws Exception {
		HttpResponse<String> respuesta = get("/api/sismos?tamano=0&pagina=-1", null);

		assertThat(respuesta.statusCode()).isEqualTo(400);
		JsonNode cuerpo = json.readTree(respuesta.body());
		assertThat(cuerpo.get("ruta").asString()).isEqualTo("/api/sismos");
		assertThat(cuerpo.get("detalles")).hasSize(2);
		assertThat(get("/api/sismos?desde=ayer", null).statusCode()).isEqualTo(400);
		assertThat(SISMOS.recibidas()).isEmpty();
	}

	@Test
	void sismosCaidoResponde503() throws Exception {
		SISMOS.responder("/internal/v1/sismos/ultimo", 503, "{}");

		HttpResponse<String> respuesta = get("/api/sismos/ultimo", null);

		assertThat(respuesta.statusCode()).isEqualTo(503);
		assertThat(json.readTree(respuesta.body()).get("mensaje").asString())
			.isEqualTo("El servicio de sismos no está disponible");
	}

	@Test
	void unTimeoutDeSismosResponde503() throws Exception {
		SISMOS.demorar("/internal/v1/sismos/indicadores", 1500, "{}");

		assertThat(get("/api/indicadores", null).statusCode()).isEqualTo(503);
	}

	@Test
	void un400DeSismosSeTraduceA400() throws Exception {
		SISMOS.responder("/internal/v1/sismos", 400, "{}");

		assertThat(get("/api/sismos", null).statusCode()).isEqualTo(400);
	}

	private HttpResponse<String> get(String ruta, String requestId) throws Exception {
		HttpRequest.Builder solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
		if (requestId != null) {
			solicitud.header("X-Request-Id", requestId);
		}
		return cliente.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
	}

}
