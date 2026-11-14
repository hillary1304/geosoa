package com.geosoa.territorio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TerritorioApplication {

	public static void main(String[] args) {
		SpringApplication.run(TerritorioApplication.class, args);
	}

}
