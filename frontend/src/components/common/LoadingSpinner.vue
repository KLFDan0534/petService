<template>
  <div class="loading">
    <div class="spinner"></div>
    <p>{{ text || '加载中...' }}</p>
  </div>
</template>

<script setup>
defineProps({ text: String })
</script>

<style scoped>
.spinner {
  width: 32px; height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin: 0 auto 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
