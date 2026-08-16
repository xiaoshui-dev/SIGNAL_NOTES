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
    <nav aria-label="页脚导航"><RouterLink to="/">站点介绍</RouterLink><RouterLink to="/blog/categories">分类</RouterLink><RouterLink to="/blog/tags">标签</RouterLink><RouterLink to="/blog/about">关于</RouterLink><RouterLink to="/blog/contact">联系</RouterLink><RouterLink to="/blog/privacy">隐私</RouterLink><RouterLink to="/blog/archives">归档</RouterLink><a href="/rss.xml"><Rss :size="14" /> RSS</a></nav>
    <small>{{ site.copyrightText }} · {{ site.licenseText }}</small>
  </footer>
</template>
