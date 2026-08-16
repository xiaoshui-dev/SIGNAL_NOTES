import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  DEFAULT_AUTHOR_NAME,
  authorInitials,
  createPostAuthor,
  displayAdminIdentity,
  resolveAuthorName,
  resolveAvatarUrl,
} from '../src/authorIdentity.js';

test('new posts use the authenticated account name instead of demo copy', () => {
  assert.equal(createPostAuthor({ siteAuthorName: '站点作者', userName: 'Sheldon' }), 'Sheldon');
  assert.equal(createPostAuthor({ siteAuthorName: 'Sheldon', userName: '  ' }), 'Sheldon');
  assert.equal(createPostAuthor({}), DEFAULT_AUTHOR_NAME);
  assert.notEqual(DEFAULT_AUTHOR_NAME, '林默');
});

test('author presentation follows the displayed name', () => {
  assert.equal(resolveAuthorName('  Sheldon  ', '站点作者'), 'Sheldon');
  assert.equal(authorInitials('Sheldon'), 'SH');
  assert.equal(authorInitials('张三'), '张三');
});

test('admin header shows the authenticated account identity', () => {
  assert.equal(displayAdminIdentity({ name: 'Sheldon', loginName: 'sheldon' }), 'Sheldon');
  assert.equal(displayAdminIdentity({ name: '', loginName: 'sheldon' }), 'sheldon');
});

test('uploaded avatars win and generated pixel avatars are deterministic local data URLs', () => {
  assert.equal(resolveAvatarUrl({ uploadedAvatarUrl: '/uploads/avatar.png', name: 'Sheldon' }), '/uploads/avatar.png');
  const first = resolveAvatarUrl({ name: 'Sheldon' });
  const second = resolveAvatarUrl({ name: '  Sheldon  ' });
  assert.equal(first, second);
  assert.match(first, /^data:image\/svg\+xml/);
  assert.doesNotMatch(first, /api\.dicebear\.com/);
});

test('account identity is rendered consistently across public and admin surfaces', () => {
  const admin = readFileSync(new URL('../src/views/AdminView.vue', import.meta.url), 'utf8');
  const article = readFileSync(new URL('../src/views/ArticleView.vue', import.meta.url), 'utf8');
  const blog = readFileSync(new URL('../src/views/BlogView.vue', import.meta.url), 'utf8');
  const poster = readFileSync(new URL('../src/components/SharePoster.vue', import.meta.url), 'utf8');
  for (const source of [admin, article, blog]) assert.match(source, /PixelAvatar/);
  assert.match(admin, /\/admin\/account\/profile/);
  assert.match(admin, /uploadAvatar/);
  assert.match(poster, /authorAvatarUrl/);
});
