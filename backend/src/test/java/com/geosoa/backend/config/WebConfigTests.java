package com.geosoa.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebConfigTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsConfiguredOrigin() throws Exception {
		mockMvc.perform(options("/api/sismos")
				.header(HttpHeaders.ORIGIN, "http://localhost:5500")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
	}

	@Test
	void rejectsUnknownOrigin() throws Exception {
		mockMvc.perform(options("/api/sismos")
				.header(HttpHeaders.ORIGIN, "http://otro-sitio.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden());
	}

	@Test
	void rejectsNonGetMethods() throws Exception {
		mockMvc.perform(options("/api/sismos")
				.header(HttpHeaders.ORIGIN, "http://localhost:5500")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
			.andExpect(status().isForbidden());
	}

}
