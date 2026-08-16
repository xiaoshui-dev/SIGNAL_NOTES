export function adminLoginPresentation({ development, demoUsername, demoPassword }) {
  if (!development) {
    return {
      credentials: { email: '', password: '' },
      showDemoHint: false,
    };
  }

  return {
    credentials: { email: demoUsername, password: demoPassword },
    showDemoHint: true,
  };
}
