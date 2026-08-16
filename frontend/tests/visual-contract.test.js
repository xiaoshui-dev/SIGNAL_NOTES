import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

test('pixel signal foundation is local, semantic, and motion-safe', () => {
  const css = readFileSync(new URL('../src/assets/styles.css', import.meta.url), 'utf8');
  const pkg = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'));

  assert.equal(pkg.dependencies['@fontsource/fusion-pixel-12px-proportional-sc'], '5.3.0');
  assert.equal(pkg.dependencies['@dicebear/core'], '9.4.2');
  assert.equal(pkg.dependencies['@dicebear/pixel-art'], '9.4.2');

  for (const token of [
    '--pixel-black',
    '--carbon',
    '--cold-paper',
    '--pixel-white',
    '--acid-signal',
    '--signal-orange',
    '--digital-blue',
    '--muted-gray',
  ]) {
    assert.match(css, new RegExp(token));
  }

  assert.doesNotMatch(css, /fonts\.googleapis\.com/);
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
  assert.match(css, /:focus-visible/);
});

test('shared chrome exposes the signal terminal language', () => {
  const index = readFileSync(new URL('../src/components/SignalIndex.vue', import.meta.url), 'utf8');
  const header = readFileSync(new URL('../src/components/BlogHeader.vue', import.meta.url), 'utf8');
  const footer = readFileSync(new URL('../src/components/BlogFooter.vue', import.meta.url), 'utf8');

  assert.match(index, /aria-current/);
  assert.match(index, /padStart\(2/);
  assert.match(header, /signal-status/);
  assert.match(footer, /signal-footer-index/);
});
