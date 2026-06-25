<template>
  <div class="empty-state">
    <div class="icon">{{ icon || '📭' }}</div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({ icon: String, title: String, description: String })
</script>
