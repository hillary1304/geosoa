package com.geosoa.meteorologia.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClienteHttpConfig {

	@Bean
	@Qualifier("senamhiRestClient")
	RestClient senamhiRestClient(SenamhiProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.connectTimeout());
		factory.setReadTimeout(properties.readTimeout());
		return RestClient.builder().baseUrl(properties.url().toString()).requestFactory(factory).build();
	}

}
