import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { fetchJob, fetchJobMatch, fetchTailoredResume, createApplication } from '../api/client';
import MatchScoreRing from '../components/MatchScoreRing';
import SkillBadge from '../components/SkillBadge';
import ResumeExportModal from '../components/ResumeExportModal';

function formatSalary(min, max, currency) {
  if (!min && !max) return null;
  const fmt = (v) => {
    if (!v) return '?';
    const n = Number(v);
    if (n >= 100000) return `${(n / 100000).toFixed(1)}L`;
    if (n >= 1000) return `${(n / 1000).toFixed(0)}K`;
    return n.toLocaleString();
  };
  const sym = currency === 'INR' ? '₹' : currency === 'EUR' ? '€' : currency === 'GBP' ? '£' : '$';
  if (min && max) return `${sym}${fmt(min)} – ${sym}${fmt(max)}`;
  if (min) return `From ${sym}${fmt(min)}`;
  return `Up to ${sym}${fmt(max)}`;
}

function formatDate(dateStr) {
  if (!dateStr) return null;
  try { return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }); }
  catch { return dateStr; }
}

const remoteLabels = { REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site', OTHER: 'Other' };
const employmentLabels = { FULL_TIME: 'Full-time', PART_TIME: 'Part-time', CONTRACT: 'Contract', INTERNSHIP: 'Internship', TEMPORARY: 'Temporary', OTHER: 'Other' };

function ScoreDimension({ label, score, max = 100 }) {
  const pct = max > 0 ? Math.min((score / max) * 100, 100) : 0;
  return (
    <div className="score-dimension">
      <div className="score-dimension__header">
        <span>{label}</span>
        <span className="score-dimension__value">{score ?? '—'}</span>
      </div>
      <div className="score-dimension__bar">
        <div className="score-dimension__fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export default function JobIntelligence() {
  const { id } = useParams();
  const [job, setJob] = useState(null);
  const [match, setMatch] = useState(null);
  const [resume, setResume] = useState(null);
  const [loadingJob, setLoadingJob] = useState(true);
  const [loadingMatch, setLoadingMatch] = useState(true);
  const [loadingResume, setLoadingResume] = useState(false);
  const [resumeRequested, setResumeRequested] = useState(false);
  const [errorJob, setErrorJob] = useState(null);
  const [errorMatch, setErrorMatch] = useState(null);
  const [errorResume, setErrorResume] = useState(null);
  const [trackedStatus, setTrackedStatus] = useState(null);

  const loadJobData = useCallback(async () => {
    setLoadingJob(true);
    setLoadingMatch(true);
    setErrorJob(null);
    setErrorMatch(null);

    try {
      const jobData = await fetchJob(id);
      setJob(jobData);
    } catch (err) {
      setErrorJob(err.message);
    } finally {
      setLoadingJob(false);
    }

    try {
      const matchData = await fetchJobMatch(id);
      setMatch(matchData);
    } catch (err) {
      setErrorMatch(err.message);
    } finally {
      setLoadingMatch(false);
    }

    try {
      const existingApp = await checkJobTrackedApi(id);
      if (existingApp) {
        setTracked(true);
        setTrackedStatus(existingApp.status);
      }
    } catch (err) {
      // Ignore check errors
    }
  }, [id]);

  useEffect(() => {
    loadJobData();
  }, [loadJobData]);

  const [tracked, setTracked] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const [trackingMsg, setTrackingMsg] = useState(null);

  async function handleTrackApplication() {
    setTrackingMsg(null);
    try {
      await createApplication({ jobId: Number(id), status: 'SAVED' });
      setTracked(true);
      setTrackingMsg('Job added to your Application Tracker!');
      setTimeout(() => setTrackingMsg(null), 4000);
    } catch (err) {
      if (err.message.includes('already exists')) {
        setTracked(true);
        setTrackingMsg('Job is already tracked in your applications.');
        setTimeout(() => setTrackingMsg(null), 4000);
      } else {
        alert(err.message);
      }
    }
  }

  async function handleTailorResume() {
    setResumeRequested(true);
    setLoadingResume(true);
    setErrorResume(null);
    try {
      const resumeData = await fetchTailoredResume(id);
      setResume(resumeData);
      setShowExportModal(true);
    } catch (err) {
      setErrorResume(err.message);
    } finally {
      setLoadingResume(false);
    }
  }

  // Loading state
  if (loadingJob) {
    return (
      <div className="job-intel">
        <div className="job-intel__loading">
          <div className="loading-spinner" />
          <p>Loading job details...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (errorJob) {
    return (
      <div className="job-intel">
        <div className="dashboard__state">
          <div className="state-icon">⚠</div>
          <h3>Failed to load job</h3>
          <p>{errorJob}</p>
          <Link to="/dashboard" className="btn-secondary">← Back to Dashboard</Link>
        </div>
      </div>
    );
  }

  if (!job) return null;

  const salary = formatSalary(job.salaryMin, job.salaryMax, job.currency);

  return (
    <div className="job-intel">
      {/* Back navigation */}
      <Link to="/dashboard" className="job-intel__back">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        Back to Dashboard
      </Link>

      <div className="job-intel__grid">
        {/* Left column: Job Details */}
        <div className="job-intel__main">
          {/* Job Header Card */}
          <section className="intel-card job-header-card">
            <div className="job-header-card__top">
              <div>
                <h1 className="job-header-card__title">{job.title}</h1>
                <p className="job-header-card__company">{job.company}</p>
              </div>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                {job.jobUrl && (
                  <a href={job.jobUrl} target="_blank" rel="noopener noreferrer" className="btn-apply">
                    Apply Manually
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                  </a>
                )}
                <button className="btn-primary" onClick={handleTrackApplication}>
                  📌 {tracked ? (trackedStatus ? `Tracked (${trackedStatus})` : 'Tracked') : 'Track Application'}
                </button>
              </div>
            </div>

            {trackingMsg && (
              <div style={{ margin: '12px 0 0', padding: '10px 14px', background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', borderRadius: '8px', fontSize: '0.85rem' }}>
                ✓ {trackingMsg}
              </div>
            )}
            <div className="job-header-card__meta">
              {job.location && <span className="meta-tag"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>{job.location}</span>}
              {job.remoteType && <span className="meta-tag meta-tag--accent">{remoteLabels[job.remoteType] || job.remoteType}</span>}
              {job.employmentType && <span className="meta-tag">{employmentLabels[job.employmentType] || job.employmentType}</span>}
              {salary && <span className="meta-tag meta-tag--salary">{salary}</span>}
              {job.source && <span className="meta-tag meta-tag--source">{job.source}</span>}
            </div>
            {job.postedDate && <p className="job-header-card__date">Posted {formatDate(job.postedDate)}</p>}
            {job.description && (
              <div className="job-header-card__desc">
                <h4>Description</h4>
                <p>{job.description}</p>
              </div>
            )}
          </section>

          {/* Skills & Requirements */}
          {match && (
            <section className="intel-card">
              <h3 className="intel-card__title">Skills & Requirements</h3>
              {match.matchedRequiredSkills?.length > 0 && (
                <div className="skill-group">
                  <h4 className="skill-group__label">Matched Required Skills</h4>
                  <div className="skill-group__badges">
                    {match.matchedRequiredSkills.map(s => <SkillBadge key={s} name={s} variant="matched" />)}
                  </div>
                </div>
              )}
              {match.missingRequiredSkills?.length > 0 && (
                <div className="skill-group">
                  <h4 className="skill-group__label">Missing Required Skills</h4>
                  <div className="skill-group__badges">
                    {match.missingRequiredSkills.map(s => <SkillBadge key={s} name={s} variant="missing" />)}
                  </div>
                </div>
              )}
              {match.matchedPreferredSkills?.length > 0 && (
                <div className="skill-group">
                  <h4 className="skill-group__label">Matched Preferred Skills</h4>
                  <div className="skill-group__badges">
                    {match.matchedPreferredSkills.map(s => <SkillBadge key={s} name={s} variant="matched-preferred" />)}
                  </div>
                </div>
              )}
              {match.missingPreferredSkills?.length > 0 && (
                <div className="skill-group">
                  <h4 className="skill-group__label">Missing Preferred Skills</h4>
                  <div className="skill-group__badges">
                    {match.missingPreferredSkills.map(s => <SkillBadge key={s} name={s} variant="missing-preferred" />)}
                  </div>
                </div>
              )}
            </section>
          )}

          {/* Strengths & Gaps */}
          {match && (match.strengths?.length > 0 || match.gaps?.length > 0) && (
            <section className="intel-card">
              <h3 className="intel-card__title">Strengths & Gaps</h3>
              {match.strengths?.length > 0 && (
                <div className="explanation-list explanation-list--strengths">
                  <h4>Strengths</h4>
                  <ul>{match.strengths.map((s, i) => <li key={i}><span className="explanation-icon">✓</span>{s}</li>)}</ul>
                </div>
              )}
              {match.gaps?.length > 0 && (
                <div className="explanation-list explanation-list--gaps">
                  <h4>Gaps</h4>
                  <ul>{match.gaps.map((g, i) => <li key={i}><span className="explanation-icon">⚠</span>{g}</li>)}</ul>
                </div>
              )}
            </section>
          )}

          {/* Analysis Details */}
          {match && (
            <section className="intel-card">
              <h3 className="intel-card__title">Detailed Analysis</h3>
              {match.experienceAnalysis && (
                <div className="analysis-block">
                  <h4>Experience</h4>
                  <p>{match.experienceAnalysis}</p>
                </div>
              )}
              {match.educationAnalysis && (
                <div className="analysis-block">
                  <h4>Education</h4>
                  <p>{match.educationAnalysis}</p>
                </div>
              )}
              {match.locationAnalysis && (
                <div className="analysis-block">
                  <h4>Location</h4>
                  <p>{match.locationAnalysis}</p>
                </div>
              )}
            </section>
          )}

          {/* Match loading / error */}
          {loadingMatch && (
            <section className="intel-card">
              <div className="intel-card__loading">
                <div className="loading-spinner loading-spinner--sm" />
                <span>Analyzing match...</span>
              </div>
            </section>
          )}
          {errorMatch && !loadingMatch && (
            <section className="intel-card">
              <div className="intel-card__error">
                <span>⚠ {errorMatch}</span>
              </div>
            </section>
          )}

          {/* Resume Tailoring */}
          <section className="intel-card">
            <h3 className="intel-card__title">Resume Tailoring</h3>
            {!resumeRequested && (
              <div className="resume-cta">
                <p className="resume-cta__desc">Generate a version of your resume tailored specifically for this job. All content is sourced exclusively from your candidate profile — nothing is fabricated.</p>
                <button className="btn-primary" onClick={handleTailorResume}>
                  ✨ Tailor My Resume
                </button>
              </div>
            )}
            {loadingResume && (
              <div className="intel-card__loading">
                <div className="loading-spinner loading-spinner--sm" />
                <span>Generating tailored resume...</span>
              </div>
            )}
            {errorResume && !loadingResume && (
              <div className="intel-card__error">
                <span>⚠ {errorResume}</span>
                <button className="btn-secondary btn--sm" onClick={handleTailorResume}>Retry</button>
              </div>
            )}
            {resume && !loadingResume && (
              <div className="tailored-resume">
                {resume.summary && (
                  <div className="resume-section">
                    <h4>Professional Summary</h4>
                    <p className="resume-summary">{resume.summary}</p>
                  </div>
                )}
                {resume.skills && (
                  <div className="resume-section">
                    <h4>Skills</h4>
                    {resume.skills.primary?.length > 0 && (
                      <div className="resume-skill-group">
                        <span className="resume-skill-label">Primary</span>
                        <div className="skill-group__badges">
                          {resume.skills.primary.map(s => <SkillBadge key={s} name={s} variant="matched" />)}
                        </div>
                      </div>
                    )}
                    {resume.skills.secondary?.length > 0 && (
                      <div className="resume-skill-group">
                        <span className="resume-skill-label">Secondary</span>
                        <div className="skill-group__badges">
                          {resume.skills.secondary.map(s => <SkillBadge key={s} name={s} variant="matched-preferred" />)}
                        </div>
                      </div>
                    )}
                  </div>
                )}
                {resume.experience?.length > 0 && (
                  <div className="resume-section">
                    <h4>Experience</h4>
                    {resume.experience.map((exp, i) => (
                      <div key={exp.id || i} className={`resume-entry ${exp.emphasized ? 'resume-entry--emphasized' : ''}`}>
                        <div className="resume-entry__header">
                          <strong>{exp.role}</strong>
                          {exp.emphasized && <span className="emphasized-badge">★ Emphasized</span>}
                        </div>
                        <span className="resume-entry__sub">{exp.company} {exp.startDate && `• ${formatDate(exp.startDate)}`} {exp.endDate ? `– ${formatDate(exp.endDate)}` : '– Present'}</span>
                        {exp.description && <p className="resume-entry__desc">{exp.description}</p>}
                        {exp.matchingKeywords?.length > 0 && (
                          <div className="resume-entry__keywords">
                            {exp.matchingKeywords.map(k => <span key={k} className="keyword-chip">{k}</span>)}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
                {resume.projects?.length > 0 && (
                  <div className="resume-section">
                    <h4>Projects</h4>
                    {resume.projects.map((proj, i) => (
                      <div key={proj.id || i} className={`resume-entry ${proj.emphasized ? 'resume-entry--emphasized' : ''}`}>
                        <div className="resume-entry__header">
                          <strong>{proj.name}</strong>
                          {proj.emphasized && <span className="emphasized-badge">★ Emphasized</span>}
                          {proj.projectScore > 0 && <span className="project-score">Score: {proj.projectScore}</span>}
                        </div>
                        {proj.description && <p className="resume-entry__desc">{proj.description}</p>}
                        {proj.technologies?.length > 0 && (
                          <div className="resume-entry__keywords">
                            {proj.technologies.map(t => <span key={t} className="keyword-chip">{t}</span>)}
                          </div>
                        )}
                        {proj.matchingKeywords?.length > 0 && (
                          <div className="resume-entry__keywords">
                            {proj.matchingKeywords.map(k => <span key={k} className="keyword-chip keyword-chip--match">{k}</span>)}
                          </div>
                        )}
                        <div className="resume-entry__links">
                          {proj.githubUrl && <a href={proj.githubUrl} target="_blank" rel="noopener noreferrer">GitHub ↗</a>}
                          {proj.liveUrl && <a href={proj.liveUrl} target="_blank" rel="noopener noreferrer">Live ↗</a>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {resume.education?.length > 0 && (
                  <div className="resume-section">
                    <h4>Education</h4>
                    {resume.education.map((edu, i) => (
                      <div key={edu.id || i} className="resume-entry">
                        <strong>{edu.degree}</strong>
                        <span className="resume-entry__sub">{edu.institution} {edu.year && `• ${edu.year}`}</span>
                      </div>
                    ))}
                  </div>
                )}
                {resume.tailoringAnalysis && (
                  <div className="resume-section tailoring-analysis">
                    <h4>Tailoring Analysis</h4>
                    {resume.tailoringAnalysis.matchedKeywords?.length > 0 && (
                      <div className="analysis-row">
                        <span className="analysis-label">Matched Keywords</span>
                        <div className="resume-entry__keywords">
                          {resume.tailoringAnalysis.matchedKeywords.map(k => <span key={k} className="keyword-chip keyword-chip--match">{k}</span>)}
                        </div>
                      </div>
                    )}
                    {resume.tailoringAnalysis.unusedJobKeywords?.length > 0 && (
                      <div className="analysis-row">
                        <span className="analysis-label">Unused Job Keywords</span>
                        <div className="resume-entry__keywords">
                          {resume.tailoringAnalysis.unusedJobKeywords.map(k => <span key={k} className="keyword-chip keyword-chip--unused">{k}</span>)}
                        </div>
                      </div>
                    )}
                    {resume.tailoringAnalysis.tailoringNotes && (
                      <p className="tailoring-notes">{resume.tailoringAnalysis.tailoringNotes}</p>
                    )}
                  </div>
                )}
                <div className="anti-fabrication-notice">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                  All resume content is sourced exclusively from your candidate profile. Nothing is fabricated.
                </div>
              </div>
            )}
          </section>
        </div>

        {/* Right column: Match Score Sidebar */}
        <div className="job-intel__sidebar">
          {match && (
            <div className="intel-card score-card">
              <h3 className="intel-card__title">Match Score</h3>
              <div className="score-card__ring">
                <MatchScoreRing score={match.overallScore} category={match.matchCategory} size={140} />
              </div>
              <div className="score-card__dimensions">
                <ScoreDimension label="Skills" score={match.skillScore} />
                <ScoreDimension label="Experience" score={match.experienceScore} />
                <ScoreDimension label="Education" score={match.educationScore} />
                <ScoreDimension label="Location" score={match.locationScore} />
              </div>
            </div>
          )}
          {loadingMatch && !match && (
            <div className="intel-card score-card">
              <div className="intel-card__loading">
                <div className="loading-spinner loading-spinner--sm" />
                <span>Calculating match...</span>
              </div>
            </div>
          )}

          {/* Apply CTA */}
          {job.jobUrl && (
            <div className="intel-card apply-card">
              <h3 className="intel-card__title">Ready to Apply?</h3>
              <p className="apply-card__desc">Review the match analysis above, tailor your resume, then apply directly on the company's site.</p>
              <a href={job.jobUrl} target="_blank" rel="noopener noreferrer" className="btn-apply btn-apply--full">
                Apply Manually
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
              </a>
              <p className="apply-card__note">Opens the original job listing in a new tab.</p>
            </div>
          )}
        </div>
      </div>

      {showExportModal && resume && (
        <ResumeExportModal
          tailoredData={resume}
          onClose={() => setShowExportModal(false)}
        />
      )}
    </div>
  );
}
