import { createRouter, createWebHistory } from 'vue-router';
import LandingView from './views/LandingView.vue';
import BlogView from './views/BlogView.vue';
import ArticleView from './views/ArticleView.vue';
import AdminView from './views/AdminView.vue';

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: LandingView },
    { path: '/blog', component: BlogView },
    { path: '/blog/posts/:slug', component: ArticleView },
    { path: '/blog/:section(.*)', component: BlogView },
    { path: '/admin/:pathMatch(.*)*', component: AdminView },
  ],
});

export default router;
