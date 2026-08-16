import test from 'node:test';
import assert from 'node:assert/strict';
import { createEditorAutosave, statusForAutosave } from '../src/editorAutosave.js';

function fakeTimers() {
  const callbacks = [];
  return {
    callbacks,
    setTimer(callback) {
      callbacks.push(callback);
      return callbacks.length - 1;
    },
    clearTimer() {},
  };
}

test('cancel invalidates a pending autosave even if its callback still runs', async () => {
  const timers = fakeTimers();
  const saved = [];
  const autosave = createEditorAutosave({ setTimer: timers.setTimer, clearTimer: timers.clearTimer });

  autosave.schedule('post-1', () => saved.push('post-1'));
  autosave.cancel();
  await timers.callbacks[0]();

  assert.deepEqual(saved, []);
});

test('rescheduling invalidates the previous editor callback', async () => {
  const timers = fakeTimers();
  const saved = [];
  const autosave = createEditorAutosave({ setTimer: timers.setTimer, clearTimer: timers.clearTimer });

  autosave.schedule('post-1', () => saved.push('post-1'));
  autosave.schedule('post-2', () => saved.push('post-2'));
  await timers.callbacks[0]();
  await timers.callbacks[1]();

  assert.deepEqual(saved, ['post-2']);
});

test('autosave preserves the status of an already published post', () => {
  assert.equal(statusForAutosave('PUBLISHED'), 'PUBLISHED');
  assert.equal(statusForAutosave('DRAFT'), 'DRAFT');
});
