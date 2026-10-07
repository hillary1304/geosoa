package com.geosoa.backend.api;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.geosoa.backend.shared.FuenteNoDisponibleException;
import com.geosoa.backend.shared.RecursoNoEncontradoException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ResponseEntity<ApiError> recursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
		return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> rutaNoEncontrada(NoResourceFoundException ex, HttpServletRequest request) {
		return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", request, List.of());
	}

	@ExceptionHandler(FuenteNoDisponibleException.class)
	ResponseEntity<ApiError> fuenteNoDisponible(FuenteNoDisponibleException ex, HttpServletRequest request) {
		log.warn("Fuente {} no disponible: {}", ex.getFuente(), ex.getMessage());
		return responder(HttpStatus.SERVICE_UNAVAILABLE, "La fuente " + ex.getFuente() + " no está disponible",
				request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> cuerpoInvalido(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<String> detalles = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.toList();
		return responder(HttpStatus.BAD_REQUEST, "Parámetros inválidos", request, detalles);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiError> parametrosInvalidos(HandlerMethodValidationException ex, HttpServletRequest request) {
		List<String> detalles = ex.getParameterValidationResults()
			.stream()
			.flatMap(resultado -> resultado.getResolvableErrors()
				.stream()
				.map(error -> resultado.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
			.toList();
		return responder(HttpStatus.BAD_REQUEST, "Parámetros inválidos", request, detalles);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro " + ex.getName(), request,
				List.of());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiError> parametroFaltante(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro " + ex.getParameterName(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> errorInesperado(Exception ex, HttpServletRequest request) {
		if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
			HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
			return responder(status, "Solicitud no válida", request, List.of());
		}
		log.error("Error inesperado en {}", request.getRequestURI(), ex);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, List.of());
	}

	private static ResponseEntity<ApiError> responder(HttpStatus status, String mensaje, HttpServletRequest request,
			List<String> detalles) {
		ApiError cuerpo = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), mensaje,
				request.getRequestURI(), detalles);
		return ResponseEntity.status(status).body(cuerpo);
	}

}
