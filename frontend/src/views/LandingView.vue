<script setup>
import { ArrowDown, ArrowRight, Menu, Moon, RadioTower, Search, Sun, X } from 'lucide-vue-next';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import BrandLogo from '../components/BrandLogo.vue';
import SignalIndex from '../components/SignalIndex.vue';
import heroDither from '../assets/generated/hero-circuit-dither.png';
import { theme, toggleTheme } from '../theme';
import { loadCategories, loadPosts } from '../api';
import { useSite } from '../site';

const menuOpen = ref(false);
const posts = ref([]);
const categories = ref([]);
const contentStatus = ref('loading');
const heroResolution = ref(38);
const activeSection = ref('intro');
const { site, loadSite } = useSite();
const sectionItems = [
  { id: 'intro', label: 'INTRO' },
  { id: 'topics', label: 'TOPICS' },
  { id: 'featured', label: 'SELECTED' },
  { id: 'about', label: 'ABOUT' },
  { id: 'footer', label: 'END' },
];
let sectionObserver;

const topics = computed(() => {
  let configured = [];
  try { configured = JSON.parse(site.landingTopics || '[]'); } catch { configured = []; }
  const configuredItems = (Array.isArray(configured) ? configured : [])
    .filter((item) => item && typeof item === 'object' && !Array.isArray(item) && typeof item.name === 'string' && item.name.trim())
    .map((item) => ({ ...item, name: item.name.trim(), summary: typeof item.summary === 'string' ? item.summary : '' }));
  const configuredByName = new Map(configuredItems.map((item) => [item.name, item]));
  const validCategories = categories.value.filter((category) => category && typeof category.name === 'string' && category.name.trim());
  if (!validCategories.length) return configuredItems;
  return validCategories.map((category, index) => {
    const saved = configuredByName.get(category.name) || {};
    return { number: String(index + 1).padStart(2, '0'), name: category.name, summary: saved.summary || (typeof category.description === 'string' ? category.description : '') };
  });
});
const featured = computed(() => posts.value.slice(0, 3));

function updateResolution(event) {
  const bounds = event.currentTarget.getBoundingClientRect();
  heroResolution.value = Math.round(Math.min(88, Math.max(14, ((event.clientX - bounds.left) / bounds.width) * 100)));
}

async function refreshLanding() {
  loadSite().catch(() => {});
  contentStatus.value = 'loading';
  try {
    const [postData, categoryData] = await Promise.all([loadPosts(), loadCategories()]);
    posts.value = Array.isArray(postData) ? postData : [];
    categories.value = Array.isArray(categoryData) ? categoryData : [];
    contentStatus.value = posts.value.length ? 'ready' : 'empty';
  } catch { contentStatus.value = 'error'; }
}

