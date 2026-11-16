package com.geosoa.consulta.ruta;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LecturaJson {

	private final JsonMapper json;

	public LecturaJson(JsonMapper json) {
		this.json = json;
	}

	public <T> T leer(Exchange exchange, TypeReference<T> tipo) {
		byte[] cuerpo = exchange.getMessage().getBody(byte[].class);
		return json.readValue(cuerpo, tipo);
	}

}
