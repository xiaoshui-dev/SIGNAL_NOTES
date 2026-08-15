<script setup>
import { Rss } from 'lucide-vue-next';
import BrandLogo from './BrandLogo.vue';
</script>

<template>
  <footer class="blog-footer">
    <div><BrandLogo to="/blog" /><p>关于 AI、系统与数字世界的独立技术博客。</p></div>
    <nav aria-label="页脚导航"><RouterLink to="/">站点介绍</RouterLink><RouterLink to="/blog/about">关于</RouterLink><RouterLink to="/blog/archives">归档</RouterLink><a href="/rss.xml"><Rss :size="14" /> RSS</a></nav>
    <small>© 2026 Signal Notes · 内容以 CC BY-NC-SA 4.0 发布</small>
  </footer>
</template>
