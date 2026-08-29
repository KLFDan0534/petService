<template>
  <div class="rt-page">
    <div class="rt-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="rt-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="rt-crumb-link">首页</router-link>
        <span class="rt-crumb-sep" aria-hidden="true">›</span>
        <span class="rt-crumb-here">我的评价</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="rt-head">
        <div class="rt-head-copy">
          <p class="rt-eyebrow" aria-hidden="true">
            <span class="rt-eyebrow-line"></span>
            <span>My reviews</span>
          </p>
          <h1 class="rt-title">我的评价</h1>
          <p class="rt-sub">你写下的每条评价都会展示在对应门店与照护师页面，也会收到他们的回复。</p>
          <dl class="rt-facts" aria-label="评价概览">
            <div>
              <dt>已发布</dt>
              <dd>{{ loading ? '--' : ratings.length }}</dd>
            </div>
            <div class="rt-fact-bordered">
              <dt>平均评分</dt>
              <dd>{{ loading ? '--' : (ratings.filter(r => Number(r.score_wsh) > 0).length ? (ratings.filter(r => Number(r.score_wsh) > 0).reduce((sum, r) => sum + Number(r.score_wsh), 0) / ratings.filter(r => Number(r.score_wsh) > 0).length).toFixed(1) : '0.0') }}</dd>
            </div>
            <div class="rt-fact-bordered">
              <dt>收到回复</dt>
              <dd>{{ loading ? '--' : ratings.filter(r => Boolean(r.reply_wsh)).length }}</dd>
            </div>
          </dl>
        </div>
        <div class="rt-actions">
          <button type="button" class="cta cta-primary" @click="openSubmit">写评价</button>
        </div>
      </header>

      <!-- ═══ 01 · 评价列表 ═══ -->
      <section class="rt-section" aria-label="评价列表">
        <header class="rt-sec-head">
          <div class="rt-sec-copy">
            <p class="rt-eyebrow rt-sec-eyebrow">
              <span class="rt-idx">01</span>
              <span class="rt-line" aria-hidden="true"></span>
              <span>Reviews</span>
            </p>
            <h2 class="rt-sec-title">评价列表</h2>
            <p class="rt-sec-desc">服务完成后写一条评价，会帮助其他家长做选择。</p>
          </div>
          <div class="rt-filters" role="group" aria-label="按评分筛选">
            <button
              v-for="f in scoreFilters"
              :key="f.key"
              type="button"
              :aria-pressed="score === f.key"
              class="rt-chip"
              :class="{ active: score === f.key }"
              @click="score = f.key"
            >{{ f.label }}</button>
          </div>
        </header>

        <div class="rt-body">
          <div v-if="loading" class="rt-skel-stack">
            <div v-for="i in 3" :key="i" class="rt-skeleton" />
          </div>

          <div v-else-if="filteredRatings.length === 0" class="rt-empty">
            <p class="rt-empty-icon" aria-hidden="true">🔍</p>
            <p class="rt-empty-title">{{ ratings.length ? '该评分下暂无评价' : '还没有写过评价' }}</p>
            <p class="rt-empty-desc">{{ ratings.length ? '换一个评分区间看看。' : '服务完成后写一条评价，会帮助其他家长做选择。' }}</p>
            <button v-if="ratings.length" type="button" class="cta cta-outline" @click="score = 0">查看全部</button>
            <button v-else type="button" class="cta cta-primary" @click="openSubmit">写一条评价</button>
          </div>

          <div v-else class="rt-list">
            <article v-for="r in filteredRatings" :key="r.id_wsh" class="rt-card">
              <div class="rt-card-top">
                <div class="rt-card-main">
                  <div class="rt-card-score">
                    <span class="rt-stars" role="img" :aria-label="`评分 ${r.score_wsh} 分，满分 5 分`">
                      <span v-for="i in 5" :key="i" class="rt-star" :class="{ on: i <= Math.floor(Number(r.score_wsh) || 0) }" aria-hidden="true">★</span>
                    </span>
                    <span class="rt-score-label">{{ { 1: '很不满意', 2: '不太满意', 3: '一般', 4: '满意', 5: '非常满意' }[Number(r.score_wsh)] || '未评分' }}</span>
                  </div>
                  <p v-if="r.serviceName || r.serviceType" class="rt-card-name">{{ r.serviceName || r.serviceType }}</p>
                  <p class="rt-card-meta">
                    <span>{{ new Date(r.created_at_wsh).toLocaleDateString() }}</span>
                    <template v-if="r.order_no_wsh">
                      <span class="rt-sep" aria-hidden="true">·</span>
                      <span>{{ r.order_no_wsh }}</span>
                    </template>
                  </p>
                </div>
                <div class="rt-card-tags">
                  <span v-if="r.merchant_name_wsh" class="rt-tag">{{ r.merchant_name_wsh }}</span>
                  <span v-if="r.keeper_name_wsh" class="rt-tag">照护师 · {{ r.keeper_name_wsh }}</span>
                </div>
              </div>

              <p v-if="r.content_wsh" class="rt-card-content">{{ r.content_wsh }}</p>

              <div v-if="r.reply_wsh" class="rt-reply">
                <p class="rt-reply-label">门店回复</p>
                <p class="rt-reply-text">{{ r.reply_wsh }}</p>
              </div>

              <div v-if="!r.reply_wsh && replyFormId !== r.id_wsh && (authStore.isMerchant || authStore.hasRole('KEEPER'))" class="rt-card-foot">
                <button type="button" class="cta cta-outline cta-sm" @click="replyFormId = r.id_wsh; replyText = ''">回复</button>
              </div>

              <div v-if="replyFormId === r.id_wsh" class="rt-reply-form">
                <textarea v-model="replyText" rows="2" class="rt-input rt-textarea" placeholder="输入回复..."></textarea>
                <div class="rt-reply-form-actions">
                  <button type="button" class="cta cta-primary cta-sm" @click="submitReply(r)">提交</button>
                  <button type="button" class="cta cta-outline cta-sm" @click="replyFormId = null">取消</button>
                </div>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>

    <!-- ═══════════════════════════════════════════
         写评价弹窗
         ═══════════════════════════════════════════ -->
    <div v-if="showForm" class="dlg-overlay" @click.self="showForm = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="写一条评价">
        <div class="dlg-head">
          <h3 class="dlg-title">写一条评价</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="showForm = false">✕</button>
        </div>
        <form @submit.prevent="submitRating">
          <div class="dlg-body">
            <div class="rt-pick" role="radiogroup" aria-label="评分">
              <button
                v-for="i in 5"
                :key="i"
                type="button"
                role="radio"
                :aria-checked="form.score_wsh === i"
                :aria-label="`${i} 分`"
                class="rt-pick-btn"
                @click="form.score_wsh = i"
              >
                <span class="rt-pick-star" :class="{ on: i <= form.score_wsh }" aria-hidden="true">★</span>
              </button>
              <span class="rt-pick-label">{{ { 1: '很不满意', 2: '不太满意', 3: '一般', 4: '满意', 5: '非常满意' }[form.score_wsh] }}</span>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="rt-content">评价内容</label>
              <textarea id="rt-content" v-model="form.content_wsh" class="dlg-textarea" rows="4" required placeholder="房间环境、照护师的沟通、日报的细致程度……"></textarea>
            </div>
          </div>
          <div class="dlg-foot">
            <button type="button" class="dlg-cancel" @click="showForm = false">取消</button>
            <button type="submit" class="cta cta-primary">发布评价</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getMyRatings, createRating, replyToRating } from '@/api/rating'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const authStore = useAuthStore()
