<template>
  <div class="kd-page">
    <div class="kd-shell">

      <!-- ═══ Breadcrumb ═══ -->
      <nav class="kd-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="kd-crumb-link">首页</router-link>
        <span class="kd-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/keepers" class="kd-crumb-link">照护师</router-link>
        <span class="kd-crumb-sep" aria-hidden="true">›</span>
        <span class="kd-crumb-here">{{ keeper?.name_wsh || '照护师详情' }}</span>
      </nav>

      <!-- ═══ Loading skeleton ═══ -->
      <div v-if="loading" class="kd-loading">
        <div class="kd-skel kd-skel-eyebrow"></div>
        <div class="kd-skel kd-skel-title"></div>
        <div class="kd-skel kd-skel-lede"></div>
        <div class="kd-skel kd-skel-cover"></div>
        <div class="kd-skel kd-skel-cover"></div>
      </div>

      <!-- ═══════════════════════════════════════════
           照护师详情
           ═══════════════════════════════════════════ -->
      <template v-else-if="keeper">
        <!-- ═══ Header ═══ -->
        <header class="kd-head">
          <div class="kd-head-copy">
            <p class="kd-eyebrow">
              <span class="kd-idx">寄养师</span>
              <span class="kd-line" aria-hidden="true"></span>
              <span>照护师详情</span>
            </p>
            <h1 class="kd-title">{{ keeper.name_wsh }}</h1>
            <p v-if="keeper.bio_wsh" class="kd-desc">{{ keeper.bio_wsh }}</p>
          </div>
          <div class="kd-head-actions">
            <button
              type="button"
              class="cta cta-outline"
              :disabled="favoriteLoading || favoriteToggling"
              @click="handleToggleFavorite"
            >
              <AppIcon :class="isFavorited ? 'kd-star-on' : 'kd-star-off'"><Star /></AppIcon>
              <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
            </button>
            <button type="button" class="cta cta-primary" @click="goToMerchant(keeper.merchant_id_wsh)">选择服务</button>
            <button type="button" class="cta cta-outline" @click="goBack">返回列表</button>
          </div>
        </header>

        <!-- ═══ Facts ═══ -->
        <dl class="kd-facts">
          <div class="kd-fact">
            <dt>评分</dt>
            <dd class="kd-fact-rating">
              <span class="kd-star" aria-hidden="true">★</span>
              <span>{{ Number(keeper.rating_wsh || 0).toFixed(1) }}</span>
            </dd>
          </div>
          <div class="kd-fact">
            <dt>日均价格</dt>
            <dd><span class="kd-price-symbol">¥</span>{{ money(keeper.price_per_day_wsh) }}</dd>
          </div>
          <div class="kd-fact">
            <dt>接单状态</dt>
            <dd class="kd-fact-badge"><span :class="['badge', warmBadge(onlineBadge)]">{{ onlineLabel }}</span></dd>
          </div>
        </dl>

        <!-- ═══ 01 照护师档案 ═══ -->
        <section class="kd-section" aria-labelledby="kd-sec-profile">
          <header class="kd-section-head">
            <p class="kd-eyebrow">
              <span class="kd-idx">01</span>
              <span class="kd-line" aria-hidden="true"></span>
              <span>简介</span>
            </p>
            <h2 id="kd-sec-profile" class="kd-section-title">照护师档案</h2>
          </header>
          <div class="kd-profile">
            <div class="kd-portrait">
              <MediaWithFallback
                :src="keeper.avatar_wsh"
                :alt="`${keeper.name_wsh || '照护师'}的照片`"
                :placeholder="keeper.name_wsh"
              />
            </div>
            <div class="kd-info-panel">
              <div class="kd-badge-row">
                <span :class="['badge', warmBadge(onlineBadge)]">{{ onlineLabel }}</span>
                <span v-if="keeper.merchant_name_wsh" class="tag-pill kd-merchant-tag" @click="goToMerchant(keeper.merchant_id_wsh)">
                  {{ keeper.merchant_name_wsh }}
                </span>
              </div>

              <dl class="kd-metrics">
                <div class="kd-metric">
                  <dt>从业年限</dt>
                  <dd>{{ keeper.experience_years_wsh || 0 }} 年</dd>
                </div>
                <div class="kd-metric">
                  <dt>累计照护</dt>
                  <dd>{{ Number(keeper.order_count_wsh || 0).toFixed(0) }}</dd>
                </div>
                <div class="kd-metric">
                  <dt>完成率</dt>
                  <dd>{{ percentLabel }}</dd>
                </div>
                <div class="kd-metric">
                  <dt>投诉率</dt>
                  <dd>{{ complaintLabel }}</dd>
                </div>
              </dl>

              <div class="kd-load-block">
                <p class="kd-load-label">当前照护负荷</p>
                <div class="kd-load-track" role="img" :aria-label="`当前照护 ${keeperCurrent} 只，上限 ${keeperMax || '不限'}`">
                  <div
                    class="kd-load-bar"
                    :class="{ 'kd-load-full': isFull }"
                    :style="{ width: loadPercent + '%' }"
                  ></div>
                </div>
                <p class="kd-load-caption">
                  {{ keeperCurrent }} / {{ keeperMax || '不限' }} · 同时照护的宠物数量有上限，满位后不再接新单。
                </p>
              </div>

              <p v-if="keeper.created_at_wsh" class="kd-joined">
                入驻时间 {{ formatDate(keeper.created_at_wsh) }}
              </p>
            </div>
          </div>
        </section>

        <!-- ═══ 02 资质核验 ═══ -->
        <section class="kd-section" aria-labelledby="kd-sec-qual">
          <header class="kd-section-head">
            <p class="kd-eyebrow">
              <span class="kd-idx">02</span>
              <span class="kd-line" aria-hidden="true"></span>
              <span>资质</span>
            </p>
            <h2 id="kd-sec-qual" class="kd-section-title">资质核验</h2>
            <p class="kd-section-desc">证书由门店提交、平台复核，状态实时同步。</p>
          </header>
          <ul v-if="qualifications.length" class="kd-qual-list">
            <li v-for="q in qualifications" :key="q.id_wsh" class="kd-qual-item">
              <span class="kd-qual-icon" aria-hidden="true">✓</span>
              <span class="kd-qual-body">
                <span class="kd-qual-title">{{ q.title_wsh }}</span>
                <span v-if="qualificationUrls(q.file_url_wsh).length" class="kd-qual-thumbs">
                  <img
                    v-for="url in qualificationUrls(q.file_url_wsh)"
                    :key="url"
                    :src="url"
                    :alt="q.title_wsh"
                  >
                </span>
              </span>
              <span
                :class="['badge', warmBadge(q.status_wsh === 'approved' ? 'badge-success' : (q.status_wsh === 'pending' ? 'badge-warning' : 'badge-danger'))]"
              >
                {{ q.status_wsh === 'approved' ? '已认证' : (q.status_wsh === 'pending' ? '审核中' : '未通过') }}
              </span>
            </li>
          </ul>
          <div v-else class="kd-empty">
            <p class="kd-empty-title">暂未上传资质证明</p>
            <p class="kd-empty-desc">该照护师尚未上传资质证明，建议先联系门店了解。</p>
          </div>
        </section>

        <!-- ═══ 03 用户评价 ═══ -->
        <section class="kd-section" aria-labelledby="kd-sec-ratings">
          <header class="kd-section-head">
            <p class="kd-eyebrow">
              <span class="kd-idx">03</span>
              <span class="kd-line" aria-hidden="true"></span>
              <span>评价</span>
            </p>
            <h2 id="kd-sec-ratings" class="kd-section-title">用户评价 ({{ ratings.length }})</h2>
          </header>
          <div v-if="ratings.length" class="kd-ratings">
            <div class="kd-score">
              <span class="kd-score-big">{{ avgScore }}</span>
              <span class="kd-score-total">/ 5</span>
              <span class="kd-score-stars" aria-hidden="true">{{ renderStars(avgScore) }}</span>
            </div>
            <div class="kd-rating-list">
              <article v-for="r in ratings" :key="r.id_wsh" class="kd-rating-item">
                <div class="kd-rating-header">
                  <strong class="kd-rating-user">用户 #{{ r.user_id_wsh }}</strong>
                  <span class="kd-rating-stars" aria-hidden="true">{{ renderStars(r.score_wsh) }}</span>
                  <span class="kd-rating-date">{{ formatDate(r.created_at_wsh) }}</span>
                </div>
                <p v-if="r.content_wsh" class="kd-rating-content">{{ r.content_wsh }}</p>
                <div v-if="r.reply_wsh" class="kd-rating-reply">
                  <span class="kd-rating-reply-label">回复</span>
                  <span>{{ r.reply_wsh }}</span>
                </div>
              </article>
            </div>
          </div>
          <div v-else class="kd-empty">
            <p class="kd-empty-title">暂无评价</p>
            <p class="kd-empty-desc">还没有用户评价。</p>
          </div>
        </section>
      </template>

      <!-- ═══ 照护师不存在 ═══ -->
      <div v-else class="kd-empty kd-empty-page">
        <p class="kd-empty-title">找不到这位照护师</p>
        <p class="kd-empty-desc">资料可能已下线，或链接有误。</p>
        <button type="button" class="cta cta-primary" @click="goBack">返回照护师列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useKeeperDetail } from '@/composables/useKeeperDetail'
