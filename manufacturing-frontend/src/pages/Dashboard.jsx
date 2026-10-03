import React, { useState, useEffect, useCallback } from 'react';
import Header from '../components/Header';
import SensorCard from '../components/SensorCard';
import AlertPanel from '../components/AlertPanel';
import SensorTable from '../components/SensorTable';
import SensorCharts from '../components/SensorCharts';
import { fetchLatest, fetchHistory, fetchHealth } from '../services/api';

// ============================================================
// Polling intervals
// ============================================================
const LATEST_POLL_MS = 5000;   // Fetch latest reading every 5 seconds
const HISTORY_POLL_MS = 15000; // Fetch history every 15 seconds
const HEALTH_POLL_MS  = 8000;  // Fetch health every 8 seconds

/**
 * Dashboard — main page of the Manufacturing Plant Monitor.
 *
 * Displays:
 * - System status header (Backend, MQTT, Database)
 * - Gas ALERT panel (only when gas_status = ALERT)
 * - Sensor metric cards (Temperature, Humidity, Gas, Distance)
 * - Statistics row
 * - Time-series charts
 * - Sensor history table
 *
 * All data comes from the Spring Boot REST API.
 * NO hardcoded sensor values.
 */
export default function Dashboard() {
  const [latest, setLatest] = useState(null);
  const [history, setHistory] = useState([]);
  const [health, setHealth] = useState(null);
  const [backendOnline, setBackendOnline] = useState(false);
  const [latestLoading, setLatestLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [error, setError] = useState(null);

  // ---- Fetch latest reading ----
  const loadLatest = useCallback(async () => {
    try {
      const data = await fetchLatest();
      // data might be a "message" object if DB is empty
      if (data && data.device) {
        setLatest(data);
        setError(null);
      } else {
        setLatest(null);
      }
      setBackendOnline(true);
      setLastUpdated(new Date());
    } catch {
      setBackendOnline(false);
      setError('Backend unavailable. Make sure Spring Boot is running on port 8080.');
    } finally {
      setLatestLoading(false);
    }
  }, []);

  // ---- Fetch history ----
  const loadHistory = useCallback(async () => {
    try {
      const data = await fetchHistory(200);
      setHistory(Array.isArray(data) ? data : []);
    } catch {
      // Don't update error state here — already shown by loadLatest
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  // ---- Fetch health ----
  const loadHealth = useCallback(async () => {
    try {
      const data = await fetchHealth();
      setHealth(data);
    } catch {
      setHealth(null);
    }
  }, []);

  // ---- Initial load ----
  useEffect(() => {
    loadLatest();
    loadHistory();
    loadHealth();
  }, [loadLatest, loadHistory, loadHealth]);

  // ---- Polling ----
  useEffect(() => {
    const latestTimer  = setInterval(loadLatest,  LATEST_POLL_MS);
    const historyTimer = setInterval(loadHistory, HISTORY_POLL_MS);
    const healthTimer  = setInterval(loadHealth,  HEALTH_POLL_MS);
    return () => {
      clearInterval(latestTimer);
      clearInterval(historyTimer);
      clearInterval(healthTimer);
    };
  }, [loadLatest, loadHistory, loadHealth]);

  // ============================================================
  // Render
  // ============================================================
  return (
    <div className="app">
      <Header health={health} backendOnline={backendOnline} />

      <main className="dashboard">

        {/* Backend offline banner */}
        {!backendOnline && !latestLoading && (
          <div className="offline-banner">
            <span>⚠</span>
            <span>Backend unavailable — make sure Spring Boot is running on <code>http://localhost:8080</code></span>
          </div>
        )}

        {/* Gas Alert panel — only shown when gas_status = ALERT */}
        {latest && <AlertPanel latest={latest} />}

        {/* Last updated indicator */}
        {lastUpdated && (
          <div className="last-updated">
            Last refreshed: {lastUpdated.toLocaleTimeString()} &nbsp;·&nbsp;
            Auto-refreshes every {LATEST_POLL_MS / 1000}s
          </div>
        )}

        {/* ======= Sensor Cards ======= */}
        <section className="section">
          <h2 className="section-title">Live Sensor Readings</h2>
          {latestLoading ? (
            <div className="cards-loading"><div className="spinner" /> <span>Connecting to backend...</span></div>
          ) : !latest ? (
            <div className="cards-empty">
              <span>📡 Waiting for sensor data...</span>
              <small>Data will appear once the Arduino and Python gateway are running and connected to AWS IoT Core.</small>
            </div>
          ) : (
            <div className="cards-grid">
              <SensorCard
                icon="🌡"
                label="TEMPERATURE"
                value={latest.temperature_c?.toFixed(1)}
                unit="°C"
                subtitle={`Device: ${latest.device}`}
              />
              <SensorCard
                icon="💧"
                label="HUMIDITY"
                value={latest.humidity_percent?.toFixed(1)}
                unit="%"
                subtitle="Relative humidity (DHT11)"
              />
              <SensorCard
                icon="💨"
                label="GAS / SMOKE"
                value={latest.gas_value}
                unit=""
                status={latest.gas_status}
                subtitle="MQ-2 raw sensor value (0–1023)"
              />
              <SensorCard
                icon="📏"
                label="DISTANCE"
                value={latest.distance_cm?.toFixed(2)}
                unit="cm"
                subtitle="HC-SR04 ultrasonic sensor"
              />
            </div>
          )}
        </section>

        {/* ======= Charts ======= */}
        <section className="section">
          <h2 className="section-title">Sensor History — Charts</h2>
          <SensorCharts history={history} loading={historyLoading} />
        </section>

        {/* ======= Sensor History Table ======= */}
        <section className="section">
          <div className="section-header-row">
            <h2 className="section-title">Sensor History — Table</h2>
            <span className="history-count">{history.length} records</span>
          </div>
          <SensorTable readings={history} loading={historyLoading} />
        </section>

        {/* ======= Footer ======= */}
        <footer className="dashboard-footer">
          <p>
            AWS Direct Connect Design Study for a Manufacturing Plant &nbsp;·&nbsp;
            Prototype: Arduino → Python → AWS IoT Core → Spring Boot → MySQL → React
          </p>
          <p className="footer-note">
            Production architecture uses AWS Direct Connect for private, dedicated connectivity
            from the plant network to the AWS VPC.
          </p>
        </footer>

      </main>
    </div>
  );
}
