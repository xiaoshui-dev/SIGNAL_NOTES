<script setup>
import { AudioWaveform } from 'lucide-vue-next';
import { onMounted } from 'vue';
import { useSite } from '../site';
defineProps({ to: { type: String, default: '/' }, light: Boolean });
const { site, loadSite } = useSite();
onMounted(() => loadSite().catch(() => {}));
</script>

<template>
  <RouterLink class="brand" :to="to" :class="{ 'brand-light': light }">
    <span class="brand-mark" aria-hidden="true"><AudioWaveform :size="18" :stroke-width="1.75" /></span>
    <span class="brand-copy"><b>{{ site.siteName }}</b><small>{{ site.siteShortName }} · {{ site.siteTagline }}</small></span>
  </RouterLink>
</template>
