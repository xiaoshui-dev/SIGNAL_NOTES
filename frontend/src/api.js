const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export function getAdminToken() { return localStorage.getItem('signal-admin-token') || ''; }

export async function apiRequest(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (!(options.body instanceof FormData)) headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) throw new Error(`API ${response.status}`);
  return response.status === 204 ? null : response.json();
}

export async function adminRequest(path, options = {}) {
  const token = getAdminToken();
  return apiRequest(path, { ...options, headers: { ...(token ? { Authorization: `Basic ${token}` } : {}), ...(options.headers || {}) } });
}

export function createAdminToken(username, password) { return btoa(`${username}:${password}`); }

export async function loadPosts(params = {}) {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value));
  return apiRequest(`/posts${query.toString() ? `?${query}` : ''}`);
}

export async function submitComment(payload) { return apiRequest('/comments', { method: 'POST', body: JSON.stringify(payload) }); }
export async function subscribe(email) { return apiRequest('/subscriptions', { method: 'POST', body: JSON.stringify({ email }) }); }
