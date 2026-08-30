const API_BASE = 'http://localhost:8080/api';

/**
 * DOT Field API client.
 * Wraps native fetch, unwraps ApiResponse<T> envelope, handles errors.
 */
async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!res.ok) {
    let errorMessage = `Request failed with status ${res.status}`;
    try {
      const errorBody = await res.json();
      if (errorBody.message) errorMessage = errorBody.message;
    } catch {
      // Response body was not JSON
    }
    throw new Error(errorMessage);
  }

  const body = await res.json();
  return body.data;
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
