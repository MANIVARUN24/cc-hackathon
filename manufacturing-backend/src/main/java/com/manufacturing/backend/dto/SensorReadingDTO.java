package com.manufacturing.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Data Transfer Object that maps 1-to-1 with the JSON published
 * by the Python gateway to AWS IoT Core.
 *
 * Exact JSON field names from the gateway:
 *   device, plant, gas_value, gas_status, temperature_c,
 *   humidity_percent, distance_cm, timestamp
 *
 * Also used as the response body for REST API endpoints.
 */
public class SensorReadingDTO {

    /** Database row ID (null for incoming MQTT messages) */
    private Long id;

    /** e.g. "ManufacturingPlantMonitor" */
    @JsonProperty("device")
    private String device;

    /** e.g. "Manufacturing Plant" */
    @JsonProperty("plant")
    private String plant;

    /**
     * Raw MQ-2 analog value (0–1023).
     * DO NOT label this as ppm.
     */
    @JsonProperty("gas_value")
    private Integer gasValue;

    /**
     * MQ-2 digital output status.
     * Values: "ALERT" or "NORMAL"
     * ALERT = digital pin LOW (gas detected above threshold)
     */
    @JsonProperty("gas_status")
    private String gasStatus;

    /** DHT11 temperature in Celsius */
    @JsonProperty("temperature_c")
    private Double temperatureC;

    /** DHT11 relative humidity percentage */
    @JsonProperty("humidity_percent")
    private Double humidityPercent;

    /** HC-SR04 distance in centimeters */
    @JsonProperty("distance_cm")
    private Double distanceCm;

    /**
     * Timestamp string from Python gateway (e.g. "2026-10-03 19:07:36")
     * Stored/returned as LocalDateTime after parsing.
     */
    @JsonProperty("timestamp")
    private String timestamp;

    // ========== Constructors ==========

    public SensorReadingDTO() {}

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getPlant() {
        return plant;
    }

    public void setPlant(String plant) {
        this.plant = plant;
    }

    public Integer getGasValue() {
        return gasValue;
    }

    public void setGasValue(Integer gasValue) {
        this.gasValue = gasValue;
    }

    public String getGasStatus() {
        return gasStatus;
    }

    public void setGasStatus(String gasStatus) {
        this.gasStatus = gasStatus;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Double getHumidityPercent() {
        return humidityPercent;
    }

    public void setHumidityPercent(Double humidityPercent) {
        this.humidityPercent = humidityPercent;
    }

    public Double getDistanceCm() {
        return distanceCm;
    }

    public void setDistanceCm(Double distanceCm) {
        this.distanceCm = distanceCm;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SensorReadingDTO{" +
                "device='" + device + '\'' +
                ", gasValue=" + gasValue +
                ", gasStatus='" + gasStatus + '\'' +
                ", temperatureC=" + temperatureC +
                ", humidityPercent=" + humidityPercent +
                ", distanceCm=" + distanceCm +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