import { useFavoriteState } from '@/composables/useFavoriteState'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { Star } from '@element-plus/icons-vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { parseCommaSeparatedUrls } from '@/utils/fileUrls'
import { formatDate, formatMoney as money } from '@/utils/format'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const {
  loading, keeper, ratings, qualifications,
  avgScore, onlineLabel, onlineBadge,
  load,
} = useKeeperDetail(route.params.id)

const favoriteTargetId = computed(() => Number(route.params.id))
const {
  isFavorited,
  loading: favoriteLoading,
  toggling: favoriteToggling,
  toggle: toggleFavoriteState,
} = useFavoriteState(favoriteTargetId, FAVORITE_TARGET_TYPES.KEEPER)

const keeperCurrent = computed(() => Number(keeper.value?.current_pets_wsh || 0))
const keeperMax = computed(() => Number(keeper.value?.max_pets_wsh || 0))
const isFull = computed(() => !!keeperMax.value && keeperCurrent.value >= keeperMax.value)
const loadPercent = computed(() =>
  keeperMax.value ? Math.min((keeperCurrent.value / keeperMax.value) * 100, 100) : 0
)
const loadText = computed(() =>
  keeperMax.value ? `${keeperCurrent.value} / ${keeperMax.value}` : String(keeperCurrent.value)
)
const percentLabel = computed(() => {
  const v = keeper.value?.completion_rate_wsh
  return v == null ? '-' : `${Number(v)}%`
})
const complaintLabel = computed(() => {
  const v = keeper.value?.complaint_rate_wsh
  return v == null ? '-' : `${Number(v)}%`
})

