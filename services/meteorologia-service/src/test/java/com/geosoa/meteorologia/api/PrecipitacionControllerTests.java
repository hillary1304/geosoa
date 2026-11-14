package com.geosoa.meteorologia.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.geosoa.meteorologia.resilience.SenamhiResiliente;

@SpringBootTest
@AutoConfigureMockMvc
class PrecipitacionControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SenamhiResiliente senamhi;

	@Test
	void rechazaCoordenadasFueraDeRangoAntesDeConsultarLaFuente() throws Exception {
		mockMvc.perform(get("/internal/v1/precipitacion").param("latitud", "91").param("longitud", "-77"))
			.andExpect(status().isBadRequest());
	}

}
