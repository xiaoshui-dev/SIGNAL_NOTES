<script setup>
import { ArrowDown, ArrowRight, Menu, Moon, Search, Sun, X } from 'lucide-vue-next';
import { onMounted, ref } from 'vue';
import BrandLogo from '../components/BrandLogo.vue';
import { theme, toggleTheme } from '../theme';
import { loadPosts } from '../api';

const menuOpen = ref(false);
const topics = [['01', '人工智能', '模型、产品与人机协作'], ['02', '软件工程', '架构、代码与团队实践'], ['03', '系统设计', '性能、可靠性与基础设施'], ['04', '数字生活', '工具、阅读与个人工作流']];
const emptyPost = { slug: '', cover: '', coverAlt: '', category: '', readMinutes: 0, title: '暂无精选文章', excerpt: '' };
const posts = ref([emptyPost, emptyPost, emptyPost]);
const contentStatus = ref('正在加载精选文章');
onMounted(async () => {
  try {
    const value = await loadPosts();
    const published = Array.isArray(value) ? value.slice(0, 3) : [];
    posts.value = [0, 1, 2].map((index) => published[index] || emptyPost);
    contentStatus.value = published.length ? '' : '暂时还没有发布文章';
  } catch (error) {
    contentStatus.value = error.message || '内容服务暂时不可用';
  }
});
</script>

<template>
  <main class="landing-page">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero-media" aria-hidden="true"><img src="/assets/hero-circuit.jpg" alt="" /><div class="hero-shade" /><div class="signal-scan" /></div>
      <header class="hero-nav">
        <BrandLogo light />
        <nav class="landing-nav" :class="{ 'is-open': menuOpen }" aria-label="介绍页导航"><RouterLink to="/blog">文章</RouterLink><a href="#topics">主题</a><a href="#about">关于</a></nav>
        <div class="hero-actions"><RouterLink class="icon-button on-visual" to="/blog/search" title="搜索"><Search :size="18" /></RouterLink><button class="icon-button on-visual" type="button" @click="toggleTheme"><Sun v-if="theme === 'dark'" :size="18" /><Moon v-else :size="18" /></button><button class="icon-button on-visual mobile-menu" type="button" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="19" /><Menu v-else :size="19" /></button></div>
      </header>
      <div class="hero-content"><div class="eyebrow reveal reveal-1"><span class="live-dot" /> TECH NOTES / 2026</div><h1 id="hero-title" class="reveal reveal-2">把复杂技术<br />讲清楚</h1><p class="hero-summary reveal reveal-3">关于 AI、软件工程、系统与数字世界的长期记录。少一点噪声，多一点真正有用的理解。</p><div class="hero-cta reveal reveal-4"><RouterLink class="button button-primary" to="/blog">进入博客 <ArrowRight :size="18" /></RouterLink><a class="button button-quiet on-dark" href="#featured">查看精选 <ArrowDown :size="17" /></a></div></div>
      <div class="hero-status reveal reveal-4"><div><span>最近更新</span><strong>2026.08.15</strong></div><div><span>正在记录</span><strong>AI · SYSTEMS · CODE</strong></div><a href="#featured">向下探索 <ArrowDown :size="15" /></a></div>
    </section>
    <section id="topics" class="landing-band topic-band"><div class="section-kicker">01 / TOPICS</div><div class="band-heading"><h2>观察技术，也观察技术如何改变人。</h2><p>从底层系统到日常工具，用真实项目和长期实践拆解变化。</p></div><div class="topic-index"><RouterLink v-for="topic in topics" :key="topic[0]" class="topic-row" :to="{ path: '/blog', query: { topic: topic[1] } }"><span>{{ topic[0] }}</span><strong>{{ topic[1] }}</strong><small>{{ topic[2] }}</small><ArrowRight :size="20" /></RouterLink></div></section>
    <section id="featured" class="landing-band featured-band"><div class="section-kicker">02 / SELECTED</div><div class="band-heading compact"><h2>从这里开始读</h2><RouterLink to="/blog">全部文章 <ArrowRight :size="17" /></RouterLink></div><div class="featured-grid"><RouterLink class="feature-story feature-main" :to="`/blog/posts/${posts[0].slug}`"><img :src="posts[0].cover" :alt="posts[0].coverAlt" /><div><span>{{ posts[0].category }} · {{ posts[0].readMinutes }} 分钟</span><h3>{{ posts[0].title }}</h3></div></RouterLink><RouterLink class="feature-story feature-text" :to="`/blog/posts/${posts[1].slug}`"><span>{{ posts[1].category }} · {{ posts[1].readMinutes }} 分钟</span><h3>{{ posts[1].title }}</h3><p>{{ posts[1].excerpt }}</p></RouterLink><RouterLink class="feature-story feature-accent" :to="`/blog/posts/${posts[2].slug}`"><span>{{ posts[2].category }} · {{ posts[2].readMinutes }} 分钟</span><h3>{{ posts[2].title }}</h3><ArrowRight :size="24" /></RouterLink></div></section>
    <section id="about" class="landing-band about-band"><div class="section-kicker">03 / ABOUT</div><div class="about-copy"><h2>写给愿意慢下来理解技术的人。</h2><p>脉冲笔记关注技术背后的结构、取舍和真实影响。这里没有追逐热点的速报，只有经过实践、验证和反思之后的记录。</p><RouterLink class="text-link" to="/blog/about">了解这个项目 <ArrowRight :size="17" /></RouterLink></div></section>
    <footer class="landing-footer"><BrandLogo light /><p>关于 AI、系统与数字世界的独立技术博客。</p><div><RouterLink to="/blog">进入博客</RouterLink><RouterLink to="/blog/about">关于</RouterLink><RouterLink to="/blog/contact">联系</RouterLink><RouterLink to="/blog/privacy">隐私</RouterLink></div><small>© 2026 Signal Notes. Hero photo from Unsplash.</small></footer>
  </main>
</template>