/** 全局 badge-* 色调 → 暖色编辑风色调 */
const WARM_TONE = {
  'badge-success': 'badge-active',
  'badge-warning': 'badge-action',
  'badge-secondary': 'badge-closed',
  'badge-info': 'badge-queued',
  'badge-primary': 'badge-done',
  'badge-danger': 'badge-danger',
  'badge-error': 'badge-danger',
}
function warmBadge(globalClass) {
  return WARM_TONE[globalClass] || 'badge-queued'
}

function renderStars(score) {
  const n = Number(score) || 0
  return '\u2605'.repeat(Math.round(n)) + '\u2606'.repeat(5 - Math.round(n))
}

function qualificationUrls(value) {
  return parseCommaSeparatedUrls(value)
}

async function handleToggleFavorite() {
  if (!authStore.isLoggedIn) {
    appStore.showLoginPrompt = true
    return
  }
  try {
    await toggleFavoriteState()
    appStore.addToast(isFavorited.value ? '已收藏' : '已取消收藏', 'success')
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  }
}

function goToMerchant(merchantId) {
  if (!merchantId) return
  router.push(`/merchants/${merchantId}`)
}

function goBack() {
  router.push('/keepers')
}

onMounted(load)
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.kd-page {
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

.kd-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.kd-crumb {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
  flex-wrap: wrap;
}
.kd-crumb-link { color: var(--ref-muted); text-decoration: none; transition: color 0.15s; }
.kd-crumb-link:hover { color: var(--ref-ink); }
.kd-crumb-sep { color: var(--ref-line); }
.kd-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex; align-items: center; gap: 6px;
  border-radius: var(--r-tag); border: 1px solid transparent;
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.badge-action { background: var(--ref-ink); color: var(--ref-cream); }
.badge-active { background: color-mix(in srgb, var(--ref-brand) 14%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand) 28%, transparent); }
.badge-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 22%, transparent); }
.badge-done { background: color-mix(in srgb, var(--ref-ink) 6%, transparent); color: var(--ref-ink-soft); }
.badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-danger { background: color-mix(in srgb, var(--ref-brand-deep) 10%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand-deep) 24%, transparent); }

