import React from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Legend
} from 'recharts';

/**
 * SensorCharts — renders four time-series line charts using real
 * historical data from GET /api/sensors/history.
 *
 * Charts:
 *   1. Temperature (°C) over time
 *   2. Humidity (%) over time
 *   3. Gas / Smoke Sensor Value over time
 *   4. Distance (cm) over time
 *
 * If data is empty, shows "Waiting for sensor data..." message.
 * NO fake chart data is generated.
 */
export default function SensorCharts({ history, loading }) {
  if (loading) {
    return (
      <div className="charts-loading">
        <div className="spinner" />
        <span>Loading chart data...</span>
      </div>
    );
  }

  if (!history || history.length === 0) {
    return (
      <div className="charts-empty">
        <span className="charts-empty-icon">📊</span>
        <p>Waiting for sensor data...</p>
        <small>Charts will appear once readings are stored in the database.</small>
      </div>
    );
  }

  // Transform history for Recharts — reverse so oldest is on left
  const chartData = [...history].reverse().map((r) => ({
    time: formatTime(r.timestamp),
    temperature: r.temperature_c,
    humidity: r.humidity_percent,
    gas: r.gas_value,
    distance: r.distance_cm,
    status: r.gas_status,
  }));

  // Show at most 100 data points to keep charts readable
  const displayData = chartData.slice(-100);

  return (
    <div className="charts-grid">
      <ChartCard title="Temperature" unit="°C" color="#f59e0b">
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={displayData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="time" tick={tickStyle} interval="preserveStartEnd" />
            <YAxis tick={tickStyle} domain={['auto', 'auto']} unit="°C" />
            <Tooltip contentStyle={tooltipStyle} formatter={(v) => [`${v} °C`, 'Temperature']} />
            <Line
              type="monotone"
              dataKey="temperature"
              stroke="#f59e0b"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      <ChartCard title="Humidity" unit="%" color="#38bdf8">
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={displayData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="time" tick={tickStyle} interval="preserveStartEnd" />
            <YAxis tick={tickStyle} domain={[0, 100]} unit="%" />
            <Tooltip contentStyle={tooltipStyle} formatter={(v) => [`${v} %`, 'Humidity']} />
            <Line
              type="monotone"
              dataKey="humidity"
              stroke="#38bdf8"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      <ChartCard title="Gas / Smoke Sensor Value" unit="" color="#f87171">
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={displayData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="time" tick={tickStyle} interval="preserveStartEnd" />
            <YAxis tick={tickStyle} domain={[0, 1023]} />
            <Tooltip
              contentStyle={tooltipStyle}
              formatter={(v) => [v, 'Gas Sensor Value (raw)']}
            />
            <Line
              type="monotone"
              dataKey="gas"
              stroke="#f87171"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      <ChartCard title="Distance" unit="cm" color="#34d399">
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={displayData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="time" tick={tickStyle} interval="preserveStartEnd" />
            <YAxis tick={tickStyle} domain={['auto', 'auto']} unit=" cm" />
            <Tooltip contentStyle={tooltipStyle} formatter={(v) => [`${v} cm`, 'Distance']} />
            <Line
              type="monotone"
              dataKey="distance"
              stroke="#34d399"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>
    </div>
  );
}

// ========================================================
// Helper components & styles
// ========================================================

function ChartCard({ title, unit, color, children }) {
  return (
    <div className="chart-card">
      <div className="chart-card-header">
        <span className="chart-dot" style={{ background: color }} />
        <h3 className="chart-title">{title}{unit ? ` (${unit})` : ''}</h3>
      </div>
      {children}
    </div>
  );
}

/** Format "yyyy-MM-dd HH:mm:ss" → "HH:mm" for axis labels */
function formatTime(timestamp) {
  if (!timestamp) return '';
  // Extract HH:mm from the timestamp string
  const parts = timestamp.split(' ');
  if (parts.length >= 2) {
    const timeParts = parts[1].split(':');
    return `${timeParts[0]}:${timeParts[1]}`;
  }
  return timestamp;
}

const tickStyle = { fill: '#64748b', fontSize: 11 };
const tooltipStyle = {
  background: '#0f172a',
  border: '1px solid #1e293b',
  borderRadius: '6px',
  color: '#e2e8f0',
  fontSize: '13px',
};
