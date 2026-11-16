package com.geosoa.consulta.evento;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

// Junta en una lista el ResultadoRama de cada consulta paralela.
class ReunirResultados implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange acumulado, Exchange nuevo) {
		Exchange destino = acumulado == null ? nuevo : acumulado;
		agregar(destino, nuevo.getMessage().getBody(ResultadoRama.class));
		return destino;
	}

	@SuppressWarnings("unchecked")
	static void agregar(Exchange exchange, ResultadoRama resultado) {
		List<ResultadoRama> resultados = exchange.getProperty(EventoRoute.RESULTADOS, List.class);
		List<ResultadoRama> copia = resultados == null ? new ArrayList<>() : new ArrayList<>(resultados);
		if (resultado != null) {
			copia.add(resultado);
		}
		exchange.setProperty(EventoRoute.RESULTADOS, copia);
	}

}
