<template>
  <div class="error-page">
    <div class="error-card">
      <div class="error-code">404</div>
      <h1>页面未找到</h1>
      <p>您访问的页面不存在或已被移除</p>
      <div class="error-actions">
        <router-link to="/" class="btn btn-primary">返回首页</router-link>
        <button class="btn btn-secondary" @click="$router.back()">返回上一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--color-background); }
.error-card { text-align: center; padding: 48px; }
.error-code { font-size: 96px; font-weight: 800; color: var(--color-muted-foreground); line-height: 1; margin-bottom: 16px; }
.error-card h1 { font-size: 24px; margin-bottom: 8px; }
.error-card p { color: var(--color-muted-foreground); margin-bottom: 24px; }
.error-actions { display: flex; gap: 12px; justify-content: center; }
</style>
