package com.geosoa.backend.orchestrator;

import java.util.EnumSet;
import java.util.List;
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

import com.geosoa.backend.adapter.igp.IgpAdapter;
import com.geosoa.backend.adapter.inei.DistritoPoblado;
import com.geosoa.backend.adapter.inei.IneiAdapter;
import com.geosoa.backend.adapter.senamhi.SenamhiAdapter;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.EventoIntegrado;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Service
public class OrquestadorService {

	private static final Logger log = LoggerFactory.getLogger(OrquestadorService.class);

	private final IgpAdapter igpAdapter;

	private final SenamhiAdapter senamhiAdapter;

	private final IneiAdapter ineiAdapter;

	private final Executor executor;

	public OrquestadorService(IgpAdapter igpAdapter, SenamhiAdapter senamhiAdapter, IneiAdapter ineiAdapter,
			@Qualifier("orquestadorExecutor") Executor executor) {
		this.igpAdapter = igpAdapter;
		this.senamhiAdapter = senamhiAdapter;
		this.ineiAdapter = ineiAdapter;
		this.executor = executor;
	}

	public Optional<EventoIntegrado> ultimoEvento() {
		return igpAdapter.ultimoSismo().map(this::integrar);
	}

	public EventoIntegrado integrar(Sismo sismo) {
		double latitud = sismo.latitud();
		double longitud = sismo.longitud();
		double radioKm = RadioAfectacion.calcularKm(sismo.magnitud(), sismo.profundidadKm());

		CompletableFuture<Optional<Precipitacion>> precipitacion = enParalelo(
				() -> senamhiAdapter.precipitacion(latitud, longitud));
		CompletableFuture<Optional<Distrito>> distrito = enParalelo(() -> ineiAdapter.distrito(latitud, longitud));
		CompletableFuture<List<DistritoPoblado>> enRadio = enParalelo(
				() -> ineiAdapter.distritosEnRadio(latitud, longitud, radioKm));

		Set<Fuente> faltantes = EnumSet.noneOf(Fuente.class);
		Precipitacion precipitacionEpicentro = resultado(precipitacion, faltantes).flatMap(p -> p).orElse(null);
		Distrito distritoEpicentro = resultado(distrito, faltantes).flatMap(d -> d).orElse(null);
		List<DistritoPoblado> afectados = resultado(enRadio, faltantes).orElse(null);

		Poblacion poblacion = poblacionDe(distritoEpicentro, afectados, faltantes);
		Long poblacionExpuesta = afectados == null ? null : poblacionExpuesta(afectados);

		return new EventoIntegrado(sismo, distritoEpicentro, precipitacionEpicentro, poblacion, radioKm,
				poblacionExpuesta, faltantes);
	}

	private <T> CompletableFuture<T> enParalelo(Supplier<T> consulta) {
		return CompletableFuture.supplyAsync(consulta, executor);
	}

	private static <T> Optional<T> resultado(CompletableFuture<T> futuro, Set<Fuente> faltantes) {
		try {
			return Optional.of(futuro.join());
		}
		catch (CompletionException ex) {
			if (ex.getCause() instanceof FuenteNoDisponibleException noDisponible) {
				log.warn("Fuente {} no disponible: {}", noDisponible.getFuente(), noDisponible.getMessage());
				faltantes.add(noDisponible.getFuente());
				return Optional.empty();
			}
			throw ex;
		}
	}

	// El distrito del epicentro suele venir ya poblado en el radio; si no, se consulta aparte.
	private Poblacion poblacionDe(Distrito distrito, List<DistritoPoblado> afectados, Set<Fuente> faltantes) {
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
		try {
			return ineiAdapter.poblacion(distrito.ubigeo()).orElse(null);
		}
		catch (FuenteNoDisponibleException ex) {
			log.warn("Fuente {} no disponible: {}", ex.getFuente(), ex.getMessage());
			faltantes.add(ex.getFuente());
			return null;
		}
	}

	private static long poblacionExpuesta(List<DistritoPoblado> afectados) {
		return afectados.stream()
			.mapToLong(d -> d.poblacion().total())
			.sum();
	}

}
