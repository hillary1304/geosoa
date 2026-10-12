package com.geosoa.backend.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DistritoRepository extends JpaRepository<DistritoEntity, String> {

	List<DistritoEntity> findByDepartamentoIgnoreCase(String departamento);

	@Query("""
			select d from DistritoEntity d
			where d.minLongitud <= :longitud and d.maxLongitud >= :longitud
				and d.minLatitud <= :latitud and d.maxLatitud >= :latitud""")
	List<DistritoEntity> conEnvolventeQueContiene(@Param("latitud") double latitud,
			@Param("longitud") double longitud);

	@Query("""
			select d from DistritoEntity d
			where d.minLongitud <= :maxLongitud and d.maxLongitud >= :minLongitud
				and d.minLatitud <= :maxLatitud and d.maxLatitud >= :minLatitud""")
	List<DistritoEntity> conEnvolventeQueCruza(@Param("minLatitud") double minLatitud,
			@Param("minLongitud") double minLongitud, @Param("maxLatitud") double maxLatitud,
			@Param("maxLongitud") double maxLongitud);

}
