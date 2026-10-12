package com.geosoa.backend.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrquestadorConfig {

	@Bean(destroyMethod = "close")
	ExecutorService orquestadorExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

}
