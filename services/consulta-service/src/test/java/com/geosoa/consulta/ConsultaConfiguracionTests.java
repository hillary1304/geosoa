package com.geosoa.consulta;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.geosoa.consulta.config.ServiciosProperties;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsultaConfiguracionTests {

	@Autowired
	private ServiciosProperties servicios;

	@LocalServerPort
	private int puerto;

	@Test
	void cargaLasUrlsYPlazosPorDefecto() {
		assertThat(servicios.sismosUrl()).isEqualTo(URI.create("http://localhost:8081"));
		assertThat(servicios.meteorologiaUrl()).isEqualTo(URI.create("http://localhost:8082"));
		assertThat(servicios.territorioUrl()).isEqualTo(URI.create("http://localhost:8083"));
		assertThat(servicios.connectTimeout()).isEqualTo(Duration.ofMillis(500));
		assertThat(servicios.readTimeout()).isEqualTo(Duration.ofSeconds(3));
		assertThat(servicios.plazoEvento()).isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	void exponeElHealthcheck() throws Exception {
		var cliente = HttpClient.newHttpClient();
		var solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/actuator/health")).build();
		var respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(respuesta.body()).contains("\"status\":\"UP\"");
	}

}
