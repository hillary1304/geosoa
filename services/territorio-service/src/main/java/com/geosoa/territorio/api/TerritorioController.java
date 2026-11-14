package com.geosoa.territorio.api;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.geosoa.territorio.adapter.inei.DistritoPoblado;
import com.geosoa.territorio.domain.Dato;
import com.geosoa.territorio.domain.Distrito;
import com.geosoa.territorio.domain.Poblacion;
import com.geosoa.territorio.resilience.IneiResiliente;

@RestController
@RequestMapping("/internal/v1/distritos")
public class TerritorioController {

	private final IneiResiliente inei;

	public TerritorioController(IneiResiliente inei) {
		this.inei = inei;
	}

	@GetMapping("/por-punto")
	public Dato<Optional<Distrito>> distrito(@RequestParam double latitud, @RequestParam double longitud) {
		validarPunto(latitud, longitud);
		return inei.distrito(latitud, longitud);
	}

	@GetMapping("/en-radio")
	public Dato<List<DistritoPoblado>> enRadio(@RequestParam double latitud, @RequestParam double longitud,
			@RequestParam double radioKm) {
		validarPunto(latitud, longitud);
		if (!(radioKm > 0) || !Double.isFinite(radioKm)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radio inválido");
		}
		return inei.distritosEnRadio(latitud, longitud, radioKm);
	}

	@GetMapping("/{ubigeo}/poblacion")
	public Dato<Optional<Poblacion>> poblacion(@PathVariable String ubigeo) {
		if (!ubigeo.matches("\\d{6}")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubigeo inválido");
		}
		return inei.poblacion(ubigeo);
	}

	private static void validarPunto(double latitud, double longitud) {
		if (!Double.isFinite(latitud) || !Double.isFinite(longitud) || latitud < -90 || latitud > 90
				|| longitud < -180 || longitud > 180) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas fuera de rango");
		}
	}

}
