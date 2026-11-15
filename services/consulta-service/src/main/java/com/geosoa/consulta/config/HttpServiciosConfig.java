package com.geosoa.consulta.config;

import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HttpServiciosConfig {

	// Plazos de las llamadas a los servicios de dominio; consulta no agrega reintentos propios.
	@Bean
	ComponentCustomizer plazosHttp(ServiciosProperties servicios) {
		return ComponentCustomizer.forType(HttpComponent.class, http -> {
			http.setConnectTimeout((int) servicios.connectTimeout().toMillis());
			http.setResponseTimeout((int) servicios.readTimeout().toMillis());
			http.setSoTimeout((int) servicios.readTimeout().toMillis());
		});
	}

}
