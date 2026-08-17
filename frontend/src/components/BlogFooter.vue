<script setup>
import { Rss } from 'lucide-vue-next';
import { onMounted } from 'vue';
import BrandLogo from './BrandLogo.vue';
import { useSite } from '../site';
const { site, loadSite } = useSite();
onMounted(() => loadSite().catch(() => {}));
</script>

<template>
  <footer class="blog-footer">
    <div><BrandLogo to="/blog" /><p>{{ site.footerDescription }}</p></div>
    <nav :aria-label="site.blogNavHomeLabel"><RouterLink to="/">{{ site.blogNavHomeLabel }}</RouterLink><RouterLink to="/blog/categories">{{ site.blogNavCategoriesLabel }}</RouterLink><RouterLink to="/blog/tags">{{ site.blogNavTagsLabel }}</RouterLink><RouterLink to="/blog/about">{{ site.blogNavAboutLabel }}</RouterLink><RouterLink to="/blog/contact">{{ site.landingFooterContactLabel }}</RouterLink><RouterLink to="/blog/privacy">{{ site.landingFooterPrivacyLabel }}</RouterLink><RouterLink to="/blog/archives">{{ site.blogNavArchivesLabel }}</RouterLink><a href="/rss.xml"><Rss :size="14" /> {{ site.blogNavRssLabel }}</a></nav>
    <span class="signal-footer-index">SYS / END 05</span>
    <small>{{ site.copyrightText }} · {{ site.licenseText }}</small>
  </footer>
</template>
