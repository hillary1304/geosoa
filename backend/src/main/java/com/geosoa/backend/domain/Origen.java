package com.geosoa.backend.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

// Ordenado de más a menos fresco.
public enum Origen {

	LIVE, CACHE, DB;

	@JsonValue
	public String valor() {
		return name().toLowerCase(Locale.ROOT);
	}

}
