package com.geosoa.backend.api;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String error, String mensaje, String ruta,
		List<String> detalles) {

	public ApiError {
		detalles = detalles == null ? List.of() : List.copyOf(detalles);
	}

}
