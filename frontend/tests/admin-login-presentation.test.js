import test from 'node:test';
import assert from 'node:assert/strict';
import { adminLoginPresentation } from '../src/adminLoginPresentation.js';

test('production login starts blank and hides demo credentials', () => {
  const presentation = adminLoginPresentation({
    development: false,
    demoUsername: 'admin',
    demoPassword: 'signal2026',
  });

  assert.deepEqual(presentation.credentials, { email: '', password: '' });
  assert.equal(presentation.showDemoHint, false);
});

test('development login may expose the local demo credentials', () => {
  const presentation = adminLoginPresentation({
    development: true,
    demoUsername: 'admin',
    demoPassword: 'signal2026',
  });

  assert.deepEqual(presentation.credentials, { email: 'admin', password: 'signal2026' });
  assert.equal(presentation.showDemoHint, true);
});
