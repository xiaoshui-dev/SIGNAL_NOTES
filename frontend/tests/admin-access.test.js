import test from 'node:test';
import assert from 'node:assert/strict';
import { adminAccessForRole } from '../src/adminAccess.js';

test('viewer has a read-only dashboard', () => {
  const access = adminAccessForRole('VIEWER');
  assert.deepEqual(access.paths, ['/admin']);
  assert.equal(access.canWritePosts, false);
  assert.equal(access.canReadCommunity, false);
});

test('author can manage posts and media without editorial access', () => {
  const access = adminAccessForRole('AUTHOR');
  assert.deepEqual(access.paths, ['/admin', '/admin/posts', '/admin/media']);
  assert.equal(access.canWritePosts, true);
  assert.equal(access.canManageEditorial, false);
});

test('editor can manage content and community without system settings', () => {
  const access = adminAccessForRole('EDITOR');
  assert.equal(access.paths.includes('/admin/inbox'), true);
  assert.equal(access.paths.includes('/admin/settings'), false);
  assert.equal(access.canManageEditorial, true);
  assert.equal(access.canReadCommunity, true);
});

test('admin can reach every admin section', () => {
  const access = adminAccessForRole('ADMIN');
  assert.equal(access.paths.includes('/admin/users'), true);
  assert.equal(access.paths.includes('/admin/settings'), true);
  assert.equal(access.paths.includes('/admin/logs'), true);
});
