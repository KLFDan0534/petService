<template>
  <div class="kp-page">
    <div class="kp-shell">

      <!-- ═══ Breadcrumb ═══ -->
      <nav class="kp-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="kp-crumb-link">首页</router-link>
        <span class="kp-crumb-sep" aria-hidden="true">›</span>
        <span class="kp-crumb-here">照护师</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="kp-head">
        <div class="kp-head-copy">
          <p class="kp-eyebrow">
            <span class="kp-idx">Keepers</span>
            <span class="kp-line" aria-hidden="true"></span>
            <span>照护师</span>
          </p>
          <h1 class="kp-title">照护师</h1>
          <p class="kp-desc">每位照护师的资质由所属门店与平台双重核验。经验年限、当前在照护数量与完成率都如实展示。</p>
        </div>
        <div class="kp-head-side">
          <dl class="kp-facts">
            <div class="kp-fact">
              <dt>照护师总数</dt>
              <dd>{{ keepers.length }}</dd>
            </div>
            <div class="kp-fact">
              <dt>当前可接单</dt>
              <dd>{{ availableCount }}</dd>
            </div>
          </dl>
          <router-link to="/merchants" class="cta cta-outline">按门店浏览</router-link>
        </div>
      </header>

      <!-- ═══ Toolbar ═══ -->
      <div class="kp-toolbar">
        <div class="kp-search">
          <svg class="kp-search-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7"></circle>
            <line x1="21" y1="21" x2="16.5" y2="16.5"></line>
          </svg>
          <input v-model="keyword" class="kp-input" type="search" placeholder="搜索姓名、门店或擅长方向" aria-label="搜索照护师" />
        </div>
        <div class="kp-sorts" role="group" aria-label="排序方式">
          <button
            v-for="opt in SORTS"
            :key="opt.key"
            type="button"
            class="kp-pill"
            :class="{ 'kp-pill-active': sort === opt.key }"
            :aria-pressed="sort === opt.key"
            @click="sort = opt.key"
          >
            {{ opt.label }}
          </button>
          <button
            type="button"
            class="kp-pill"
            :class="{ 'kp-pill-brand': availableOnly }"
            :aria-pressed="availableOnly"
            @click="availableOnly = !availableOnly"
          >
            只看可接单
          </button>
        </div>
      </div>

      <!-- ═══ Directory ═══ -->
      <section class="kp-section" aria-labelledby="kp-sec-directory">
        <header class="kp-section-head">
          <p class="kp-eyebrow">
            <span class="kp-idx">01</span>
            <span class="kp-line" aria-hidden="true"></span>
            <span>Directory</span>
          </p>
          <h2 id="kp-sec-directory" class="kp-section-title">全部照护师</h2>
          <p class="kp-section-desc">支持按姓名、所属门店或擅长方向搜索。</p>
        </header>

        <div v-if="loading" class="kp-skeleton-grid">
          <div v-for="i in 3" :key="i" class="kp-skeleton"></div>
        </div>

        <div v-else-if="visible.length" class="kp-grid">
          <article
            v-for="k in visible"
            :key="k.id_wsh"
            class="kp-card"
            role="button"
            tabindex="0"
            :aria-label="`查看 ${k.name_wsh || '照护师'} 主页`"
            @click="goDetail(k)"
            @keydown.enter="goDetail(k)"
          >
            <div class="kp-top-row">
              <div class="kp-media">
                <MediaWithFallback
                  :src="k.avatar_wsh"
                  :alt="`${k.name_wsh || '照护师'}的照片`"
                  :placeholder="k.name_wsh"
                />
              </div>
              <div class="kp-body">
                <div class="kp-headline">
                  <div class="kp-name-wrap">
                    <h3 class="kp-name">{{ k.name_wsh || '照护师' }}</h3>
                    <p class="kp-merchant">{{ k.merchant_name_wsh || '待分配门店' }}</p>
                  </div>
                  <span :class="['tag-pill', isFull(k) ? 'kp-tag-full' : 'kp-tag-open']">
                    {{ isFull(k) ? '已满位' : '可接单' }}
                  </span>
                </div>
                <div class="kp-rating">
                  <span class="kp-star" aria-hidden="true">★</span>
                  <b>{{ Number(k.rating_wsh || 0).toFixed(1) }}</b>
                </div>
                <dl class="kp-facts-grid">
                  <div class="kp-fact2">
                    <dt>经验</dt>
                    <dd>{{ k.experience_years_wsh || 0 }} 年</dd>
                  </div>
                  <div class="kp-fact2">
                    <dt>在照护</dt>
                    <dd>{{ currentLabel(k) }}</dd>
                  </div>
                  <div class="kp-fact2">
                    <dt>完成率</dt>
                    <dd>{{ percentLabel(k) }}</dd>
                  </div>
                </dl>
              </div>
            </div>

            <div v-if="k.bio_wsh" class="kp-bio">
              <p class="kp-bio-text">{{ k.bio_wsh }}</p>
            </div>

            <div class="kp-foot">
              <span class="kp-price">
                <span class="kp-price-symbol">¥</span>{{ money(k.price_per_day_wsh) }}<small>/ 天</small>
              </span>
              <div class="kp-actions" @click.stop @keydown.stop>
                <button type="button" class="cta cta-outline cta-sm" @click="goDetail(k)">查看主页</button>
                <button type="button" class="cta cta-primary cta-sm" @click="goToMerchant(k)">选择服务</button>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="kp-empty">
          <p class="kp-empty-title">没有符合条件的照护师</p>
          <p class="kp-empty-desc">试试清空搜索词，或关掉「只看可接单」。</p>
          <button type="button" class="cta cta-outline" @click="clearFilters">清空筛选</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getKeepers } from '@/api/keeper'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const router = useRouter()
