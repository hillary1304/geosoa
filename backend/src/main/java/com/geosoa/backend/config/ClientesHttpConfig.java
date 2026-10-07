package com.geosoa.backend.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientesHttpConfig {

	@Bean
	RestClient igpRestClient(IgpProperties properties) {
		return restClient(properties.url(), properties.connectTimeout(), properties.readTimeout());
	}

	@Bean
	RestClient senamhiRestClient(SenamhiProperties properties) {
		return restClient(properties.url(), properties.connectTimeout(), properties.readTimeout());
	}

	private static RestClient restClient(URI url, Duration connectTimeout, Duration readTimeout) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);
		return RestClient.builder()
			.baseUrl(url.toString())
			.requestFactory(requestFactory)
			.build();
	}

}
