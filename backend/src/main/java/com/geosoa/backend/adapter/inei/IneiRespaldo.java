package com.geosoa.backend.adapter.inei;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.domain.Procedencia;
import com.geosoa.backend.persistence.DistritoEntity;
import com.geosoa.backend.persistence.DistritoRepository;

// Responde con los distritos que el adaptador guardó en MySQL. Solo conoce los ya consultados,
// así que no encontrar uno no prueba que el punto esté fuera del Perú.
@Component
public class IneiRespaldo {

	private final DistritoRepository repository;

	public IneiRespaldo(DistritoRepository repository) {
		this.repository = repository;
	}

	public Optional<Dato<Optional<Distrito>>> distrito(double latitud, double longitud) {
		return repository.conEnvolventeQueContiene(latitud, longitud).stream()
			.filter(d -> d.getLimites() != null && Multipoligono.desdeJson(d.getLimites()).contiene(latitud, longitud))
			.findFirst()
			.map(d -> new Dato<>(Optional.of(d.toDistrito()), procedencia(d)));
	}

	public Optional<Dato<Optional<Poblacion>>> poblacion(String ubigeo) {
		return repository.findById(ubigeo)
			.flatMap(d -> d.toPoblacion().map(p -> new Dato<>(Optional.of(p), procedencia(d))));
	}

	public Optional<Dato<List<DistritoPoblado>>> distritosEnRadio(double latitud, double longitud, double radioKm) {
		double deltaLatitud = radioKm / IneiAdapter.KM_POR_GRADO;
		double deltaLongitud = radioKm
				/ (IneiAdapter.KM_POR_GRADO * Math.max(Math.cos(Math.toRadians(latitud)), 0.01));
		List<DistritoEntity> enRadio = repository
			.conEnvolventeQueCruza(latitud - deltaLatitud, longitud - deltaLongitud, latitud + deltaLatitud,
					longitud + deltaLongitud)
			.stream()
			.filter(d -> d.getLimites() != null && d.toPoblacion().isPresent())
			.filter(d -> Multipoligono.desdeJson(d.getLimites()).distanciaKm(latitud, longitud) <= radioKm)
			.toList();
		if (enRadio.isEmpty()) {
			return Optional.empty();
		}
		Instant masAntigua = enRadio.stream()
			.map(DistritoEntity::getActualizadoEn)
			.min(Comparator.naturalOrder())
			.orElseThrow();
		List<DistritoPoblado> distritos = enRadio.stream()
			.map(d -> new DistritoPoblado(d.toDistrito(), d.toPoblacion().orElseThrow()))
			.toList();
		return Optional.of(new Dato<>(distritos, new Procedencia(Origen.DB, masAntigua)));
	}

	private static Procedencia procedencia(DistritoEntity distrito) {
		return new Procedencia(Origen.DB, distrito.getActualizadoEn());
	}

}
