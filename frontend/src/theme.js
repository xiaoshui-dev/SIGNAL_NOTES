import { ref, watchEffect } from 'vue';

const initial = localStorage.getItem('signal-theme') || (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
export const theme = ref(initial);
watchEffect(() => {
  document.documentElement.dataset.theme = theme.value;
  localStorage.setItem('signal-theme', theme.value);
});
export function toggleTheme() { theme.value = theme.value === 'dark' ? 'light' : 'dark'; }
