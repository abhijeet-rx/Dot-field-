import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const location = useLocation();
  const { isAuthenticated, user, logout } = useAuth();
  const isDashboard = location.pathname.startsWith('/dashboard');
  const isLogin = location.pathname === '/login';
  const isRegister = location.pathname === '/register';

  return (
    <header className="navbar">
      <Link to="/" className="brand-logo" style={{ textDecoration: 'none' }}>
        <div className="brand-dot" />
        <span>DotField</span>
      </Link>
      <nav className="nav-links">
        <Link
          to="/"
          className={`nav-link ${location.pathname === '/' ? 'nav-link--active' : ''}`}
        >
          Home
        </Link>
        
        {isAuthenticated && (
          <Link
            to="/dashboard"
            className={`nav-link ${isDashboard ? 'nav-link--active' : ''}`}
          >
            Dashboard
          </Link>
        )}

        <a
          href="https://github.com/abhijeet-rx/Dot-field-"
          target="_blank"
          rel="noopener noreferrer"
          className="btn-github"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          <span>GitHub</span>
        </a>

        {isAuthenticated ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '0.5rem' }}>
            <span style={{ fontSize: '0.85rem', color: '#94a3b8', background: 'rgba(30, 41, 59, 0.8)', padding: '0.35rem 0.75rem', borderRadius: '20px', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
              {user?.email} {user?.role === 'ADMIN' && <strong style={{ color: '#818cf8', marginLeft: '4px' }}>(Admin)</strong>}
            </span>
            <button
              onClick={logout}
              style={{
                background: 'transparent',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#f8fafc',
                padding: '0.4rem 0.85rem',
                borderRadius: '6px',
                fontSize: '0.85rem',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'all 0.2s ease'
              }}
            >
              Sign Out
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginLeft: '0.5rem' }}>
            <Link
              to="/login"
              className={`nav-link ${isLogin ? 'nav-link--active' : ''}`}
            >
              Sign In
            </Link>
            <Link
              to="/register"
              style={{
                background: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
                color: '#ffffff',
                padding: '0.45rem 1rem',
                borderRadius: '6px',
                fontSize: '0.875rem',
                fontWeight: '600',
                textDecoration: 'none',
                boxShadow: '0 2px 8px rgba(99, 102, 241, 0.3)'
              }}
            >
              Register
            </Link>
          </div>
        )}
      </nav>
    </header>
  );
}
