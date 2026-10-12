package com.geosoa.backend.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.geosoa.backend.adapter.inei.DistritoPoblado;
import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.EventoIntegrado;
import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.domain.Precipitacion;
import com.geosoa.backend.domain.Procedencia;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.resilience.IgpResiliente;
import com.geosoa.backend.resilience.IneiResiliente;
import com.geosoa.backend.resilience.SenamhiResiliente;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

class OrquestadorServiceTests {

	private static final Sismo SISMO = new Sismo("2026-0512", ZonedDateTime.parse("2026-10-01T08:15:00-05:00[America/Lima]"),
			-12.05, -77.03, 30, 6.0, "IV", "10 km al O de Lima", "LIMA", true);

	private static final double RADIO_KM = 95.4;

	private static final Distrito LIMA = new Distrito("150101", "LIMA", "LIMA", "LIMA");

	private static final Distrito CALLAO = new Distrito("070101", "CALLAO", "CALLAO", "CALLAO");

	private static final Poblacion POBLACION_LIMA = new Poblacion("150101", 268352, 12253.5);

	private static final Poblacion POBLACION_CALLAO = new Poblacion("070101", 451260, 9894.1);

	private static final Precipitacion PRECIPITACION = new Precipitacion("ANUAL", 10, 15.0);

	private static final Procedencia EN_VIVO = new Procedencia(Origen.LIVE, Instant.parse("2026-10-07T15:00:00Z"));

	private static final Procedencia ALMACENADA = new Procedencia(Origen.DB, Instant.parse("2026-10-06T12:00:00Z"));

	private IgpResiliente igp;

	private SenamhiResiliente senamhi;

	private IneiResiliente inei;

	private OrquestadorService orquestador;

	@BeforeEach
	void configurar() {
		igp = mock(IgpResiliente.class);
		senamhi = mock(SenamhiResiliente.class);
		inei = mock(IneiResiliente.class);
		orquestador = new OrquestadorService(igp, senamhi, inei, Runnable::run);
	}

