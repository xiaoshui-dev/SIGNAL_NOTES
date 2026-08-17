import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const article = readFileSync(new URL('../src/views/ArticleView.vue', import.meta.url), 'utf8');
const poster = readFileSync(new URL('../src/components/SharePoster.vue', import.meta.url), 'utf8');
const status = readFileSync(new URL('../src/views/StatusView.vue', import.meta.url), 'utf8');

test('article exposes progress, indexed chapters, and accessible copy controls', () => {
  assert.match(article, /article-progress-bar/);
  assert.match(article, /SignalIndex/);
  assert.match(article, /aria-label.*复制代码/);
  assert.match(article, /Copy.*size/);
});

test('share poster includes complete identity and scannable poster metadata', () => {
  assert.match(poster, /1200/);
  assert.match(poster, /1440/);
  assert.match(poster, /authorName/);
  assert.match(poster, /publishedAt/);
  assert.match(poster, /margin:\s*4/);
  assert.match(poster, /aria-busy/);
});

test('status screen has a signal index and recovery paths', () => {
  assert.match(status, /SignalIndex/);
  assert.match(status, /status-signal-shell/);
  assert.match(status, /to="\/blog"/);
});
