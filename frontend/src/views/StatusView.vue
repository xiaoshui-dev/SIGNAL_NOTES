<script setup>
import { ArrowLeft, RefreshCw, Search } from 'lucide-vue-next';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BlogHeader from '../components/BlogHeader.vue';
import BlogFooter from '../components/BlogFooter.vue';

const route = useRoute();
const router = useRouter();
const code = computed(() => String(route.params.code || '404'));
const copy = computed(() => ({
  '403': ['403 / ACCESS DENIED', '这里的信号只对授权用户开放。', '返回来源页或使用管理员账号重新登录。'],
  '404': ['404 / LOST SIGNAL', '这个页面没有找到。', '链接可能已经改变，先从博客首页继续阅读。'],
  '500': ['500 / SYSTEM PAUSED', '服务暂时没有回应。', '可以重试一次，或返回博客继续浏览已缓存的内容。'],
  '503': ['MAINTENANCE / BACK SOON', '站点正在进行短暂维护。', '预计很快恢复，RSS 和已分享的文章链接仍然有效。'],
}[code.value] || ['404 / LOST SIGNAL', '这个页面没有找到。', '从博客首页继续阅读。']));
</script>

<template>
  <div class="blog-shell"><BlogHeader /><main class="blog-main status-page"><span class="page-index">{{ copy[0] }}</span><h1>{{ copy[1] }}</h1><p>{{ copy[2] }}</p><div class="status-actions"><button class="button button-primary" @click="router.go(0)"><RefreshCw :size="16" />重试</button><RouterLink class="button" to="/blog"><ArrowLeft :size="16" />返回博客</RouterLink><RouterLink v-if="code==='404'" class="button" to="/blog/search"><Search :size="16" />搜索文章</RouterLink></div></main><BlogFooter /></div>
</template>
