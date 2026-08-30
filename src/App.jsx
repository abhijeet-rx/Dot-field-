import { useState } from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import DotField from './components/DotField';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import JobIntelligence from './pages/JobIntelligence';
import './index.css';

const THEMES = [
  {
    name: 'Purple Nebula',
    gradientFrom: 'rgba(168, 85, 247, 0.45)',
    gradientTo: 'rgba(236, 72, 153, 0.35)',
    glowColor: 'rgba(168, 85, 247, 0.25)',
    chipColor: '#a855f7'
  },
  {
    name: 'Cyber Cyan',
    gradientFrom: 'rgba(0, 242, 254, 0.45)',
    gradientTo: 'rgba(79, 172, 254, 0.35)',
    glowColor: 'rgba(0, 242, 254, 0.25)',
    chipColor: '#00f2fe'
  },
  {
    name: 'Emerald Matrix',
    gradientFrom: 'rgba(16, 185, 129, 0.45)',
    gradientTo: 'rgba(52, 211, 153, 0.35)',
    glowColor: 'rgba(16, 185, 129, 0.25)',
    chipColor: '#10b981'
  },
  {
    name: 'Neon Sunset',
    gradientFrom: 'rgba(244, 63, 94, 0.45)',
    gradientTo: 'rgba(251, 146, 60, 0.35)',
    glowColor: 'rgba(244, 63, 94, 0.25)',
    chipColor: '#f43f5e'
  }
];

/* ─── Landing Page ────────────────────────────────────────── */
function LandingPage() {
  return (
    <>
      {/* Full-Screen DotField Background */}
      <DotField
        dotRadius={1.5}
        dotSpacing={14}
        bulgeStrength={67}
        glowRadius={160}
        sparkle={true}
        waveAmplitude={0}
        bulgeOnly={true}
        gradientFrom="rgba(168, 85, 247, 0.45)"
        gradientTo="rgba(236, 72, 153, 0.35)"
        glowColor="rgba(168, 85, 247, 0.25)"
      />

      {/* Main Landing Showcase Content */}
      <main className="main-content">
        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-badge">
            <span>✨ Job Intelligence Platform</span>
          </div>

          <h1 className="hero-title">
            Your Career <br />
            <span className="title-accent">Intelligence Hub</span>
          </h1>

          <p className="hero-desc">
            Discover jobs, analyze your fit, understand requirements, tailor your resume, and make informed career decisions — all powered by DOT Field's intelligent matching engine.
          </p>

          <div className="hero-actions">
            <Link to="/dashboard" className="btn-primary">
              <span>Open Dashboard →</span>
            </Link>
          </div>

          {/* Feature Highlights Grid */}
          <div className="feature-grid" id="features">
            <div className="feature-card">
              <div className="feature-icon">🎯</div>
              <div className="feature-title">Match Analysis</div>
              <div className="feature-desc">See exactly how your profile matches each job with detailed scoring.</div>
            </div>

            <div className="feature-card">
              <div className="feature-icon">📝</div>
              <div className="feature-title">Resume Tailoring</div>
              <div className="feature-desc">Generate job-specific resumes from your real profile data — zero fabrication.</div>
            </div>

            <div className="feature-card">
              <div className="feature-icon">🔍</div>
              <div className="feature-title">Skill Insights</div>
              <div className="feature-desc">Understand which skills match, which are missing, and where to grow.</div>
            </div>
          </div>
        </section>
      </main>
    </>
  );
}

/* ─── App Root ────────────────────────────────────────────── */
export default function App() {
  return (
    <div className="app-viewport">
      {/* Background Noise Texture */}
      <div className="noise-overlay" />

      {/* Shared Navbar */}
      <Navbar />

      {/* Routes */}
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/dashboard/:id" element={<JobIntelligence />} />
      </Routes>
    </div>
  );
}