/* ═══ Tag pill ═══ */
.tag-pill {
  display: inline-flex; align-items: center;
  border-radius: var(--r-tag); border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; color: var(--ref-ink-soft);
  white-space: nowrap;
}
.kd-merchant-tag { cursor: pointer; transition: border-color 0.15s, color 0.15s; }
.kd-merchant-tag:hover { border-color: color-mix(in srgb, var(--ref-brand) 40%, transparent); color: var(--ref-brand-deep); }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }
.cta-sm { height: 36px; padding: 0 14px; font-size: 12.5px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Header ═══ */
.kd-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 24px; padding: 20px 0 8px;
}
.kd-head-copy { min-width: 0; flex: 1 1 320px; }
.kd-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.kd-idx { font-variant-numeric: tabular-nums; }
.kd-line { width: 24px; height: 1px; background: var(--ref-line); }
.kd-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.kd-desc { margin: 10px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.kd-head-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.kd-head-actions .el-icon { font-size: 14px; }
.kd-star-on { color: var(--ref-brand); }
.kd-star-off { color: var(--ref-muted); }

/* ═══ Facts ═══ */
.kd-facts {
  display: flex; flex-wrap: wrap; gap: 0; margin: 22px 0 0;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); overflow: hidden;
}
.kd-fact { padding: 14px 22px; border-left: 1px solid var(--ref-line); }
.kd-fact:first-child { border-left: 0; }
.kd-fact dt { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.kd-fact dd {
  margin: 6px 0 0; font-family: var(--ref-font-display);
  font-size: 22px; font-weight: 400; line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
}
.kd-fact-rating { display: inline-flex; align-items: center; gap: 6px; }
.kd-fact-badge { padding-top: 4px; }
.kd-star { color: var(--ref-brand); font-size: 16px; }
.kd-price-symbol { font-size: 13px; vertical-align: top; color: var(--ref-brand-deep); }

/* ═══ Section ═══ */
.kd-section { margin-top: 40px; }
.kd-section-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 4px 24px; }
.kd-section-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.kd-section-desc { margin: 8px 0 0; max-width: 640px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }

/* ═══ 01 照护师档案 ═══ */
.kd-profile {
  display: grid; grid-template-columns: 0.85fr 1.15fr; gap: 20px; margin-top: 22px;
}
.kd-portrait {
  min-height: 320px; overflow: hidden; aspect-ratio: 3 / 4;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-sand);
}
.kd-portrait :deep(.media-image) { width: 100%; height: 100%; object-fit: cover; }
.kd-portrait :deep(.media-placeholder) { width: 100%; height: 100%; background: var(--ref-sand); }
.kd-portrait :deep(.media-placeholder .el-icon) { color: var(--ref-brand); }
.kd-portrait :deep(.media-placeholder span) { color: var(--ref-muted); }
.kd-info-panel {
  padding: 24px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
}
.kd-badge-row { display: flex; flex-wrap: wrap; gap: 8px; }
.kd-metrics {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  margin: 24px 0 0; padding-top: 20px; border-top: 1px solid var(--ref-line);
}
.kd-metric { min-width: 0; }
.kd-metric dt { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--ref-muted); }
.kd-metric dd {
  margin: 8px 0 0; font-family: var(--ref-font-display);
  font-size: 22px; font-weight: 400; line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.kd-load-block { margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--ref-line); }
