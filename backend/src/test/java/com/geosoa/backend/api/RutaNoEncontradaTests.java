package com.geosoa.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RutaNoEncontradaTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rutaInexistenteDevuelveFormatoUniforme() throws Exception {
		mockMvc.perform(get("/api/inexistente"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.mensaje").value("La ruta solicitada no existe"))
			.andExpect(jsonPath("$.ruta").value("/api/inexistente"));
	}

	@Test
	void metodoNoPermitidoConservaSuEstado() throws Exception {
		mockMvc.perform(post("/actuator/health"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.status").value(405));
	}

}
