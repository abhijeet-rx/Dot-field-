import React from 'react';

export default function ProfileCompletenessBar({ completeness }) {
  if (!completeness) return null;

  const { score = 0, sections = {}, missingRecommendations = [] } = completeness;

  return (
    <div className="completeness-card" style={{
      background: 'rgba(255, 255, 255, 0.03)',
      border: '1px solid rgba(255, 255, 255, 0.08)',
      borderRadius: '16px',
      padding: '24px',
      marginBottom: '32px',
      backdropFilter: 'blur(12px)'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-main, #f8fafc)' }}>
            Profile Completeness
          </h3>
          <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--text-muted, #94a3b8)' }}>
            Complete your profile to get the most accurate fit scores and tailored resumes.
          </p>
        </div>
        <div style={{
          fontSize: '1.5rem',
          fontWeight: 700,
          background: 'linear-gradient(135deg, #a855f7 0%, #ec4899 100%)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent'
        }}>
          {score}%
        </div>
      </div>

      {/* Progress Track */}
      <div style={{
        width: '100%',
        height: '10px',
        background: 'rgba(255, 255, 255, 0.08)',
        borderRadius: '999px',
        overflow: 'hidden',
        marginBottom: '16px'
      }}>
        <div style={{
          width: `${score}%`,
          height: '100%',
          background: score >= 80 ? 'linear-gradient(90deg, #10b981, #3b82f6)' : 'linear-gradient(90deg, #a855f7, #ec4899)',
          transition: 'width 0.6s cubic-bezier(0.4, 0, 0.2, 1)',
          borderRadius: '999px'
        }} />
      </div>

      {/* Section breakdown chips */}
      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: missingRecommendations.length > 0 ? '16px' : '0' }}>
        {Object.entries(sections).map(([key, val]) => (
          <span key={key} style={{
            fontSize: '0.75rem',
            padding: '4px 10px',
            borderRadius: '999px',
            background: val === 100 ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.06)',
            color: val === 100 ? '#34d399' : 'var(--text-muted, #94a3b8)',
            border: val === 100 ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid rgba(255, 255, 255, 0.08)',
            textTransform: 'capitalize'
          }}>
            {key}: {val}%
          </span>
        ))}
      </div>

      {/* Missing recommendations */}
      {missingRecommendations.length > 0 && (
        <div style={{ borderTop: '1px solid rgba(255, 255, 255, 0.06)', paddingTop: '12px' }}>
          <p style={{ margin: '0 0 8px', fontSize: '0.8rem', fontWeight: 600, color: '#fbbf24' }}>
            💡 Recommendations to reach 100%:
          </p>
          <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '0.8rem', color: 'var(--text-muted, #94a3b8)' }}>
            {missingRecommendations.map((rec, i) => (
              <li key={i} style={{ marginBottom: '4px' }}>{rec}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
