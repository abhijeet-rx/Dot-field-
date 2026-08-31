import React from 'react';

export default function ApplicationAnalytics({ analytics }) {
  if (!analytics) return null;

  const {
    totalApplications = 0,
    statusCounts = {},
    responseRate = 0,
    interviewRate = 0,
    averageFitScore = 0
  } = analytics;

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
      gap: '16px',
      marginBottom: '32px'
    }}>
      <div style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '12px',
        padding: '20px',
        backdropFilter: 'blur(12px)'
      }}>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted, #94a3b8)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Total Tracked
        </div>
        <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#f8fafc', marginTop: '6px' }}>
          {totalApplications}
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #94a3b8)', marginTop: '4px' }}>
          {statusCounts.APPLIED || 0} applied
        </div>
      </div>

      <div style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '12px',
        padding: '20px',
        backdropFilter: 'blur(12px)'
      }}>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted, #94a3b8)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Response Rate
        </div>
        <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#38bdf8', marginTop: '6px' }}>
          {responseRate}%
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #94a3b8)', marginTop: '4px' }}>
          Recruiter responses
        </div>
      </div>

      <div style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '12px',
        padding: '20px',
        backdropFilter: 'blur(12px)'
      }}>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted, #94a3b8)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Interview Rate
        </div>
        <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#a855f7', marginTop: '6px' }}>
          {interviewRate}%
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #94a3b8)', marginTop: '4px' }}>
          {statusCounts.INTERVIEW || 0} active interviews
        </div>
      </div>

      <div style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '12px',
        padding: '20px',
        backdropFilter: 'blur(12px)'
      }}>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted, #94a3b8)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Avg Match Fit
        </div>
        <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#34d399', marginTop: '6px' }}>
          {averageFitScore}%
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #94a3b8)', marginTop: '4px' }}>
          Intelligence score
        </div>
      </div>
    </div>
  );
}
