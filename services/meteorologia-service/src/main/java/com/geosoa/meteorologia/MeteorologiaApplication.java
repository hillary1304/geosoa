package com.geosoa.meteorologia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeteorologiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeteorologiaApplication.class, args);
	}

}
