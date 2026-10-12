package com.geosoa.backend.adapter.inei;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.geosoa.backend.domain.Dato;
import com.geosoa.backend.domain.Distrito;
import com.geosoa.backend.domain.Origen;
import com.geosoa.backend.domain.Poblacion;
import com.geosoa.backend.persistence.DistritoEntity;
import com.geosoa.backend.persistence.DistritoRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IneiRespaldoTests {

	private static final Distrito OESTE = new Distrito("150101", "LIMA", "LIMA", "OESTE");

	private static final Distrito ESTE = new Distrito("150102", "LIMA", "LIMA", "ESTE");

	@Autowired
	private DistritoRepository repository;

	private IneiRespaldo respaldo;

	@BeforeEach
	void configurar() {
		respaldo = new IneiRespaldo(repository);
		guardar(OESTE, cuadrado(-77.10, -12.10, -77.00, -12.00), 1000);
		guardar(ESTE, cuadrado(-76.90, -12.10, -76.80, -12.00), 2000);
	}

	@Test
	void ubicaElDistritoQueContieneElPunto() {
		Dato<Optional<Distrito>> dato = respaldo.distrito(-12.05, -77.03).orElseThrow();

		assertThat(dato.valor()).contains(OESTE);
		assertThat(dato.procedencia().origen()).isEqualTo(Origen.DB);
		assertThat(dato.procedencia().actualizadoEn()).isNotNull();
	}

	@Test
	void sinDistritoAlmacenadoNoHayRespaldo() {
		assertThat(respaldo.distrito(-12.05, -76.95)).isEmpty();
		assertThat(respaldo.distrito(-15.0, -75.0)).isEmpty();
	}

	@Test
	void devuelveLaPoblacionAlmacenada() {
		assertThat(respaldo.poblacion("150102").orElseThrow().valor()).contains(new Poblacion("150102", 2000, 9.0));
		assertThat(respaldo.poblacion("999999")).isEmpty();
	}

	@Test
	void devuelveLosDistritosAlmacenadosDentroDelRadio() {
		assertThat(nombres(respaldo.distritosEnRadio(-12.05, -77.03, 10).orElseThrow())).containsExactly("OESTE");
		assertThat(nombres(respaldo.distritosEnRadio(-12.05, -77.03, 20).orElseThrow()))
			.containsExactlyInAnyOrder("OESTE", "ESTE");
		assertThat(respaldo.distritosEnRadio(-15.0, -75.0, 20)).isEmpty();
	}

	@Test
	void unPuntoEnUnHuecoNoPerteneceAlDistrito() {
		Multipoligono conHueco = Multipoligono.desdeJson("""
				[[[[-77.1,-12.1],[-77.0,-12.1],[-77.0,-12.0],[-77.1,-12.0],[-77.1,-12.1]],
				[[-77.04,-12.06],[-77.02,-12.06],[-77.02,-12.04],[-77.04,-12.04],[-77.04,-12.06]]]]""");

		assertThat(conHueco.contiene(-12.05, -77.03)).isFalse();
		assertThat(conHueco.contiene(-12.08, -77.08)).isTrue();
		assertThat(conHueco.distanciaKm(-12.08, -77.08)).isZero();
	}

	private void guardar(Distrito distrito, String limites, long poblacion) {
		DistritoEntity entity = DistritoEntity.from(distrito);
		entity.asignarPoblacion(new Poblacion(distrito.ubigeo(), poblacion, 9.0));
		Multipoligono.Envolvente envolvente = Multipoligono.desdeJson(limites).envolvente();
		entity.asignarLimites(limites, envolvente.minLongitud(), envolvente.minLatitud(), envolvente.maxLongitud(),
				envolvente.maxLatitud());
		repository.saveAndFlush(entity);
	}

	private static String cuadrado(double minLon, double minLat, double maxLon, double maxLat) {
		return "[[[[%s,%s],[%s,%s],[%s,%s],[%s,%s],[%s,%s]]]]".formatted(minLon, minLat, maxLon, minLat, maxLon, maxLat,
				minLon, maxLat, minLon, minLat);
	}

	private static List<String> nombres(Dato<List<DistritoPoblado>> dato) {
		return dato.valor().stream().map(d -> d.distrito().nombre()).toList();
	}

}
