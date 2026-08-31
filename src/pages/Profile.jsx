import { useState, useEffect, useCallback } from 'react';
import {
  fetchProfile,
  updateProfileApi,
  fetchProfileCompleteness,
  addSkillApi,
  deleteSkillApi,
  addExperienceApi,
  deleteExperienceApi,
  addEducationApi,
  deleteEducationApi,
  addProjectApi,
  deleteProjectApi
} from '../api/client';
import ProfileCompletenessBar from '../components/ProfileCompletenessBar';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [completeness, setCompleteness] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState(null);

  // Form states
  const [contactForm, setContactForm] = useState({
    name: '',
    phone: '',
    location: '',
    linkedinUrl: '',
    githubUrl: '',
    portfolioUrl: ''
  });

  // Modal / Add item states
  const [showSkillModal, setShowSkillModal] = useState(false);
  const [skillInput, setSkillInput] = useState({ name: '', category: 'TECHNICAL' });

  const [showExpModal, setShowExpModal] = useState(false);
  const [expInput, setExpInput] = useState({ company: '', role: '', description: '', startDate: '', endDate: '' });

  const [showEduModal, setShowEduModal] = useState(false);
  const [eduInput, setEduInput] = useState({ institution: '', degree: '', fieldOfStudy: '', startDate: '', endDate: '', grade: '' });

  const [showProjModal, setShowProjModal] = useState(false);
  const [projInput, setProjInput] = useState({ name: '', description: '', githubUrl: '', liveUrl: '', technologies: '' });

  const loadProfileData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [profData, compData] = await Promise.all([
        fetchProfile(),
        fetchProfileCompleteness()
      ]);
      setProfile(profData);
      setCompleteness(compData);
      if (profData) {
        setContactForm({
          name: profData.name || '',
          phone: profData.phone || '',
          location: profData.location || '',
          linkedinUrl: profData.linkedinUrl || '',
          githubUrl: profData.githubUrl || '',
          portfolioUrl: profData.portfolioUrl || ''
        });
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfileData();
  }, [loadProfileData]);

  async function handleSaveContact(e) {
    e.preventDefault();
    setSaving(true);
    setMsg(null);
    try {
      const updated = await updateProfileApi(contactForm);
      setProfile(updated);
      const updatedComp = await fetchProfileCompleteness();
      setCompleteness(updatedComp);
      setMsg('Contact information saved!');
      setTimeout(() => setMsg(null), 3000);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAddSkill(e) {
    e.preventDefault();
    if (!skillInput.name.trim()) return;
    try {
      await addSkillApi(skillInput);
      setSkillInput({ name: '', category: 'TECHNICAL' });
      setShowSkillModal(false);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDeleteSkill(id) {
    try {
      await deleteSkillApi(id);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleAddExperience(e) {
    e.preventDefault();
    if (!expInput.company.trim() || !expInput.role.trim()) return;
    try {
      await addExperienceApi({
        company: expInput.company,
        role: expInput.role,
        description: expInput.description || null,
        startDate: expInput.startDate || null,
        endDate: expInput.endDate || null
      });
      setExpInput({ company: '', role: '', description: '', startDate: '', endDate: '' });
      setShowExpModal(false);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDeleteExperience(id) {
    try {
      await deleteExperienceApi(id);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleAddEducation(e) {
    e.preventDefault();
    if (!eduInput.institution.trim() || !eduInput.degree.trim()) return;
    try {
      await addEducationApi({
        institution: eduInput.institution,
        degree: eduInput.degree,
        fieldOfStudy: eduInput.fieldOfStudy || null,
        startDate: eduInput.startDate || null,
        endDate: eduInput.endDate || null,
        grade: eduInput.grade || null
      });
      setEduInput({ institution: '', degree: '', fieldOfStudy: '', startDate: '', endDate: '', grade: '' });
      setShowEduModal(false);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDeleteEducation(id) {
    try {
      await deleteEducationApi(id);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleAddProject(e) {
    e.preventDefault();
    if (!projInput.name.trim()) return;
    try {
      const techList = projInput.technologies
        ? projInput.technologies.split(',').map(t => t.trim()).filter(Boolean)
        : [];
      await addProjectApi({
        name: projInput.name,
        description: projInput.description || null,
        githubUrl: projInput.githubUrl || null,
        liveUrl: projInput.liveUrl || null,
        technologies: techList
      });
      setProjInput({ name: '', description: '', githubUrl: '', liveUrl: '', technologies: '' });
      setShowProjModal(false);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDeleteProject(id) {
    try {
      await deleteProjectApi(id);
      await loadProfileData();
    } catch (err) {
      alert(err.message);
    }
  }

  if (loading) {
    return (
      <div style={{ padding: '40px 24px', maxWidth: '1000px', margin: '0 auto' }}>
        <div style={{ color: 'var(--text-muted)' }}>Loading profile...</div>
      </div>
    );
  }

  return (
    <div style={{ padding: '40px 24px', maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 700, margin: 0 }}>Candidate Profile</h1>
        <p style={{ color: 'var(--text-muted)', margin: '4px 0 0' }}>
          Manage your candidate details, skills, experience, and projects.
        </p>
      </div>

      {/* Completeness Meter */}
      <ProfileCompletenessBar completeness={completeness} />

      {msg && (
        <div style={{
          padding: '12px 16px',
          background: 'rgba(16, 185, 129, 0.15)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          color: '#34d399',
          borderRadius: '8px',
          marginBottom: '24px'
        }}>
          ✓ {msg}
        </div>
      )}

      {error && (
        <div style={{
          padding: '12px 16px',
          background: 'rgba(239, 68, 68, 0.15)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          color: '#f87171',
          borderRadius: '8px',
          marginBottom: '24px'
        }}>
          ⚠ {error}
        </div>
      )}

      {/* 1. Contact Info Form */}
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

      {/* 2. Skills Section */}
      <div className="profile-section-card" style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '16px',
        padding: '24px',
        marginBottom: '24px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '1.25rem', margin: 0, fontWeight: 600 }}>2. Skills & Technologies</h2>
          <button className="btn-secondary" onClick={() => setShowSkillModal(true)}>+ Add Skill</button>
        </div>

        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {profile?.skills && profile.skills.length > 0 ? (
            profile.skills.map(s => (
              <span key={s.id} style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 12px',
                borderRadius: '999px',
                background: 'rgba(168, 85, 247, 0.15)',
                border: '1px solid rgba(168, 85, 247, 0.3)',
                color: '#e9d5ff',
                fontSize: '0.85rem'
              }}>
                {s.name} <small style={{ opacity: 0.7 }}>({s.category})</small>
                <button
                  onClick={() => handleDeleteSkill(s.id)}
                  style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', padding: 0, marginLeft: '4px' }}
                >
                  ×
                </button>
              </span>
            ))
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>No skills added yet.</p>
          )}
        </div>
      </div>

      {/* 3. Experience Section */}
      <div className="profile-section-card" style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '16px',
        padding: '24px',
        marginBottom: '24px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '1.25rem', margin: 0, fontWeight: 600 }}>3. Work Experience</h2>
          <button className="btn-secondary" onClick={() => setShowExpModal(true)}>+ Add Experience</button>
        </div>

        {profile?.experiences && profile.experiences.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {profile.experiences.map(exp => (
              <div key={exp.id} style={{
                padding: '16px',
                background: 'rgba(255, 255, 255, 0.02)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                borderRadius: '8px',
                display: 'flex',
                justifyContent: 'space-between'
              }}>
                <div>
                  <h4 style={{ margin: 0, fontSize: '1rem', color: '#f8fafc' }}>{exp.role} <span style={{ color: 'var(--text-muted)' }}>at {exp.company}</span></h4>
                  <p style={{ margin: '4px 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>{exp.description}</p>
                </div>
                <button onClick={() => handleDeleteExperience(exp.id)} className="btn-clear-filters" style={{ height: 'fit-content' }}>
                  Delete
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>No work experience entries.</p>
        )}
      </div>

      {/* 4. Education Section */}
      <div className="profile-section-card" style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '16px',
        padding: '24px',
        marginBottom: '24px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '1.25rem', margin: 0, fontWeight: 600 }}>4. Education</h2>
          <button className="btn-secondary" onClick={() => setShowEduModal(true)}>+ Add Education</button>
        </div>

        {profile?.educations && profile.educations.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {profile.educations.map(edu => (
              <div key={edu.id} style={{
                padding: '16px',
                background: 'rgba(255, 255, 255, 0.02)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                borderRadius: '8px',
                display: 'flex',
                justifyContent: 'space-between'
              }}>
                <div>
                  <h4 style={{ margin: 0, fontSize: '1rem', color: '#f8fafc' }}>{edu.degree} <span style={{ color: 'var(--text-muted)' }}>— {edu.institution}</span></h4>
                  {edu.fieldOfStudy && <p style={{ margin: '4px 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>Field: {edu.fieldOfStudy}</p>}
                </div>
                <button onClick={() => handleDeleteEducation(edu.id)} className="btn-clear-filters" style={{ height: 'fit-content' }}>
                  Delete
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>No education entries.</p>
        )}
      </div>

      {/* Add Skill Modal */}
      {showSkillModal && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#1e1b4b', padding: '24px', borderRadius: '16px', width: '400px', border: '1px solid rgba(255,255,255,0.1)' }}>
            <h3 style={{ margin: '0 0 16px' }}>Add Skill</h3>
            <form onSubmit={handleAddSkill}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Skill Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Java, React, SQL"
                  value={skillInput.name}
                  onChange={e => setSkillInput({ ...skillInput, name: e.target.value })}
                  className="filter-input"
                  style={{ width: '100%', marginTop: '4px' }}
                />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Category</label>
                <select
                  value={skillInput.category}
                  onChange={e => setSkillInput({ ...skillInput, category: e.target.value })}
                  className="filter-select"
                  style={{ width: '100%', marginTop: '4px' }}
                >
                  <option value="TECHNICAL">Technical</option>
                  <option value="FRAMEWORK">Framework</option>
                  <option value="DATABASE">Database</option>
                  <option value="TOOL">Tool</option>
                  <option value="SOFT">Soft Skill</option>
                </select>
              </div>
              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn-secondary" onClick={() => setShowSkillModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Add Skill</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Experience Modal */}
      {showExpModal && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#1e1b4b', padding: '24px', borderRadius: '16px', width: '500px', border: '1px solid rgba(255,255,255,0.1)' }}>
            <h3 style={{ margin: '0 0 16px' }}>Add Experience</h3>
            <form onSubmit={handleAddExperience}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Company</label>
                  <input
                    type="text"
                    required
                    value={expInput.company}
                    onChange={e => setExpInput({ ...expInput, company: e.target.value })}
                    className="filter-input"
                    style={{ width: '100%', marginTop: '4px' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Role Title</label>
                  <input
                    type="text"
                    required
                    value={expInput.role}
                    onChange={e => setExpInput({ ...expInput, role: e.target.value })}
                    className="filter-input"
                    style={{ width: '100%', marginTop: '4px' }}
                  />
                </div>
              </div>
              <div style={{ marginBottom: '16px' }}>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Description</label>
                <textarea
                  rows={3}
                  value={expInput.description}
                  onChange={e => setExpInput({ ...expInput, description: e.target.value })}
                  className="filter-input"
                  style={{ width: '100%', marginTop: '4px' }}
                />
              </div>
              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn-secondary" onClick={() => setShowExpModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Add Entry</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Education Modal */}
      {showEduModal && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#1e1b4b', padding: '24px', borderRadius: '16px', width: '500px', border: '1px solid rgba(255,255,255,0.1)' }}>
            <h3 style={{ margin: '0 0 16px' }}>Add Education</h3>
            <form onSubmit={handleAddEducation}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Institution</label>
                  <input
                    type="text"
                    required
                    value={eduInput.institution}
                    onChange={e => setEduInput({ ...eduInput, institution: e.target.value })}
                    className="filter-input"
                    style={{ width: '100%', marginTop: '4px' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Degree</label>
                  <input
                    type="text"
                    required
                    value={eduInput.degree}
                    onChange={e => setEduInput({ ...eduInput, degree: e.target.value })}
                    className="filter-input"
                    style={{ width: '100%', marginTop: '4px' }}
                  />
                </div>
              </div>
              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn-secondary" onClick={() => setShowEduModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Add Education</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
