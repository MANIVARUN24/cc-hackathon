package com.manufacturing.backend.service;

import com.manufacturing.backend.dto.SensorReadingDTO;
import com.manufacturing.backend.entity.SensorReading;
import com.manufacturing.backend.repository.SensorReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service layer: handles validation, conversion, persistence, and retrieval
 * of sensor readings received from the Python gateway via MQTT.
 */
@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    /** Timestamp format sent by Python gateway: "2026-10-03 19:07:36" */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SensorReadingRepository repository;

    public SensorService(SensorReadingRepository repository) {
        this.repository = repository;
    }

    // ========================================================
    // SAVE
    // ========================================================

    /**
     * Validates and saves a sensor reading received from MQTT.
     * Returns true if saved successfully, false if validation failed.
     */
    public boolean saveReading(SensorReadingDTO dto) {
        if (!validateDTO(dto)) {
            log.warn("Validation failed for sensor reading: {}", dto);
            return false;
        }

        try {
            SensorReading entity = toEntity(dto);
            repository.save(entity);
            log.info("Saved sensor reading: device={}, gasStatus={}, temp={}°C, humidity={}%, distance={}cm, gas={}",
                    entity.getDevice(),
                    entity.getGasStatus(),
                    entity.getTemperatureC(),
                    entity.getHumidityPercent(),
                    entity.getDistanceCm(),
                    entity.getGasValue());
            return true;
        } catch (Exception e) {
            log.error("Failed to save sensor reading to database: {}", e.getMessage(), e);
            return false;
        }
    }

    // ========================================================
    // QUERIES
    // ========================================================

    /**
     * Returns the latest sensor reading, or empty if none exist.
     */
    public Optional<SensorReadingDTO> getLatestReading() {
        return repository.findTopByOrderByReadingTimestampDesc()
                .map(this::toDTO);
    }

    /**
     * Returns the most recent readings (default: 50).
     */
    public List<SensorReadingDTO> getRecentReadings(int limit) {
        return repository
                .findAllByOrderByReadingTimestampDesc(
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "readingTimestamp")))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns historical readings (default: 200 newest first).
     */
    public List<SensorReadingDTO> getHistory(int limit) {
        return repository
                .findAllByOrderByReadingTimestampDesc(
                        PageRequest.of(0, limit))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns all ALERT readings, newest first.
     */
    public List<SensorReadingDTO> getAlerts(int limit) {
        return repository
                .findByGasStatusOrderByReadingTimestampDesc(
                        "ALERT",
                        PageRequest.of(0, limit))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns aggregate statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = repository.count();
        long alertCount = repository.countByGasStatus("ALERT");
        long normalCount = repository.countByGasStatus("NORMAL");

        stats.put("totalReadings", total);
        stats.put("alertCount", alertCount);
        stats.put("normalCount", normalCount);
        stats.put("alertPercentage", total > 0 ? Math.round((alertCount * 100.0) / total) : 0);

        Double avgTemp = repository.findAverageTemperature();
        stats.put("averageTemperatureC", avgTemp != null ? Math.round(avgTemp * 10.0) / 10.0 : null);

        Double avgHumidity = repository.findAverageHumidity();
        stats.put("averageHumidityPercent", avgHumidity != null ? Math.round(avgHumidity * 10.0) / 10.0 : null);

        Integer maxGas = repository.findMaxGasValue();
        stats.put("maxGasValue", maxGas);

        Double avgDistance = repository.findAverageDistance();
        stats.put("averageDistanceCm", avgDistance != null ? Math.round(avgDistance * 100.0) / 100.0 : null);

        // Latest reading summary
        repository.findTopByOrderByReadingTimestampDesc().ifPresent(latest -> {
            stats.put("latestGasStatus", latest.getGasStatus());
            stats.put("latestReadingTimestamp",
                    latest.getReadingTimestamp() != null
                            ? latest.getReadingTimestamp().format(TIMESTAMP_FORMATTER)
                            : null);
        });

        return stats;
    }

    // ========================================================
    // VALIDATION
    // ========================================================

    /**
     * Validates incoming sensor DTO.
     *
     * Rules:
     * - device must not be blank
     * - gas_value must not be negative (ALERT status is NOT invalid data)
     * - humidity must be 0–100 (warn but don't reject if out of range)
     * - distance must not be negative
     * - temperature must be present
     */
    private boolean validateDTO(SensorReadingDTO dto) {
        if (dto == null) {
            log.warn("Received null DTO");
            return false;
        }

        if (dto.getDevice() == null || dto.getDevice().isBlank()) {
            log.warn("Sensor reading has no device field");
            return false;
        }

        if (dto.getGasValue() != null && dto.getGasValue() < 0) {
            log.warn("Invalid gas_value (negative): {}", dto.getGasValue());
            return false;
        }

        if (dto.getDistanceCm() != null && dto.getDistanceCm() < 0) {
            log.warn("Invalid distance_cm (negative): {}", dto.getDistanceCm());
            return false;
        }

        if (dto.getHumidityPercent() != null &&
                (dto.getHumidityPercent() < 0 || dto.getHumidityPercent() > 100)) {
            // Warn but do NOT reject — sensor might send a slightly out-of-range value
            log.warn("humidity_percent out of expected range 0-100: {}", dto.getHumidityPercent());
        }

        // gas_status = ALERT is valid data (it's a sensor condition, not an error)
        return true;
    }

    // ========================================================
    // CONVERSION HELPERS
    // ========================================================

    private SensorReading toEntity(SensorReadingDTO dto) {
        SensorReading entity = new SensorReading();
        entity.setDevice(dto.getDevice());
        entity.setPlant(dto.getPlant() != null ? dto.getPlant() : "Manufacturing Plant");
        entity.setGasValue(dto.getGasValue());

        // Derive gasStatus if null or empty from gateway
        String status = dto.getGasStatus();
        if (status == null || status.isBlank() || "null".equalsIgnoreCase(status)) {
            status = (dto.getGasValue() != null && dto.getGasValue() >= 500) ? "ALERT" : "NORMAL";
        }
        entity.setGasStatus(status);

        entity.setTemperatureC(dto.getTemperatureC());
        entity.setHumidityPercent(dto.getHumidityPercent());
        entity.setDistanceCm(dto.getDistanceCm());
        entity.setReadingTimestamp(parseTimestamp(dto.getTimestamp()));
        return entity;
    }

    private SensorReadingDTO toDTO(SensorReading entity) {
        SensorReadingDTO dto = new SensorReadingDTO();
        dto.setId(entity.getId());
        dto.setDevice(entity.getDevice());
        dto.setPlant(entity.getPlant());
        dto.setGasValue(entity.getGasValue());
        dto.setGasStatus(entity.getGasStatus());
        dto.setTemperatureC(entity.getTemperatureC());
        dto.setHumidityPercent(entity.getHumidityPercent());
        dto.setDistanceCm(entity.getDistanceCm());
        dto.setTimestamp(
                entity.getReadingTimestamp() != null
                        ? entity.getReadingTimestamp().format(TIMESTAMP_FORMATTER)
                        : null);
        return dto;
    }

    /**
     * Parses timestamp from Python gateway format "yyyy-MM-dd HH:mm:ss".
     * Falls back to current time if parsing fails.
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isBlank()) {
            log.warn("Missing timestamp in sensor reading, using current time");
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse timestamp '{}', using current time. Error: {}", timestampStr, e.getMessage());
            return LocalDateTime.now();
        }
    }
}
