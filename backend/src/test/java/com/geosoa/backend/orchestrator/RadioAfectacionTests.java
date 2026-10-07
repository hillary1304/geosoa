package com.geosoa.backend.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RadioAfectacionTests {

	@Test
	void creceConLaMagnitud() {
		assertThat(RadioAfectacion.calcularKm(6.0, 30)).isEqualTo(95.4);
		assertThat(RadioAfectacion.calcularKm(6.5, 30)).isEqualTo(175.3);
	}

	@Test
	void seLimitaAlMaximoEnSismosMuyFuertes() {
		assertThat(RadioAfectacion.calcularKm(8.0, 30)).isEqualTo(RadioAfectacion.MAXIMO_KM);
	}

	@Test
	void disminuyeConLaProfundidad() {
		assertThat(RadioAfectacion.calcularKm(6.0, 80)).isLessThan(RadioAfectacion.calcularKm(6.0, 10));
	}

	@Test
	void usaElMinimoCuandoElSismoEsProfundoODebil() {
		assertThat(RadioAfectacion.calcularKm(4.0, 120)).isEqualTo(RadioAfectacion.MINIMO_KM);
		assertThat(RadioAfectacion.calcularKm(2.0, 5)).isEqualTo(RadioAfectacion.MINIMO_KM);
	}

	@Test
	void rechazaValoresInvalidos() {
		assertThatThrownBy(() -> RadioAfectacion.calcularKm(Double.NaN, 10))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> RadioAfectacion.calcularKm(5, -1)).isInstanceOf(IllegalArgumentException.class);
	}

}
