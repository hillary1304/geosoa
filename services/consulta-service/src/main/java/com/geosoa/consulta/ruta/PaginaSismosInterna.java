package com.geosoa.consulta.ruta;

import java.util.List;

import com.geosoa.consulta.domain.Procedencia;
import com.geosoa.consulta.domain.Sismo;

// Forma del contrato interno de sismos-service; no se expone tal cual.
record PaginaSismosInterna(List<Sismo> elementos, int pagina, int tamano, Long total, Procedencia procedencia) {
}
