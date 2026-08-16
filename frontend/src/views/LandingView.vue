<script setup>
import { ArrowDown, ArrowRight, Menu, Moon, Search, Sun, X } from 'lucide-vue-next';
import { computed, onMounted, ref } from 'vue';
import BrandLogo from '../components/BrandLogo.vue';
import { theme, toggleTheme } from '../theme';
import { loadCategories, loadPosts } from '../api';
import { useSite } from '../site';

const menuOpen = ref(false);
const posts = ref([]);
const categories = ref([]);
const contentStatus = ref('正在加载精选文章');
const { site, loadSite } = useSite();
const topics = computed(() => {
  try { const configured = JSON.parse(site.landingTopics || '[]'); if (Array.isArray(configured) && configured.length) return configured; } catch {}
  return categories.value.map((category, index) => ({ number: String(index + 1).padStart(2, '0'), name: category.name, summary: category.description }));
});
const featured = computed(() => posts.value.slice(0, 3));

onMounted(async () => {
  loadSite().catch(() => {});
  try {
    const [postData, categoryData] = await Promise.all([loadPosts(), loadCategories()]);
    posts.value = Array.isArray(postData) ? postData : [];
    categories.value = Array.isArray(categoryData) ? categoryData : [];
    contentStatus.value = posts.value.length ? '' : '暂时还没有发布文章';
  } catch (error) { contentStatus.value = error.message || '内容服务暂时不可用'; }
});
</script>

<template>
  <main class="landing-page">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero-media" aria-hidden="true"><img src="/assets/hero-circuit.jpg" alt="" /><div class="hero-shade" /><div class="signal-scan" /></div>
      <header class="hero-nav"><BrandLogo light /><nav class="landing-nav" :class="{ 'is-open': menuOpen }" aria-label="介绍页导航"><RouterLink to="/blog">文章</RouterLink><a href="#topics">主题</a><a href="#about">关于</a></nav><div class="hero-actions"><RouterLink class="icon-button on-visual" to="/blog/search" title="搜索" aria-label="搜索"><Search :size="18" /></RouterLink><button class="icon-button on-visual" type="button" title="切换明暗主题" aria-label="切换明暗主题" @click="toggleTheme"><Sun v-if="theme === 'dark'" :size="18" /><Moon v-else :size="18" /></button><button class="icon-button on-visual mobile-menu" type="button" title="打开菜单" aria-label="打开菜单" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="19" /><Menu v-else :size="19" /></button></div></header>
      <div class="hero-content"><div class="eyebrow reveal reveal-1"><span class="live-dot" /> {{ site.heroEyebrow }}</div><h1 id="hero-title" class="reveal reveal-2">{{ site.heroTitle }}</h1><p class="hero-summary reveal reveal-3">{{ site.heroSummary }}</p><div class="hero-cta reveal reveal-4"><RouterLink class="button button-primary" to="/blog">进入博客 <ArrowRight :size="18" /></RouterLink><a class="button button-quiet on-dark" href="#featured">查看精选 <ArrowDown :size="17" /></a></div></div>
      <div class="hero-status reveal reveal-4"><div><span>最近更新</span><strong>{{ posts[0]?.publishedAt || '—' }}</strong></div><div><span>正在记录</span><strong>AI · SYSTEMS · CODE</strong></div><a href="#featured">向下探索 <ArrowDown :size="15" /></a></div>
    </section>
    <section id="topics" class="landing-band topic-band"><div class="section-kicker">01 / TOPICS</div><div class="band-heading"><h2>观察技术，也观察技术如何改变人。</h2><p>从底层系统到日常工具，用真实项目和长期实践拆解变化。</p></div><div class="topic-index"><RouterLink v-for="topic in topics" :key="topic.number || topic.name" class="topic-row" :to="{ path: '/blog', query: { topic: topic.name } }"><span>{{ topic.number }}</span><strong>{{ topic.name }}</strong><small>{{ topic.summary }}</small><ArrowRight :size="20" /></RouterLink></div></section>
    <section id="featured" class="landing-band featured-band"><div class="section-kicker">02 / SELECTED</div><div class="band-heading compact"><h2>从这里开始读</h2><RouterLink to="/blog">全部文章 <ArrowRight :size="17" /></RouterLink></div><div v-if="featured.length" class="featured-grid"><RouterLink v-for="(post, index) in featured" :key="post.id" class="feature-story" :class="index === 0 ? 'feature-main' : index === 1 ? 'feature-text' : 'feature-accent'" :to="`/blog/posts/${post.slug}`"><img v-if="index === 0" :src="post.cover" :alt="post.coverAlt" /><div><span>{{ post.category }} · {{ post.readMinutes }} 分钟</span><h3>{{ post.title }}</h3><p v-if="index === 1">{{ post.excerpt }}</p><ArrowRight v-if="index === 2" :size="24" /></div></RouterLink></div><div v-else class="landing-empty"><p>{{ contentStatus }}</p><RouterLink class="button" to="/blog">查看博客</RouterLink></div></section>
    <section id="about" class="landing-band about-band"><div class="section-kicker">03 / ABOUT</div><div class="about-copy"><h2>{{ site.aboutTitle }}</h2><p>{{ site.aboutLead }} {{ site.aboutBody }}</p><RouterLink class="text-link" to="/blog/about">了解这个项目 <ArrowRight :size="17" /></RouterLink></div></section>
    <footer class="landing-footer"><BrandLogo light /><p>{{ site.footerDescription }}</p><div><RouterLink to="/blog">进入博客</RouterLink><RouterLink to="/blog/about">关于</RouterLink><RouterLink to="/blog/contact">联系</RouterLink><RouterLink to="/blog/privacy">隐私</RouterLink></div><small>{{ site.copyrightText }} · {{ site.licenseText }}</small></footer>
  </main>
</template>
