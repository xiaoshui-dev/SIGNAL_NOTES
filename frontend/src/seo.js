import { site } from './site';

function upsert(name, content, attribute = 'name') {
  if (!content) return;
  let node = document.head.querySelector(`meta[${attribute}="${name}"]`);
  if (!node) { node = document.createElement('meta'); node.setAttribute(attribute, name); document.head.appendChild(node); }
  node.setAttribute('content', content);
}

export function setPageSeo({ title = `${site.siteName} | ${site.siteShortName}`, description = site.heroSummary, canonical = window.location.href, type = 'website' } = {}) {
  document.title = title;
  upsert('description', description);
  upsert('og:title', title, 'property'); upsert('og:description', description, 'property'); upsert('og:type', type, 'property');
  upsert('twitter:card', 'summary_large_image'); upsert('twitter:title', title); upsert('twitter:description', description);
  let link = document.head.querySelector('link[rel="canonical"]');
  if (!link) { link = document.createElement('link'); link.rel = 'canonical'; document.head.appendChild(link); }
  link.href = canonical;
}

export function setArticleJsonLd(article) {
  const id = 'signal-article-jsonld'; document.getElementById(id)?.remove();
  const script = document.createElement('script'); script.id = id; script.type = 'application/ld+json';
  script.textContent = JSON.stringify({ '@context': 'https://schema.org', '@type': 'Article', headline: article.title, description: article.excerpt, datePublished: article.publishedAt, dateModified: article.updatedAt, author: { '@type': 'Person', name: article.authorName || '林默' }, mainEntityOfPage: article.canonicalUrl || window.location.href, image: article.cover ? [new URL(article.cover, window.location.origin).href] : [] });
  document.head.appendChild(script);
}
