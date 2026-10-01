package com.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CafeReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(CafeReservationApplication.class, args);
	}

}
