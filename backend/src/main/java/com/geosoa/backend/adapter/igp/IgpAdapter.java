package com.geosoa.backend.adapter.igp;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.geosoa.backend.domain.Fuente;
import com.geosoa.backend.domain.Sismo;
import com.geosoa.backend.shared.FuenteNoDisponibleException;

@Component
public class IgpAdapter {

	static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

	private static final Locale LOCALE_PERU = Locale.of("es", "PE");

	private static final DateTimeFormatter FECHA_LOCAL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	static final int TAMANO_MAXIMO_PAGINA = 2000;

	private static final DateTimeFormatter TIMESTAMP_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZoneOffset.UTC);

	private static final ParameterizedTypeReference<RespuestaArcGis<UltimoSismoAtributos>> RESPUESTA_ULTIMO_SISMO =
			new ParameterizedTypeReference<>() {
			};

	private static final ParameterizedTypeReference<RespuestaArcGis<SismoReportadoAtributos>> RESPUESTA_SISMOS_REPORTADOS =
			new ParameterizedTypeReference<>() {
			};

	private static final String RUTA_SISMOS_REPORTADOS = "/SismosReportados/MapServer/0/query";

	private final RestClient restClient;

	public IgpAdapter(@Qualifier("igpRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public Optional<Sismo> ultimoSismo() {
		RespuestaArcGis<UltimoSismoAtributos> respuesta = consultar(RESPUESTA_ULTIMO_SISMO,
				uri -> uri.path("/UltimoSismo/MapServer/0/query")
					.queryParam("where", "{where}")
					.queryParam("outFields", "*")
					.queryParam("returnGeometry", false)
					.queryParam("f", "json")
					.build("1=1"));
		return respuesta.features().stream()
			.map(RespuestaArcGis.Feature::attributes)
			.flatMap(atributos -> aSismo(atributos).stream())
			.findFirst();
	}

	public List<Sismo> sismosReportados(FiltroSismos filtro, int pagina, int tamano) {
		if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
			throw new IllegalArgumentException(
					"Página desde 0 y tamaño entre 1 y " + TAMANO_MAXIMO_PAGINA + " (recibido: " + pagina + ", " + tamano + ")");
		}
		RespuestaArcGis<SismoReportadoAtributos> respuesta = consultar(RESPUESTA_SISMOS_REPORTADOS,
				uri -> uri.path(RUTA_SISMOS_REPORTADOS)
					.queryParam("where", "{where}")
					.queryParam("outFields", "*")
					.queryParam("orderByFields", "{orden}")
					.queryParam("resultOffset", (long) pagina * tamano)
					.queryParam("resultRecordCount", tamano)
					.queryParam("returnGeometry", false)
					.queryParam("f", "json")
					.build(where(filtro), "fechaevento DESC"));
		return respuesta.features().stream()
			.map(RespuestaArcGis.Feature::attributes)
			.flatMap(atributos -> aSismo(atributos).stream())
			.toList();
	}

	public long contarSismosReportados(FiltroSismos filtro) {
		RespuestaArcGis<SismoReportadoAtributos> respuesta = consultar(RESPUESTA_SISMOS_REPORTADOS,
				uri -> uri.path(RUTA_SISMOS_REPORTADOS)
					.queryParam("where", "{where}")
					.queryParam("returnCountOnly", true)
					.queryParam("f", "json")
					.build(where(filtro)));
		if (respuesta.count() == null) {
			throw new FuenteNoDisponibleException(Fuente.IGP, "El IGP no devolvió el conteo de sismos", null);
		}
		return respuesta.count();
	}

	// Hay registros con todos los campos nulos, por eso siempre se exige fechaevento.
	static String where(FiltroSismos filtro) {
		List<String> condiciones = new ArrayList<>();
		condiciones.add("fechaevento IS NOT NULL");
		if (filtro.desde() != null) {
			condiciones.add("fechaevento >= TIMESTAMP '" + inicioDelDiaEnUtc(filtro.desde()) + "'");
		}
		if (filtro.hasta() != null) {
			condiciones.add("fechaevento < TIMESTAMP '" + inicioDelDiaEnUtc(filtro.hasta().plusDays(1)) + "'");
		}
		if (filtro.magnitudMinima() != null) {
			condiciones.add("magnitud >= " + BigDecimal.valueOf(filtro.magnitudMinima()).toPlainString());
		}
		String departamento = textoONulo(filtro.departamento());
		if (departamento != null) {
			condiciones.add("departamento = '" + departamento.toUpperCase(LOCALE_PERU).replace("'", "''") + "'");
		}
		return String.join(" AND ", condiciones);
	}

	// ArcGIS compara las fechas en UTC; el día se toma en hora de Lima.
	private static String inicioDelDiaEnUtc(LocalDate dia) {
		return TIMESTAMP_UTC.format(dia.atStartOfDay(ZONA_LIMA));
	}

	private <T> RespuestaArcGis<T> consultar(ParameterizedTypeReference<RespuestaArcGis<T>> tipo,
			Function<UriBuilder, URI> uri) {
		RespuestaArcGis<T> respuesta;
		try {
			respuesta = restClient.get().uri(uri).retrieve().body(tipo);
		}
		catch (RestClientException ex) {
			throw new FuenteNoDisponibleException(Fuente.IGP, "Error al consultar el IGP", ex);
		}
		if (respuesta == null) {
			throw new FuenteNoDisponibleException(Fuente.IGP, "El IGP devolvió una respuesta vacía", null);
		}
		// ArcGIS informa los errores con HTTP 200 y un objeto "error" en el cuerpo.
		if (respuesta.error() != null) {
			throw new FuenteNoDisponibleException(Fuente.IGP, "El IGP devolvió un error: " + respuesta.error().message(),
					null);
		}
		return respuesta;
	}

	private static Optional<Sismo> aSismo(UltimoSismoAtributos atributos) {
		if (atributos == null || atributos.magnitud() == null || atributos.profundidad() == null
				|| atributos.latitud() == null || atributos.longitud() == null || atributos.fechaLocal() == null
				|| atributos.horaLocal() == null) {
			return Optional.empty();
		}
		ZonedDateTime fechaHora;
		try {
			fechaHora = ZonedDateTime.of(LocalDate.parse(atributos.fechaLocal(), FECHA_LOCAL),
					LocalTime.parse(atributos.horaLocal()), ZONA_LIMA);
		}
		catch (DateTimeParseException ex) {
			return Optional.empty();
		}
		String intensidad = textoONulo(atributos.intensidad());
		// Esta capa no trae código, departamento ni "sentido"; se derivan de la referencia y la intensidad.
		return Optional.of(new Sismo(null, fechaHora, atributos.latitud(), atributos.longitud(), atributos.profundidad(),
				atributos.magnitud(), intensidad, textoONulo(atributos.referencia()),
				departamentoDeReferencia(atributos.referencia()), intensidad != null));
	}

	private static Optional<Sismo> aSismo(SismoReportadoAtributos atributos) {
		if (atributos == null || textoONulo(atributos.code()) == null || atributos.fechaevento() == null
				|| atributos.lat() == null || atributos.lon() == null || atributos.prof() == null
				|| atributos.magnitud() == null) {
			return Optional.empty();
		}
		ZonedDateTime fechaHora = Instant.ofEpochMilli(atributos.fechaevento()).atZone(ZONA_LIMA);
		return Optional.of(new Sismo(atributos.code().trim(), fechaHora, atributos.lat(), atributos.lon(),
				atributos.prof(), atributos.magnitud(), textoONulo(atributos.intensidad()), textoONulo(atributos.ref()),
				textoONulo(atributos.departamento()), "1".equals(atributos.sentido())));
	}

	static String departamentoDeReferencia(String referencia) {
		if (referencia == null || !referencia.contains(" - ")) {
			return null;
		}
		return textoONulo(referencia.substring(referencia.lastIndexOf(" - ") + 3).toUpperCase(LOCALE_PERU));
	}

	private static String textoONulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

}
