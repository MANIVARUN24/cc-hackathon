package com.manufacturing.backend.repository;

import com.manufacturing.backend.entity.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SensorReading entities.
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    /**
     * Returns the most recent sensor reading (by reading_timestamp DESC).
     */
    Optional<SensorReading> findTopByOrderByReadingTimestampDesc();

    /**
     * Returns the N most recent readings, newest first.
     */
    List<SensorReading> findAllByOrderByReadingTimestampDesc(Pageable pageable);

    /**
     * Returns all readings where gas_status = 'ALERT', newest first.
     */
    List<SensorReading> findByGasStatusOrderByReadingTimestampDesc(String gasStatus, Pageable pageable);

    /**
     * Returns count of ALERT records.
     */
    long countByGasStatus(String gasStatus);

    /**
     * Returns average temperature from last N records.
     */
    @Query("SELECT AVG(s.temperatureC) FROM SensorReading s")
    Double findAverageTemperature();

    /**
     * Returns average humidity from all records.
     */
    @Query("SELECT AVG(s.humidityPercent) FROM SensorReading s")
    Double findAverageHumidity();

    /**
     * Returns the maximum gas value recorded.
     */
    @Query("SELECT MAX(s.gasValue) FROM SensorReading s")
    Integer findMaxGasValue();

    /**
     * Returns average distance from all records.
     */
    @Query("SELECT AVG(s.distanceCm) FROM SensorReading s")
    Double findAverageDistance();
}
