import React from 'react';

/**
 * Individual sensor metric card.
 * Displays the sensor name, current value, unit, and optional status badge.
 */
export default function SensorCard({ icon, label, value, unit, status, subtitle }) {
  const isAlert = status === 'ALERT';
  const isNormal = status === 'NORMAL';

  return (
    <div className={`sensor-card ${isAlert ? 'sensor-card--alert' : ''}`}>
      <div className="sensor-card-header">
        <span className="sensor-card-icon">{icon}</span>
        <span className="sensor-card-label">{label}</span>
        {status && (
          <span className={`sensor-badge ${isAlert ? 'badge-alert' : isNormal ? 'badge-normal' : 'badge-unknown'}`}>
            {isAlert && <span className="badge-alert-dot" />}
            {status}
          </span>
        )}
      </div>

      <div className="sensor-card-value">
        {value !== null && value !== undefined ? (
          <>
            <span className="value-number">{value}</span>
            {unit && <span className="value-unit">{unit}</span>}
          </>
        ) : (
          <span className="value-waiting">—</span>
        )}
      </div>

      {subtitle && (
        <div className="sensor-card-subtitle">{subtitle}</div>
      )}
    </div>
  );
}
