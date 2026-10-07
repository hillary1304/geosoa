package com.geosoa.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest(properties = "management.endpoint.health.show-details=always")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstadoFuentesTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CircuitBreakerRegistry circuitBreakers;

	@AfterEach
	void cerrarCircuitos() {
		circuitBreakers.getAllCircuitBreakers().forEach(cb -> cb.transitionToClosedState());
	}

	@Test
	void muestraElEstadoDeCadaFuente() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.circuitBreakers.details.igp.details.state").value("CLOSED"))
			.andExpect(jsonPath("$.components.circuitBreakers.details.senamhi.details.state").value("CLOSED"))
			.andExpect(jsonPath("$.components.circuitBreakers.details.inei.details.state").value("CLOSED"));
	}

	@Test
	void unaFuenteCaidaNoTumbaLaAplicacion() throws Exception {
		circuitBreakers.circuitBreaker("inei").transitionToOpenState();

		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components.circuitBreakers.details.inei.status").value("CIRCUIT_OPEN"))
			.andExpect(jsonPath("$.components.circuitBreakers.details.igp.status").value("UP"));
	}

}
