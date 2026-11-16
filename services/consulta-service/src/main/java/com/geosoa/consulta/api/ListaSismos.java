package com.geosoa.consulta.api;

import java.util.List;

import com.geosoa.consulta.domain.Procedencia;
import com.geosoa.consulta.domain.Sismo;

public record ListaSismos(List<Sismo> items, int pagina, int tamano, boolean hayMas, Long total,
		Procedencia procedencia) {

	public ListaSismos {
		items = items == null ? List.of() : List.copyOf(items);
	}

}
