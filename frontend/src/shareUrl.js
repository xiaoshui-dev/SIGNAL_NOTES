export function resolveShareUrl(post, origin) {
  const canonical = String(post?.canonicalUrl || '').trim();
  if (canonical) {
    try {
      const parsed = new URL(canonical);
      if (parsed.protocol === 'http:' || parsed.protocol === 'https:') return parsed.toString();
    } catch {
      // Invalid canonical URLs fall back to the current site route.
    }
  }
  return `${String(origin || '').replace(/\/$/, '')}/blog/posts/${encodeURIComponent(post?.slug || '')}`;
}
