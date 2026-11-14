package com.geosoa.sismos.resilience;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.geosoa.sismos.adapter.igp.FiltroSismos;
import com.geosoa.sismos.adapter.igp.IgpAdapter;
import com.geosoa.sismos.domain.Dato;
import com.geosoa.sismos.domain.Origen;
import com.geosoa.sismos.domain.Procedencia;
import com.geosoa.sismos.domain.Sismo;
import com.geosoa.sismos.persistence.SismoEntity;
import com.geosoa.sismos.persistence.SismoRepository;

@Service
public class IgpResiliente {

	private final IgpAdapter adapter;

	private final FuenteResiliente fuente;

	private final SismoRepository repository;

	public IgpResiliente(IgpAdapter adapter, @Qualifier("fuenteIgp") FuenteResiliente fuente,
			SismoRepository repository) {
		this.adapter = adapter;
		this.fuente = fuente;
		this.repository = repository;
	}

	public Dato<Optional<Sismo>> ultimoSismo() {
		return fuente.obtener("ultimoSismo", adapter::ultimoSismo,
				() -> repository.findFirstByOrderByFechaEventoDesc()
					.map(s -> new Dato<>(Optional.of(s.toDomain()), new Procedencia(Origen.DB, s.getActualizadoEn()))));
	}

	public Dato<Optional<Sismo>> sismoPorCodigo(String codigo) {
		return fuente.obtener(new DetalleSismo(codigo), () -> adapter.sismoPorCodigo(codigo),
				() -> repository.findByCodigo(codigo).map(s -> new Dato<>(Optional.of(s.toDomain()),
						new Procedencia(Origen.DB, s.getActualizadoEn()))));
	}

	public Dato<List<Sismo>> sismosReportados(FiltroSismos filtro, int pagina, int tamano) {
		return fuente.obtener(new ConsultaSismos(filtro, pagina, tamano),
				() -> adapter.sismosReportados(filtro, pagina, tamano),
				() -> almacenados(filtro, pagina, tamano));
	}

	public Dato<Long> contarSismos(FiltroSismos filtro) {
		return fuente.obtener(new ConteoSismos(filtro), () -> adapter.contarSismosReportados(filtro),
				() -> conteoAlmacenado(filtro));
	}

	// Una página vacía en MySQL no prueba que no haya sismos, así que no sirve como respaldo.
	private Optional<Dato<List<Sismo>>> almacenados(FiltroSismos filtro, int pagina, int tamano) {
		Page<SismoEntity> encontrados = repository.buscar(inicioDelDia(filtro.desde()),
				filtro.hasta() == null ? null : inicioDelDia(filtro.hasta().plusDays(1)), filtro.magnitudMinima(),
				filtro.departamento() == null || filtro.departamento().isBlank() ? null : filtro.departamento().trim(),
				PageRequest.of(pagina, tamano));
		if (encontrados.isEmpty()) {
			return Optional.empty();
		}
		Instant masAntigua = encontrados.stream()
			.map(SismoEntity::getActualizadoEn)
			.min(Comparator.naturalOrder())
			.orElseThrow();
		List<Sismo> sismos = encontrados.stream().map(SismoEntity::toDomain).toList();
		return Optional.of(new Dato<>(sismos, new Procedencia(Origen.DB, masAntigua)));
	}

	private Optional<Dato<Long>> conteoAlmacenado(FiltroSismos filtro) {
		Page<SismoEntity> encontrados = repository.buscar(inicioDelDia(filtro.desde()),
				filtro.hasta() == null ? null : inicioDelDia(filtro.hasta().plusDays(1)), filtro.magnitudMinima(),
				filtro.departamento() == null || filtro.departamento().isBlank() ? null : filtro.departamento().trim(),
				PageRequest.of(0, 1));
		if (encontrados.isEmpty()) {
			return Optional.empty();
		}
		Instant actualizadoEn = repository.findFirstByOrderByActualizadoEnDesc().orElseThrow().getActualizadoEn();
		return Optional.of(new Dato<>(encontrados.getTotalElements(), new Procedencia(Origen.DB, actualizadoEn)));
	}

	private static Instant inicioDelDia(LocalDate dia) {
		return dia == null ? null : dia.atStartOfDay(SismoEntity.ZONA_LIMA).toInstant();
	}

	private record ConsultaSismos(FiltroSismos filtro, int pagina, int tamano) {
	}

	private record ConteoSismos(FiltroSismos filtro) {
	}

	private record DetalleSismo(String codigo) {
	}

}