onMounted(() => {
  refreshLanding();
  sectionObserver = new IntersectionObserver((entries) => {
    const visible = entries.filter((entry) => entry.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
    if (visible?.target?.id) activeSection.value = visible.target.id;
  }, { rootMargin: '-20% 0px -55%', threshold: [0.05, 0.35, 0.65] });
  sectionItems.forEach(({ id }) => {
    const element = document.getElementById(id);
    if (element) sectionObserver.observe(element);
  });
});
onBeforeUnmount(() => sectionObserver?.disconnect());
</script>

<template>
  <main class="landing-page">
    <section id="intro" class="hero" aria-labelledby="hero-title" @pointermove="updateResolution" @pointerleave="heroResolution = 38">
      <div class="hero-media" aria-hidden="true" :style="{ '--hero-resolution': `${heroResolution}%` }">
        <img class="hero-photo" src="/assets/hero-circuit.jpg" alt="" />
        <img class="hero-dither" :src="heroDither" alt="" />
        <div class="hero-shade" />
      </div>
      <header class="hero-nav">
        <BrandLogo light />
        <div class="signal-terminal-bar"><RadioTower :size="14" aria-hidden="true" /><span>LIVE</span><span>SYS.2026.08</span><span>{{ posts.length }} NOTES</span></div>
        <nav class="landing-nav" :class="{ 'is-open': menuOpen }" aria-label="介绍页导航">
          <RouterLink to="/blog">{{ site.landingNavPosts }}</RouterLink><a href="#topics">{{ site.landingNavTopics }}</a><a href="#about">{{ site.landingNavAbout }}</a>
        </nav>
        <div class="hero-actions">
          <RouterLink class="icon-button on-visual" to="/blog/search" title="搜索" aria-label="搜索"><Search :size="18" /></RouterLink>
          <button class="icon-button on-visual" type="button" title="切换明暗主题" aria-label="切换明暗主题" @click="toggleTheme"><Sun v-if="theme === 'dark'" :size="18" /><Moon v-else :size="18" /></button>
          <button class="icon-button on-visual mobile-menu" type="button" :title="menuOpen ? '关闭菜单' : '打开菜单'" :aria-label="menuOpen ? '关闭菜单' : '打开菜单'" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="19" /><Menu v-else :size="19" /></button>
        </div>
      </header>
      <div class="hero-content">
        <div class="eyebrow pixel-label reveal reveal-1">{{ site.heroEyebrow }} / SIGNAL FOUND</div>
        <h1 id="hero-title" class="reveal reveal-2">{{ site.heroTitle }}</h1>
        <p class="hero-summary reveal reveal-3">{{ site.heroSummary }}</p>
        <div class="hero-cta reveal reveal-4"><RouterLink class="button button-primary" to="/blog">{{ site.heroEnterBlog }} <ArrowRight :size="18" /></RouterLink><a class="button button-quiet on-dark" href="#featured">{{ site.heroViewFeatured }} <ArrowDown :size="17" /></a></div>
      </div>
      <SignalIndex class="hero-section-index" :items="sectionItems" :active-id="activeSection" />
      <div class="hero-status reveal reveal-4"><div><span>{{ site.landingRecentLabel }}</span><strong>{{ posts[0]?.publishedAt || '—' }}</strong></div><div><span>{{ site.landingTopicsLabel }}</span><strong>{{ site.landingStatusTopics }}</strong></div><div><span>RESOLUTION</span><strong>{{ heroResolution }}%</strong></div><a href="#featured">{{ site.landingExploreLabel }} <ArrowDown :size="15" /></a></div>
    </section>

    <section id="topics" class="landing-band topic-band">
      <div class="section-kicker">01 / {{ site.landingTopicsSectionLabel }}</div>
      <div class="band-heading"><h2>{{ site.landingTopicsTitle }}</h2><p>{{ site.landingTopicsIntro }}</p></div>
      <div class="topic-index"><RouterLink v-for="topic in topics" :key="topic.name" class="topic-row" :to="{ path: '/blog', query: { topic: topic.name } }"><span>{{ topic.number }}</span><strong>{{ topic.name }}</strong><small>{{ topic.summary }}</small><ArrowRight :size="20" /></RouterLink></div>
    </section>

    <section id="featured" class="landing-band featured-band">
      <div class="section-kicker">02 / {{ site.landingSelectedLabel }}</div>
      <div class="band-heading compact"><h2>{{ site.featuredTitle }}</h2><RouterLink to="/blog">{{ site.landingAllPostsLabel }} <ArrowRight :size="17" /></RouterLink></div>
      <div v-if="featured.length" class="featured-grid"><RouterLink v-for="(post, index) in featured" :key="post.id" class="feature-story" :class="index === 0 ? 'feature-main' : index === 1 ? 'feature-text' : 'feature-accent'" :to="`/blog/posts/${post.slug}`"><img v-if="index === 0" :src="post.cover" :alt="post.coverAlt" /><div><span>{{ post.category }} · {{ post.readMinutes }} 分钟</span><h3>{{ post.title }}</h3><p v-if="index === 1">{{ post.excerpt }}</p><ArrowRight v-if="index === 2" :size="24" /></div></RouterLink></div>
      <div v-else class="landing-empty"><span>{{ contentStatus === 'error' ? site.noConnectionLabel : contentStatus === 'empty' ? (site.noPublicPosts || site.noNotesLabel) : (site.landingLoadingLabel || site.noPublicPostsDescription) }}</span><h3 v-if="contentStatus === 'error'">{{ site.noConnectionTitle || site.noNotesLabel }}</h3><h3 v-else-if="contentStatus === 'empty'">{{ site.noPublicPosts || site.noNotesLabel }}</h3><p>{{ contentStatus === 'error' ? site.noConnectionDescription : contentStatus === 'empty' ? site.noPublicPostsDescription : (site.landingLoadingLabel || site.noPublicPostsDescription) }}</p><button v-if="contentStatus === 'error'" class="button" type="button" @click="refreshLanding">{{ site.reconnectLabel }}</button><RouterLink v-else class="button" to="/blog">{{ site.landingAllPostsLabel }}</RouterLink></div>
    </section>

    <section id="about" class="landing-band about-band"><div class="section-kicker">03 / {{ site.landingAboutSectionLabel }}</div><div class="about-copy"><h2>{{ site.aboutTitle }}</h2><p>{{ site.aboutLead }} {{ site.aboutBody }}</p><RouterLink class="text-link" to="/blog/about">{{ site.landingAboutLink }} <ArrowRight :size="17" /></RouterLink></div></section>
    <footer id="footer" class="landing-footer"><BrandLogo light /><p>{{ site.footerDescription }}</p><div><RouterLink to="/blog">{{ site.landingFooterEnterLabel }}</RouterLink><RouterLink to="/blog/about">{{ site.landingFooterAboutLabel }}</RouterLink><RouterLink to="/blog/contact">{{ site.landingFooterContactLabel }}</RouterLink><RouterLink to="/blog/privacy">{{ site.landingFooterPrivacyLabel }}</RouterLink></div><small>{{ site.copyrightText }} · {{ site.licenseText }}</small></footer>
  </main>
</template>
