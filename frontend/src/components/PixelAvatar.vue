<script setup>
import { computed } from 'vue';
import { resolveAvatarUrl } from '../authorIdentity';

const props = defineProps({
  name: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
  size: { type: Number, default: 48 },
});

const source = computed(() => resolveAvatarUrl({ uploadedAvatarUrl: props.avatarUrl, name: props.name }));
</script>

<template>
  <span class="pixel-avatar" :style="{ '--avatar-size': `${size}px` }">
    <img :src="source" :alt="`${name || '作者'}的头像`" />
  </span>
</template>