const keepers = ref([])
const loading = ref(true)
const keyword = ref('')
const sort = ref('rating')
const availableOnly = ref(false)

const SORTS = [
  { key: 'rating', label: '评分优先' },
  { key: 'experience', label: '经验优先' },
  { key: 'price', label: '价格从低到高' },
]

function money(v) { return Number(v || 0).toFixed(2) }

function isFull(k) {
  const max = Number(k.max_pets_wsh || 0)
  const current = Number(k.current_pets_wsh || 0)
  return !!max && current >= max
}

function isAvailable(k) {
  return !isFull(k)
}

function currentLabel(k) {
  const max = Number(k.max_pets_wsh || 0)
  const current = Number(k.current_pets_wsh || 0)
  return max ? `${current} / ${max}` : String(current)
}

function percentLabel(k) {
  const v = k.completion_rate_wsh
  return v == null ? '-' : `${Number(v)}%`
}

const availableCount = computed(() => keepers.value.filter(isAvailable).length)

const visible = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = keepers.value.filter((k) => {
    if (availableOnly.value && !isAvailable(k)) return false
    if (!kw) return true
    return [k.name_wsh, k.merchant_name_wsh, k.bio_wsh].some((v) =>
      String(v || '').toLowerCase().includes(kw)
    )
  })
  const s = sort.value
  if (s === 'rating') {
    list = [...list].sort((a, b) => Number(b.rating_wsh || 0) - Number(a.rating_wsh || 0))
  } else if (s === 'experience') {
    list = [...list].sort((a, b) => Number(b.experience_years_wsh || 0) - Number(a.experience_years_wsh || 0))
  } else if (s === 'price') {
    list = [...list].sort((a, b) => Number(a.price_per_day_wsh || 0) - Number(b.price_per_day_wsh || 0))
  }
  return list
})

function goDetail(k) {
  router.push(`/keepers/${k.id_wsh}`)
}

function goToMerchant(keeper) {
  if (!keeper?.merchant_id_wsh) return
  router.push(`/merchants/${keeper.merchant_id_wsh}`)
}

function clearFilters() {
  keyword.value = ''
  availableOnly.value = false
}

async function loadKeepers() {
  loading.value = true
  try {
    const r = await getKeepers()
    if (r.code === 200) keepers.value = r.data || []
  } catch (e) {
    // keepers list failure is non-fatal
  } finally {
    loading.value = false
  }
}

onMounted(loadKeepers)
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.kp-page {
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.kp-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.kp-crumb {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.kp-crumb-link { color: var(--ref-muted); text-decoration: none; }
.kp-crumb-link:hover { color: var(--ref-ink); }
.kp-crumb-sep { color: var(--ref-line); }
.kp-crumb-here { color: var(--ref-ink-soft); }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }
.cta-sm { height: 34px; padding: 0 13px; font-size: 12.5px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Tag pill ═══ */
.tag-pill {
  display: inline-flex; align-items: center;
  border-radius: var(--r-tag); border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; color: var(--ref-ink-soft);
  white-space: nowrap;
}
.kp-tag-open {
  background: color-mix(in srgb, var(--ref-brand) 14%, transparent);
  color: var(--ref-brand-deep);
  border-color: color-mix(in srgb, var(--ref-brand) 28%, transparent);
}
.kp-tag-full {
  background: transparent; color: var(--ref-muted); border-color: var(--ref-line);
}

/* ═══ Header ═══ */
.kp-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 40px; padding: 20px 0 8px;
}
.kp-head-copy { min-width: 0; }
.kp-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.kp-idx { font-variant-numeric: tabular-nums; }
.kp-line { width: 24px; height: 1px; background: var(--ref-line); }
.kp-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.kp-desc { margin: 10px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.kp-head-side { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 14px; }
.kp-facts {
  display: flex; gap: 0; margin: 0;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); overflow: hidden;
}
.kp-fact { padding: 14px 22px; border-left: 1px solid var(--ref-line); }
.kp-fact:first-child { border-left: 0; }
.kp-fact dt { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.kp-fact dd {
  margin: 6px 0 0; font-family: var(--ref-font-display);
  font-size: 26px; font-weight: 400; line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
}

/* ═══ Toolbar ═══ */
.kp-toolbar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 12px;
  margin-top: 24px; padding: 14px 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
}
.kp-search { position: relative; min-width: 0; flex: 1 1 240px; }
.kp-search-icon {
  position: absolute; left: 12px; top: 50%; transform: translateY(-50%);
  color: var(--ref-muted); pointer-events: none;
}
.kp-input {
  width: 100%; height: 40px; padding: 0 14px 0 36px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 13px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.kp-input::placeholder { color: var(--ref-muted); }
.kp-input:focus { outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent); }
.kp-sorts { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.kp-pill {
  height: 34px; padding: 0 14px; border-radius: 999px;
  border: 1px solid var(--ref-line); background: var(--ref-surface);
  color: var(--ref-ink-soft); font-size: 12px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.kp-pill:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); background: color-mix(in srgb, var(--ref-sand) 60%, var(--ref-surface)); }
.kp-pill-active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }
.kp-pill-brand { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }

