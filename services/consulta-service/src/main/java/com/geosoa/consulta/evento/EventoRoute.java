package com.geosoa.consulta.evento;

import java.time.Duration;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.geosoa.consulta.config.ServiciosProperties;
import com.geosoa.consulta.domain.Dato;
import com.geosoa.consulta.domain.Distrito;
import com.geosoa.consulta.domain.DistritoPoblado;
import com.geosoa.consulta.domain.Poblacion;
import com.geosoa.consulta.domain.Precipitacion;
import com.geosoa.consulta.domain.Sismo;
import com.geosoa.consulta.ruta.LecturaJson;
import com.geosoa.consulta.ruta.RecursoNoEncontradoException;
import com.geosoa.consulta.ruta.Servicio;

import tools.jackson.core.type.TypeReference;

@Component
public class EventoRoute extends RouteBuilder {

	public static final String CONSULTAR = "direct:consultarEvento";

	public static final String CODIGO = "codigo";

	static final String SISMO = "geosoa.sismo";

	static final String RADIO_KM = "geosoa.radioKm";

	static final String LIMITE = "geosoa.limiteNanos";

	static final String RESULTADOS = "geosoa.resultados";

	static final String POBLACION_CONSULTADA = "geosoa.poblacionConsultada";

	private static final Logger log = LoggerFactory.getLogger(EventoRoute.class);

	private final LecturaJson lectura;

	private final ServiciosProperties servicios;

	public EventoRoute(LecturaJson lectura, ServiciosProperties servicios) {
		this.lectura = lectura;
		this.servicios = servicios;
	}

	@Override
	public void configure() {
		errorHandler(noErrorHandler());

		from(CONSULTAR).routeId("consultar-evento")
			.process(exchange -> exchange.setProperty(LIMITE, System.nanoTime() + servicios.plazoEvento().toNanos()))
			.setHeader(Exchange.HTTP_PATH, simple("/sismos/${header." + CODIGO + "}"))
			.doTry()
				.to(Servicio.SISMOS.endpointLlamada())
			.doCatch(RecursoNoEncontradoException.class)
				.process(exchange -> {
					throw new RecursoNoEncontradoException("Sismo no encontrado");
				})
			.end()
			.process(this::guardarSismo)
			.multicast(new ReunirResultados()).parallelProcessing().timeout(plazoParalelo().toMillis())
				.to("direct:evento-precipitacion", "direct:evento-distrito", "direct:evento-en-radio")
			.end()
			.process(this::decidirPoblacion)
			.choice()
				.when(exchangeProperty(POBLACION_CONSULTADA).isEqualTo(true))
					.to("direct:evento-poblacion")
					.process(exchange -> ReunirResultados.agregar(exchange,
							exchange.getMessage().getBody(ResultadoRama.class)))
			.end()
			.process(this::componer);

		rama("direct:evento-precipitacion", Rama.PRECIPITACION, Servicio.METEOROLOGIA, "/precipitacion",
				new TypeReference<Dato<Precipitacion>>() {
				});
		rama("direct:evento-distrito", Rama.DISTRITO, Servicio.TERRITORIO, "/distritos/por-punto",
				new TypeReference<Dato<Distrito>>() {
				});
		rama("direct:evento-en-radio", Rama.EN_RADIO, Servicio.TERRITORIO, "/distritos/en-radio",
				new TypeReference<Dato<List<DistritoPoblado>>>() {
				});
		rama("direct:evento-poblacion", Rama.POBLACION, Servicio.TERRITORIO, null,
				new TypeReference<Dato<Poblacion>>() {
				});
	}

	// Cada rama atrapa su propio fallo para que el evento siga siendo parcial y no un error.
	private void rama(String endpoint, Rama rama, Servicio servicio, String ruta, TypeReference<? extends Dato<?>> tipo) {
		from(endpoint).routeId(endpoint.substring("direct:".length()))
			.process(exchange -> prepararRama(exchange, rama, ruta))
			.doTry()
				.to(servicio.endpointLlamada())
				.process(exchange -> exchange.getMessage().setBody(ResultadoRama.exito(rama, lectura.leer(exchange, tipo))))
			.doCatch(Exception.class)
				.process(exchange -> {
					Exception error = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
					log.warn("Consulta {} sin respuesta: {}", rama, String.valueOf(error));
					exchange.getMessage().setBody(ResultadoRama.fallo(rama));
				})
			.end();
	}

