package com.geosoa.meteorologia.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.geosoa.meteorologia.shared.FuenteNoDisponibleException;

@RestControllerAdvice
public class ErroApiHandler {

	@ExceptionHandler(FuenteNoDisponibleException.class)
	ResponseEntity<Map<String, String>> fuenteNoDisponible(FuenteNoDisponibleException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(Map.of("codigo", "FUENTE_NO_DISPONIBLE", "mensaje", ex.getMessage()));
	}

}
