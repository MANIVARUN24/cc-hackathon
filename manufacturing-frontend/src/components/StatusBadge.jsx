import React from 'react';

/**
 * StatusBadge — small inline badge showing gas_status value.
 * ALERT = red/amber, NORMAL = green, other = grey.
 */
export default function StatusBadge({ status }) {
  if (!status) return <span className="badge badge-unknown">—</span>;

  const isAlert = status === 'ALERT';
  const isNormal = status === 'NORMAL';

  return (
    <span className={`badge ${isAlert ? 'badge-alert' : isNormal ? 'badge-normal' : 'badge-unknown'}`}>
      {isAlert && '⚠ '}
      {status}
    </span>
  );
}
