<template>
  <div class="loading" role="status" aria-live="polite" :aria-label="text || '加载中...'">
    <div class="spinner"></div>
    <p>{{ text || '加载中...' }}</p>
  </div>
</template>

<script setup>
defineProps({ text: String })
</script>

<style scoped>
/* 三边灰、只给顶边上色，转起来就有一道光扫过去 */
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
