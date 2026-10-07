package com.geosoa.backend.adapter.igp;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

	private static final ParameterizedTypeReference<RespuestaArcGis<UltimoSismoAtributos>> RESPUESTA_ULTIMO_SISMO =
			new ParameterizedTypeReference<>() {
			};

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
