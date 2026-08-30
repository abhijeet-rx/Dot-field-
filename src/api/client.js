const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * DOT Field API client.
 * Wraps native fetch, unwraps ApiResponse<T> envelope, auto-injects Bearer auth header, and handles errors.
 */
async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const token = localStorage.getItem('dotfield_token');

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(url, {
    ...options,
    headers,
  });

  if (!res.ok) {
    let errorMessage = `Request failed with status ${res.status}`;
    try {
      const errorBody = await res.json();
      if (errorBody.message) errorMessage = errorBody.message;
    } catch {
      // Response body was not JSON
    }

    if (res.status === 401 && !endpoint.startsWith('/auth/login')) {
      // Token expired or invalid
      localStorage.removeItem('dotfield_token');
      window.dispatchEvent(new Event('dotfield_auth_expired'));
    }

    throw new Error(errorMessage);
  }

  const body = await res.json();
  return body.data;
}

/** Authentication API */
export function registerApi({ email, password, name }) {
  return request('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, name }),
  });
}

export function loginApi({ email, password }) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function fetchMeApi() {
  return request('/auth/me');
}

/** GET /api/jobs with pagination and filters */
export function fetchJobs({ page = 0, size = 20, status, company, source, remoteType, employmentType } = {}) {
  const params = new URLSearchParams();
  params.set('page', page);
  params.set('size', size);
  if (status) params.set('status', status);
  if (company) params.set('company', company);
  if (source) params.set('source', source);
  if (remoteType) params.set('remoteType', remoteType);
  if (employmentType) params.set('employmentType', employmentType);
  return request(`/jobs?${params.toString()}`);
}

/** GET /api/jobs/{id} */
export function fetchJob(id) {
  return request(`/jobs/${id}`);
}

/** GET /api/jobs/{id}/match */
export function fetchJobMatch(id) {
  return request(`/jobs/${id}/match`);
}

/** GET /api/jobs/{id}/resume/tailor */
export function fetchTailoredResume(id) {
  return request(`/jobs/${id}/resume/tailor`);
}

/** GET /api/profile */
export function fetchProfile() {
  return request('/profile');
}
