package com.geosoa.consulta.ruta;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.geosoa.consulta.api.IndicadoresSismos;
import com.geosoa.consulta.api.ListaSismos;
import com.geosoa.consulta.domain.Dato;
import com.geosoa.consulta.domain.Sismo;

import tools.jackson.core.type.TypeReference;

@Component
public class SismosRoute extends RouteBuilder {

	public static final String LISTAR = "direct:listarSismos";

	public static final String ULTIMO = "direct:ultimoSismo";

	public static final String INDICADORES = "direct:indicadores";

	static final String[] FILTROS = { "desde", "hasta", "magnitudMinima", "departamento", "pagina", "tamano" };

	private final LecturaJson lectura;

	public SismosRoute(LecturaJson lectura) {
		this.lectura = lectura;
	}

	@Override
	public void configure() {
		// Los fallos los traduce ManejadorErrores; Camel no reintenta ni los registra dos veces.
		errorHandler(noErrorHandler());
		from(LISTAR).routeId("listar-sismos")
			.process(SismosRoute::filtrosComoQuery)
			.setHeader(Exchange.HTTP_PATH, constant("/sismos"))
			.to(Servicio.SISMOS.endpointLlamada())
			.process(exchange -> exchange.getMessage()
				.setBody(aListaPublica(lectura.leer(exchange, new TypeReference<PaginaSismosInterna>() {
				}))));

		from(ULTIMO).routeId("ultimo-sismo")
			.setHeader(Exchange.HTTP_PATH, constant("/sismos/ultimo"))
			.to(Servicio.SISMOS.endpointLlamada())
			.process(exchange -> exchange.getMessage()
				.setBody(lectura.leer(exchange, new TypeReference<Dato<Sismo>>() {
				})));

		from(INDICADORES).routeId("indicadores")
			.setHeader(Exchange.HTTP_PATH, constant("/sismos/indicadores"))
			.to(Servicio.SISMOS.endpointLlamada())
			.process(exchange -> exchange.getMessage()
				.setBody(lectura.leer(exchange, new TypeReference<IndicadoresSismos>() {
				})));
	}

	static void filtrosComoQuery(Exchange exchange) {
		StringJoiner query = new StringJoiner("&");
		for (String filtro : FILTROS) {
			Object valor = exchange.getMessage().getHeader(filtro);
			if (valor != null) {
				query.add(filtro + "=" + URLEncoder.encode(valor.toString(), StandardCharsets.UTF_8));
			}
		}
		exchange.getMessage().setHeader(Exchange.HTTP_QUERY, query.toString());
	}

	// Sin total no se inventa uno: hayMas solo se afirma si la página vino llena.
	static ListaSismos aListaPublica(PaginaSismosInterna interna) {
		int recibidos = interna.elementos() == null ? 0 : interna.elementos().size();
		boolean hayMas = interna.total() != null
				? (long) (interna.pagina() + 1) * interna.tamano() < interna.total()
				: recibidos == interna.tamano();
		return new ListaSismos(interna.elementos(), interna.pagina(), interna.tamano(), hayMas, interna.total(),
				interna.procedencia());
	}

}
