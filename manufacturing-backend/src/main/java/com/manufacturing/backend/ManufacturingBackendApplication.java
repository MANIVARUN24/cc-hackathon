package com.manufacturing.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Manufacturing Plant Monitor - Spring Boot Application Entry Point
 *
 * This backend:
 * 1. Subscribes to AWS IoT Core MQTT topic (sdk/test/python)
 * 2. Receives real sensor readings from the Python gateway
 * 3. Stores readings in MySQL database
 * 4. Exposes REST API for the React dashboard
 *
 * Architecture:
 * Arduino → Python Gateway → AWS IoT Core (MQTT) → This Backend → MySQL → React
 */
@SpringBootApplication
@EnableScheduling
public class ManufacturingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManufacturingBackendApplication.class, args);
    }
}
