package com.manufacturing.backend.controller;

import com.manufacturing.backend.dto.SensorReadingDTO;
import com.manufacturing.backend.service.SensorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing sensor data endpoints for the React dashboard.
 *
 * All endpoints return JSON.
 * CORS is handled by CorsConfig (allows http://localhost:5173).
 */
@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    /**
     * GET /api/sensors/latest
     * Returns the most recent sensor reading.
     *
     * Example response:
     * {
     *   "device": "ManufacturingPlantMonitor",
     *   "plant": "Manufacturing Plant",
     *   "gas_value": 635,
     *   "gas_status": "ALERT",
     *   "temperature_c": 31.8,
     *   "humidity_percent": 70.0,
     *   "distance_cm": 6.29,
     *   "timestamp": "2026-10-03 19:07:36"
     * }
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        return sensorService.getLatestReading()
                .map(dto -> ResponseEntity.ok((Object) dto))
                .orElse(ResponseEntity.ok(Map.of("message", "No sensor data yet. Waiting for first reading.")));
    }

    /**
     * GET /api/sensors
     * Returns the 50 most recent sensor readings.
     */
    @GetMapping
    public ResponseEntity<List<SensorReadingDTO>> getRecent(
            @RequestParam(defaultValue = "50") int limit) {
        List<SensorReadingDTO> readings = sensorService.getRecentReadings(
                Math.min(limit, 500)); // cap at 500
        return ResponseEntity.ok(readings);
    }

    /**
     * GET /api/sensors/history
     * Returns historical sensor readings (up to 200).
     */
    @GetMapping("/history")
    public ResponseEntity<List<SensorReadingDTO>> getHistory(
            @RequestParam(defaultValue = "200") int limit) {
        List<SensorReadingDTO> history = sensorService.getHistory(
                Math.min(limit, 1000)); // cap at 1000
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/sensors/alerts
     * Returns sensor readings where gas_status = 'ALERT'.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<SensorReadingDTO>> getAlerts(
            @RequestParam(defaultValue = "100") int limit) {
        List<SensorReadingDTO> alerts = sensorService.getAlerts(
                Math.min(limit, 500));
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/sensors/stats
     * Returns aggregate statistics.
     *
     * Example response:
     * {
     *   "totalReadings": 150,
     *   "alertCount": 23,
     *   "normalCount": 127,
     *   "alertPercentage": 15,
     *   "averageTemperatureC": 31.4,
     *   "averageHumidityPercent": 68.2,
     *   "maxGasValue": 635,
     *   "averageDistanceCm": 12.4,
     *   "latestGasStatus": "ALERT",
     *   "latestReadingTimestamp": "2026-10-03 19:07:36"
     * }
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(sensorService.getStats());
    }
}
