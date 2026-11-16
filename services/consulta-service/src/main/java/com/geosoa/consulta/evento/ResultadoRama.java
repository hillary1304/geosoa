package com.geosoa.consulta.evento;

import com.geosoa.consulta.domain.Dato;

// dato es null cuando la rama falló; un dato con valor null es falta de cobertura, no una caída.
record ResultadoRama(Rama rama, Dato<?> dato) {

	static ResultadoRama exito(Rama rama, Dato<?> dato) {
		return new ResultadoRama(rama, dato);
	}

	static ResultadoRama fallo(Rama rama) {
		return new ResultadoRama(rama, null);
	}

	boolean fallo() {
		return dato == null;
	}

}
