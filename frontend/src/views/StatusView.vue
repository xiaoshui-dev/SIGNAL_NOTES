<script setup>
import { ArrowLeft, RefreshCw, Search } from 'lucide-vue-next';
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BlogHeader from '../components/BlogHeader.vue';
import BlogFooter from '../components/BlogFooter.vue';
import { useSite } from '../site';

const route = useRoute();
const router = useRouter();
const { site, loadSite } = useSite();
const code = computed(() => String(route.params.code || '404'));
const copy = computed(() => {
  const prefix = ['403', '404', '500', '503'].includes(code.value) ? `status${code.value}` : 'statusDefault';
  return [site[`${prefix}Label`], site[`${prefix}Title`], site[`${prefix}Description`]];
});
onMounted(() => { loadSite().catch(() => {}); });
</script>

<template>
  <div class="blog-shell"><BlogHeader /><main class="blog-main status-page"><span class="page-index">{{ copy[0] }}</span><h1>{{ copy[1] }}</h1><p>{{ copy[2] }}</p><div class="status-actions"><button class="button button-primary" @click="router.go(0)"><RefreshCw :size="16" />{{ site.statusRetryLabel }}</button><RouterLink class="button" to="/blog"><ArrowLeft :size="16" />{{ site.statusBackLabel }}</RouterLink><RouterLink v-if="code==='404'" class="button" to="/blog/search"><Search :size="16" />{{ site.statusSearchLabel }}</RouterLink></div></main><BlogFooter /></div>
</template>
