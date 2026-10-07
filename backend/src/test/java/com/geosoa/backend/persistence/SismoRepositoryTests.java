package com.geosoa.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.geosoa.backend.domain.Sismo;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SismoRepositoryTests {

	@Autowired
	private SismoRepository repository;

	@Test
	void guardaYRecuperaSismoEnHoraDeLima() {
		Sismo sismo = sismo("2026-716", "2026-10-04T19:50:19-05:00", 3.5);

		SismoEntity guardado = repository.saveAndFlush(SismoEntity.from(sismo));

		assertThat(guardado.getId()).isNotNull();
		assertThat(guardado.getActualizadoEn()).isNotNull();
		Sismo recuperado = repository.findByCodigo("2026-716").orElseThrow().toDomain();
		assertThat(recuperado).isEqualTo(sismo);
		assertThat(recuperado.fechaHora().getZone()).isEqualTo(SismoEntity.ZONA_LIMA);
	}

	@Test
	void devuelveElSismoMasReciente() {
		repository.save(SismoEntity.from(sismo("2026-700", "2026-10-01T08:00:00-05:00", 4.0)));
		repository.save(SismoEntity.from(sismo("2026-716", "2026-10-04T19:50:19-05:00", 3.5)));
		repository.save(SismoEntity.from(sismo("2026-710", "2026-10-03T12:00:00-05:00", 5.1)));

		assertThat(repository.findFirstByOrderByFechaEventoDesc()).get()
			.extracting(SismoEntity::getCodigo)
			.isEqualTo("2026-716");
	}

	@Test
	void filtraPorFechaYMagnitud() {
		repository.save(SismoEntity.from(sismo("2026-690", "2026-09-28T08:00:00-05:00", 5.0)));
		repository.save(SismoEntity.from(sismo("2026-700", "2026-10-01T08:00:00-05:00", 4.0)));
		repository.save(SismoEntity.from(sismo("2026-710", "2026-10-03T12:00:00-05:00", 5.1)));
		repository.save(SismoEntity.from(sismo("2026-716", "2026-10-04T19:50:19-05:00", 3.5)));

		var resultado = repository.findByFechaEventoGreaterThanEqualAndMagnitudGreaterThanEqualOrderByFechaEventoDesc(
				ZonedDateTime.parse("2026-10-01T00:00:00-05:00").toInstant(), 4.0);

		assertThat(resultado).extracting(SismoEntity::getCodigo).containsExactly("2026-710", "2026-700");
	}

	@Test
	void rechazaCodigoDuplicado() {
		repository.saveAndFlush(SismoEntity.from(sismo("2026-716", "2026-10-04T19:50:19-05:00", 3.5)));

		assertThatThrownBy(
				() -> repository.saveAndFlush(SismoEntity.from(sismo("2026-716", "2026-10-04T19:50:19-05:00", 3.5))))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static Sismo sismo(String codigo, String fechaHora, double magnitud) {
		return new Sismo(codigo, ZonedDateTime.parse(fechaHora).withZoneSameInstant(SismoEntity.ZONA_LIMA), -10.56,
				-75.39, 10, magnitud, "II-III Oxapampa", "2 km al NE de Oxapampa, Oxapampa - Pasco", "PASCO", true);
	}

}
