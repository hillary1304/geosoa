package com.geosoa.backend.persistence;

import java.time.Instant;
import java.time.ZoneId;

import com.geosoa.backend.domain.Sismo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "sismo")
public class SismoEntity {

	public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String codigo;

	@Column(name = "fecha_evento", nullable = false)
	private Instant fechaEvento;

	@Column(nullable = false)
	private double latitud;

	@Column(nullable = false)
	private double longitud;

	@Column(name = "profundidad_km", nullable = false)
	private double profundidadKm;

	@Column(nullable = false)
	private double magnitud;

	private String intensidad;

	private String referencia;

	@Column(length = 100)
	private String departamento;

	@Column(nullable = false)
	private boolean sentido;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected SismoEntity() {
	}

	public static SismoEntity from(Sismo sismo) {
		SismoEntity entity = new SismoEntity();
		entity.actualizarDesde(sismo);
		return entity;
	}

	public void actualizarDesde(Sismo sismo) {
		codigo = sismo.codigo();
		fechaEvento = sismo.fechaHora().toInstant();
		latitud = sismo.latitud();
		longitud = sismo.longitud();
		profundidadKm = sismo.profundidadKm();
		magnitud = sismo.magnitud();
		intensidad = sismo.intensidad();
		referencia = sismo.referencia();
		departamento = sismo.departamento();
		sentido = sismo.sentido();
	}

	public Sismo toDomain() {
		return new Sismo(codigo, fechaEvento.atZone(ZONA_LIMA), latitud, longitud, profundidadKm, magnitud,
				intensidad, referencia, departamento, sentido);
	}

	@PrePersist
	@PreUpdate
	void marcarActualizacion() {
		actualizadoEn = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public Instant getFechaEvento() {
		return fechaEvento;
	}

	public Instant getActualizadoEn() {
		return actualizadoEn;
	}

}
