import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { fetchJobs } from '../api/client';

const REMOTE_TYPES = ['', 'REMOTE', 'HYBRID', 'ONSITE', 'OTHER'];
const EMPLOYMENT_TYPES = ['', 'FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'TEMPORARY', 'OTHER'];
const JOB_STATUSES = ['', 'SAVED', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED', 'ARCHIVED'];

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
  try {
    return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  } catch { return dateStr; }
}

function remoteLabel(type) {
  const labels = { REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site', OTHER: 'Other' };
  return labels[type] || type || '';
}

function employmentLabel(type) {
  const labels = { FULL_TIME: 'Full-time', PART_TIME: 'Part-time', CONTRACT: 'Contract', INTERNSHIP: 'Internship', TEMPORARY: 'Temporary', OTHER: 'Other' };
  return labels[type] || type || '';
}

function statusLabel(s) {
  const labels = { SAVED: 'Saved', APPLIED: 'Applied', INTERVIEW: 'Interview', OFFER: 'Offer', REJECTED: 'Rejected', ARCHIVED: 'Archived' };
  return labels[s] || s || '';
}

export default function Dashboard() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [company, setCompany] = useState('');
  const [status, setStatus] = useState('');
  const [remoteType, setRemoteType] = useState('');
  const [employmentType, setEmploymentType] = useState('');
  const [source, setSource] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(false);

  const loadJobs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchJobs({ page, size: 12, status: status || undefined, company: company || undefined, source: source || undefined, remoteType: remoteType || undefined, employmentType: employmentType || undefined });
      setJobs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message);
      setJobs([]);
    } finally {
      setLoading(false);
    }
  }, [page, status, company, source, remoteType, employmentType]);

  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  function clearFilters() {
    setCompany('');
    setStatus('');
    setRemoteType('');
    setEmploymentType('');
    setSource('');
    setPage(0);
  }

  const hasActiveFilters = company || status || remoteType || employmentType || source;

  return (
    <div className="dashboard">
      {/* Dashboard Header */}
      <div className="dashboard__header">
        <div>
          <h1 className="dashboard__title">Job Intelligence</h1>
          <p className="dashboard__subtitle">
            {totalElements > 0 ? `${totalElements} job${totalElements !== 1 ? 's' : ''} discovered` : 'Browse your job opportunities'}
          </p>
        </div>
        <button
          className={`btn-filter-toggle ${filtersOpen ? 'active' : ''}`}
          onClick={() => setFiltersOpen(p => !p)}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <line x1="4" y1="6" x2="20" y2="6" /><line x1="8" y1="12" x2="20" y2="12" /><line x1="12" y1="18" x2="20" y2="18" />
          </svg>
          Filters
          {hasActiveFilters && <span className="filter-dot" />}
        </button>
      </div>

      {/* Filters Panel */}
      {filtersOpen && (
        <div className="filters-panel">
          <div className="filters-grid">
            <div className="filter-group">
              <label>Company</label>
              <input
                type="text"
                placeholder="Search company..."
                value={company}
                onChange={e => { setCompany(e.target.value); setPage(0); }}
                className="filter-input"
              />
            </div>
            <div className="filter-group">
              <label>Status</label>
              <select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }} className="filter-select">
                {JOB_STATUSES.map(s => <option key={s} value={s}>{s ? statusLabel(s) : 'All Statuses'}</option>)}
              </select>
            </div>
            <div className="filter-group">
              <label>Remote</label>
              <select value={remoteType} onChange={e => { setRemoteType(e.target.value); setPage(0); }} className="filter-select">
                {REMOTE_TYPES.map(r => <option key={r} value={r}>{r ? remoteLabel(r) : 'All Types'}</option>)}
              </select>
            </div>
            <div className="filter-group">
              <label>Employment</label>
              <select value={employmentType} onChange={e => { setEmploymentType(e.target.value); setPage(0); }} className="filter-select">
                {EMPLOYMENT_TYPES.map(e => <option key={e} value={e}>{e ? employmentLabel(e) : 'All Types'}</option>)}
              </select>
            </div>
            <div className="filter-group">
              <label>Source</label>
              <input
                type="text"
                placeholder="e.g. COMPANY_WEBSITE"
                value={source}
                onChange={e => { setSource(e.target.value); setPage(0); }}
                className="filter-input"
              />
            </div>
          </div>
          {hasActiveFilters && (
            <button className="btn-clear-filters" onClick={clearFilters}>
              Clear all filters
            </button>
          )}
        </div>
      )}

      {/* Content */}
      {loading && (
        <div className="dashboard__loading">
          <div className="loading-grid">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="skeleton-card">
                <div className="skeleton-line skeleton-line--title" />
                <div className="skeleton-line skeleton-line--subtitle" />
                <div className="skeleton-line skeleton-line--meta" />
                <div className="skeleton-line skeleton-line--short" />
              </div>
            ))}
          </div>
        </div>
      )}

      {!loading && error && (
        <div className="dashboard__state">
          <div className="state-icon">⚠</div>
          <h3>Something went wrong</h3>
          <p>{error}</p>
          <button className="btn-primary" onClick={loadJobs}>Try Again</button>
        </div>
      )}

      {!loading && !error && jobs.length === 0 && (
        <div className="dashboard__state">
          <div className="state-icon">📭</div>
          <h3>No jobs found</h3>
          <p>{hasActiveFilters ? 'Try changing your filters.' : 'Discover jobs using the API to see them here.'}</p>
          {hasActiveFilters && (
            <button className="btn-secondary" onClick={clearFilters}>Clear Filters</button>
          )}
        </div>
      )}

      {!loading && !error && jobs.length > 0 && (
        <>
          <div className="job-grid">
            {jobs.map(job => (
              <Link to={`/dashboard/${job.id}`} key={job.id} className="job-card">
                <div className="job-card__header">
                  <h3 className="job-card__title">{job.title || 'Untitled Position'}</h3>
                  <span className={`status-chip status-chip--${(job.status || '').toLowerCase()}`}>
                    {statusLabel(job.status)}
                  </span>
                </div>
                <p className="job-card__company">{job.company || 'Unknown Company'}</p>
                <div className="job-card__meta">
                  {job.location && (
                    <span className="meta-tag">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                      {job.location}
                    </span>
                  )}
                  {job.remoteType && (
                    <span className="meta-tag meta-tag--accent">{remoteLabel(job.remoteType)}</span>
                  )}
                </div>
                <div className="job-card__details">
                  {job.employmentType && (
                    <span className="detail-chip">{employmentLabel(job.employmentType)}</span>
                  )}
                  {formatSalary(job.salaryMin, job.salaryMax, job.currency) && (
                    <span className="detail-chip detail-chip--salary">
                      {formatSalary(job.salaryMin, job.salaryMax, job.currency)}
                    </span>
                  )}
                </div>
                {job.postedDate && (
                  <p className="job-card__date">Posted {formatDate(job.postedDate)}</p>
                )}
                <div className="job-card__cta">
                  <span className="view-job-link">
                    View Intelligence
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
                  </span>
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="pagination__btn"
                disabled={page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                ← Previous
              </button>
              <span className="pagination__info">
                Page {page + 1} of {totalPages}
              </span>
              <button
                className="pagination__btn"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
