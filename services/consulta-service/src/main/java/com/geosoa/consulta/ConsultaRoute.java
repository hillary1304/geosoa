package com.geosoa.consulta;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ConsultaRoute extends RouteBuilder {

	@Override
	public void configure() {
		from("direct:consulta-prueba")
			.routeId("consulta-prueba")
			.setBody(constant("Camel listo"));
	}

}