.kd-load-label { margin: 0; font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--ref-muted); }
.kd-load-track {
  margin-top: 10px; height: 8px; min-width: 0; overflow: hidden;
  border-radius: var(--radius-pill); background: var(--ref-sand);
}
.kd-load-bar { height: 100%; border-radius: var(--radius-pill); background: var(--ref-brand); transition: width 0.3s ease; }
.kd-load-bar.kd-load-full { background: var(--ref-brand-deep); }
.kd-load-caption { margin: 10px 0 0; font-size: 12px; line-height: 1.6; color: var(--ref-muted); }
.kd-joined { margin: 18px 0 0; font-size: 12px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }

/* ═══ 02 资质核验 ═══ */
.kd-qual-list {
  margin: 22px 0 0; padding: 0; list-style: none;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); overflow: hidden;
}
.kd-qual-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-top: 1px solid var(--ref-line);
}
.kd-qual-item:first-child { border-top: 0; }
.kd-qual-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--ref-sand); color: var(--ref-brand); font-size: 13px; flex-shrink: 0;
}
.kd-qual-body { min-width: 0; flex: 1; display: grid; gap: 6px; }
.kd-qual-title { font-size: 13.5px; font-weight: 500; color: var(--ref-ink); }
.kd-qual-thumbs { display: flex; flex-wrap: wrap; gap: 6px; }
.kd-qual-thumbs img {
  width: 44px; height: var(--control-height); border-radius: var(--radius-inline); object-fit: cover;
  border: 1px solid var(--ref-line);
}

/* ═══ 03 用户评价 ═══ */
.kd-ratings { display: grid; gap: 28px; margin-top: 22px; }
.kd-score { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.kd-score-big {
  font-family: var(--ref-font-display); font-size: 44px; font-weight: 400;
  line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
}
.kd-score-total { font-size: 14px; color: var(--ref-muted); }
.kd-score-stars { font-size: 15px; color: var(--ref-brand); letter-spacing: 2px; margin-left: 6px; }
.kd-rating-list { display: grid; gap: 0; }
.kd-rating-item { padding: 18px 2px; border-top: 1px solid var(--ref-line); }
.kd-rating-item:first-child { border-top: 0; }
.kd-rating-header { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.kd-rating-user { font-size: 13px; font-weight: 500; color: var(--ref-ink); }
.kd-rating-stars { font-size: 12px; color: var(--ref-brand); letter-spacing: 2px; }
.kd-rating-date { font-size: 12px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.kd-rating-content { margin: 8px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }
.kd-rating-reply {
  margin-top: 10px; padding: 10px 14px;
  border-left: 2px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
  border-radius: 0 var(--r-btn) var(--r-btn) 0;
  font-size: 12.5px; line-height: 1.7; color: var(--ref-ink-soft);
}
.kd-rating-reply-label {
  display: block; font-size: 10.5px; letter-spacing: 0.14em; text-transform: uppercase;
  color: var(--ref-muted); margin-bottom: 2px;
}

/* ═══ Empty ═══ */
.kd-empty {
  margin-top: 22px; padding: 40px 24px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.kd-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--ref-ink); }
.kd-empty-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }
.kd-empty-page { padding: 72px 24px; }
.kd-empty-page .cta { margin-top: 22px; }

/* ═══ Loading skeleton ═══ */
.kd-loading { display: grid; gap: 16px; padding: 24px 0 0; }
.kd-skel {
  border-radius: 10px;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: kd-shimmer 1.3s linear infinite;
}
.kd-skel-eyebrow { width: 96px; height: 12px; }
.kd-skel-title { width: 60%; max-width: 360px; height: 40px; }
.kd-skel-lede { width: 70%; max-width: 460px; height: 14px; }
.kd-skel-cover { width: 100%; height: 300px; }

/* ═══ Animations ═══ */
@keyframes kd-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .kd-profile { grid-template-columns: 1fr; }
  .kd-portrait { min-height: 240px; aspect-ratio: 16 / 9; }
  .kd-metrics { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .kd-shell { padding: 0 16px; }
  .kd-facts { width: 100%; }
  .kd-fact { flex: 1; padding: 12px 14px; }
  .kd-fact dd { font-size: 19px; }
}
@media (prefers-reduced-motion: reduce) {
  .kd-skel { animation: none; }
  .kd-load-bar { transition: none; }
  .cta:hover { transform: none; }
}
</style>
