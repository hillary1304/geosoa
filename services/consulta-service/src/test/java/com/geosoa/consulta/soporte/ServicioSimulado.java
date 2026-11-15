package com.geosoa.consulta.soporte;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Servicio de dominio falso para pruebas: responde por ruta y registra lo que recibió.
public final class ServicioSimulado implements AutoCloseable {

	public record Respuesta(int estado, String cuerpo, long demoraMs) {
	}

	public record Solicitud(String ruta, String query, String requestId) {
	}

	private final HttpServer servidor;

	private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();

	private final List<Solicitud> recibidas = new CopyOnWriteArrayList<>();

	public ServicioSimulado() {
		try {
			servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		servidor.setExecutor(Executors.newCachedThreadPool());
		servidor.createContext("/", this::atender);
		servidor.start();
	}

	public String url() {
		return "http://localhost:" + servidor.getAddress().getPort();
	}

	public void responder(String ruta, int estado, String cuerpo) {
		respuestas.put(ruta, new Respuesta(estado, cuerpo, 0));
	}

	public void demorar(String ruta, long demoraMs, String cuerpo) {
		respuestas.put(ruta, new Respuesta(200, cuerpo, demoraMs));
	}

	public List<Solicitud> recibidas() {
		return List.copyOf(recibidas);
	}

	public void reiniciar() {
		respuestas.clear();
		recibidas.clear();
	}

	private void atender(HttpExchange intercambio) throws IOException {
		String ruta = intercambio.getRequestURI().getPath();
		recibidas.add(new Solicitud(ruta, intercambio.getRequestURI().getRawQuery(),
				intercambio.getRequestHeaders().getFirst("X-Request-Id")));
		Respuesta respuesta = respuestas.getOrDefault(ruta, new Respuesta(404, "{}", 0));
		try {
			Thread.sleep(respuesta.demoraMs());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
		intercambio.getResponseHeaders().add("Content-Type", "application/json");
		intercambio.sendResponseHeaders(respuesta.estado(), cuerpo.length);
		intercambio.getResponseBody().write(cuerpo);
		intercambio.close();
	}

	@Override
	public void close() {
		servidor.stop(0);
	}

}
