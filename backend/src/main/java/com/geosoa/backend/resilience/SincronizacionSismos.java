package com.geosoa.backend.resilience;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.geosoa.backend.adapter.igp.FiltroSismos;
import com.geosoa.backend.adapter.igp.IgpAdapter;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.persistence.SismoEntity;
import com.geosoa.backend.persistence.SismoRepository;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

// Mantiene en MySQL los sismos recientes del IGP para servirlos cuando el IGP no responda.
@Component
@ConditionalOnProperty(name = "geosoa.sincronizacion.habilitada", havingValue = "true", matchIfMissing = true)
public class SincronizacionSismos {

	private static final Logger log = LoggerFactory.getLogger(SincronizacionSismos.class);

	static final int TAMANO_PAGINA = 2000;

	private final IgpAdapter igp;

	private final SismoRepository repository;

	private final int dias;

	public SincronizacionSismos(IgpAdapter igp, SismoRepository repository,
			@Value("${geosoa.sincronizacion.dias:7}") int dias) {
		this.igp = igp;
		this.repository = repository;
		this.dias = dias;
	}

	@Scheduled(initialDelayString = "${geosoa.sincronizacion.retraso-inicial:10s}",
			fixedDelayString = "${geosoa.sincronizacion.intervalo:5m}")
	public void sincronizar() {
		LocalDate desde = LocalDate.now(SismoEntity.ZONA_LIMA).minusDays(dias);
		List<Sismo> sismos;
		try {
			sismos = igp.sismosReportados(new FiltroSismos(desde, null, null, null), 0, TAMANO_PAGINA);
		}
		catch (FuenteNoDisponibleException ex) {
			log.warn("No se pudo sincronizar los sismos del IGP: {}", ex.getMessage());
			return;
		}
		try {
			guardar(sismos);
			log.info("Sincronizados {} sismos del IGP desde {}", sismos.size(), desde);
		}
		catch (DataAccessException ex) {
			log.warn("No se pudo guardar la sincronización de sismos", ex);
		}
	}

	private void guardar(List<Sismo> sismos) {
		Map<String, SismoEntity> existentes = repository
			.findByCodigoIn(sismos.stream().map(Sismo::codigo).toList())
			.stream()
			.collect(Collectors.toMap(SismoEntity::getCodigo, Function.identity()));
		List<SismoEntity> entidades = sismos.stream().map(sismo -> {
			SismoEntity existente = existentes.get(sismo.codigo());
			if (existente == null) {
				return SismoEntity.from(sismo);
			}
			existente.actualizarDesde(sismo);
			return existente;
		}).toList();
		repository.saveAll(entidades);
	}

}
