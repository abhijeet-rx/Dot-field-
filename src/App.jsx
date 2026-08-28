import { useState } from 'react';
import DotField from './components/DotField';
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

export default function App() {
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
    <div className="app-viewport">
      {/* Background Noise Texture */}
      <div className="noise-overlay" />

      {/* React Bits Full-Screen DotField Background */}
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

      {/* Navbar Header */}
      <header className="navbar">
        <div className="brand-logo">
          <div className="brand-dot" />
          <span>DotField</span>
        </div>
        <nav className="nav-links">
          <a href="#features" className="nav-link">Features</a>
          <a href="#controls" className="nav-link">Controls</a>
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
        </nav>
      </header>

      {/* Main Landing Showcase Content */}
      <main className="main-content">
        {/* Left Hero Column */}
        <section className="hero-section">
          <div className="hero-badge">
            <span>✨ Interactive Canvas Engine</span>
          </div>

          <h1 className="hero-title">
            Dynamic Particle <br />
            <span className="title-accent">Dot Field Background</span>
          </h1>

          <p className="hero-desc">
            A high-performance reactive particle dot field background built with WebGL physics, cursor bulge distortion, speed velocity tracking, and customizable glowing gradients.
          </p>

          <div className="hero-actions">
            <button className="btn-primary" onClick={() => setSparkle(prev => !prev)}>
              <span>{sparkle ? 'Disable Sparkle' : 'Enable Sparkle ✨'}</span>
            </button>
            <a href="#controls" className="btn-secondary">
              <span>Customize Params</span>
            </a>
          </div>

          {/* Feature Highlights Grid */}
          <div className="feature-grid" id="features">
            <div className="feature-card">
              <div className="feature-icon">⚡</div>
              <div className="feature-title">Velocity Response</div>
              <div className="feature-desc">Dots bulge and distort based on mouse motion speed.</div>
            </div>

            <div className="feature-card">
              <div className="feature-icon">🎨</div>
              <div className="feature-title">Gradient Shader</div>
              <div className="feature-desc">Smooth linear particle gradients with SVG radial cursor aura.</div>
            </div>

            <div className="feature-card">
              <div className="feature-icon">🌊</div>
              <div className="feature-title">Wave Displacement</div>
              <div className="feature-desc">Optional procedural sine wave physics for ambient motion.</div>
            </div>
          </div>
        </section>

        {/* Right Live Controls Panel Column */}
        <aside className="control-panel" id="controls">
          <div className="panel-header">
            <h2 className="panel-title">Live Parameters</h2>
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

          {/* Code Snippet Box */}
          <div className="code-box">
            <code>
              {`<DotField
  dotRadius={${dotRadius}}
  dotSpacing={${dotSpacing}}
  bulgeStrength={${bulgeStrength}}
  sparkle={${sparkle}}
  waveAmplitude={${waveAmplitude}}
/>`}
            </code>
          </div>
        </aside>
      </main>
    </div>
  );
}
