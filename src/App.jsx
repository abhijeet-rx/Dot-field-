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
  const [themeIndex, setThemeIndex] = useState(0);
  const [dotRadius, setDotRadius] = useState(1.5);
  const [dotSpacing, setDotSpacing] = useState(14);
  const [bulgeStrength, setBulgeStrength] = useState(67);
  const [glowRadius, setGlowRadius] = useState(160);
  const [sparkle, setSparkle] = useState(true);
  const [waveAmplitude, setWaveAmplitude] = useState(0);
  const [bulgeOnly, setBulgeOnly] = useState(true);

  const currentTheme = THEMES[themeIndex];

  return (
    <>
      {/* Full-Screen DotField Background */}
      <DotField
        dotRadius={dotRadius}
        dotSpacing={dotSpacing}
        bulgeStrength={bulgeStrength}
        glowRadius={glowRadius}
        sparkle={sparkle}
        waveAmplitude={waveAmplitude}
        bulgeOnly={bulgeOnly}
        gradientFrom={currentTheme.gradientFrom}
        gradientTo={currentTheme.gradientTo}
        glowColor={currentTheme.glowColor}
      />

      {/* Main Landing Showcase Content */}
      <main className="main-content">
        {/* Left Hero Column */}
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
            <a href="#controls" className="btn-secondary">
              <span>Customize Theme</span>
            </a>
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

        {/* Right Live Controls Panel Column */}
        <aside className="control-panel" id="controls">
          <div className="panel-header">
            <h2 className="panel-title">Theme Controls</h2>
            <div className="preset-selector" title="Color Theme Presets">
              {THEMES.map((t, i) => (
                <div
                  key={i}
                  className={`preset-chip ${i === themeIndex ? 'active' : ''}`}
                  style={{ backgroundColor: t.chipColor }}
                  onClick={() => setThemeIndex(i)}
                  title={t.name}
                />
              ))}
            </div>
          </div>

          {/* Dot Radius Slider */}
          <div className="control-group">
            <div className="label-row">
              <span>Dot Radius</span>
              <span className="value-badge">{dotRadius}px</span>
            </div>
            <input
              type="range"
              min="0.8"
              max="5"
              step="0.2"
              value={dotRadius}
              onChange={e => setDotRadius(parseFloat(e.target.value))}
            />
          </div>

          {/* Dot Spacing Slider */}
          <div className="control-group">
            <div className="label-row">
              <span>Dot Spacing</span>
              <span className="value-badge">{dotSpacing}px</span>
            </div>
            <input
              type="range"
              min="8"
              max="28"
              step="1"
              value={dotSpacing}
              onChange={e => setDotSpacing(parseInt(e.target.value, 10))}
            />
          </div>

          {/* Bulge Strength Slider */}
          <div className="control-group">
            <div className="label-row">
              <span>Bulge Strength</span>
              <span className="value-badge">{bulgeStrength}</span>
            </div>
            <input
              type="range"
              min="10"
              max="150"
              step="5"
              value={bulgeStrength}
              onChange={e => setBulgeStrength(parseInt(e.target.value, 10))}
            />
          </div>

          {/* Glow Radius Slider */}
          <div className="control-group">
            <div className="label-row">
              <span>Glow Radius</span>
              <span className="value-badge">{glowRadius}px</span>
            </div>
            <input
              type="range"
              min="80"
              max="350"
              step="10"
              value={glowRadius}
              onChange={e => setGlowRadius(parseInt(e.target.value, 10))}
            />
          </div>

          {/* Wave Amplitude Slider */}
          <div className="control-group">
            <div className="label-row">
              <span>Wave Motion</span>
              <span className="value-badge">{waveAmplitude}</span>
            </div>
            <input
              type="range"
              min="0"
              max="15"
              step="0.5"
              value={waveAmplitude}
              onChange={e => setWaveAmplitude(parseFloat(e.target.value))}
            />
          </div>

          {/* Toggles */}
          <div className="toggle-row">
            <span className="label-row">Sparkle Effect</span>
            <div
              className={`toggle-switch ${sparkle ? 'on' : ''}`}
              onClick={() => setSparkle(prev => !prev)}
            >
              <div className="toggle-knob" />
            </div>
          </div>

          <div className="toggle-row">
            <span className="label-row">Bulge Only Physics</span>
            <div
              className={`toggle-switch ${bulgeOnly ? 'on' : ''}`}
              onClick={() => setBulgeOnly(prev => !prev)}
            >
              <div className="toggle-knob" />
            </div>
          </div>
        </aside>
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
