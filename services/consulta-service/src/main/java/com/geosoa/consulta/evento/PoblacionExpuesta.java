package com.geosoa.consulta.evento;

import java.util.List;

import com.geosoa.consulta.domain.DistritoPoblado;

// Estimación del método actual: suma la población de los distritos dentro del radio, no cuenta afectados.
final class PoblacionExpuesta {

	private PoblacionExpuesta() {
	}

	// Si falta la lista o la población de algún distrito, la estimación es desconocida y queda null.
	static Long estimar(List<DistritoPoblado> enRadio) {
		if (enRadio == null || enRadio.stream().anyMatch(d -> d.poblacion() == null)) {
			return null;
		}
		return enRadio.stream().mapToLong(d -> d.poblacion().total()).sum();
	}

}
