<template>
  <div class="nd-page">
    <div class="nd-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="nd-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="nd-crumb-link">首页</router-link>
        <span class="nd-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/notifications" class="nd-crumb-link">站内通知</router-link>
        <span class="nd-crumb-sep" aria-hidden="true">›</span>
        <span class="nd-crumb-here">公告详情</span>
      </nav>

      <!-- ═══ Loading ═══ -->
      <div v-if="loading" class="nd-skeleton" aria-hidden="true">
        <div class="nd-skel nd-skel-eyebrow" />
        <div class="nd-skel nd-skel-title" />
        <div class="nd-skel nd-skel-meta" />
        <div class="nd-skel-body">
          <div class="nd-skel nd-skel-line" />
          <div class="nd-skel nd-skel-line" />
          <div class="nd-skel nd-skel-line nd-skel-line-80" />
          <div class="nd-skel nd-skel-line nd-skel-line-60" />
        </div>
      </div>

      <!-- ═══ Empty ═══ -->
      <div v-else-if="!notice" class="nd-empty">
        <svg class="nd-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="m3 11 18-5v12L3 14v-3z" />
          <path d="M11.6 16.8a3 3 0 1 1-5.8-1.6" />
        </svg>
        <h3>公告不存在或已删除</h3>
        <p>它可能已经下线。返回通知列表看看其他消息。</p>
        <button type="button" class="cta cta-outline" @click="router.back()">返回通知</button>
      </div>

      <!-- ═══ Article ═══ -->
      <article v-else class="nd-article">
        <p class="nd-eyebrow">公告</p>
        <h1 class="nd-title">{{ notice.title_wsh || '平台公告' }}</h1>
        <p class="nd-meta">发布于 {{ new Date(notice.created_at_wsh).toLocaleString() }}</p>
        <div class="nd-content">{{ notice.content_wsh || '暂无内容' }}</div>
        <div class="nd-foot">
          <button type="button" class="cta cta-primary" @click="router.back()">返回通知列表</button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoticeById, markNoticeRead } from '@/api/notice'

const route = useRoute()
const router = useRouter()
const notice = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const r = await getNoticeById(route.params.id)
    if (r.code === 200) notice.value = r.data
    try {
      await markNoticeRead(route.params.id)
    } catch (e) {} // 阅读回执失败不影响内容展示
  } catch (e) {}
  finally { loading.value = false }
})
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.nd-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.nd-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.nd-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.nd-crumb-link { color: var(--ref-muted); text-decoration: none; }
.nd-crumb-link:hover { color: var(--ref-ink); }
.nd-crumb-sep { color: var(--ref-line); }
.nd-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Article ═══ */
.nd-article {
  max-width: 720px;
  margin: 0 auto;
  padding: 40px 0 0;
}
.nd-eyebrow {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.nd-title {
  margin: 16px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(30px, 4vw, 42px);
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  text-wrap: balance;
}
.nd-meta {
  margin: 18px 0 0;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--ref-line);
  font-size: 12px;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}
.nd-content {
  margin-top: 28px;
  font-size: 15px;
  line-height: 1.85;
  color: color-mix(in srgb, var(--ref-ink-soft) 90%, transparent);
  white-space: pre-wrap;
  word-break: break-word;
}
.nd-foot {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--ref-line);
}

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Skeleton ═══ */
.nd-skeleton {
  max-width: 720px;
  margin: 40px auto 0;
}
.nd-skel {
  height: 12px;
  border-radius: var(--radius-inline);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: nd-shimmer 1.3s linear infinite;
}
.nd-skel-eyebrow { width: 96px; }
.nd-skel-title { width: 75%; height: 40px; margin-top: 18px; }
.nd-skel-meta { width: 132px; margin-top: 20px; }
.nd-skel-body {
  display: grid;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--ref-line);
}
.nd-skel-line-80 { width: 80%; }
.nd-skel-line-60 { width: 60%; }

/* ═══ Empty ═══ */
.nd-empty {
  max-width: 640px;
  margin: 40px auto 0;
  padding: 64px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
  text-align: center;
}
.nd-empty-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto 14px;
  color: var(--ref-brand);
}
.nd-empty h3 {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.nd-empty p { margin: 10px 0 0; font-size: 14px; color: var(--ref-muted); }
.nd-empty .cta { margin-top: 22px; }

/* ═══ Animations ═══ */
@keyframes nd-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 520px) {
  .nd-shell { padding: 0 16px; }
  .nd-article { padding: 30px 0 0; }
  .nd-title { font-size: 28px; }
  .nd-content { font-size: 14.5px; }
  .nd-empty { padding: 48px 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .nd-skel { animation: none; }
}
</style>
