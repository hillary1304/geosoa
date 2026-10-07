package com.geosoa.backend.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SismoRepository extends JpaRepository<SismoEntity, Long> {

	Optional<SismoEntity> findByCodigo(String codigo);

	Optional<SismoEntity> findFirstByOrderByFechaEventoDesc();

	List<SismoEntity> findByFechaEventoGreaterThanEqualAndMagnitudGreaterThanEqualOrderByFechaEventoDesc(
			Instant desde, double magnitudMinima);

}