	@Test
	void integraLasTresFuentes() {
		when(senamhi.precipitacion(-12.05, -77.03)).thenReturn(enVivo(Optional.of(PRECIPITACION)));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM)).thenReturn(enVivo(List.of(
				new DistritoPoblado(LIMA, POBLACION_LIMA), new DistritoPoblado(CALLAO, POBLACION_CALLAO))));

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento).isEqualTo(new EventoIntegrado(SISMO, LIMA, PRECIPITACION, POBLACION_LIMA, RADIO_KM,
				268352L + 451260L, Set.of(), Map.of(Fuente.SENAMHI, EN_VIVO, Fuente.INEI, EN_VIVO)));
		assertThat(evento.completo()).isTrue();
		verify(inei, never()).poblacion(LIMA.ubigeo());
	}

	@Test
	void consultaLaPoblacionDelEpicentroSiNoVieneEnElRadio() {
		when(senamhi.precipitacion(-12.05, -77.03)).thenReturn(enVivo(Optional.of(PRECIPITACION)));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenReturn(enVivo(List.of(new DistritoPoblado(CALLAO, POBLACION_CALLAO))));
		when(inei.poblacion("150101")).thenReturn(enVivo(Optional.of(POBLACION_LIMA)));

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento.poblacion()).isEqualTo(POBLACION_LIMA);
		assertThat(evento.poblacionExpuesta()).isEqualTo(451260L);
	}

	@Test
	void respondeParcialmenteSiElSenamhiNoResponde() {
		when(senamhi.precipitacion(-12.05, -77.03))
			.thenThrow(new FuenteNoDisponibleException(Fuente.SENAMHI, "Error al consultar el SENAMHI", null));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenReturn(enVivo(List.of(new DistritoPoblado(LIMA, POBLACION_LIMA))));

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento.precipitacion()).isNull();
		assertThat(evento.distrito()).isEqualTo(LIMA);
		assertThat(evento.poblacion()).isEqualTo(POBLACION_LIMA);
		assertThat(evento.fuentesFaltantes()).containsExactly(Fuente.SENAMHI);
		assertThat(evento.completo()).isFalse();
	}

	@Test
	void respondeParcialmenteSiElIneiNoResponde() {
		FuenteNoDisponibleException caida = new FuenteNoDisponibleException(Fuente.INEI, "Error al consultar el INEI",
				null);
		when(senamhi.precipitacion(-12.05, -77.03)).thenReturn(enVivo(Optional.of(PRECIPITACION)));
		when(inei.distrito(-12.05, -77.03)).thenThrow(caida);
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM)).thenThrow(caida);

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento.precipitacion()).isEqualTo(PRECIPITACION);
		assertThat(evento.distrito()).isNull();
		assertThat(evento.poblacion()).isNull();
		assertThat(evento.poblacionExpuesta()).isNull();
		assertThat(evento.radioAfectacionKm()).isEqualTo(RADIO_KM);
		assertThat(evento.fuentesFaltantes()).containsExactly(Fuente.INEI);
	}

	@Test
	void marcaAlIneiSiFallaSoloLaConsultaDePoblacion() {
		when(senamhi.precipitacion(-12.05, -77.03)).thenReturn(enVivo(Optional.of(PRECIPITACION)));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenThrow(new FuenteNoDisponibleException(Fuente.INEI, "Error al consultar el INEI", null));
		when(inei.poblacion("150101"))
			.thenThrow(new FuenteNoDisponibleException(Fuente.INEI, "Error al consultar el INEI", null));

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento.distrito()).isEqualTo(LIMA);
		assertThat(evento.poblacion()).isNull();
		assertThat(evento.fuentesFaltantes()).containsExactly(Fuente.INEI);
	}

	@Test
	void noMarcaFaltantesCuandoElEpicentroEstaEnElMar() {
		when(senamhi.precipitacion(-12.05, -77.03)).thenReturn(enVivo(Optional.empty()));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.empty()));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenReturn(enVivo(List.of(new DistritoPoblado(CALLAO, POBLACION_CALLAO))));

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento.distrito()).isNull();
		assertThat(evento.precipitacion()).isNull();
		assertThat(evento.poblacion()).isNull();
		assertThat(evento.poblacionExpuesta()).isEqualTo(451260L);
		assertThat(evento.completo()).isTrue();
	}

	@Test
	void consultaAlSenamhiYAlIneiEnParalelo() throws Exception {
		CountDownLatch ambasEnCurso = new CountDownLatch(2);
		when(senamhi.precipitacion(-12.05, -77.03)).thenAnswer(invocacion -> {
			esperarA(ambasEnCurso);
			return enVivo(Optional.of(PRECIPITACION));
		});
		when(inei.distrito(-12.05, -77.03)).thenAnswer(invocacion -> {
			esperarA(ambasEnCurso);
			return enVivo(Optional.of(LIMA));
		});
		when(inei.distritosEnRadio(eq(-12.05), eq(-77.03), anyDouble()))
			.thenReturn(enVivo(List.of(new DistritoPoblado(LIMA, POBLACION_LIMA))));

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			EventoIntegrado evento = new OrquestadorService(igp, senamhi, inei, executor).integrar(SISMO);

			assertThat(evento.completo()).isTrue();
		}
	}

	@Test
	void integraElUltimoSismoDelIgp() {
		when(igp.ultimoSismo()).thenReturn(enVivo(Optional.of(SISMO)));
		when(senamhi.precipitacion(-12.05, -77.03)).thenReturn(enVivo(Optional.of(PRECIPITACION)));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenReturn(enVivo(List.of(new DistritoPoblado(LIMA, POBLACION_LIMA))));

		assertThat(orquestador.ultimoEvento()).get().extracting(EventoIntegrado::sismo).isEqualTo(SISMO);
	}

	@Test
	void noIntegraNadaSiElIgpNoTieneSismos() {
		when(igp.ultimoSismo()).thenReturn(enVivo(Optional.empty()));

		assertThat(orquestador.ultimoEvento()).isEmpty();
	}

	@Test
	void informaLaProcedenciaMenosFrescaDeCadaFuente() {
		when(igp.ultimoSismo()).thenReturn(new Dato<>(Optional.of(SISMO), ALMACENADA));
		when(senamhi.precipitacion(-12.05, -77.03))
			.thenReturn(new Dato<>(Optional.of(PRECIPITACION), new Procedencia(Origen.CACHE, EN_VIVO.actualizadoEn())));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenReturn(new Dato<>(List.of(new DistritoPoblado(LIMA, POBLACION_LIMA)), ALMACENADA));

		EventoIntegrado evento = orquestador.ultimoEvento().orElseThrow();

		assertThat(evento.procedencias()).containsExactlyInAnyOrderEntriesOf(Map.of(
				Fuente.IGP, ALMACENADA,
				Fuente.SENAMHI, new Procedencia(Origen.CACHE, EN_VIVO.actualizadoEn()),
				Fuente.INEI, ALMACENADA));
	}

	@Test
	void noInformaProcedenciaDeLasFuentesFaltantes() {
		when(senamhi.precipitacion(-12.05, -77.03))
			.thenThrow(new FuenteNoDisponibleException(Fuente.SENAMHI, "Error al consultar el SENAMHI", null));
		when(inei.distrito(-12.05, -77.03)).thenReturn(enVivo(Optional.of(LIMA)));
		when(inei.distritosEnRadio(-12.05, -77.03, RADIO_KM))
			.thenReturn(enVivo(List.of(new DistritoPoblado(LIMA, POBLACION_LIMA))));

		EventoIntegrado evento = orquestador.integrar(SISMO);

		assertThat(evento.procedencias()).containsOnlyKeys(Fuente.INEI);
	}

	private static <T> Dato<T> enVivo(T valor) {
		return new Dato<>(valor, EN_VIVO);
	}

	// Solo termina si la otra consulta corre al mismo tiempo; con consultas en serie fallaría por tiempo.
	private static void esperarA(CountDownLatch ambasEnCurso) throws InterruptedException {
		ambasEnCurso.countDown();
		if (!ambasEnCurso.await(5, TimeUnit.SECONDS)) {
			throw new AssertionError("Las consultas no se ejecutaron en paralelo");
		}
	}

}
