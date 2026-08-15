import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  preview: { host: '127.0.0.1', port: 5173 },
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8081',
      '/uploads': process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8081',
    },
  },
});
