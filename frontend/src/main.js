import { createApp } from 'vue';
import '@fontsource/fusion-pixel-12px-proportional-sc/400.css';
import '@fontsource/noto-sans-sc/400.css';
import '@fontsource/noto-sans-sc/500.css';
import '@fontsource/noto-sans-sc/600.css';
import '@fontsource/noto-sans-sc/700.css';
import '@fontsource/noto-serif-sc/600.css';
import '@fontsource/noto-serif-sc/700.css';
import '@fontsource/noto-serif-sc/900.css';
import App from './App.vue';
import router from './router';
import './assets/styles.css';
import { setPageSeo } from './seo';
import { loadSite } from './site';

setPageSeo();
const app = createApp(App).use(router);
router.afterEach((to) => setPageSeo({ canonical: new URL(to.fullPath, window.location.origin).href }));
app.mount('#app');
loadSite().then(() => {
  const current = router.currentRoute.value;
  if (!current.path.startsWith('/blog/posts/')) {
    setPageSeo({ canonical: new URL(current.fullPath, window.location.origin).href });
  }
}).catch(() => {});
