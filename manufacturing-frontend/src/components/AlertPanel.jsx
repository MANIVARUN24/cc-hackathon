import React from 'react';

/**
 * AlertPanel — displayed prominently when the latest gas_status is "ALERT".
 *
 * This component only renders if gas_status === "ALERT".
 * It shows the gas value, status, and a clear warning message.
 * It does NOT generate fake alerts.
 */
export default function AlertPanel({ latest }) {
  if (!latest || latest.gas_status !== 'ALERT') return null;

  return (
    <div className="alert-panel" role="alert" aria-live="assertive">
      <div className="alert-panel-inner">
        <div className="alert-icon-wrap">
          <span className="alert-icon">⚠</span>
        </div>
        <div className="alert-content">
          <h2 className="alert-title">GAS / SMOKE ALERT</h2>
          <div className="alert-details">
            <div className="alert-detail-item">
              <span className="alert-detail-label">Device</span>
              <span className="alert-detail-value">{latest.device}</span>
            </div>
            <div className="alert-detail-item">
              <span className="alert-detail-label">Gas Sensor Status</span>
              <span className="alert-detail-value alert-status-text">ALERT</span>
            </div>
            <div className="alert-detail-item">
              <span className="alert-detail-label">Gas / Smoke Sensor Value</span>
              <span className="alert-detail-value alert-value-num">{latest.gas_value}</span>
            </div>
            <div className="alert-detail-item">
              <span className="alert-detail-label">Detected At</span>
              <span className="alert-detail-value">{latest.timestamp}</span>
            </div>
          </div>
          <p className="alert-note">
            MQ-2 digital output is <strong>LOW</strong> — gas or smoke detected above threshold.
            Check the manufacturing area immediately.
          </p>
        </div>
      </div>
    </div>
  );
}
