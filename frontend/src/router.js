import { createRouter, createWebHistory } from 'vue-router';
import LandingView from './views/LandingView.vue';
import BlogView from './views/BlogView.vue';
import ArticleView from './views/ArticleView.vue';
import AdminView from './views/AdminView.vue';
import StatusView from './views/StatusView.vue';

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: LandingView },
    { path: '/blog', component: BlogView },
    { path: '/blog/posts/:slug', component: ArticleView },
    { path: '/blog/categories/:slug', component: BlogView },
    { path: '/blog/tags', component: BlogView },
    { path: '/blog/tags/:slug', component: BlogView },
    { path: '/blog/authors/:id', component: BlogView },
    { path: '/blog/:section(.*)', component: BlogView },
    { path: '/status/:code', component: StatusView },
    { path: '/admin/:pathMatch(.*)*', component: AdminView },
    { path: '/:pathMatch(.*)*', component: StatusView, props: { code: '404' } },
  ],
});

export default router;
