package com.geosoa.backend.orchestrator;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.geosoa.backend.adapter.inei.DistritoPoblado;
import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.EventoIntegrado;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.domain.Procedencia;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.resilience.IgpResiliente;
import com.geosoa.backend.resilience.IneiResiliente;
import com.geosoa.backend.resilience.SenamhiResiliente;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Service
public class OrquestadorService {

	private static final Logger log = LoggerFactory.getLogger(OrquestadorService.class);

	private final IgpResiliente igp;

	private final SenamhiResiliente senamhi;

	private final IneiResiliente inei;

	private final Executor executor;

	public OrquestadorService(IgpResiliente igp, SenamhiResiliente senamhi, IneiResiliente inei,
			@Qualifier("orquestadorExecutor") Executor executor) {
		this.igp = igp;
		this.senamhi = senamhi;
		this.inei = inei;
		this.executor = executor;
	}

	public Optional<EventoIntegrado> ultimoEvento() {
		Dato<Optional<Sismo>> ultimo = igp.ultimoSismo();
		return ultimo.valor().map(sismo -> integrar(sismo, ultimo.procedencia()));
	}

	public EventoIntegrado integrar(Sismo sismo) {
		return integrar(sismo, null);
	}

	private EventoIntegrado integrar(Sismo sismo, Procedencia procedenciaIgp) {
		double latitud = sismo.latitud();
		double longitud = sismo.longitud();
		double radioKm = RadioAfectacion.calcularKm(sismo.magnitud(), sismo.profundidadKm());

		CompletableFuture<Dato<Optional<Precipitacion>>> precipitacion = enParalelo(
				() -> senamhi.precipitacion(latitud, longitud));
		CompletableFuture<Dato<Optional<Distrito>>> distrito = enParalelo(() -> inei.distrito(latitud, longitud));
		CompletableFuture<Dato<List<DistritoPoblado>>> enRadio = enParalelo(
				() -> inei.distritosEnRadio(latitud, longitud, radioKm));

		Resultados resultados = new Resultados();
		if (procedenciaIgp != null) {
			resultados.procedencias.put(Fuente.IGP, procedenciaIgp);
		}
		Precipitacion precipitacionEpicentro = resultados.de(Fuente.SENAMHI, precipitacion::join).flatMap(p -> p)
			.orElse(null);
		Distrito distritoEpicentro = resultados.de(Fuente.INEI, distrito::join).flatMap(d -> d).orElse(null);
		List<DistritoPoblado> afectados = resultados.de(Fuente.INEI, enRadio::join).orElse(null);

		Poblacion poblacion = poblacionDe(distritoEpicentro, afectados, resultados);
		Long poblacionExpuesta = afectados == null ? null : poblacionExpuesta(afectados);

		return new EventoIntegrado(sismo, distritoEpicentro, precipitacionEpicentro, poblacion, radioKm,
				poblacionExpuesta, resultados.faltantes, resultados.procedencias);
	}

	private <T> CompletableFuture<T> enParalelo(Supplier<T> consulta) {
		return CompletableFuture.supplyAsync(consulta, executor);
	}

	// El distrito del epicentro suele venir ya poblado en el radio; si no, se consulta aparte.
	private Poblacion poblacionDe(Distrito distrito, List<DistritoPoblado> afectados, Resultados resultados) {
		if (distrito == null) {
			return null;
		}
		if (afectados != null) {
			Optional<Poblacion> enRadio = afectados.stream()
				.filter(d -> d.distrito().ubigeo().equals(distrito.ubigeo()))
				.map(DistritoPoblado::poblacion)
				.findFirst();
			if (enRadio.isPresent()) {
				return enRadio.get();
			}
		}
		return resultados.de(Fuente.INEI, () -> inei.poblacion(distrito.ubigeo())).flatMap(p -> p).orElse(null);
	}

	private static long poblacionExpuesta(List<DistritoPoblado> afectados) {
		return afectados.stream()
			.mapToLong(d -> d.poblacion().total())
			.sum();
	}

	// Fuentes que faltaron y procedencia de las que respondieron, combinando varias consultas a una misma fuente.
	private static final class Resultados {

		private final Set<Fuente> faltantes = EnumSet.noneOf(Fuente.class);

		private final Map<Fuente, Procedencia> procedencias = new EnumMap<>(Fuente.class);

		<T> Optional<T> de(Fuente fuente, Supplier<Dato<T>> consulta) {
			try {
				Dato<T> dato = consulta.get();
				procedencias.merge(fuente, dato.procedencia(), Procedencia::combinar);
				return Optional.of(dato.valor());
			}
			catch (FuenteNoDisponibleException ex) {
				return faltante(ex);
			}
			catch (CompletionException ex) {
				if (ex.getCause() instanceof FuenteNoDisponibleException noDisponible) {
					return faltante(noDisponible);
				}
				throw ex;
			}
		}

		private <T> Optional<T> faltante(FuenteNoDisponibleException ex) {
			log.warn("Fuente {} no disponible: {}", ex.getFuente(), ex.getMessage());
			faltantes.add(ex.getFuente());
			return Optional.empty();
		}

	}

}
