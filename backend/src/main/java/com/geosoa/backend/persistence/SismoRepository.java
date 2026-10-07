package com.geosoa.backend.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SismoRepository extends JpaRepository<SismoEntity, Long> {

	Optional<SismoEntity> findByCodigo(String codigo);

	Optional<SismoEntity> findFirstByOrderByFechaEventoDesc();

	List<SismoEntity> findByFechaEventoGreaterThanEqualAndMagnitudGreaterThanEqualOrderByFechaEventoDesc(
			Instant desde, double magnitudMinima);

	// Los filtros nulos se ignoran; hasta es exclusivo.
	@Query("""
			select s from SismoEntity s
			where (cast(:desde as Instant) is null or s.fechaEvento >= :desde)
				and (cast(:hasta as Instant) is null or s.fechaEvento < :hasta)
				and (cast(:magnitudMinima as Double) is null or s.magnitud >= :magnitudMinima)
				and (cast(:departamento as String) is null or upper(s.departamento) = upper(:departamento))
			order by s.fechaEvento desc""")
	Page<SismoEntity> buscar(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
			@Param("magnitudMinima") Double magnitudMinima, @Param("departamento") String departamento,
			Pageable pagina);

}
