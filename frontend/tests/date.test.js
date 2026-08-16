import test from 'node:test';
import assert from 'node:assert/strict';
import { formatDate } from '../src/date.js';

test('formats stored article dates and tolerates missing values', () => {
  assert.equal(formatDate(''), '');
  assert.match(formatDate('2026-08-17'), /2026.*8.*17/);
  assert.equal(formatDate('2026-08-17T03:00:00Z'), formatDate('2026-08-17'));
});
