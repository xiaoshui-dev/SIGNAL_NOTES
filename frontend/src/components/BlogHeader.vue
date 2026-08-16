<script setup>
import { Home, Menu, Moon, Search, Sun, X } from 'lucide-vue-next';
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import BrandLogo from './BrandLogo.vue';
import { theme, toggleTheme } from '../theme';
import { useSite } from '../site';

const open = ref(false);
const query = ref('');
const router = useRouter();
const { site, loadSite } = useSite();
loadSite().catch(() => {});
function search() { if (query.value.trim()) router.push({ path: '/blog/search', query: { q: query.value.trim() } }); }
</script>

<template>
  <header class="blog-header">
    <BrandLogo to="/blog" />
    <nav class="blog-nav" :class="{ 'is-open': open }" aria-label="博客导航">
      <RouterLink to="/blog">{{ site.blogNavPostsLabel }}</RouterLink><RouterLink to="/blog/categories">{{ site.blogNavCategoriesLabel }}</RouterLink><RouterLink to="/blog/tags">{{ site.blogNavTagsLabel }}</RouterLink><RouterLink to="/blog/archives">{{ site.blogNavArchivesLabel }}</RouterLink><RouterLink to="/blog/about">{{ site.blogNavAboutLabel }}</RouterLink>
    </nav>
    <form class="header-search" role="search" @submit.prevent="search"><Search :size="16" /><input v-model="query" type="search" :aria-label="site.blogNavSearchPlaceholder" :placeholder="site.blogNavSearchPlaceholder" /></form>
    <div class="blog-header-actions">
      <RouterLink class="icon-button" to="/" :title="site.blogNavHomeLabel" :aria-label="site.blogNavHomeLabel"><Home :size="18" /></RouterLink>
      <button class="icon-button" type="button" title="切换明暗主题" aria-label="切换明暗主题" @click="toggleTheme"><Sun v-if="theme === 'dark'" :size="18" /><Moon v-else :size="18" /></button>
      <button class="icon-button blog-menu-button" type="button" :title="open ? '关闭菜单' : '打开菜单'" :aria-label="open ? '关闭菜单' : '打开菜单'" @click="open = !open"><X v-if="open" :size="18" /><Menu v-else :size="18" /></button>
    </div>
  </header>
</template>
