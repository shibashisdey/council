package com.council.availabilityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AvailabilityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvailabilityServiceApplication.class, args);
	}

}
