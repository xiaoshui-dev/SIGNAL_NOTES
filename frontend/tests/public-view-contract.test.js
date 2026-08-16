import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const landing = readFileSync(new URL('../src/views/LandingView.vue', import.meta.url), 'utf8');
const blog = readFileSync(new URL('../src/views/BlogView.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8');

test('landing is a five-stage interactive signal terminal', () => {
  assert.match(landing, /SignalIndex/);
  assert.match(landing, /hero-circuit-dither\.png/);
  assert.match(landing, /hero-resolution/);
  assert.match(landing, /signal-terminal-bar/);
  for (const id of ['intro', 'topics', 'featured', 'about', 'footer']) assert.match(landing, new RegExp(id));
});

test('public index keeps every established route and numbered scan rows', () => {
  assert.match(blog, /post-scan-number/);
  assert.match(blog, /terminal-filter/);
  assert.match(router, /\/blog\/:section\(\.\*\)/);
  assert.match(router, /\/blog\/categories\/:slug/);
  assert.match(router, /\/blog\/tags\/:slug/);
  assert.match(router, /\/blog\/authors\/:id/);
  for (const section of ['categories', 'tags', 'archives', 'search', 'about', 'contact', 'privacy']) {
    assert.match(blog, new RegExp(`section === ["']${section}["']`));
  }
});
