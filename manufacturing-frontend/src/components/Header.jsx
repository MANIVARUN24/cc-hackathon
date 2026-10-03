import React from 'react';

/**
 * Header component — displays the application title, subtitle,
 * and system status indicators (Backend, MQTT, Database).
 */
export default function Header({ health, backendOnline }) {
  const mqttStatus = health?.mqtt ?? 'UNKNOWN';
  const dbStatus = health?.database ?? 'UNKNOWN';
  const overallStatus = health?.status ?? (backendOnline ? 'UP' : 'OFFLINE');

  return (
    <header className="app-header">
      <div className="header-brand">
        <div className="header-icon">
          <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
            <rect width="36" height="36" rx="8" fill="#1a6ed8"/>
            <path d="M8 26V16l5-4 5 4v10M18 26V20l5-3 5 3v6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <circle cx="10" cy="10" r="2" fill="#60a5fa"/>
            <circle cx="18" cy="8" r="2" fill="#34d399"/>
            <circle cx="26" cy="10" r="2" fill="#f59e0b"/>
          </svg>
        </div>
        <div className="header-title-group">
          <h1 className="header-title">MANUFACTURING PLANT MONITOR</h1>
          <p className="header-subtitle">AWS Connected Industrial Monitoring System</p>
        </div>
      </div>

      <div className="header-status-bar">
        <StatusPill label="Backend" status={backendOnline ? 'UP' : 'OFFLINE'} />
        <StatusPill label="MQTT" status={mqttStatus} />
        <StatusPill label="Database" status={dbStatus} />
        {health?.mqttMessagesReceived !== undefined && (
          <div className="msg-count">
            <span className="msg-count-num">{health.mqttMessagesReceived}</span>
            <span className="msg-count-label">msgs received</span>
          </div>
        )}
      </div>
    </header>
  );
}

function StatusPill({ label, status }) {
  const isGood = status === 'UP' || status === 'CONNECTED';
  const isBad = status === 'OFFLINE' || status === 'DISCONNECTED';

  return (
    <div className={`status-pill ${isGood ? 'status-good' : isBad ? 'status-bad' : 'status-unknown'}`}>
      <span className="status-dot" />
      <span className="status-label">{label}</span>
      <span className="status-value">{status}</span>
    </div>
  );
}
