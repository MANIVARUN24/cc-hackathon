package com.manufacturing.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing one sensor reading stored in MySQL.
 *
 * Maps exactly to the JSON fields sent by the Python gateway:
 *   device, plant, gas_value, gas_status, temperature_c,
 *   humidity_percent, distance_cm, timestamp
 */
@Entity
@Table(
    name = "sensor_readings",
    indexes = {
        @Index(name = "idx_device", columnList = "device"),
        @Index(name = "idx_reading_timestamp", columnList = "reading_timestamp")
    }
)
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Device identifier from the Python gateway JSON (e.g. "ManufacturingPlantMonitor") */
    @Column(name = "device", nullable = false, length = 100)
    private String device;

    /** Plant name from the Python gateway JSON (e.g. "Manufacturing Plant") */
    @Column(name = "plant", nullable = false, length = 150)
    private String plant;

    /** Raw analog value from MQ-2 sensor (0-1023). NOT in ppm. */
    @Column(name = "gas_value")
    private Integer gasValue;

    /** Gas status from MQ-2 digital output: "ALERT" or "NORMAL" */
    @Column(name = "gas_status", length = 30)
    private String gasStatus;

    /** Temperature in Celsius from DHT11 */
    @Column(name = "temperature_c")
    private Double temperatureC;

    /** Relative humidity percentage from DHT11 */
    @Column(name = "humidity_percent")
    private Double humidityPercent;

    /** Distance in centimeters from HC-SR04 ultrasonic sensor */
    @Column(name = "distance_cm")
    private Double distanceCm;

    /** Timestamp of the reading as reported by the Python gateway */
    @Column(name = "reading_timestamp")
    private LocalDateTime readingTimestamp;

    /** Timestamp when this record was inserted into the database */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ========== Constructors ==========

    public SensorReading() {}

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

    public LocalDateTime getReadingTimestamp() {
        return readingTimestamp;
    }

    public void setReadingTimestamp(LocalDateTime readingTimestamp) {
        this.readingTimestamp = readingTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SensorReading{" +
                "id=" + id +
                ", device='" + device + '\'' +
                ", gasValue=" + gasValue +
                ", gasStatus='" + gasStatus + '\'' +
                ", temperatureC=" + temperatureC +
                ", humidityPercent=" + humidityPercent +
                ", distanceCm=" + distanceCm +
                ", readingTimestamp=" + readingTimestamp +
                '}';
    }
}
