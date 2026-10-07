package com.geosoa.backend.resilience;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.geosoa.backend.adapter.senamhi.CapaPrecipitacion;
import com.geosoa.backend.adapter.senamhi.SenamhiAdapter;
import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Precipitacion;

// El SENAMHI no tiene respaldo en MySQL: ante una caída solo cuenta con la caché.
@Service
public class SenamhiResiliente {

	private final SenamhiAdapter adapter;

	private final FuenteResiliente fuente;

	public SenamhiResiliente(SenamhiAdapter adapter, @Qualifier("fuenteSenamhi") FuenteResiliente fuente) {
		this.adapter = adapter;
		this.fuente = fuente;
	}

	public Dato<Optional<Precipitacion>> precipitacion(double latitud, double longitud) {
		return fuente.obtener(List.of("precipitacion", latitud, longitud),
				() -> adapter.precipitacion(latitud, longitud));
	}

	public Dato<Optional<Precipitacion>> precipitacion(double latitud, double longitud, CapaPrecipitacion capa) {
		return fuente.obtener(List.of("precipitacion", latitud, longitud, capa),
				() -> adapter.precipitacion(latitud, longitud, capa));
	}

}
