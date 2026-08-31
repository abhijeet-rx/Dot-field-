import React from 'react';
import ApplicationCard from './ApplicationCard';

const STAGES = [
  { key: 'SAVED', label: 'Saved', color: '#94a3b8' },
  { key: 'APPLIED', label: 'Applied', color: '#38bdf8' },
  { key: 'SCREENING', label: 'Screening', color: '#818cf8' },
  { key: 'INTERVIEW', label: 'Interview', color: '#c084fc' },
  { key: 'OFFER', label: 'Offer', color: '#34d399' },
  { key: 'REJECTED', label: 'Rejected', color: '#f87171' },
  { key: 'WITHDRAWN', label: 'Withdrawn', color: '#64748b' }
];

export default function ApplicationKanban({ applications = [], onUpdateStatus, onUpdateNotes, onDelete }) {
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
      gap: '16px',
      overflowX: 'auto',
      paddingBottom: '16px'
    }}>
      {STAGES.map(stage => {
        const stageApps = applications.filter(a => a.status === stage.key);
        return (
          <div key={stage.key} style={{
            background: 'rgba(255, 255, 255, 0.02)',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            borderRadius: '12px',
            padding: '16px',
            minHeight: '400px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
          }}>
            <div style={{
              display: 'flex',
              justify: 'space-between',
              alignItems: 'center',
              paddingBottom: '8px',
              borderBottom: `2px solid ${stage.color}`
            }}>
              <span style={{ fontWeight: 600, fontSize: '0.9rem', color: stage.color }}>
                {stage.label}
              </span>
              <span style={{
                fontSize: '0.75rem',
                background: 'rgba(255, 255, 255, 0.08)',
                padding: '2px 8px',
                borderRadius: '999px',
                color: 'var(--text-muted)'
              }}>
                {stageApps.length}
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
              {stageApps.map(app => (
                <ApplicationCard
                  key={app.id}
                  application={app}
                  onUpdateStatus={onUpdateStatus}
                  onUpdateNotes={onUpdateNotes}
                  onDelete={onDelete}
                />
              ))}
              {stageApps.length === 0 && (
                <div style={{ textAlign: 'center', padding: '24px 8px', color: 'var(--text-muted)', fontSize: '0.8rem', fontStyle: 'italic' }}>
                  No applications in {stage.label}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
