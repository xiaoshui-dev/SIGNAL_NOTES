import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './assets/styles.css';
import { setPageSeo } from './seo';

setPageSeo();
const app = createApp(App).use(router);
router.afterEach((to) => setPageSeo({ canonical: new URL(to.fullPath, window.location.origin).href }));
app.mount('#app');
