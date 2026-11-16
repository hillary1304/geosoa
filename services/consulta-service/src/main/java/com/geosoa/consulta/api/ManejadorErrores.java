package com.geosoa.consulta.api;

import java.time.Instant;
import java.util.List;

import org.apache.camel.CamelExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.geosoa.consulta.ruta.RecursoNoEncontradoException;
import com.geosoa.consulta.ruta.ServicioNoDisponibleException;
import com.geosoa.consulta.ruta.SolicitudRechazadaException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
class ManejadorErrores {

	private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

	@ExceptionHandler(SolicitudInvalidaException.class)
	ResponseEntity<ErrorApi> invalida(SolicitudInvalidaException ex, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.detalles());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorApi> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Parámetros inválidos", request,
				List.of(ex.getName() + ": valor con formato inválido"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorApi> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", request, List.of());
	}

	@ExceptionHandler(CamelExecutionException.class)
	ResponseEntity<ErrorApi> enRuta(CamelExecutionException ex, HttpServletRequest request) {
		Throwable causa = ex.getCause();
		if (causa instanceof ServicioNoDisponibleException noDisponible) {
			log.warn("{}: {}", noDisponible.getMessage(), String.valueOf(noDisponible.getCause()));
			return error(HttpStatus.SERVICE_UNAVAILABLE, noDisponible.getMessage(), request, List.of());
		}
		if (causa instanceof RecursoNoEncontradoException noEncontrado) {
			return error(HttpStatus.NOT_FOUND, noEncontrado.getMessage(), request, List.of());
		}
		if (causa instanceof SolicitudRechazadaException rechazada) {
			return error(HttpStatus.BAD_REQUEST, rechazada.getMessage(), request, List.of());
		}
		log.error("Error no controlado en la ruta", ex);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", request, List.of());
	}

	private static ResponseEntity<ErrorApi> error(HttpStatus estado, String mensaje, HttpServletRequest request,
			List<String> detalles) {
		return ResponseEntity.status(estado)
			.body(new ErrorApi(Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje,
					request.getRequestURI(), detalles));
	}

}
