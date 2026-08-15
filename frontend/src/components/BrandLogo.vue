<script setup>
defineProps({ to: { type: String, default: '/' }, light: Boolean });
</script>

<template>
  <RouterLink class="brand" :to="to" :class="{ 'brand-light': light }">
    <span class="brand-mark" aria-hidden="true"><i /><i /><i /></span>
    <span class="brand-copy"><b>脉冲笔记</b><small>SIGNAL NOTES</small></span>
  </RouterLink>
</template>
