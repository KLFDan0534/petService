<template>
  <div class="empty-state">
    <div class="icon">{{ icon || '📭' }}</div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
// 三样都是可选的，图标不传就退回那个空信箱
defineProps({ icon: String, title: String, description: String })
</script>
