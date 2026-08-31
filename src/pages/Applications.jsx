import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  fetchApplications,
  updateApplicationStatus,
  updateApplicationNotes,
  deleteApplication,
  fetchApplicationAnalytics
} from '../api/client';
import ApplicationAnalytics from '../components/ApplicationAnalytics';
import ApplicationKanban from '../components/ApplicationKanban';

const STATUS_OPTIONS = ['', 'SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'];

export default function Applications() {
  const [applications, setApplications] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [viewMode, setViewMode] = useState('kanban'); // 'kanban' or 'table'
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const loadTrackerData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [appsData, analyticsData] = await Promise.all([
        fetchApplications({ page: 0, size: 100, status: statusFilter || undefined }),
        fetchApplicationAnalytics()
      ]);
      setApplications(appsData.content || []);
      setAnalytics(analyticsData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    loadTrackerData();
  }, [loadTrackerData]);

  async function handleUpdateStatus(id, newStatus) {
    try {
      await updateApplicationStatus(id, newStatus);
      await loadTrackerData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleUpdateNotes(id, newNotes) {
    try {
      await updateApplicationNotes(id, newNotes);
      await loadTrackerData();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Are you sure you want to remove this application from tracking?')) return;
    try {
      await deleteApplication(id);
      await loadTrackerData();
    } catch (err) {
      alert(err.message);
    }
  }

  const filteredApps = applications.filter(app => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    const title = (app.job?.title || '').toLowerCase();
    const company = (app.job?.company || '').toLowerCase();
    return title.includes(q) || company.includes(q);
  });

  return (
    <div style={{ padding: '40px 24px', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 700, margin: 0 }}>Application Tracker</h1>
          <p style={{ color: 'var(--text-muted)', margin: '4px 0 0' }}>
            Manage your job application pipeline and career response metrics.
          </p>
        </div>

        {/* View mode toggle */}
        <div style={{ display: 'flex', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '8px', padding: '4px' }}>
          <button
            onClick={() => setViewMode('kanban')}
            className={`btn-secondary ${viewMode === 'kanban' ? 'active' : ''}`}
            style={{
              padding: '6px 16px',
              fontSize: '0.85rem',
              borderRadius: '6px',
              background: viewMode === 'kanban' ? 'var(--primary, #a855f7)' : 'transparent',
              color: '#fff',
              border: 'none',
              cursor: 'pointer'
            }}
          >
            Kanban Board
          </button>
          <button
            onClick={() => setViewMode('table')}
            className={`btn-secondary ${viewMode === 'table' ? 'active' : ''}`}
            style={{
              padding: '6px 16px',
              fontSize: '0.85rem',
              borderRadius: '6px',
              background: viewMode === 'table' ? 'var(--primary, #a855f7)' : 'transparent',
              color: '#fff',
              border: 'none',
              cursor: 'pointer'
            }}
          >
            Table View
          </button>
        </div>
      </div>

      {/* Analytics Banner */}
      <ApplicationAnalytics analytics={analytics} />

      {/* Search & Filter Bar */}
      <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Filter by company or position..."
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          className="filter-input"
          style={{ flex: 1, minWidth: '250px' }}
        />
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          className="filter-select"
        >
          <option value="">All Statuses</option>
          {STATUS_OPTIONS.filter(Boolean).map(s => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      {loading && (
        <div style={{ color: 'var(--text-muted)', padding: '40px 0' }}>Loading applications...</div>
      )}

      {error && (
        <div style={{ padding: '16px', background: 'rgba(239, 68, 68, 0.15)', color: '#f87171', borderRadius: '8px' }}>
          ⚠ {error}
        </div>
      )}

      {!loading && !error && (
        <>
          {viewMode === 'kanban' ? (
            <ApplicationKanban
              applications={filteredApps}
              onUpdateStatus={handleUpdateStatus}
              onUpdateNotes={handleUpdateNotes}
              onDelete={handleDelete}
            />
          ) : (
            <div style={{ background: 'rgba(255, 255, 255, 0.03)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '12px', overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ background: 'rgba(255, 255, 255, 0.05)', borderBottom: '1px solid rgba(255, 255, 255, 0.08)', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    <th style={{ padding: '12px 16px' }}>Position</th>
                    <th style={{ padding: '12px 16px' }}>Company</th>
                    <th style={{ padding: '12px 16px' }}>Fit Match</th>
                    <th style={{ padding: '12px 16px' }}>Status</th>
                    <th style={{ padding: '12px 16px' }}>Notes</th>
                    <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredApps.map(app => (
                    <tr key={app.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)', fontSize: '0.9rem' }}>
                      <td style={{ padding: '12px 16px' }}>
                        <Link to={`/dashboard/${app.job?.id}`} style={{ color: '#f8fafc', fontWeight: 600, textDecoration: 'none' }}>
                          {app.job?.title || 'Untitled Position'}
                        </Link>
                      </td>
                      <td style={{ padding: '12px 16px', color: 'var(--text-muted)' }}>{app.job?.company || 'Unknown'}</td>
                      <td style={{ padding: '12px 16px' }}>
                        {app.fitScore != null ? (
                          <span style={{
                            fontWeight: 700,
                            color: app.fitScore >= 70 ? '#34d399' : app.fitScore >= 40 ? '#fbbf24' : '#f87171'
                          }}>
                            {app.fitScore}%
                          </span>
                        ) : '—'}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <select
                          value={app.status}
                          onChange={(e) => handleUpdateStatus(app.id, e.target.value)}
                          className="filter-select"
                          style={{ fontSize: '0.8rem' }}
                        >
                          {STATUS_OPTIONS.filter(Boolean).map(s => (
                            <option key={s} value={s}>{s}</option>
                          ))}
                        </select>
                      </td>
                      <td style={{ padding: '12px 16px', color: '#cbd5e1', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {app.notes || '—'}
                      </td>
                      <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                        <button
                          onClick={() => {
                            const newNotes = prompt('Edit notes:', app.notes || '');
                            if (newNotes !== null) handleUpdateNotes(app.id, newNotes);
                          }}
                          style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', marginRight: '8px' }}
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(app.id)}
                          style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer' }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                  {filteredApps.length === 0 && (
                    <tr>
                      <td colSpan={6} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                        No applications found matching criteria.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
