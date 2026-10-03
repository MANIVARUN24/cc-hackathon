-- ============================================================
-- Manufacturing Plant Monitor - Sample Data (PostgreSQL)
-- ============================================================
-- PURPOSE: For UI/database testing ONLY when hardware is NOT running.
-- The real application uses live MQTT data from the Arduino.
-- DO NOT use this as a substitute for real sensor readings.
-- ============================================================
-- Run inside pgAdmin Query Tool after connecting to manufacturing_monitor,
-- or via psql:
--   psql -U postgres -d manufacturing_monitor -f database/sample-data.sql
-- ============================================================

INSERT INTO sensor_readings
    (device, plant, gas_value, gas_status, temperature_c, humidity_percent, distance_cm, reading_timestamp)
VALUES
-- Normal readings
('ManufacturingPlantMonitor', 'Manufacturing Plant', 142, 'NORMAL', 28.5, 62.0, 45.12, '2026-10-03 18:00:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 156, 'NORMAL', 29.1, 63.5, 44.80, '2026-10-03 18:05:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 148, 'NORMAL', 29.3, 64.0, 44.55, '2026-10-03 18:10:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 162, 'NORMAL', 29.8, 65.0, 43.90, '2026-10-03 18:15:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 171, 'NORMAL', 30.2, 66.5, 43.20, '2026-10-03 18:20:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 180, 'NORMAL', 30.5, 67.0, 42.80, '2026-10-03 18:25:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 198, 'NORMAL', 30.8, 67.5, 42.10, '2026-10-03 18:30:00'),

-- Transition to elevated readings
('ManufacturingPlantMonitor', 'Manufacturing Plant', 310, 'NORMAL', 31.0, 68.0, 41.50, '2026-10-03 18:35:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 420, 'NORMAL', 31.2, 68.5, 40.90, '2026-10-03 18:40:00'),

-- ALERT readings (MQ-2 digital pin went LOW)
('ManufacturingPlantMonitor', 'Manufacturing Plant', 580, 'ALERT',  31.5, 69.0, 22.30, '2026-10-03 18:45:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 635, 'ALERT',  31.8, 70.0,  6.29, '2026-10-03 18:50:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 610, 'ALERT',  31.6, 70.5,  5.80, '2026-10-03 18:55:00'),

-- Recovery
('ManufacturingPlantMonitor', 'Manufacturing Plant', 380, 'NORMAL', 31.3, 69.5, 18.40, '2026-10-03 19:00:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 210, 'NORMAL', 31.0, 68.0, 35.70, '2026-10-03 19:05:00'),
('ManufacturingPlantMonitor', 'Manufacturing Plant', 635, 'ALERT',  31.8, 70.0,  6.29, '2026-10-03 19:07:36');

-- ============================================================
-- Verify (PostgreSQL syntax)
-- ============================================================
SELECT COUNT(*) AS total_readings FROM sensor_readings;
SELECT gas_status, COUNT(*) AS count FROM sensor_readings GROUP BY gas_status;
