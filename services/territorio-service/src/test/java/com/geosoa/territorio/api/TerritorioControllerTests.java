package com.geosoa.territorio.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.geosoa.territorio.resilience.IneiResiliente;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TerritorioControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IneiResiliente inei;

	@Test
	void rechazaCoordenadasInvalidas() throws Exception {
		mockMvc.perform(get("/internal/v1/distritos/por-punto").param("latitud", "91").param("longitud", "-77"))
			.andExpect(status().isBadRequest());
	}

}
