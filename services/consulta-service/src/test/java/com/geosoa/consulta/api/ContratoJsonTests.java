package com.geosoa.consulta.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.geosoa.consulta.soporte.ServicioSimulado;

import tools.jackson.databind.json.JsonMapper;

// La salida pública debe coincidir exactamente con los fixtures: nombres, enums, fechas y procedencias.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoJsonTests {

	private static final ServicioSimulado SISMOS = new ServicioSimulado();

	private static final ServicioSimulado METEOROLOGIA = new ServicioSimulado();

	private static final ServicioSimulado TERRITORIO = new ServicioSimulado();

	private static final String EN_VIVO = """
			{"origen":"live","actualizadoEn":"2026-10-07T15:00:00Z"}""";

	private static final String ALMACENADA = """
			{"origen":"db","actualizadoEn":"2026-10-06T12:00:00Z"}""";

	private static final String EN_CACHE = """
			{"origen":"cache","actualizadoEn":"2026-10-07T15:00:00Z"}""";

	private static final String SISMO = """
			{"codigo":"2026-0512","fechaHora":"2026-10-01T13:15:00Z","latitud":-12.05,"longitud":-77.03,
			"profundidadKm":30,"magnitud":6.0,"intensidad":"IV","referencia":"10 km al O de Lima",
			"departamento":"LIMA","sentido":true}""";

	private static final String EN_RADIO = """
			[{"distrito":{"ubigeo":"150101","departamento":"LIMA","provincia":"LIMA","nombre":"LIMA"},
			"poblacion":{"ubigeo":"150101","total":268352,"densidadHabKm2":12253.5}},
			{"distrito":{"ubigeo":"070101","departamento":"CALLAO","provincia":"CALLAO","nombre":"CALLAO"},
			"poblacion":{"ubigeo":"070101","total":451260,"densidadHabKm2":9894.1}}]""";

	@DynamicPropertySource
	static void servicios(DynamicPropertyRegistry registro) {
		registro.add("geosoa.servicios.sismos-url", SISMOS::url);
		registro.add("geosoa.servicios.meteorologia-url", METEOROLOGIA::url);
		registro.add("geosoa.servicios.territorio-url", TERRITORIO::url);
	}

	@AfterAll
	static void cerrar() {
		SISMOS.close();
		METEOROLOGIA.close();
		TERRITORIO.close();
	}

	@LocalServerPort
	private int puerto;

	@BeforeEach
	void limpiar() {
		SISMOS.reiniciar();
		METEOROLOGIA.reiniciar();
		TERRITORIO.reiniciar();
	}

	@Test
	void listaDeSismos() throws Exception {
		SISMOS.responder("/internal/v1/sismos", 200, """
				{"elementos":[%s],"pagina":0,"tamano":1,"total":132,"procedencia":%s}""".formatted(SISMO, EN_VIVO));

		coincide("lista-sismos.json", get("/api/sismos?tamano=1"), 200);
	}

	@Test
	void ultimoSismo() throws Exception {
		SISMOS.responder("/internal/v1/sismos/ultimo", 200, dato(SISMO, ALMACENADA));

		coincide("ultimo-sismo.json", get("/api/sismos/ultimo"), 200);
	}

	@Test
	void indicadores() throws Exception {
		SISMOS.responder("/internal/v1/sismos/indicadores", 200, """
				{"totalAlmacenado":1250,"magnitudMaximaAlmacenada":7.1,
				"cobertura":"respaldo local: alcance dependiente de la sincronización y migración, no equivale al histórico total"}""");

		coincide("indicadores.json", get("/api/indicadores"), 200);
	}

	@Test
	void eventoCompleto() throws Exception {
		contextoDelEvento();
		METEOROLOGIA.responder("/internal/v1/precipitacion", 200,
				dato("{\"capa\":\"ANUAL\",\"minimoMm\":10.0,\"maximoMm\":15.0}", EN_CACHE));

		coincide("evento-completo.json", get("/api/sismos/2026-0512/evento"), 200);
	}

	@Test
	void eventoParcial() throws Exception {
		contextoDelEvento();
		METEOROLOGIA.responder("/internal/v1/precipitacion", 503, "{}");

		coincide("evento-parcial.json", get("/api/sismos/2026-0512/evento"), 200);
	}

	@Test
	void errorDeValidacion() throws Exception {
		coincideError("error-400.json", get("/api/sismos?pagina=-1&tamano=0"), 400);
	}

	@Test
	void errorDeSismoInexistente() throws Exception {
		SISMOS.responder("/internal/v1/sismos/2026-9999", 404, "{}");

		coincideError("error-404.json", get("/api/sismos/2026-9999/evento"), 404);
	}

	@Test
	void errorDeSismosCaido() throws Exception {
		SISMOS.responder("/internal/v1/sismos/ultimo", 503, "{}");

		coincideError("error-503.json", get("/api/sismos/ultimo"), 503);
	}

	private void contextoDelEvento() {
		SISMOS.responder("/internal/v1/sismos/2026-0512", 200, dato(SISMO, ALMACENADA));
		TERRITORIO.responder("/internal/v1/distritos/por-punto", 200,
				dato("{\"ubigeo\":\"150101\",\"departamento\":\"LIMA\",\"provincia\":\"LIMA\",\"nombre\":\"LIMA\"}", EN_VIVO));
		TERRITORIO.responder("/internal/v1/distritos/en-radio", 200, dato(EN_RADIO, EN_VIVO));
	}

	private static void coincide(String fixture, HttpResponse<String> respuesta, int estado) throws Exception {
		assertThat(respuesta.statusCode()).isEqualTo(estado);
		JSONAssert.assertEquals(leer(fixture), respuesta.body(), JSONCompareMode.STRICT);
	}

	// El timestamp cambia en cada respuesta: se compara el resto y se valida que sea un instante ISO-8601.
	private static void coincideError(String fixture, HttpResponse<String> respuesta, int estado) throws Exception {
		assertThat(respuesta.statusCode()).isEqualTo(estado);
		JSONAssert.assertEquals(leer(fixture), respuesta.body(), JSONCompareMode.STRICT_ORDER);
		String timestamp = JsonMapper.builder().build().readTree(respuesta.body()).get("timestamp").asString();
		assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");
	}

	private static String leer(String fixture) throws Exception {
		try (InputStream entrada = ContratoJsonTests.class.getResourceAsStream("/fixtures/" + fixture)) {
			return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private HttpResponse<String> get(String ruta) throws Exception {
		return HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).build(),
					HttpResponse.BodyHandlers.ofString());
	}

	private static String dato(String valor, String procedencia) {
		return "{\"valor\":" + valor + ",\"procedencia\":" + procedencia + "}";
	}

}
