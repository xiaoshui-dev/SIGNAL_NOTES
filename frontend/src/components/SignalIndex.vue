<script setup>
defineProps({
  items: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  orientation: { type: String, default: 'vertical' },
});
</script>

<template>
  <nav class="signal-index" :class="orientation" aria-label="页面章节">
    <a
      v-for="(item, index) in items"
      :key="item.id"
      :href="`#${item.id}`"
      :aria-current="item.id === activeId ? 'location' : undefined"
    >
      <span>{{ String(index + 1).padStart(2, '0') }}</span>
      <strong>{{ item.label }}</strong>
    </a>
  </nav>
</template>
