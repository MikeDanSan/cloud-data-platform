package com.michael.backendservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendServiceApplication {

	private static final Logger log = 
		LoggerFactory.getLogger(BackendServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BackendServiceApplication.class, args);
		log.info("Backend Service started successfully");
	}

}