	private void prepararRama(Exchange exchange, Rama rama, String ruta) {
		Sismo sismo = (Sismo) exchange.getProperty(SISMO, Dato.class).valor();
		String punto = "latitud=" + sismo.latitud() + "&longitud=" + sismo.longitud();
		switch (rama) {
			case PRECIPITACION, DISTRITO -> {
				exchange.getMessage().setHeader(Exchange.HTTP_PATH, ruta);
				exchange.getMessage().setHeader(Exchange.HTTP_QUERY, punto);
			}
			case EN_RADIO -> {
				exchange.getMessage().setHeader(Exchange.HTTP_PATH, ruta);
				exchange.getMessage()
					.setHeader(Exchange.HTTP_QUERY, punto + "&radioKm=" + exchange.getProperty(RADIO_KM, Double.class));
			}
			case POBLACION -> {
				Distrito distrito = ComposicionEvento.distritoEpicentro(resultados(exchange)).orElseThrow();
				exchange.getMessage().setHeader(Exchange.HTTP_PATH, "/distritos/" + distrito.ubigeo() + "/poblacion");
				exchange.getMessage().removeHeader(Exchange.HTTP_QUERY);
			}
		}
	}

	private void guardarSismo(Exchange exchange) {
		Dato<Sismo> sismo = lectura.leer(exchange, new TypeReference<Dato<Sismo>>() {
		});
		if (sismo.valor() == null) {
			throw new RecursoNoEncontradoException("Sismo no encontrado");
		}
		exchange.setProperty(SISMO, sismo);
		exchange.setProperty(RADIO_KM, RadioAfectacion.calcularKm(sismo.valor().magnitud(), sismo.valor().profundidadKm()));
		exchange.removeProperty(RESULTADOS);
	}

	// Solo se consulta la población si el distrito del epicentro no vino en el radio y queda plazo para una llamada.
	private void decidirPoblacion(Exchange exchange) {
		List<ResultadoRama> resultados = resultados(exchange);
		Distrito distrito = ComposicionEvento.distritoEpicentro(resultados).orElse(null);
		boolean falta = distrito != null
				&& ComposicionEvento.poblacionEnRadio(distrito, ComposicionEvento.enRadio(resultados)).isEmpty();
		long restante = exchange.getProperty(LIMITE, Long.class) - System.nanoTime();
		boolean alcanza = restante >= servicios.connectTimeout().plus(servicios.readTimeout()).toNanos();
		exchange.setProperty(POBLACION_CONSULTADA, falta && alcanza);
		if (falta && !alcanza) {
			log.warn("Sin plazo para consultar la población del distrito {}", distrito.ubigeo());
			ReunirResultados.agregar(exchange, ResultadoRama.fallo(Rama.POBLACION));
		}
	}

	@SuppressWarnings("unchecked")
	private void componer(Exchange exchange) {
		Dato<Sismo> sismo = exchange.getProperty(SISMO, Dato.class);
		boolean consultada = Boolean.TRUE.equals(exchange.getProperty(POBLACION_CONSULTADA, Boolean.class));
		exchange.getMessage()
			.setBody(ComposicionEvento.componer(sismo, exchange.getProperty(RADIO_KM, Double.class),
					resultados(exchange), consultada));
	}

	@SuppressWarnings("unchecked")
	private static List<ResultadoRama> resultados(Exchange exchange) {
		List<ResultadoRama> resultados = exchange.getProperty(RESULTADOS, List.class);
		return resultados == null ? List.of() : resultados;
	}

	// Lo que queda del plazo total tras la peor llamada a sismos; acota la espera de las consultas paralelas.
	private Duration plazoParalelo() {
		Duration restante = servicios.plazoEvento().minus(servicios.connectTimeout()).minus(servicios.readTimeout());
		return restante.compareTo(Duration.ofMillis(1)) < 0 ? Duration.ofMillis(1) : restante;
	}

}
