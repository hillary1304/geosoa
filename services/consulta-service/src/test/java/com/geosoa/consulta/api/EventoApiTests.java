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

// Casos del orquestador del monolito, ahora sobre HTTP: controlador → Camel → tres servicios simulados.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventoApiTests {

	private static final ServicioSimulado SISMOS = new ServicioSimulado();

	private static final ServicioSimulado METEOROLOGIA = new ServicioSimulado();

	private static final ServicioSimulado TERRITORIO = new ServicioSimulado();

	private static final String DETALLE = "/internal/v1/sismos/2026-0512";

	private static final String PRECIPITACION = "/internal/v1/precipitacion";

	private static final String POR_PUNTO = "/internal/v1/distritos/por-punto";

	private static final String EN_RADIO = "/internal/v1/distritos/en-radio";

	private static final String POBLACION_LIMA = "/internal/v1/distritos/150101/poblacion";

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

	private static final String LIMA = """
			{"ubigeo":"150101","departamento":"LIMA","provincia":"LIMA","nombre":"LIMA"}""";

	private static final String CALLAO = """
			{"ubigeo":"070101","departamento":"CALLAO","provincia":"CALLAO","nombre":"CALLAO"}""";

	private static final String HAB_LIMA = """
			{"ubigeo":"150101","total":268352,"densidadHabKm2":12253.5}""";

	private static final String HAB_CALLAO = """
			{"ubigeo":"070101","total":451260,"densidadHabKm2":9894.1}""";

	private static final String LLUVIA = """
			{"capa":"ANUAL","minimoMm":10.0,"maximoMm":15.0}""";

	@DynamicPropertySource
	static void servicios(DynamicPropertyRegistry registro) {
		registro.add("geosoa.servicios.sismos-url", SISMOS::url);
		registro.add("geosoa.servicios.meteorologia-url", METEOROLOGIA::url);
		registro.add("geosoa.servicios.territorio-url", TERRITORIO::url);
		registro.add("geosoa.servicios.read-timeout", () -> "1s");
	}

	@AfterAll
	static void cerrar() {
		SISMOS.close();
		METEOROLOGIA.close();
		TERRITORIO.close();
	}

	@LocalServerPort
	private int puerto;

	private final HttpClient cliente = HttpClient.newHttpClient();

	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void limpiar() {
		SISMOS.reiniciar();
		METEOROLOGIA.reiniciar();
		TERRITORIO.reiniciar();
		SISMOS.responder(DETALLE, 200, dato(SISMO, EN_VIVO));
	}

	@Test
	void integraLasTresFuentes() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(LIMA, HAB_LIMA), poblado(CALLAO, HAB_CALLAO)), EN_VIVO));

		HttpResponse<String> respuesta = evento("prueba-evento");

		assertThat(respuesta.statusCode()).isEqualTo(200);
		JsonNode evento = json.readTree(respuesta.body());
		assertThat(evento.at("/sismo/fechaHora").asString()).isEqualTo("2026-10-01T08:15:00-05:00");
		assertThat(evento.at("/distrito/ubigeo").asString()).isEqualTo("150101");
		assertThat(evento.at("/precipitacion/maximoMm").asDouble()).isEqualTo(15.0);
		assertThat(evento.at("/poblacion/total").asLong()).isEqualTo(268352);
		assertThat(evento.get("radioAfectacionKm").asDouble()).isEqualTo(95.4);
		assertThat(evento.get("poblacionExpuesta").asLong()).isEqualTo(268352L + 451260L);
		assertThat(evento.get("fuentesFaltantes")).isEmpty();
		assertThat(evento.get("procedencias").propertyNames()).containsExactlyInAnyOrder("IGP", "SENAMHI", "INEI");

		assertThat(TERRITORIO.recibidas()).extracting(ServicioSimulado.Solicitud::ruta).doesNotContain(POBLACION_LIMA);
		assertThat(TERRITORIO.recibidas()).filteredOn(s -> s.ruta().equals(EN_RADIO))
			.singleElement()
			.satisfies(s -> assertThat(s.query()).isEqualTo("latitud=-12.05&longitud=-77.03&radioKm=95.4"));
		assertThat(SISMOS.recibidas().getFirst().requestId()).isEqualTo("prueba-evento");
		assertThat(METEOROLOGIA.recibidas().getFirst().requestId()).isEqualTo("prueba-evento");
		assertThat(TERRITORIO.recibidas()).allSatisfy(s -> assertThat(s.requestId()).isEqualTo("prueba-evento"));
	}

	@Test
	void consultaLaPoblacionDelEpicentroSiNoVieneEnElRadio() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(CALLAO, HAB_CALLAO)), EN_VIVO));
		TERRITORIO.responder(POBLACION_LIMA, 200, dato(HAB_LIMA, EN_VIVO));

		JsonNode evento = json.readTree(evento(null).body());

		assertThat(evento.at("/poblacion/total").asLong()).isEqualTo(268352);
		assertThat(evento.get("poblacionExpuesta").asLong()).isEqualTo(451260);
	}

	@Test
	void respondeParcialmenteSiMeteorologiaNoResponde() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 503, "{}");
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(LIMA, HAB_LIMA)), EN_VIVO));

		HttpResponse<String> respuesta = evento(null);

		assertThat(respuesta.statusCode()).isEqualTo(200);
		JsonNode evento = json.readTree(respuesta.body());
		assertThat(evento.get("precipitacion").isNull()).isTrue();
		assertThat(evento.at("/distrito/ubigeo").asString()).isEqualTo("150101");
		assertThat(evento.at("/poblacion/total").asLong()).isEqualTo(268352);
		assertThat(textos(evento.get("fuentesFaltantes"))).containsExactly("SENAMHI");
		assertThat(evento.get("procedencias").propertyNames()).containsExactlyInAnyOrder("IGP", "INEI");
		assertThat(METEOROLOGIA.recibidas()).as("consulta no reintenta").hasSize(1);
	}

	@Test
	void respondeParcialmenteSiTerritorioNoResponde() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 503, "{}");
		TERRITORIO.responder(EN_RADIO, 503, "{}");

		JsonNode evento = json.readTree(evento(null).body());

		assertThat(evento.at("/precipitacion/capa").asString()).isEqualTo("ANUAL");
		assertThat(evento.get("distrito").isNull()).isTrue();
		assertThat(evento.get("poblacion").isNull()).isTrue();
		assertThat(evento.get("poblacionExpuesta").isNull()).isTrue();
		assertThat(evento.get("radioAfectacionKm").asDouble()).isEqualTo(95.4);
		assertThat(textos(evento.get("fuentesFaltantes"))).containsExactly("INEI");
	}

	@Test
	void marcaAlIneiSiFallaLaConsultaDePoblacion() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 503, "{}");
		TERRITORIO.responder(POBLACION_LIMA, 503, "{}");

		JsonNode evento = json.readTree(evento(null).body());

		assertThat(evento.at("/distrito/ubigeo").asString()).isEqualTo("150101");
		assertThat(evento.get("poblacion").isNull()).isTrue();
		assertThat(textos(evento.get("fuentesFaltantes"))).containsExactly("INEI");
	}

	@Test
	void noMarcaFaltantesCuandoElEpicentroEstaEnElMar() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato("null", EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato("null", EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(CALLAO, HAB_CALLAO)), EN_VIVO));

		JsonNode evento = json.readTree(evento(null).body());

		assertThat(evento.get("distrito").isNull()).isTrue();
		assertThat(evento.get("precipitacion").isNull()).isTrue();
		assertThat(evento.get("poblacion").isNull()).isTrue();
		assertThat(evento.get("poblacionExpuesta").asLong()).isEqualTo(451260);
		assertThat(evento.get("fuentesFaltantes")).isEmpty();
	}

	@Test
	void unTimeoutDeMeteorologiaDejaElEventoParcial() throws Exception {
		METEOROLOGIA.demorar(PRECIPITACION, 1500, dato(LLUVIA, EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(LIMA, HAB_LIMA)), EN_VIVO));

		HttpResponse<String> respuesta = evento(null);

		assertThat(respuesta.statusCode()).isEqualTo(200);
		JsonNode evento = json.readTree(respuesta.body());
		assertThat(evento.get("precipitacion").isNull()).isTrue();
		assertThat(textos(evento.get("fuentesFaltantes"))).containsExactly("SENAMHI");
		assertThat(evento.get("procedencias").propertyNames()).containsExactlyInAnyOrder("IGP", "INEI");
	}

	@Test
	void unTimeoutDeTerritorioDejaElEventoParcial() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_VIVO));
		TERRITORIO.demorar(POR_PUNTO, 1500, dato(LIMA, EN_VIVO));
		TERRITORIO.demorar(EN_RADIO, 1500, dato(enRadio(poblado(LIMA, HAB_LIMA)), EN_VIVO));

		HttpResponse<String> respuesta = evento(null);

		assertThat(respuesta.statusCode()).isEqualTo(200);
		JsonNode evento = json.readTree(respuesta.body());
		assertThat(evento.get("distrito").isNull()).isTrue();
		assertThat(evento.get("poblacionExpuesta").isNull()).isTrue();
		assertThat(evento.at("/precipitacion/capa").asString()).isEqualTo("ANUAL");
		assertThat(textos(evento.get("fuentesFaltantes"))).containsExactly("INEI");
		assertThat(evento.get("procedencias").propertyNames()).containsExactlyInAnyOrder("IGP", "SENAMHI");
	}

	@Test
	void sinCoberturaDelSenamhiNoMarcaLaFuenteComoCaida() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato("null", EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(LIMA, HAB_LIMA)), EN_VIVO));

		JsonNode evento = json.readTree(evento(null).body());

		assertThat(evento.get("precipitacion").isNull()).isTrue();
		assertThat(evento.get("fuentesFaltantes")).isEmpty();
		assertThat(evento.at("/procedencias/SENAMHI/origen").asString()).isEqualTo("live");
	}

	@Test
	void laPoblacionDesconocidaQuedaNullYNoCero() throws Exception {
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_VIVO));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(LIMA, "null"), poblado(CALLAO, HAB_CALLAO)), EN_VIVO));
		TERRITORIO.responder(POBLACION_LIMA, 200, dato("null", EN_VIVO));

		JsonNode evento = json.readTree(evento(null).body());

		assertThat(evento.get("poblacion").isNull()).isTrue();
		assertThat(evento.get("poblacionExpuesta").isNull()).isTrue();
		assertThat(evento.get("fuentesFaltantes")).isEmpty();
		assertThat(TERRITORIO.recibidas()).extracting(ServicioSimulado.Solicitud::ruta).contains(POBLACION_LIMA);
	}

	@Test
	void consultaMeteorologiaYTerritorioEnParalelo() throws Exception {
		METEOROLOGIA.demorar(PRECIPITACION, 600, dato(LLUVIA, EN_VIVO));
		TERRITORIO.demorar(POR_PUNTO, 600, dato(LIMA, EN_VIVO));
		TERRITORIO.demorar(EN_RADIO, 600, dato(enRadio(poblado(LIMA, HAB_LIMA)), EN_VIVO));

		long inicio = System.nanoTime();
		JsonNode evento = json.readTree(evento(null).body());
		long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;

		assertThat(evento.get("fuentesFaltantes")).isEmpty();
		assertThat(transcurridoMs).isLessThan(1500);
	}

	@Test
	void informaLaProcedenciaMenosFrescaDeCadaFuente() throws Exception {
		SISMOS.responder(DETALLE, 200, dato(SISMO, ALMACENADA));
		METEOROLOGIA.responder(PRECIPITACION, 200, dato(LLUVIA, EN_CACHE));
		TERRITORIO.responder(POR_PUNTO, 200, dato(LIMA, EN_VIVO));
		TERRITORIO.responder(EN_RADIO, 200, dato(enRadio(poblado(LIMA, HAB_LIMA)), ALMACENADA));

		JsonNode procedencias = json.readTree(evento(null).body()).get("procedencias");

		assertThat(procedencias.at("/IGP/origen").asString()).isEqualTo("db");
		assertThat(procedencias.at("/SENAMHI/origen").asString()).isEqualTo("cache");
		assertThat(procedencias.at("/INEI/origen").asString()).isEqualTo("db");
		assertThat(procedencias.at("/INEI/actualizadoEn").asString()).isEqualTo("2026-10-06T12:00:00Z");
	}

	@Test
	void sismoInexistenteResponde404SinConsultarElContexto() throws Exception {
		SISMOS.responder(DETALLE, 404, "{}");

		HttpResponse<String> respuesta = evento(null);

		assertThat(respuesta.statusCode()).isEqualTo(404);
		assertThat(json.readTree(respuesta.body()).get("mensaje").asString()).isEqualTo("Sismo no encontrado");
		assertThat(METEOROLOGIA.recibidas()).isEmpty();
		assertThat(TERRITORIO.recibidas()).isEmpty();
	}

	@Test
	void sismosCaidoResponde503() throws Exception {
		SISMOS.responder(DETALLE, 503, "{}");

		assertThat(evento(null).statusCode()).isEqualTo(503);
		assertThat(METEOROLOGIA.recibidas()).isEmpty();
	}

	@Test
	void codigoInvalidoResponde400() throws Exception {
		HttpResponse<String> respuesta = get("/api/sismos/2026%240512/evento");

		assertThat(respuesta.statusCode()).isEqualTo(400);
		assertThat(SISMOS.recibidas()).isEmpty();
	}

	private HttpResponse<String> evento(String requestId) throws Exception {
		HttpRequest.Builder solicitud = HttpRequest
			.newBuilder(URI.create("http://localhost:" + puerto + "/api/sismos/2026-0512/evento"));
		if (requestId != null) {
			solicitud.header("X-Request-Id", requestId);
		}
		return cliente.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String ruta) throws Exception {
		return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static String dato(String valor, String procedencia) {
		return "{\"valor\":" + valor + ",\"procedencia\":" + procedencia + "}";
	}

	private static String poblado(String distrito, String poblacion) {
		return "{\"distrito\":" + distrito + ",\"poblacion\":" + poblacion + "}";
	}

	private static String enRadio(String... poblados) {
		return "[" + String.join(",", poblados) + "]";
	}

	private static java.util.List<String> textos(JsonNode arreglo) {
		return arreglo.valueStream().map(JsonNode::asString).toList();
	}

}
