<script setup>
import { Home, Menu, Moon, Search, Sun, X } from 'lucide-vue-next';
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import BrandLogo from './BrandLogo.vue';
import { theme, toggleTheme } from '../theme';

const open = ref(false);
const query = ref('');
const router = useRouter();
function search() { if (query.value.trim()) router.push({ path: '/blog/search', query: { q: query.value.trim() } }); }
</script>

<template>
  <header class="blog-header">
    <BrandLogo to="/blog" />
    <nav class="blog-nav" :class="{ 'is-open': open }" aria-label="博客导航">
      <RouterLink to="/blog">文章</RouterLink><RouterLink to="/blog/categories">分类</RouterLink><RouterLink to="/blog/archives">归档</RouterLink><RouterLink to="/blog/about">关于</RouterLink>
    </nav>
    <form class="header-search" role="search" @submit.prevent="search"><Search :size="16" /><input v-model="query" type="search" aria-label="搜索文章" placeholder="搜索" /></form>
    <div class="blog-header-actions">
      <RouterLink class="icon-button" to="/" title="站点介绍" aria-label="返回站点介绍"><Home :size="18" /></RouterLink>
      <button class="icon-button" type="button" title="切换明暗主题" aria-label="切换明暗主题" @click="toggleTheme"><Sun v-if="theme === 'dark'" :size="18" /><Moon v-else :size="18" /></button>
      <button class="icon-button blog-menu-button" type="button" @click="open = !open"><X v-if="open" :size="18" /><Menu v-else :size="18" /></button>
    </div>
  </header>
</template>
