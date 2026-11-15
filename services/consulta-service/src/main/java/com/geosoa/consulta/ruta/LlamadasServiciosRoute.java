package com.geosoa.consulta.ruta;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.stereotype.Component;

import com.geosoa.consulta.config.ServiciosProperties;

// Una subruta por servicio de dominio: envía solo X-Request-Id y traduce los fallos HTTP a errores propios.
@Component
public class LlamadasServiciosRoute extends RouteBuilder {

	public static final String REQUEST_ID = "X-Request-Id";

	private final ServiciosProperties servicios;

	public LlamadasServiciosRoute(ServiciosProperties servicios) {
		this.servicios = servicios;
	}

	@Override
	public void configure() {
		// Los fallos los traduce ManejadorErrores; Camel no reintenta ni los registra dos veces.
		errorHandler(noErrorHandler());
		for (Servicio servicio : Servicio.values()) {
			from(servicio.endpointLlamada()).routeId("llamar-" + servicio.id())
				.removeHeaders("*", REQUEST_ID, Exchange.HTTP_PATH, Exchange.HTTP_QUERY)
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setBody(constant(null))
				.doTry()
					.to(baseUrl(servicio))
				.doCatch(Exception.class)
					.process(exchange -> {
						throw traducir(servicio, exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class));
					})
				.end();
		}
	}

	private String baseUrl(Servicio servicio) {
		String url = switch (servicio) {
			case SISMOS -> servicios.sismosUrl().toString();
			case METEOROLOGIA -> servicios.meteorologiaUrl().toString();
			case TERRITORIO -> servicios.territorioUrl().toString();
		};
		return url.replaceAll("/+$", "") + "/internal/v1";
	}

	static RuntimeException traducir(Servicio servicio, Exception error) {
		if (error instanceof HttpOperationFailedException http) {
			return switch (http.getStatusCode()) {
				case 404 -> new RecursoNoEncontradoException("Recurso no encontrado en el servicio de " + servicio.id());
				case 400 -> new SolicitudRechazadaException("El servicio de " + servicio.id() + " rechazó los parámetros");
				default -> new ServicioNoDisponibleException(servicio, error);
			};
		}
		return new ServicioNoDisponibleException(servicio, error);
	}

}
