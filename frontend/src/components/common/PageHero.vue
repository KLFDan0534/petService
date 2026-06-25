<template>
  <div class="page-hero">
    <h3>{{ title }}</h3>
    <p v-if="subtitle">{{ subtitle }}</p>
  </div>
</template>

<script setup>
defineProps({ title: String, subtitle: String })
</script>
