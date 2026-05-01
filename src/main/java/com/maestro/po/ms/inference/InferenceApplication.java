package com.maestro.po.ms.inference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the DocMaestroEngine Spring Boot application.
 * Enables async execution, retry support, and scheduled task processing.
 *
 * @author Ganesh Punde
 */
@Configuration
@SpringBootApplication
@ComponentScan(basePackages = { "com.maestro.po.ms.inference" })
@EnableAsync
@EnableRetry
@EnableScheduling
public class InferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InferenceApplication.class, args);
	}

}
