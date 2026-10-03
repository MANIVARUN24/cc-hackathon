/**
 * API service layer for the Manufacturing Plant Monitor dashboard.
 * All requests go to the Spring Boot backend at http://localhost:8080.
 *
 * DO NOT hardcode sensor values. All data comes from the backend REST API,
 * which in turn receives real MQTT data from the Python gateway.
 */

const BASE_URL = '/api';

/**
 * Fetch the latest sensor reading.
 * GET /api/sensors/latest
 */
export async function fetchLatest() {
  const response = await fetch(`${BASE_URL}/sensors/latest`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

/**
 * Fetch recent sensor readings (default 50).
 * GET /api/sensors?limit=50
 */
export async function fetchRecent(limit = 50) {
  const response = await fetch(`${BASE_URL}/sensors?limit=${limit}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

/**
 * Fetch historical sensor readings for charts (default 200).
 * GET /api/sensors/history?limit=200
 */
export async function fetchHistory(limit = 200) {
  const response = await fetch(`${BASE_URL}/sensors/history?limit=${limit}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

/**
 * Fetch all ALERT readings.
 * GET /api/sensors/alerts
 */
export async function fetchAlerts(limit = 100) {
  const response = await fetch(`${BASE_URL}/sensors/alerts?limit=${limit}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

/**
 * Fetch aggregate statistics.
 * GET /api/sensors/stats
 */
export async function fetchStats() {
  const response = await fetch(`${BASE_URL}/sensors/stats`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

/**
 * Fetch system health status (MQTT + Database connection status).
 * GET /api/health
 */
export async function fetchHealth() {
  const response = await fetch(`${BASE_URL}/health`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}
