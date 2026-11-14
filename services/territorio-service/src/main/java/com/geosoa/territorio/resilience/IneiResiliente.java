package com.geosoa.territorio.resilience;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.geosoa.territorio.adapter.inei.DistritoPoblado;
import com.geosoa.territorio.adapter.inei.IneiAdapter;
import com.geosoa.territorio.adapter.inei.IneiRespaldo;
import com.geosoa.territorio.domain.Dato;
import com.geosoa.territorio.domain.Distrito;
import com.geosoa.territorio.domain.Poblacion;

@Service
public class IneiResiliente {

	private final IneiAdapter adapter;

	private final FuenteResiliente fuente;

	private final IneiRespaldo respaldo;

	public IneiResiliente(IneiAdapter adapter, @Qualifier("fuenteInei") FuenteResiliente fuente,
			IneiRespaldo respaldo) {
		this.adapter = adapter;
		this.fuente = fuente;
		this.respaldo = respaldo;
	}

	public Dato<Optional<Distrito>> distrito(double latitud, double longitud) {
		return fuente.obtener(List.of("distrito", latitud, longitud), () -> adapter.distrito(latitud, longitud),
				() -> respaldo.distrito(latitud, longitud));
	}

	public Dato<Optional<Poblacion>> poblacion(String ubigeo) {
		return fuente.obtener(List.of("poblacion", ubigeo), () -> adapter.poblacion(ubigeo),
				() -> respaldo.poblacion(ubigeo));
	}

	public Dato<List<DistritoPoblado>> distritosEnRadio(double latitud, double longitud, double radioKm) {
		return fuente.obtener(List.of("radio", latitud, longitud, radioKm),
				() -> adapter.distritosEnRadio(latitud, longitud, radioKm),
				() -> respaldo.distritosEnRadio(latitud, longitud, radioKm));
	}

}
