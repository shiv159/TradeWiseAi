package com.tradewise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TradewiseaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradewiseaiApplication.class, args);
	}

}
