package com.geosoa.meteorologia.api;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.geosoa.meteorologia.adapter.senamhi.CapaPrecipitacion;
import com.geosoa.meteorologia.domain.Dato;
import com.geosoa.meteorologia.domain.Precipitacion;
import com.geosoa.meteorologia.resilience.SenamhiResiliente;

@RestController
@RequestMapping("/internal/v1/precipitacion")
public class PrecipitacionController {

	private final SenamhiResiliente senamhi;

	public PrecipitacionController(SenamhiResiliente senamhi) {
		this.senamhi = senamhi;
	}

	@GetMapping
	public Dato<Optional<Precipitacion>> precipitacion(@RequestParam double latitud,
			@RequestParam double longitud, @RequestParam(defaultValue = "ANUAL") CapaPrecipitacion capa) {
		if (!Double.isFinite(latitud) || !Double.isFinite(longitud) || latitud < -90 || latitud > 90
				|| longitud < -180 || longitud > 180) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas fuera de rango");
		}
		return senamhi.precipitacion(latitud, longitud, capa);
	}

}
