import React from 'react';
import StatusBadge from './StatusBadge';

/**
 * SensorTable — displays recent/historical sensor readings in a table.
 * Columns: Timestamp | Temperature | Humidity | Gas Value | Gas Status | Distance
 *
 * Props:
 *   readings: array of sensor reading objects from the API
 *   loading:  boolean
 */
export default function SensorTable({ readings, loading }) {
  if (loading) {
    return (
      <div className="table-state">
        <div className="spinner" />
        <span>Loading sensor history...</span>
      </div>
    );
  }

  if (!readings || readings.length === 0) {
    return (
      <div className="table-state">
        <span className="table-empty-icon">📡</span>
        <span>Waiting for sensor data...</span>
        <small>Data will appear here once the Arduino and Python gateway are running.</small>
      </div>
    );
  }

  return (
    <div className="table-wrapper">
      <table className="sensor-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Temp (°C)</th>
            <th>Humidity (%)</th>
            <th>Gas / Smoke Value</th>
            <th>Gas Status</th>
            <th>Distance (cm)</th>
          </tr>
        </thead>
        <tbody>
          {readings.map((r, i) => (
            <tr key={r.id ?? i} className={r.gas_status === 'ALERT' ? 'row-alert' : ''}>
              <td className="td-timestamp">{r.timestamp ?? '—'}</td>
              <td>{r.temperature_c != null ? r.temperature_c.toFixed(1) : '—'}</td>
              <td>{r.humidity_percent != null ? r.humidity_percent.toFixed(1) : '—'}</td>
              <td className="td-gas">{r.gas_value ?? '—'}</td>
              <td><StatusBadge status={r.gas_status} /></td>
              <td>{r.distance_cm != null ? r.distance_cm.toFixed(2) : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
