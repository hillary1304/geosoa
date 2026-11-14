package com.geosoa.backend.resilience;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.geosoa.backend.adapter.senamhi.CapaPrecipitacion;
import com.geosoa.backend.adapter.senamhi.MeteorologiaClient;
import com.geosoa.backend.adapter.senamhi.SenamhiAdapter;
import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Precipitacion;

// El SENAMHI no tiene respaldo en MySQL: ante una caída solo cuenta con la caché.
@Service
public class SenamhiResiliente {

	private final SenamhiAdapter adapter;

	private final FuenteResiliente fuente;

	private final MeteorologiaClient remoto;

	private final boolean usarRemoto;

	@Autowired
	public SenamhiResiliente(SenamhiAdapter adapter, @Qualifier("fuenteSenamhi") FuenteResiliente fuente,
			MeteorologiaClient remoto, @Value("${geosoa.meteorologia.remota:false}") boolean usarRemoto) {
		this.adapter = adapter;
		this.fuente = fuente;
		this.remoto = remoto;
		this.usarRemoto = usarRemoto;
	}

	public SenamhiResiliente(SenamhiAdapter adapter, FuenteResiliente fuente) {
		this(adapter, fuente, null, false);
	}

	public Dato<Optional<Precipitacion>> precipitacion(double latitud, double longitud) {
		if (usarRemoto) {
			return remoto.precipitacion(latitud, longitud, CapaPrecipitacion.ANUAL);
		}
		return fuente.obtener(List.of("precipitacion", latitud, longitud),
				() -> adapter.precipitacion(latitud, longitud));
	}

	public Dato<Optional<Precipitacion>> precipitacion(double latitud, double longitud, CapaPrecipitacion capa) {
		if (usarRemoto) {
			return remoto.precipitacion(latitud, longitud, capa);
		}
		return fuente.obtener(List.of("precipitacion", latitud, longitud, capa),
				() -> adapter.precipitacion(latitud, longitud, capa));
	}

}
