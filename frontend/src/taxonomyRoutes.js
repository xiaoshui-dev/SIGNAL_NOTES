function normalized(value) {
  return String(value || '').trim().toLocaleLowerCase();
}

function decoded(value) {
  try {
    return decodeURIComponent(String(value || ''));
  } catch {
    return String(value || '');
  }
}

export function taxonomyPath(kind, taxonomy) {
  return `/blog/${kind}/${encodeURIComponent(taxonomy.slug)}`;
}

export function resolveTaxonomy(taxonomies, routeSlug) {
  const slug = normalized(decoded(routeSlug));
  return taxonomies.find((taxonomy) => normalized(taxonomy.slug) === slug) || null;
}

export function postsForTaxonomy(posts, taxonomy, kind) {
  if (!taxonomy) return [];
  const name = normalized(taxonomy.name);
  if (kind === 'tag') {
    return posts.filter((post) => (post.tags || []).some((tag) => normalized(tag) === name));
  }
  return posts.filter((post) => normalized(post.category) === name);
}
