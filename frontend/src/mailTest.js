const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

export async function runMailTest({ recipient, saveCurrentSettings, sendTest, updateStatus }) {
  const normalizedRecipient = recipient.trim();
  if (!EMAIL_PATTERN.test(normalizedRecipient)) {
    const status = { pending: false, tone: 'error', message: '请输入有效的测试邮箱' };
    updateStatus(status);
    return { sent: false, ...status };
  }

  updateStatus({ pending: true, tone: 'info', message: '正在保存配置并连接 SMTP…' });
  try {
    await saveCurrentSettings();
    const result = await sendTest(normalizedRecipient);
    const status = {
      pending: false,
      tone: result.sent ? 'success' : result.configured ? 'error' : 'info',
      message: result.message || '测试邮件已处理',
    };
    updateStatus(status);
    return { ...result, ...status };
  } catch (error) {
    const status = { pending: false, tone: 'error', message: error.message || '邮件测试失败' };
    updateStatus(status);
    return { sent: false, ...status };
  }
}
