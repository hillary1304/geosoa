package com.geosoa.consulta;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConsultaRouteTests {

	@Autowired
	private ProducerTemplate producerTemplate;

	@Test
	void arrancaCamelYProcesaUnaRutaDirecta() {
		String respuesta = producerTemplate.requestBody("direct:consulta-prueba", null, String.class);

		assertThat(respuesta).isEqualTo("Camel listo");
	}

}
