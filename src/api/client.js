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

  if (res.status === 204) {
    return null;
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

/** Profile API */
export function fetchProfile() {
  return request('/profile');
}

export function updateProfileApi(data) {
  return request('/profile', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function fetchProfileCompleteness() {
  return request('/profile/completeness');
}

export function addSkillApi(skillData) {
  return request('/profile/skills', {
    method: 'POST',
    body: JSON.stringify(skillData),
  });
}

export function deleteSkillApi(skillId) {
  return request(`/profile/skills/${skillId}`, {
    method: 'DELETE',
  });
}

export function addExperienceApi(expData) {
  return request('/profile/experience', {
    method: 'POST',
    body: JSON.stringify(expData),
  });
}

export function deleteExperienceApi(expId) {
  return request(`/profile/experience/${expId}`, {
    method: 'DELETE',
  });
}

export function addEducationApi(eduData) {
  return request('/profile/education', {
    method: 'POST',
    body: JSON.stringify(eduData),
  });
}

export function deleteEducationApi(eduId) {
  return request(`/profile/education/${eduId}`, {
    method: 'DELETE',
  });
}

export function addProjectApi(projData) {
  return request('/profile/projects', {
    method: 'POST',
    body: JSON.stringify(projData),
  });
}

export function deleteProjectApi(projId) {
  return request(`/profile/projects/${projId}`, {
    method: 'DELETE',
  });
}

/** Jobs API */
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

export function fetchJob(id) {
  return request(`/jobs/${id}`);
}

export function fetchJobMatch(id) {
  return request(`/jobs/${id}/match`);
}

export function fetchTailoredResume(id) {
  return request(`/jobs/${id}/resume/tailor`);
}

/** Applications API */
export function fetchApplications({ page = 0, size = 20, status, search } = {}) {
  const params = new URLSearchParams();
  params.set('page', page);
  params.set('size', size);
  if (status) params.set('status', status);
  if (search) params.set('search', search);
  return request(`/applications?${params.toString()}`);
}

export function checkJobTrackedApi(jobId) {
  return request(`/applications/check?jobId=${jobId}`);
}

export function fetchApplication(id) {
  return request(`/applications/${id}`);
}

export function createApplication({ jobId, status = 'SAVED', notes = '' }) {
  return request('/applications', {
    method: 'POST',
    body: JSON.stringify({ jobId, status, notes }),
  });
}

export function updateApplicationStatus(id, status) {
  return request(`/applications/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export function updateApplicationNotes(id, notes) {
  return request(`/applications/${id}/notes`, {
    method: 'PUT',
    body: JSON.stringify({ notes }),
  });
}

export function deleteApplication(id) {
  return request(`/applications/${id}`, {
    method: 'DELETE',
  });
}

export function fetchApplicationAnalytics() {
  return request('/applications/analytics');
}
