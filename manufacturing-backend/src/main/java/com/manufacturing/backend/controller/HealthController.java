package com.manufacturing.backend.controller;

import com.manufacturing.backend.mqtt.MqttSensorListener;
import com.manufacturing.backend.repository.SensorReadingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health endpoint for the React dashboard status indicators.
 *
 * GET /api/health
 *
 * Example response when everything is up:
 * {
 *   "status": "UP",
 *   "mqtt": "CONNECTED",
 *   "database": "CONNECTED",
 *   "mqttMessagesReceived": 42
 * }
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final MqttSensorListener mqttSensorListener;
    private final SensorReadingRepository repository;

    public HealthController(MqttSensorListener mqttSensorListener,
                            SensorReadingRepository repository) {
        this.mqttSensorListener = mqttSensorListener;
        this.repository = repository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Check MQTT connection
        boolean mqttConnected = mqttSensorListener.isConnected();
        String mqttStatus = mqttConnected ? "CONNECTED" : "DISCONNECTED";

        // Check Database connection
        String dbStatus;
        try {
            repository.count(); // lightweight DB probe
            dbStatus = "CONNECTED";
        } catch (Exception e) {
            dbStatus = "DISCONNECTED";
        }

        String overallStatus = (mqttConnected && "CONNECTED".equals(dbStatus)) ? "UP" : "DEGRADED";

        status.put("status", overallStatus);
        status.put("mqtt", mqttStatus);
        status.put("database", dbStatus);
        status.put("mqttMessagesReceived", mqttSensorListener.getMessagesReceived());
        status.put("service", "Manufacturing Plant Monitor");

        return ResponseEntity.ok(status);
    }
}
