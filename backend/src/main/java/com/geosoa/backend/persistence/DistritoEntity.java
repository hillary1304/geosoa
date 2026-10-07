package com.geosoa.backend.persistence;

import java.time.Instant;
import java.util.Optional;

import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Poblacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "distrito")
public class DistritoEntity {

	@Id
	@Column(length = 6, columnDefinition = "char(6)")
	private String ubigeo;

	@Column(nullable = false, length = 100)
	private String departamento;

	@Column(nullable = false, length = 100)
	private String provincia;

	@Column(nullable = false, length = 100)
	private String nombre;

	private Long poblacion;

	@Column(name = "densidad_hab_km2")
	private Double densidadHabKm2;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected DistritoEntity() {
	}

	public static DistritoEntity from(Distrito distrito) {
		DistritoEntity entity = new DistritoEntity();
		entity.ubigeo = distrito.ubigeo();
		entity.departamento = distrito.departamento();
		entity.provincia = distrito.provincia();
		entity.nombre = distrito.nombre();
		return entity;
	}

	public void asignarPoblacion(Poblacion poblacion) {
		this.poblacion = poblacion.total();
		this.densidadHabKm2 = poblacion.densidadHabKm2();
	}

	public Distrito toDistrito() {
		return new Distrito(ubigeo, departamento, provincia, nombre);
	}

	public Optional<Poblacion> toPoblacion() {
		return Optional.ofNullable(poblacion).map(total -> new Poblacion(ubigeo, total, densidadHabKm2));
	}

	@PrePersist
	@PreUpdate
	void marcarActualizacion() {
		actualizadoEn = Instant.now();
	}

	public String getUbigeo() {
		return ubigeo;
	}

	public Instant getActualizadoEn() {
		return actualizadoEn;
	}

}
