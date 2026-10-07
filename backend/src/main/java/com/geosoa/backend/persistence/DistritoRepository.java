package com.geosoa.backend.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DistritoRepository extends JpaRepository<DistritoEntity, String> {

	List<DistritoEntity> findByDepartamentoIgnoreCase(String departamento);

}
