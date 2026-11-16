package com.geosoa.consulta.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class LecturaJsonTests {

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	void leeElDetalleDeSismosYLoPresentaEnHoraDeLima() {
		Dato<Sismo> dato = json.readValue("""
				{"valor":{"codigo":"2026-0512","fechaHora":"2026-10-01T13:15:00Z","latitud":-12.05,
				"longitud":-77.03,"profundidadKm":30,"magnitud":6.0,"intensidad":"IV",
				"referencia":"10 km al O de Lima","departamento":"LIMA","sentido":true},
				"procedencia":{"origen":"db","actualizadoEn":"2026-10-06T12:00:00Z"}}
				""", new TypeReference<Dato<Sismo>>() {
		});

		assertThat(dato.valor().fechaHora()).isEqualTo(OffsetDateTime.of(2026, 10, 1, 8, 15, 0, 0, ZoneOffset.ofHours(-5)));
		assertThat(dato.procedencia()).isEqualTo(new Procedencia(Origen.DB, Instant.parse("2026-10-06T12:00:00Z")));
	}

	@Test
	void distingueLaFaltaDeCoberturaConValorNull() {
		Dato<Optional<Precipitacion>> dato = json.readValue("""
				{"valor":null,"procedencia":{"origen":"live","actualizadoEn":"2026-11-14T05:00:00Z"}}
				""", new TypeReference<Dato<Optional<Precipitacion>>>() {
		});

		assertThat(dato.valor()).isEmpty();
		assertThat(dato.procedencia().origen()).isEqualTo(Origen.LIVE);
	}

	@Test
	void leeLosDistritosEnRadioConSuPoblacion() {
		Dato<List<DistritoPoblado>> dato = json.readValue("""
				{"valor":[{"distrito":{"ubigeo":"070101","departamento":"CALLAO","provincia":"CALLAO","nombre":"CALLAO"},
				"poblacion":{"ubigeo":"070101","total":451260,"densidadHabKm2":9894.1}}],
				"procedencia":{"origen":"cache","actualizadoEn":"2026-11-14T05:00:00Z"}}
				""", new TypeReference<Dato<List<DistritoPoblado>>>() {
		});

		assertThat(dato.valor()).singleElement().satisfies(d -> {
			assertThat(d.distrito().ubigeo()).isEqualTo("070101");
			assertThat(d.poblacion().total()).isEqualTo(451260L);
		});
	}

	@Test
	void combinaLaProcedenciaMenosFresca() {
		Procedencia vivo = new Procedencia(Origen.LIVE, Instant.parse("2026-10-07T15:00:00Z"));
		Procedencia almacenada = new Procedencia(Origen.DB, Instant.parse("2026-10-06T12:00:00Z"));

		assertThat(vivo.combinar(almacenada)).isEqualTo(almacenada);
		assertThat(vivo.combinar(null)).isEqualTo(vivo);
	}

}
