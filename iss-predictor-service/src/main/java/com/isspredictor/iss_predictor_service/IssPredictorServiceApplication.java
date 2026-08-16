package com.isspredictor.iss_predictor_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the ISS Location & Pass Predictor microservice.
 */
@SpringBootApplication
@EnableScheduling
public class IssPredictorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IssPredictorServiceApplication.class, args);
    }
}
