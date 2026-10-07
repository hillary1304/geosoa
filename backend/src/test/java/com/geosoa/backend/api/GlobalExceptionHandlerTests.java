package com.geosoa.backend.api;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.geosoa.backend.shared.RecursoNoEncontradoException;

import jakarta.validation.constraints.Min;

class GlobalExceptionHandlerTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new PruebaController())
			.setValidator(validator)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void recursoNoEncontradoDevuelve404() throws Exception {
		mockMvc.perform(get("/prueba/sismos/2026-999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.mensaje").value("No existe el sismo 2026-999"))
			.andExpect(jsonPath("$.ruta").value("/prueba/sismos/2026-999"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void fuenteNoDisponibleDevuelve503() throws Exception {
		mockMvc.perform(get("/prueba/inei"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.mensaje").value("La fuente INEI no está disponible"));
	}

	@Test
	void parametroConTipoInvalidoDevuelve400() throws Exception {
		mockMvc.perform(get("/prueba/magnitud").param("minima", "alta"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensaje").value("Valor inválido para el parámetro minima"));
	}

	@Test
	void parametroFueraDeRangoDevuelve400ConDetalles() throws Exception {
		mockMvc.perform(get("/prueba/magnitud").param("minima", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensaje").value("Parámetros inválidos"))
			.andExpect(jsonPath("$.detalles[0]").value(startsWith("minima: ")));
	}

	@Test
	void parametroFaltanteDevuelve400() throws Exception {
		mockMvc.perform(get("/prueba/magnitud"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensaje").value("Falta el parámetro minima"));
	}

	@Test
	void errorInesperadoDevuelve500SinExponerDetalles() throws Exception {
		mockMvc.perform(get("/prueba/fallo"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
	}

	@RestController
	static class PruebaController {

		@GetMapping("/prueba/sismos/{codigo}")
		String sismo(@PathVariable String codigo) {
			throw new RecursoNoEncontradoException("No existe el sismo " + codigo);
		}

		@GetMapping("/prueba/inei")
		String inei() {
			throw new FuenteNoDisponibleException(Fuente.INEI, "HTTP 503", null);
		}

		@GetMapping("/prueba/magnitud")
		double magnitud(@RequestParam @Min(0) double minima) {
			return minima;
		}

		@GetMapping("/prueba/fallo")
		String fallo() {
			throw new IllegalStateException("detalle interno");
		}

	}

}
