<script setup>
import { ArrowLeft, RefreshCw } from 'lucide-vue-next';
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BlogHeader from '../components/BlogHeader.vue';
import BlogFooter from '../components/BlogFooter.vue';
import SignalIndex from '../components/SignalIndex.vue';
import { useSite } from '../site';

const route = useRoute();
const router = useRouter();
const { site, loadSite } = useSite();
const code = computed(() => String(route.params.code || '404'));
const copy = computed(() => {
  const prefix = ['403', '404', '500', '503'].includes(code.value) ? `status${code.value}` : 'statusDefault';
  return [site[`${prefix}Label`], site[`${prefix}Title`], site[`${prefix}Description`]];
});
const signalItems = computed(() => [{ id: `status-${code.value}`, label: copy.value[0] }]);
onMounted(() => { loadSite().catch(() => {}); });
</script>

<template>
  <div class="blog-shell"><BlogHeader /><main :id="`status-${code}`" class="blog-main status-page status-signal-shell"><SignalIndex :items="signalItems" :active-id="`status-${code}`" /><span class="page-index">{{ copy[0] }}</span><h1>{{ copy[1] }}</h1><p>{{ copy[2] }}</p><div class="status-actions"><button class="button button-primary" type="button" @click="router.go(0)"><RefreshCw :size="16" />{{ site.statusRetryLabel }}</button><RouterLink class="button" to="/blog"><ArrowLeft :size="16" />{{ site.statusBackLabel }}</RouterLink></div></main><BlogFooter /></div>
</template>
