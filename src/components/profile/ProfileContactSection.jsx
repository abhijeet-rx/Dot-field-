import React from 'react';

export default function ProfileContactSection({ contactForm, setContactForm, handleSaveContact, saving }) {
  return (
    <div className="profile-section-card" style={{
      background: 'rgba(255, 255, 255, 0.03)',
      border: '1px solid rgba(255, 255, 255, 0.08)',
      borderRadius: '16px',
      padding: '24px',
      marginBottom: '24px'
    }}>
      <h2 style={{ fontSize: '1.25rem', margin: '0 0 16px', fontWeight: 600 }}>1. Personal & Contact Details</h2>
      <form onSubmit={handleSaveContact} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '6px' }}>Full Name</label>
          <input
            type="text"
            required
            value={contactForm.name}
            onChange={e => setContactForm({ ...contactForm, name: e.target.value })}
            className="filter-input"
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '6px' }}>Phone Number</label>
          <input
            type="text"
            placeholder="+1 (555) 000-0000"
            value={contactForm.phone}
            onChange={e => setContactForm({ ...contactForm, phone: e.target.value })}
            className="filter-input"
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '6px' }}>Location</label>
          <input
            type="text"
            placeholder="e.g. San Francisco, CA"
            value={contactForm.location}
            onChange={e => setContactForm({ ...contactForm, location: e.target.value })}
            className="filter-input"
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '6px' }}>LinkedIn URL</label>
          <input
            type="url"
            placeholder="https://linkedin.com/in/..."
            value={contactForm.linkedinUrl}
            onChange={e => setContactForm({ ...contactForm, linkedinUrl: e.target.value })}
            className="filter-input"
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '6px' }}>GitHub URL</label>
          <input
            type="url"
            placeholder="https://github.com/..."
            value={contactForm.githubUrl}
            onChange={e => setContactForm({ ...contactForm, githubUrl: e.target.value })}
            className="filter-input"
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '6px' }}>Portfolio URL</label>
          <input
            type="url"
            placeholder="https://myportfolio.com"
            value={contactForm.portfolioUrl}
            onChange={e => setContactForm({ ...contactForm, portfolioUrl: e.target.value })}
            className="filter-input"
            style={{ width: '100%' }}
          />
        </div>
        <div style={{ gridColumn: 'span 2', textAlign: 'right', marginTop: '8px' }}>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? 'Saving...' : 'Save Details'}
          </button>
        </div>
      </form>
    </div>
  );
}
