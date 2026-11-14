package com.geosoa.sismos.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.geosoa.sismos.adapter.igp.FiltroSismos;
import com.geosoa.sismos.domain.Dato;
import com.geosoa.sismos.domain.Sismo;
import com.geosoa.sismos.persistence.SismoRepository;
import com.geosoa.sismos.resilience.IgpResiliente;

@RestController
@RequestMapping("/internal/v1/sismos")
public class SismosController {

	private final IgpResiliente igp;
	private final SismoRepository repository;

	public SismosController(IgpResiliente igp, SismoRepository repository) {
		this.igp = igp;
		this.repository = repository;
	}

	@GetMapping
	PaginaSismos listar(@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta,
			@RequestParam(required = false) Double magnitudMinima,
			@RequestParam(required = false) String departamento,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "50") int tamano) {
		if (pagina < 0 || tamano < 1 || tamano > 2000 || (desde != null && hasta != null && hasta.isBefore(desde))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros o paginación fuera de rango");
		}
		FiltroSismos filtro = new FiltroSismos(desde, hasta, magnitudMinima, departamento);
		Dato<List<Sismo>> resultado = igp.sismosReportados(filtro, pagina, tamano);
		Dato<Long> conteo = igp.contarSismos(filtro);
		long total = conteo.valor();
		return new PaginaSismos(resultado.valor(), pagina, tamano, total,
				resultado.procedencia().combinar(conteo.procedencia()));
	}

	@GetMapping("/ultimo")
	Dato<java.util.Optional<Sismo>> ultimo() {
		return igp.ultimoSismo();
	}

	@GetMapping("/{codigo}")
	Dato<Sismo> detalle(@PathVariable String codigo) {
		Dato<java.util.Optional<Sismo>> resultado = igp.sismoPorCodigo(codigo);
		return resultado.valor().map(sismo -> new Dato<>(sismo, resultado.procedencia()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sismo no encontrado"));
	}

	@GetMapping("/indicadores")
	Indicadores indicadores() {
		return new Indicadores(repository.count(), repository.maxMagnitud(),
				"respaldo local: alcance dependiente de la sincronización y migración, no equivale al histórico total");
	}

	record PaginaSismos(List<Sismo> elementos, int pagina, int tamano, long total,
			com.geosoa.sismos.domain.Procedencia procedencia) {
	}

	record Indicadores(long totalAlmacenado, double magnitudMaximaAlmacenada, String cobertura) {
	}

}