const appStore = useAppStore()
const ratings = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = reactive({ score_wsh: 5, content_wsh: '' })
const replyFormId = ref(null)
const replyText = ref('')
const score = ref(0)

const scoreFilters = [
  { key: 0, label: '全部' },
  { key: 5, label: '5 分' },
  { key: 4, label: '4 分' },
  { key: 3, label: '3 分及以下' },
]

const filteredRatings = computed(() => {
  if (score.value === 0) return ratings.value
  if (score.value === 3) return ratings.value.filter((r) => Number(r.score_wsh) <= 3)
  return ratings.value.filter((r) => Number(r.score_wsh) === score.value)
})

function openSubmit() { form.score_wsh = 5; form.content_wsh = ''; showForm.value = true }

onMounted(async () => {
  try { const r = await getMyRatings(); if (r.code === 200) ratings.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function submitRating() {
  try {
    const r = await createRating(form)
    if (r.code === 200) {
      appStore.addToast('评价成功', 'success')
      showForm.value = false
      ratings.value.unshift(r.data)
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}

async function submitReply(r) {
  if (!replyText.value.trim()) return appStore.addToast('请输入回复内容', 'error')
  try {
    const res = await replyToRating(r.id_wsh, replyText.value)
    if (res.code === 200) { appStore.addToast('回复成功', 'success'); r.reply_wsh = replyText.value; replyFormId.value = null; replyText.value = '' }
  } catch (e) { appStore.addToast('回复失败', 'error') }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Editorial warm — uses shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode is handled globally
   via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.rt-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.rt-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.rt-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.rt-crumb-link { color: var(--ref-muted); text-decoration: none; }
.rt-crumb-link:hover { color: var(--ref-ink); }
.rt-crumb-sep { color: var(--ref-line); }
.rt-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.rt-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rt-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.rt-idx { font-variant-numeric: tabular-nums; }
.rt-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Header ═══ */
.rt-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 30px;
}
.rt-head-copy { min-width: 0; }
.rt-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.rt-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.rt-facts {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0 28px;
  margin: 28px 0 0;
}
.rt-facts div { display: flex; flex-direction: column; }
.rt-fact-bordered { border-left: 1px solid var(--ref-line); padding-left: 28px; }
.rt-facts dt {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rt-facts dd {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 26px;
  font-weight: 400;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: var(--ref-ink);
}
.rt-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ Filter chips ═══ */
.rt-filters { display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.rt-chip {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.rt-chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: color-mix(in srgb, var(--ref-sand) 60%, transparent); }
.rt-chip.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }

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
.cta-sm { height: 36px; padding: 0 14px; font-size: 12.5px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Section ═══ */
.rt-section { margin-top: 44px; }
.rt-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.rt-sec-copy { min-width: 0; }
.rt-sec-eyebrow { margin: 0; }
.rt-sec-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.rt-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Body / skeleton / empty ═══ */
.rt-body { margin-top: 24px; }
.rt-skel-stack { display: grid; gap: 14px; }
.rt-skeleton {
  height: 168px;
  border-radius: 16px;
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: rt-shimmer 1.3s linear infinite;
}
.rt-empty {
  padding: 64px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  text-align: center;
}
.rt-empty-icon { margin: 0 0 14px; font-size: 38px; }
.rt-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.rt-empty-desc { margin: 10px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.rt-empty .cta { margin-top: 22px; }

/* ═══ Cards ═══ */
.rt-list { display: grid; gap: 14px; }
.rt-card {
  padding: 22px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  transition: border-color 0.18s, transform 0.18s, box-shadow 0.18s;
}
.rt-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.rt-card-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 24px;
}
.rt-card-main { min-width: 0; flex: 1; }
.rt-card-score {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.rt-stars { display: inline-flex; align-items: center; gap: 3px; }
.rt-star {
  font-size: 15px;
  line-height: 1;
  color: var(--ref-line);
  transition: color 0.15s;
}
.rt-star.on { color: var(--ref-brand); }
.rt-score-label { font-size: 13px; color: var(--ref-ink-soft); }
.rt-card-name {
  margin: 10px 0 0;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.rt-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
}
.rt-sep { color: var(--ref-line); }
.rt-card-tags { display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.rt-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
  white-space: nowrap;
}
.rt-card-content {
  margin: 14px 0 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 90%, transparent);
}
.rt-reply {
  margin: 14px 0 0;
  padding-left: 14px;
  border-left: 2px solid color-mix(in srgb, var(--ref-brand) 35%, transparent);
}
.rt-reply-label {
  margin: 0;
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rt-reply-text {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.rt-card-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.rt-reply-form {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}
.rt-reply-form-actions { display: flex; justify-content: flex-end; gap: 10px; }
.rt-input,
.rt-textarea {
  width: 100%;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.rt-textarea { padding: 10px 12px; resize: vertical; line-height: 1.6; }
.rt-textarea::placeholder { color: var(--ref-muted); }
.rt-textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(10, 8, 6, 0.5);
  backdrop-filter: blur(2px);
  animation: rt-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 440px;
  max-height: min(90vh, 720px);
  overflow-y: auto;
  border-radius: 20px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  animation: rt-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px 0;
}
.dlg-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.dlg-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--ref-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-textarea {
  width: 100%;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  font-family: inherit;
  padding: 12px 14px;
  resize: vertical;
  line-height: 1.6;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-textarea::placeholder { color: var(--ref-muted); }
.dlg-textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.dlg-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══ Star picker ═══ */
.rt-pick { display: flex; align-items: center; gap: 4px; }
.rt-pick-btn {
  display: inline-flex;
  padding: 3px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: transform 0.15s;
}
.rt-pick-btn:hover { transform: scale(1.12); }
.rt-pick-star {
  font-size: 26px;
  line-height: 1;
  color: var(--ref-line);
  transition: color 0.15s;
}
.rt-pick-star.on { color: var(--ref-brand); }
.rt-pick-label { margin-left: 8px; font-size: 13px; color: var(--ref-ink-soft); }

/* ═══ Animations ═══ */
@keyframes rt-shimmer { to { background-position: -200% 0; } }
@keyframes rt-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes rt-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ Responsive ═══ */
@media (max-width: 760px) {
  .rt-card-top { flex-direction: column; }
}
@media (max-width: 560px) {
  .rt-shell { padding: 0 16px; }
  .rt-head { padding: 30px 0 22px; }
  .rt-sub { font-size: 13.5px; }
  .rt-facts { gap: 0 20px; }
  .rt-fact-bordered { padding-left: 20px; }
  .rt-facts dd { font-size: 22px; }
  .rt-actions { width: 100%; }
  .rt-actions .cta { flex: 1; }
  .rt-card { padding: 18px 18px; }
  .rt-card-foot { justify-content: flex-start; }
}
@media (prefers-reduced-motion: reduce) {
  .rt-skeleton { animation: none; }
}
</style>
