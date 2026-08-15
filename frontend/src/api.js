const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080/api';

export async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, { headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }, ...options });
  if (!response.ok) throw new Error(`API ${response.status}`);
  return response.status === 204 ? null : response.json();
}

export async function loadPosts(params = {}) {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value));
  return apiRequest(`/posts${query.toString() ? `?${query}` : ''}`);
}

export async function submitComment(payload) { return apiRequest('/comments', { method: 'POST', body: JSON.stringify(payload) }); }
export async function subscribe(email) { return apiRequest('/subscriptions', { method: 'POST', body: JSON.stringify({ email }) }); }
