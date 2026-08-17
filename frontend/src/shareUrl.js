export function canonicalShareUrl(value) {
  const parsed = new URL(value);
  parsed.search = '';
  parsed.hash = '';
  return parsed.toString();
}

export function resolveShareUrl(post, origin) {
  const canonical = String(post?.canonicalUrl || '').trim();
  if (canonical) {
    try {
      const parsed = new URL(canonical);
      if (parsed.protocol === 'http:' || parsed.protocol === 'https:') return canonicalShareUrl(parsed.toString());
    } catch {
      // Invalid canonical URLs fall back to the current site route.
    }
  }
  return canonicalShareUrl(`${String(origin || '').replace(/\/$/, '')}/blog/posts/${encodeURIComponent(post?.slug || '')}`);
}
