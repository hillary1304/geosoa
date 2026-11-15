package com.geosoa.consulta.api;

import java.time.Instant;
import java.util.List;

public record ErrorApi(Instant timestamp, int status, String error, String mensaje, String ruta, List<String> detalles) {
}
