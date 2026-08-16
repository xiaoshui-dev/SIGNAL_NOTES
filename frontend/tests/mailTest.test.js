import test from 'node:test';
import assert from 'node:assert/strict';
import { runMailTest } from '../src/mailTest.js';

test('mail test saves the current form before sending and reports nearby progress', async () => {
  const events = [];
  const result = await runMailTest({
    recipient: 'reader@example.com',
    saveCurrentSettings: async () => { events.push('saved'); },
    sendTest: async (recipient) => {
      events.push(`sent:${recipient}`);
      return { sent: true, configured: true, message: '测试邮件已发送' };
    },
    updateStatus: (status) => events.push(status),
  });

  assert.deepEqual(events, [
    { pending: true, tone: 'info', message: '正在保存配置并连接 SMTP…' },
    'saved',
    'sent:reader@example.com',
    { pending: false, tone: 'success', message: '测试邮件已发送' },
  ]);
  assert.equal(result.sent, true);
});

test('mail test exposes a useful failure beside the button', async () => {
  const events = [];
  await runMailTest({
    recipient: 'reader@example.com',
    saveCurrentSettings: async () => {},
    sendTest: async () => ({ sent: false, configured: true, message: '邮件发送失败，请检查端口和邮箱授权码' }),
    updateStatus: (status) => events.push(status),
  });

  assert.deepEqual(events.at(-1), {
    pending: false,
    tone: 'error',
    message: '邮件发送失败，请检查端口和邮箱授权码',
  });
});

test('mail test rejects an invalid recipient before saving settings', async () => {
  let saved = false;
  const events = [];
  const result = await runMailTest({
    recipient: 'not-an-email',
    saveCurrentSettings: async () => { saved = true; },
    sendTest: async () => ({ sent: true }),
    updateStatus: (status) => events.push(status),
  });

  assert.equal(saved, false);
  assert.equal(result.sent, false);
  assert.deepEqual(events, [{ pending: false, tone: 'error', message: '请输入有效的测试邮箱' }]);
});
