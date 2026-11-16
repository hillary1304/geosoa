package com.geosoa.consulta.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.ProducerTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosoa.consulta.domain.Dato;
import com.geosoa.consulta.domain.EventoIntegrado;
import com.geosoa.consulta.domain.Sismo;
import com.geosoa.consulta.evento.EventoRoute;
import com.geosoa.consulta.ruta.LlamadasServiciosRoute;
import com.geosoa.consulta.ruta.SismosRoute;

@RestController
@RequestMapping("/api")
class SismosController {

	private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9_-]{1,40}");

	private final ProducerTemplate producer;

	SismosController(ProducerTemplate producer) {
		this.producer = producer;
	}

	@GetMapping("/sismos")
	ListaSismos listar(@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta,
			@RequestParam(required = false) Double magnitudMinima,
			@RequestParam(required = false) String departamento,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "50") int tamano,
			@RequestAttribute(RequestIdFilter.ATRIBUTO) String requestId) {
		List<String> detalles = new ArrayList<>();
		if (pagina < 0) {
			detalles.add("pagina: debe ser mayor o igual a 0");
		}
		if (tamano < 1 || tamano > 2000) {
			detalles.add("tamano: debe estar entre 1 y 2000");
		}
		if (desde != null && hasta != null && hasta.isBefore(desde)) {
			detalles.add("hasta: no puede ser anterior a desde");
		}
		if (magnitudMinima != null && (!Double.isFinite(magnitudMinima) || magnitudMinima < 0)) {
			detalles.add("magnitudMinima: debe ser un número mayor o igual a 0");
		}
		if (!detalles.isEmpty()) {
			throw new SolicitudInvalidaException(detalles);
		}
		Map<String, Object> encabezados = encabezados(requestId);
		encabezados.put("desde", desde);
		encabezados.put("hasta", hasta);
		encabezados.put("magnitudMinima", magnitudMinima);
		encabezados.put("departamento", departamento == null || departamento.isBlank() ? null : departamento.trim());
		encabezados.put("pagina", pagina);
		encabezados.put("tamano", tamano);
		return producer.requestBodyAndHeaders(SismosRoute.LISTAR, null, encabezados, ListaSismos.class);
	}

	@GetMapping("/sismos/ultimo")
	@SuppressWarnings("unchecked")
	Dato<Sismo> ultimo(@RequestAttribute(RequestIdFilter.ATRIBUTO) String requestId) {
		return producer.requestBodyAndHeaders(SismosRoute.ULTIMO, null, encabezados(requestId), Dato.class);
	}

	@GetMapping("/sismos/{codigo}/evento")
	EventoIntegrado evento(@PathVariable String codigo, @RequestAttribute(RequestIdFilter.ATRIBUTO) String requestId) {
		if (!CODIGO.matcher(codigo).matches()) {
			throw new SolicitudInvalidaException(List.of("codigo: solo letras, números, guion o guion bajo (máx. 40)"));
		}
		Map<String, Object> encabezados = encabezados(requestId);
		encabezados.put(EventoRoute.CODIGO, codigo);
		return producer.requestBodyAndHeaders(EventoRoute.CONSULTAR, null, encabezados, EventoIntegrado.class);
	}

	@GetMapping("/indicadores")
	IndicadoresSismos indicadores(@RequestAttribute(RequestIdFilter.ATRIBUTO) String requestId) {
		return producer.requestBodyAndHeaders(SismosRoute.INDICADORES, null, encabezados(requestId),
				IndicadoresSismos.class);
	}

	private static Map<String, Object> encabezados(String requestId) {
		Map<String, Object> encabezados = new HashMap<>();
		encabezados.put(LlamadasServiciosRoute.REQUEST_ID, requestId);
		return encabezados;
	}

}
