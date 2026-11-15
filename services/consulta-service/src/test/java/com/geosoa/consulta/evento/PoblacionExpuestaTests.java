package com.geosoa.consulta.evento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.geosoa.consulta.domain.Distrito;
import com.geosoa.consulta.domain.DistritoPoblado;
import com.geosoa.consulta.domain.Poblacion;

class PoblacionExpuestaTests {

	private static final DistritoPoblado LIMA = new DistritoPoblado(new Distrito("150101", "LIMA", "LIMA", "LIMA"),
			new Poblacion("150101", 268352, 12253.5));

	private static final DistritoPoblado CALLAO = new DistritoPoblado(
			new Distrito("070101", "CALLAO", "CALLAO", "CALLAO"), new Poblacion("070101", 451260, 9894.1));

	@Test
	void sumaLaPoblacionDeLosDistritosEnElRadio() {
		assertThat(PoblacionExpuesta.estimar(List.of(CALLAO))).isEqualTo(451260L);
		assertThat(PoblacionExpuesta.estimar(List.of(LIMA, CALLAO))).isEqualTo(719612L);
	}

	@Test
	void sinDistritosEnElRadioLaEstimacionEsCero() {
		assertThat(PoblacionExpuesta.estimar(List.of())).isZero();
	}

	@Test
	void quedaDesconocidaSiFaltanDatos() {
		assertThat(PoblacionExpuesta.estimar(null)).isNull();
		DistritoPoblado sinPoblacion = new DistritoPoblado(LIMA.distrito(), null);
		assertThat(PoblacionExpuesta.estimar(List.of(CALLAO, sinPoblacion))).isNull();
	}

}
