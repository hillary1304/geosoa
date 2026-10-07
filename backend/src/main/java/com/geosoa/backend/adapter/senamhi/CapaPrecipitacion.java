package com.geosoa.backend.adapter.senamhi;

public enum CapaPrecipitacion {

	ENERO(1), FEBRERO(2), MARZO(3), ABRIL(4), MAYO(5), JUNIO(6), JULIO(7), AGOSTO(8), SETIEMBRE(9), OCTUBRE(10),
	NOVIEMBRE(11), DICIEMBRE(12), ANUAL(13), VERANO(14), OTONO(15), INVIERNO(16), PRIMAVERA(17);

	static final String WORKSPACE = "g_05_02";

	private final int codigo;

	CapaPrecipitacion(int codigo) {
		this.codigo = codigo;
	}

	String typeName() {
		return WORKSPACE + ":05_02_%03d_03_001_512_0000_00_00".formatted(codigo);
	}

}