/* ═══ Section ═══ */
.kp-section { margin-top: 40px; }
.kp-section-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 4px 24px; }
.kp-section-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.kp-section-desc { margin: 8px 0 0; max-width: 640px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }

/* ═══ Cards ═══ */
.kp-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 22px;
}
.kp-card {
  display: flex; flex-direction: column; overflow: hidden; cursor: pointer;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
  transition: transform 0.2s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.2s, border-color 0.2s;
}
.kp-card:hover, .kp-card:focus-visible {
  transform: translateY(-4px); outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 28px 60px -40px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.kp-top-row { display: flex; gap: 16px; padding: 18px 18px 0; }
.kp-media {
  width: 104px; height: 132px; flex-shrink: 0; overflow: hidden;
  border-radius: var(--r-btn); background: var(--ref-sand);
  border: 1px solid var(--ref-line);
}
.kp-media :deep(.media-image) { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.kp-card:hover .kp-media :deep(.media-image) { transform: scale(1.05); }
.kp-media :deep(.media-placeholder) { width: 100%; height: 100%; background: var(--ref-sand); }
.kp-media :deep(.media-placeholder .el-icon) { color: var(--ref-brand); }
.kp-media :deep(.media-placeholder span) { color: var(--ref-muted); }
.kp-body { flex: 1; min-width: 0; }
.kp-headline { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.kp-name-wrap { min-width: 0; }
.kp-name {
  margin: 0; font-family: var(--ref-font-display);
  font-size: 19px; font-weight: 500; line-height: 1.25; letter-spacing: -0.01em; color: var(--ref-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  transition: color 0.15s;
}
.kp-card:hover .kp-name { color: var(--ref-brand); }
.kp-merchant { margin: 4px 0 0; font-size: 12px; color: var(--ref-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.kp-rating { display: inline-flex; align-items: center; gap: 5px; margin-top: 10px; font-size: 12.5px; }
.kp-star { color: var(--ref-brand); font-size: 13px; }
.kp-rating b { color: var(--ref-ink); font-variant-numeric: tabular-nums; font-weight: 500; }
.kp-facts-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px 8px;
  margin: 14px 0 0; padding-top: 12px; border-top: 1px solid var(--ref-line);
}
.kp-fact2 { min-width: 0; }
.kp-fact2 dt { font-size: 9.5px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--ref-muted); }
.kp-fact2 dd { margin: 4px 0 0; font-size: 13px; font-weight: 500; color: var(--ref-ink); font-variant-numeric: tabular-nums; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.kp-bio {
  margin: 16px 18px 0; padding: 12px 14px;
  border-radius: var(--r-btn);
  background: color-mix(in srgb, var(--ref-cream) 45%, var(--ref-surface));
}
.kp-bio-text {
  margin: 0; font-size: 12.5px; line-height: 1.6; color: var(--ref-ink-soft); opacity: 0.9;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.kp-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  margin-top: auto; padding: 14px 18px 16px;
}
.kp-price { font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); font-variant-numeric: tabular-nums; white-space: nowrap; }
.kp-price-symbol { font-size: 12px; vertical-align: top; margin-right: 1px; color: var(--ref-brand-deep); }
.kp-price small { font-size: 11.5px; font-weight: 400; color: var(--ref-muted); margin-left: 2px; }
.kp-actions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }

/* ═══ Loading / Empty ═══ */
.kp-skeleton-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 22px;
}
.kp-skeleton {
  height: 380px; border-radius: var(--r-card); border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: kp-shimmer 1.3s linear infinite;
}
.kp-empty {
  margin-top: 22px; padding: 56px 24px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.kp-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.kp-empty-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }
.kp-empty .cta { margin-top: 20px; }

/* ═══ Animations ═══ */
@keyframes kp-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .kp-grid, .kp-skeleton-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .kp-shell { padding: 0 16px; }
  .kp-grid, .kp-skeleton-grid { grid-template-columns: 1fr; }
  .kp-facts { width: 100%; }
  .kp-fact { flex: 1; padding: 12px 16px; }
}
@media (max-width: 380px) {
  .kp-top-row { flex-direction: column; align-items: stretch; }
  .kp-media { width: 100%; height: 200px; }
  .kp-actions { justify-content: flex-start; }
}
@media (prefers-reduced-motion: reduce) {
  .kp-skeleton { animation: none; }
  .kp-card:hover { transform: none; }
  .kp-media :deep(.media-image) { transition: none; }
}
</style>
