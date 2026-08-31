import React from 'react';
import { Link } from 'react-router-dom';

/**
 * Issue 2: Lifecycle-aware status transition map.
 * Frontend mirrors backend transitions for UX only — backend remains the source of truth.
 */
const VALID_TRANSITIONS = {
  SAVED:     ['SAVED', 'APPLIED', 'WITHDRAWN'],
  APPLIED:   ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'],
  SCREENING: ['SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'],
  INTERVIEW: ['INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'],
  OFFER:     ['OFFER', 'WITHDRAWN'],
  REJECTED:  ['REJECTED'],
  WITHDRAWN: ['WITHDRAWN']
};

export default function ApplicationCard({ application, onUpdateStatus, onUpdateNotes, onDelete }) {
  if (!application) return null;

  const { id, job = {}, status, notes, fitScore } = application;
  const availableStatuses = VALID_TRANSITIONS[status] || [status];

  return (
    <div style={{
      background: 'rgba(255, 255, 255, 0.03)',
      border: '1px solid rgba(255, 255, 255, 0.08)',
      borderRadius: '12px',
      padding: '16px',
      display: 'flex',
      flexDirection: 'column',
      gap: '12px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Link to={`/dashboard/${job.id}`} style={{ textDecoration: 'none', color: '#f8fafc', fontWeight: 600, fontSize: '1rem' }}>
            {job.title || 'Untitled Position'}
          </Link>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted, #94a3b8)', marginTop: '2px' }}>
            {job.company || 'Unknown Company'}
          </div>
        </div>

        {fitScore != null && (
          <span style={{
            fontSize: '0.75rem',
            fontWeight: 700,
            padding: '2px 8px',
            borderRadius: '999px',
            background: fitScore >= 70 ? 'rgba(16, 185, 129, 0.2)' : fitScore >= 40 ? 'rgba(245, 158, 11, 0.2)' : 'rgba(239, 68, 68, 0.2)',
            color: fitScore >= 70 ? '#34d399' : fitScore >= 40 ? '#fbbf24' : '#f87171',
            border: '1px solid rgba(255, 255, 255, 0.1)'
          }}>
            {fitScore}% Fit
          </span>
        )}
      </div>

      {notes && (
        <div style={{ fontSize: '0.8rem', color: '#cbd5e1', background: 'rgba(0, 0, 0, 0.2)', padding: '8px', borderRadius: '6px' }}>
          📝 {notes}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto', paddingTop: '8px', borderTop: '1px solid rgba(255, 255, 255, 0.06)' }}>
        <select
          value={status}
          onChange={(e) => onUpdateStatus(id, e.target.value)}
          className="filter-select"
          style={{ fontSize: '0.75rem', padding: '2px 6px' }}
        >
          {availableStatuses.map(s => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            onClick={() => {
              const newNotes = prompt('Update notes:', notes || '');
              if (newNotes !== null) onUpdateNotes(id, newNotes);
            }}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '0.8rem' }}
          >
            Notes
          </button>
          <button
            onClick={() => onDelete(id)}
            style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', fontSize: '0.8rem' }}
          >
            Remove
          </button>
        </div>
      </div>
    </div>
  );
}
