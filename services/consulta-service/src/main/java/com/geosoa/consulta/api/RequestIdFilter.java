package com.geosoa.consulta.api;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.geosoa.consulta.ruta.LlamadasServiciosRoute;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Conserva el X-Request-Id recibido si es seguro; si no, genera uno para propagarlo a las dependencias.
@Component
class RequestIdFilter extends OncePerRequestFilter {

	static final String ATRIBUTO = "geosoa.requestId";

	private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String recibido = request.getHeader(LlamadasServiciosRoute.REQUEST_ID);
		String id = recibido != null && VALIDO.matcher(recibido).matches() ? recibido : UUID.randomUUID().toString();
		request.setAttribute(ATRIBUTO, id);
		response.setHeader(LlamadasServiciosRoute.REQUEST_ID, id);
		chain.doFilter(request, response);
	}

}
