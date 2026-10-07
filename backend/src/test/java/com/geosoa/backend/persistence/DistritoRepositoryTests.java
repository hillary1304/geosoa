package com.geosoa.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Poblacion;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DistritoRepositoryTests {

	private static final Distrito APLAO = new Distrito("040402", "AREQUIPA", "CASTILLA", "APLAO");

	@Autowired
	private DistritoRepository repository;

	@Test
	void guardaDistritoConPoblacion() {
		DistritoEntity entity = DistritoEntity.from(APLAO);
		entity.asignarPoblacion(new Poblacion("040402", 8_544, 13.4));
		repository.saveAndFlush(entity);

		DistritoEntity recuperado = repository.findById("040402").orElseThrow();

		assertThat(recuperado.toDistrito()).isEqualTo(APLAO);
		assertThat(recuperado.toPoblacion()).contains(new Poblacion("040402", 8_544, 13.4));
		assertThat(recuperado.getActualizadoEn()).isNotNull();
	}

	@Test
	void distritoSinPoblacionDevuelveVacio() {
		repository.saveAndFlush(DistritoEntity.from(APLAO));

		assertThat(repository.findById("040402").orElseThrow().toPoblacion()).isEmpty();
	}

	@Test
	void buscaPorDepartamentoSinDistinguirMayusculas() {
		repository.save(DistritoEntity.from(APLAO));
		repository.save(DistritoEntity.from(new Distrito("150101", "LIMA", "LIMA", "LIMA")));

		assertThat(repository.findByDepartamentoIgnoreCase("arequipa")).extracting(DistritoEntity::getUbigeo)
			.containsExactly("040402");
	}

}
