package com.geosoa.consulta.evento;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.geosoa.consulta.domain.Dato;
import com.geosoa.consulta.domain.Distrito;
import com.geosoa.consulta.domain.DistritoPoblado;
import com.geosoa.consulta.domain.EventoIntegrado;
import com.geosoa.consulta.domain.Fuente;
import com.geosoa.consulta.domain.Poblacion;
import com.geosoa.consulta.domain.Precipitacion;
import com.geosoa.consulta.domain.Procedencia;
import com.geosoa.consulta.domain.Sismo;

// Arma el evento con lo que respondió; una rama sin resultado (falló o venció el plazo) deja su fuente faltante.
final class ComposicionEvento {

	private ComposicionEvento() {
	}

	static Optional<Distrito> distritoEpicentro(List<ResultadoRama> resultados) {
		return valor(resultados, Rama.DISTRITO, Distrito.class);
	}

	@SuppressWarnings("unchecked")
	static List<DistritoPoblado> enRadio(List<ResultadoRama> resultados) {
		return buscar(resultados, Rama.EN_RADIO).filter(r -> !r.fallo())
			.map(r -> (List<DistritoPoblado>) r.dato().valor())
			.orElse(null);
	}

	// El distrito del epicentro suele venir ya poblado en el radio.
	static Optional<Poblacion> poblacionEnRadio(Distrito distrito, List<DistritoPoblado> enRadio) {
		if (distrito == null || enRadio == null) {
			return Optional.empty();
		}
		return enRadio.stream()
			.filter(d -> d.distrito() != null && distrito.ubigeo().equals(d.distrito().ubigeo()))
			.map(DistritoPoblado::poblacion)
			.filter(Objects::nonNull)
			.findFirst();
	}

	static EventoIntegrado componer(Dato<Sismo> sismo, double radioKm, List<ResultadoRama> resultados,
			boolean poblacionConsultada) {
		Set<Fuente> faltantes = EnumSet.noneOf(Fuente.class);
		Map<Fuente, Procedencia> procedencias = new EnumMap<>(Fuente.class);
		if (sismo.procedencia() != null) {
			procedencias.put(Fuente.IGP, sismo.procedencia());
		}
		for (Rama rama : List.of(Rama.PRECIPITACION, Rama.DISTRITO, Rama.EN_RADIO)) {
			if (buscar(resultados, rama).isEmpty()) {
				faltantes.add(rama.fuente());
			}
		}
		if (poblacionConsultada && buscar(resultados, Rama.POBLACION).isEmpty()) {
			faltantes.add(Rama.POBLACION.fuente());
		}
		for (ResultadoRama resultado : resultados) {
			if (resultado.fallo()) {
				faltantes.add(resultado.rama().fuente());
			}
			else if (resultado.dato().procedencia() != null) {
				procedencias.merge(resultado.rama().fuente(), resultado.dato().procedencia(), Procedencia::combinar);
			}
		}

		Distrito distrito = distritoEpicentro(resultados).orElse(null);
		List<DistritoPoblado> enRadio = enRadio(resultados);
		Poblacion poblacion = distrito == null ? null
				: poblacionEnRadio(distrito, enRadio)
					.or(() -> valor(resultados, Rama.POBLACION, Poblacion.class))
					.orElse(null);
		Precipitacion precipitacion = valor(resultados, Rama.PRECIPITACION, Precipitacion.class).orElse(null);

		return new EventoIntegrado(sismo.valor(), distrito, precipitacion, poblacion, radioKm,
				PoblacionExpuesta.estimar(enRadio), faltantes, procedencias);
	}

	private static Optional<ResultadoRama> buscar(List<ResultadoRama> resultados, Rama rama) {
		return resultados.stream().filter(r -> r.rama() == rama).findFirst();
	}

	private static <T> Optional<T> valor(List<ResultadoRama> resultados, Rama rama, Class<T> tipo) {
		return buscar(resultados, rama).filter(r -> !r.fallo())
			.map(r -> r.dato().valor())
			.filter(tipo::isInstance)
			.map(tipo::cast);
	}

}
