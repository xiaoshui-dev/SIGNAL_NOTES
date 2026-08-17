import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const admin = readFileSync(new URL('../src/views/AdminView.vue', import.meta.url), 'utf8');
const css = readFileSync(new URL('../src/assets/styles.css', import.meta.url), 'utf8');

test('admin shell uses the authenticated identity and command navigation', () => {
  assert.doesNotMatch(admin, /林默\s*·\s*管理员/);
  assert.match(admin, /currentUser\.name/);
  assert.match(admin, /admin-command-rail/);
  assert.match(css, /\.admin-command-rail/);
  assert.match(css, /router-link-active/);
});

test('admin operations expose nearby busy and result states', () => {
  assert.match(admin, /:aria-busy="saving"/);
  assert.match(admin, /:aria-busy="settingsSaving"/);
  assert.match(admin, /:aria-busy="mailTestStatus\.pending"/);
  assert.match(admin, /admin-notice/);
});

test('account avatar controls only expose generated or local media sources', () => {
  assert.doesNotMatch(admin, /头像地址/);
  assert.match(admin, /从媒体库选择/);
  assert.match(admin, /uploadAvatar/);
  assert.match(admin, /使用自动生成头像/);
});
