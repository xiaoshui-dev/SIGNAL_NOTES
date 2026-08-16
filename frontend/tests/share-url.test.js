import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveShareUrl } from '../src/shareUrl.js';

test('uses a valid configured canonical URL for sharing and QR codes', () => {
  assert.equal(
    resolveShareUrl({ slug: 'local-slug', canonicalUrl: 'https://notes.example.com/essays/canonical' }, 'http://127.0.0.1:5174'),
    'https://notes.example.com/essays/canonical',
  );
});

test('falls back to the current origin for missing or unsafe canonical URLs', () => {
  assert.equal(
    resolveShareUrl({ slug: 'system design', canonicalUrl: 'javascript:alert(1)' }, 'http://127.0.0.1:5174'),
    'http://127.0.0.1:5174/blog/posts/system%20design',
  );
});
