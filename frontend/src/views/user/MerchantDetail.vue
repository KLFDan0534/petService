<template>
  <div class="md-page">
    <div class="md-shell">

      <!-- ═══ Breadcrumb ═══ -->
      <nav class="md-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="md-crumb-link">首页</router-link>
        <span class="md-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/merchants" class="md-crumb-link">门店</router-link>
        <span class="md-crumb-sep" aria-hidden="true">›</span>
        <span class="md-crumb-here">{{ merchant?.name_wsh || '门店详情' }}</span>
      </nav>

      <!-- ═══ Loading skeleton ═══ -->
      <div v-if="loading" class="md-loading">
        <div class="md-skel md-skel-eyebrow"></div>
        <div class="md-skel md-skel-title"></div>
        <div class="md-skel md-skel-lede"></div>
        <div class="md-skel md-skel-cover"></div>
        <div class="md-skel md-skel-cover"></div>
      </div>

      <!-- ═══════════════════════════════════════════
           门店详情
           ═══════════════════════════════════════════ -->
      <template v-else-if="merchant">
        <!-- ═══ Header ═══ -->
        <header class="md-head">
          <div class="md-head-copy">
            <p class="md-eyebrow">
              <span class="md-idx">Store</span>
              <span class="md-line" aria-hidden="true"></span>
              <span>门店详情</span>
            </p>
            <h1 class="md-title">{{ merchant.name_wsh }}</h1>
            <p v-if="merchant.description_wsh" class="md-desc">{{ merchant.description_wsh }}</p>
          </div>
          <div class="md-head-actions">
            <button
              type="button"
              class="cta cta-outline"
              :disabled="favoriteLoading || favoriteToggling"
              @click="handleToggleFavorite"
            >
              <el-icon :class="isFavorited ? 'md-star-on' : 'md-star-off'"><Star /></el-icon>
              <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
            </button>
            <button type="button" class="cta cta-primary" @click="goToOwnerHome">联系商家</button>
            <button type="button" class="cta cta-outline" @click="goBack">返回列表</button>
          </div>
        </header>

        <!-- ═══ Facts ═══ -->
        <dl class="md-facts">
          <div class="md-fact">
            <dt>评分</dt>
            <dd class="md-fact-rating">
              <span class="md-star" aria-hidden="true">★</span>
              <span>{{ Number(merchant.rating_wsh || 0).toFixed(1) }}</span>
            </dd>
          </div>
          <div class="md-fact">
            <dt>营业状态</dt>
            <dd>{{ getStatusLabel(MerchantStoreStatus, merchant.store_status_wsh) }}</dd>
          </div>
          <div class="md-fact">
            <dt>可预约服务</dt>
            <dd>{{ services.length }}</dd>
          </div>
          <div class="md-fact">
            <dt>用户评价</dt>
            <dd>{{ ratings.length }}</dd>
          </div>
        </dl>

        <!-- ═══ 01 门店概况 ═══ -->
        <section class="md-section" aria-labelledby="md-sec-overview">
          <header class="md-section-head">
            <p class="md-eyebrow">
              <span class="md-idx">01</span>
              <span class="md-line" aria-hidden="true"></span>
              <span>At a glance</span>
            </p>
            <h2 id="md-sec-overview" class="md-section-title">门店概况</h2>
          </header>
          <div class="md-overview">
            <div class="md-cover">
              <MediaWithFallback
                :src="merchant.business_license_wsh"
                :alt="`${merchant.name_wsh || '门店'}的照片`"
                :placeholder="merchant.name_wsh"
              />
            </div>
            <div class="md-info-panel">
              <div class="md-badge-row">
                <span :class="['badge', warmBadge(statusBadge)]">{{ statusLabel }}</span>
                <span :class="['badge', warmBadge(getStatusBadge(MerchantStoreMode, merchant.store_mode_wsh))]">
                  {{ getStatusLabel(MerchantStoreMode, merchant.store_mode_wsh) }}
                </span>
                <span :class="['badge', warmBadge(getStatusBadge(MerchantStoreStatus, merchant.store_status_wsh))]">
                  {{ getStatusLabel(MerchantStoreStatus, merchant.store_status_wsh) }}
                </span>
              </div>
              <dl class="md-info-list">
                <div class="md-info-row">
                  <dt>地址</dt>
                  <dd>{{ merchant.address_wsh || '地址待补充' }}</dd>
                </div>
                <div class="md-info-row">
                  <dt>联系电话</dt>
                  <dd>{{ merchant.phone_wsh || '未公开' }}</dd>
                </div>
                <div class="md-info-row">
                  <dt>负责人</dt>
                  <dd class="md-owner-cell">
                    <img v-if="merchant.owner_avatar_wsh" :src="merchant.owner_avatar_wsh" class="md-owner-avatar" alt="店主头像">
                    <span v-else class="md-owner-avatar md-owner-avatar-fallback">{{ (merchant.owner_name_wsh || '店').charAt(0) }}</span>
                    <button v-if="ownerHomeId" type="button" class="md-link-btn" @click="goToOwnerHome">{{ merchant.owner_name_wsh || '-' }}</button>
                    <span v-else>{{ merchant.owner_name_wsh || '-' }}</span>
                  </dd>
                </div>
                <div class="md-info-row">
                  <dt>营业模式</dt>
                  <dd>{{ getStatusLabel(MerchantStoreMode, merchant.store_mode_wsh) }}</dd>
                </div>
                <div v-if="merchant.created_at_wsh" class="md-info-row">
                  <dt>入驻时间</dt>
                  <dd>{{ formatDate(merchant.created_at_wsh) }}</dd>
                </div>
              </dl>
            </div>
          </div>
        </section>

        <!-- ═══ 02 资质核验 ═══ -->
        <section class="md-section" aria-labelledby="md-sec-qual">
          <header class="md-section-head">
            <p class="md-eyebrow">
              <span class="md-idx">02</span>
              <span class="md-line" aria-hidden="true"></span>
              <span>Credentials</span>
            </p>
            <h2 id="md-sec-qual" class="md-section-title">资质核验</h2>
            <p class="md-section-desc">平台逐项核验门店提交的证件，状态实时同步。</p>
          </header>
          <ul v-if="qualifications.length" class="md-qual-list">
            <li v-for="q in qualifications" :key="q.id_wsh" class="md-qual-item">
              <span class="md-qual-icon" aria-hidden="true">✓</span>
              <span class="md-qual-body">
                <span class="md-qual-title">{{ q.title_wsh }}</span>
                <span v-if="qualificationUrls(q.file_url_wsh).length" class="md-qual-thumbs">
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
          <div v-else class="md-empty">
            <p class="md-empty-title">暂未上传资质证明</p>
            <p class="md-empty-desc">该门店尚未上传资质证明，建议先咨询客服再下单。</p>
          </div>
        </section>

        <!-- ═══ 03 营业时间 ═══ -->
        <section class="md-section" aria-labelledby="md-sec-hours">
          <header class="md-section-head">
            <p class="md-eyebrow">
              <span class="md-idx">03</span>
              <span class="md-line" aria-hidden="true"></span>
              <span>Hours</span>
            </p>
            <h2 id="md-sec-hours" class="md-section-title">营业时间</h2>
          </header>
          <div v-if="hours.length" class="md-hours-card">
            <div v-for="h in hours" :key="h.day_of_week_wsh" class="md-hour-row">
              <span class="md-hour-day">{{ dayLabels[h.day_of_week_wsh] || `周${h.day_of_week_wsh}` }}</span>
              <span v-if="Number(h.is_closed_wsh) === 1" class="md-hour-closed">休息</span>
              <span v-else class="md-hour-time">{{ formatTime(h.open_time_wsh) }} — {{ formatTime(h.close_time_wsh) }}</span>
            </div>
          </div>
          <div v-else class="md-empty">
            <p class="md-empty-title">暂无营业时间</p>
            <p class="md-empty-desc">该商家还没有公布营业时间。</p>
          </div>
        </section>

        <!-- ═══ 04 服务项目 ═══ -->
        <section class="md-section" aria-labelledby="md-sec-services">
          <header class="md-section-head">
            <p class="md-eyebrow">
              <span class="md-idx">04</span>
              <span class="md-line" aria-hidden="true"></span>
              <span>Services</span>
            </p>
            <h2 id="md-sec-services" class="md-section-title">服务项目 ({{ services.length }})</h2>
            <p class="md-section-desc">门店可预约的服务清单，价格以商家实际确认为准。</p>
          </header>
          <div v-if="services.length" class="md-service-list">
            <article v-for="svc in services" :key="svc.id_wsh" class="md-service-item">
              <div class="md-service-info">
                <img v-if="firstImage(svc.images_wsh)" :src="firstImage(svc.images_wsh)" class="md-service-img" :alt="svc.name_wsh">
                <span v-else class="md-service-img md-service-img-fallback">{{ (svc.name_wsh || '服').charAt(0) }}</span>
                <div class="md-service-body">
                  <strong class="md-service-name">{{ svc.name_wsh }}</strong>
                  <span class="md-service-cat">{{ svc.category_name_wsh || svc.type_wsh }}</span>
                  <p v-if="svc.description_wsh" class="md-service-desc">{{ svc.description_wsh }}</p>
                </div>
              </div>
              <div class="md-service-action">
                <span class="md-price"><span class="md-price-symbol">¥</span>{{ money(svc.price_wsh) }}<small>/{{ unitLabel(svc.unit_wsh) || '次' }}</small></span>
                <button type="button" class="cta cta-primary cta-sm" :disabled="bookDisabled" @click="bookService(svc)">
                  {{ isOpen ? '预约' : '休息中·可预约' }}
                </button>
              </div>
            </article>
          </div>
          <div v-else class="md-empty">
            <p class="md-empty-title">暂无服务</p>
            <p class="md-empty-desc">该商家还没有上架服务项目。</p>
          </div>
        </section>

        <!-- ═══ 05 用户评价 ═══ -->
        <section class="md-section" aria-labelledby="md-sec-ratings">
          <header class="md-section-head">
            <p class="md-eyebrow">
              <span class="md-idx">05</span>
              <span class="md-line" aria-hidden="true"></span>
              <span>Reviews</span>
            </p>
            <h2 id="md-sec-ratings" class="md-section-title">用户评价 ({{ ratings.length }})</h2>
          </header>
          <div v-if="ratings.length" class="md-rating-list">
            <article v-for="r in ratings" :key="r.id_wsh" class="md-rating-item">
              <div class="md-rating-header">
                <strong class="md-rating-user">用户 #{{ r.user_id_wsh }}</strong>
                <span class="md-rating-stars" aria-hidden="true">{{ renderStars(r.score_wsh) }}</span>
                <span class="md-rating-date">{{ formatDate(r.created_at_wsh) }}</span>
              </div>
              <p v-if="r.content_wsh" class="md-rating-content">{{ r.content_wsh }}</p>
              <div v-if="r.reply_wsh" class="md-rating-reply">
                <span class="md-rating-reply-label">商家回复</span>
                <span>{{ r.reply_wsh }}</span>
              </div>
            </article>
          </div>
          <div v-else class="md-empty">
            <p class="md-empty-title">暂无评价</p>
            <p class="md-empty-desc">还没有用户留下评价。</p>
          </div>
        </section>
      </template>

      <!-- ═══ 门店不存在 ═══ -->
      <div v-else class="md-empty md-empty-page">
        <p class="md-empty-title">门店不存在</p>
        <p class="md-empty-desc">没有找到该商家的信息，它可能已下线或链接有误。</p>
        <button type="button" class="cta cta-primary" @click="goBack">返回门店列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useMerchantDetail } from '@/composables/useMerchantDetail'
import { useFavoriteState } from '@/composables/useFavoriteState'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { Star } from '@element-plus/icons-vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { MerchantStoreMode, MerchantStoreStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { parseCommaSeparatedUrls } from '@/utils/fileUrls'
import { unitLabel } from '@/domain/BookingUnit'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const {
  loading, merchant, services, hours, ratings,
  qualifications, statusLabel, statusBadge, dayLabels,
  load,
} = useMerchantDetail(route.params.id)

const favoriteTargetId = computed(() => Number(route.params.id))
const {
  isFavorited,
  loading: favoriteLoading,
  toggling: favoriteToggling,
  toggle: toggleFavoriteState,
} = useFavoriteState(favoriteTargetId, FAVORITE_TARGET_TYPES.MERCHANT)

const isOpen = computed(() => Number(merchant.value?.store_status_wsh) === 1)
const futureBookable = computed(() => Number(merchant.value?.future_booking_enabled_wsh) === 1)
const ownerHomeId = computed(() => merchant.value?.owner_keeper_id_wsh || null)
const bookDisabled = computed(() => !futureBookable.value)

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
  const n = Math.max(0, Math.round(Number(score) || 0))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

function qualificationUrls(value) {
  return parseCommaSeparatedUrls(value)
}

function firstImage(images) {
  if (!images) return null
  return String(images).split(',')[0]
}

function money(v) {
  return Number(v || 0).toFixed(2)
}

function formatDate(v) {
  return v ? String(v).slice(0, 10) : '-'
}

function formatTime(v) {
  return v ? String(v).slice(0, 5) : '-'
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

function bookService(svc) {
  if (!futureBookable.value) {
    appStore.addToast('该商家未开放未来预约', 'info')
    return
  }
  router.push({ path: `/services/${svc.id_wsh}`, query: { book: '1' } })
}

function goToOwnerHome() {
  if (!authStore.isLoggedIn) {
    appStore.showLoginPrompt = true
    return
  }
  if (!ownerHomeId.value) {
    appStore.addToast('该商家暂未开通店主主页', 'info')
    return
  }
  router.push(`/keepers/${ownerHomeId.value}`)
}

function goBack() {
  router.push('/merchants')
}

onMounted(load)
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.md-page {
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

.md-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.md-crumb {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
  flex-wrap: wrap;
}
.md-crumb-link { color: var(--ref-muted); text-decoration: none; transition: color 0.15s; }
.md-crumb-link:hover { color: var(--ref-ink); }
.md-crumb-sep { color: var(--ref-line); }
.md-crumb-here { color: var(--ref-ink-soft); }

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
.md-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 24px; padding: 20px 0 8px;
}
.md-head-copy { min-width: 0; flex: 1 1 320px; }
.md-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.md-idx { font-variant-numeric: tabular-nums; }
.md-line { width: 24px; height: 1px; background: var(--ref-line); }
.md-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.md-desc { margin: 10px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.md-head-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.md-head-actions .el-icon { font-size: 14px; }
.md-star-on { color: var(--ref-brand); }
.md-star-off { color: var(--ref-muted); }

/* ═══ Facts ═══ */
.md-facts {
  display: flex; flex-wrap: wrap; gap: 0; margin: 22px 0 0;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); overflow: hidden;
}
.md-fact { padding: 14px 22px; border-left: 1px solid var(--ref-line); }
.md-fact:first-child { border-left: 0; }
.md-fact dt { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.md-fact dd {
  margin: 6px 0 0; font-family: var(--ref-font-display);
  font-size: 22px; font-weight: 400; line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
}
.md-fact-rating { display: inline-flex; align-items: center; gap: 6px; }
.md-star { color: var(--ref-brand); font-size: 16px; }

/* ═══ Section ═══ */
.md-section { margin-top: 40px; }
.md-section-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 4px 24px; }
.md-section-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.md-section-desc { margin: 8px 0 0; max-width: 640px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }

/* ═══ 01 门店概况 ═══ */
.md-overview {
  display: grid; grid-template-columns: 1.35fr 1fr; gap: 20px; margin-top: 22px;
}
.md-cover {
  min-height: 320px; overflow: hidden; aspect-ratio: 16 / 10;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-sand);
}
.md-cover :deep(.media-image) { width: 100%; height: 100%; object-fit: cover; }
.md-cover :deep(.media-placeholder) { width: 100%; height: 100%; background: var(--ref-sand); }
.md-cover :deep(.media-placeholder .el-icon) { color: var(--ref-brand); }
.md-cover :deep(.media-placeholder span) { color: var(--ref-muted); }
.md-info-panel {
  padding: 22px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
}
.md-badge-row { display: flex; flex-wrap: wrap; gap: 8px; }
.md-info-list { display: grid; gap: 14px; margin: 20px 0 0; }
.md-info-row {
  display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 12px;
  padding-top: 12px; border-top: 1px solid var(--ref-line);
}
.md-info-row dt { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); padding-top: 2px; }
.md-info-row dd { margin: 0; font-size: 13.5px; line-height: 1.6; color: var(--ref-ink); min-width: 0; word-break: break-word; }
.md-owner-cell { display: flex; align-items: center; gap: 8px; }
.md-owner-avatar { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.md-owner-avatar-fallback {
  background: color-mix(in srgb, var(--ref-brand) 16%, transparent);
  color: var(--ref-brand-deep); font-size: 13px; font-weight: 600;
  display: inline-flex; align-items: center; justify-content: center;
}
.md-link-btn {
  background: none; border: none; padding: 0; cursor: pointer;
  color: var(--ref-brand); font-size: 13.5px; font-weight: 500; text-decoration: underline;
  text-underline-offset: 3px; transition: color 0.15s;
}
.md-link-btn:hover { color: var(--ref-brand-deep); }

/* ═══ 02 资质核验 ═══ */
.md-qual-list {
  margin: 22px 0 0; padding: 0; list-style: none;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); overflow: hidden;
}
.md-qual-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-top: 1px solid var(--ref-line);
}
.md-qual-item:first-child { border-top: 0; }
.md-qual-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--ref-sand); color: var(--ref-brand); font-size: 13px; flex-shrink: 0;
}
.md-qual-body { min-width: 0; flex: 1; display: grid; gap: 6px; }
.md-qual-title { font-size: 13.5px; font-weight: 500; color: var(--ref-ink); }
.md-qual-thumbs { display: flex; flex-wrap: wrap; gap: 6px; }
.md-qual-thumbs img {
  width: 44px; height: 44px; border-radius: 6px; object-fit: cover;
  border: 1px solid var(--ref-line);
}

/* ═══ 03 营业时间 ═══ */
.md-hours-card {
  margin: 22px 0 0; padding: 6px 22px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
}
.md-hour-row {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 13px 0; border-bottom: 1px solid var(--ref-line);
}
.md-hour-row:last-child { border-bottom: 0; }
.md-hour-day { font-size: 13.5px; font-weight: 500; color: var(--ref-ink); }
.md-hour-closed { font-size: 12.5px; color: var(--ref-muted); }
.md-hour-time { font-size: 12.5px; color: var(--ref-ink-soft); font-variant-numeric: tabular-nums; }

/* ═══ 04 服务项目 ═══ */
.md-service-list { display: grid; gap: 12px; margin-top: 22px; }
.md-service-item {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
  transition: border-color 0.15s, box-shadow 0.15s;
}
.md-service-item:hover { border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent); }
.md-service-info { display: flex; gap: 14px; align-items: flex-start; flex: 1; min-width: 0; }
.md-service-img {
  width: 68px; height: 68px; border-radius: 10px; object-fit: cover; flex-shrink: 0;
  border: 1px solid var(--ref-line);
}
.md-service-img-fallback {
  display: inline-flex; align-items: center; justify-content: center;
  background: var(--ref-sand); color: var(--ref-brand-deep);
  font-family: var(--ref-font-display); font-size: 24px;
}
.md-service-body { min-width: 0; }
.md-service-name { display: block; font-size: 15px; font-weight: 500; color: var(--ref-ink); }
.md-service-cat { display: block; margin-top: 3px; font-size: 12px; color: var(--ref-muted); }
.md-service-desc { margin: 6px 0 0; font-size: 12.5px; line-height: 1.6; color: var(--ref-ink-soft); opacity: 0.85; }
.md-service-action { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; flex-shrink: 0; }
.md-price { font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--ref-brand-deep); font-variant-numeric: tabular-nums; }
.md-price-symbol { font-size: 13px; vertical-align: top; margin-right: 1px; }
.md-price small { font-size: 12px; font-weight: 400; color: var(--ref-muted); margin-left: 2px; }

/* ═══ 05 用户评价 ═══ */
.md-rating-list { display: grid; gap: 0; margin-top: 22px; }
.md-rating-item {
  padding: 18px 2px; border-top: 1px solid var(--ref-line);
}
.md-rating-item:first-child { border-top: 0; }
.md-rating-header { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.md-rating-user { font-size: 13px; font-weight: 500; color: var(--ref-ink); }
.md-rating-stars { font-size: 12px; color: var(--ref-brand); letter-spacing: 2px; }
.md-rating-date { font-size: 12px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.md-rating-content { margin: 8px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }
.md-rating-reply {
  margin-top: 10px; padding: 10px 14px;
  border-left: 2px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
  border-radius: 0 var(--r-btn) var(--r-btn) 0;
  font-size: 12.5px; line-height: 1.7; color: var(--ref-ink-soft);
}
.md-rating-reply-label {
  display: block; font-size: 10.5px; letter-spacing: 0.14em; text-transform: uppercase;
  color: var(--ref-muted); margin-bottom: 2px;
}

/* ═══ Empty ═══ */
.md-empty {
  margin-top: 22px; padding: 40px 24px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.md-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--ref-ink); }
.md-empty-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }
.md-empty-page { padding: 72px 24px; }
.md-empty-page .cta { margin-top: 22px; }

/* ═══ Loading skeleton ═══ */
.md-loading { display: grid; gap: 16px; padding: 24px 0 0; }
.md-skel {
  border-radius: 10px;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: md-shimmer 1.3s linear infinite;
}
.md-skel-eyebrow { width: 96px; height: 12px; }
.md-skel-title { width: 60%; max-width: 360px; height: 40px; }
.md-skel-lede { width: 70%; max-width: 460px; height: 14px; }
.md-skel-cover { width: 100%; height: 300px; }

/* ═══ Animations ═══ */
@keyframes md-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .md-overview { grid-template-columns: 1fr; }
  .md-cover { min-height: 240px; aspect-ratio: 16 / 9; }
}
@media (max-width: 600px) {
  .md-shell { padding: 0 16px; }
  .md-facts { width: 100%; }
  .md-fact { flex: 1; padding: 12px 14px; }
  .md-fact dd { font-size: 19px; }
  .md-service-item { align-items: stretch; flex-direction: column; }
  .md-service-action { align-items: flex-start; flex-direction: row; justify-content: space-between; }
  .md-info-row { grid-template-columns: 1fr; gap: 4px; }
}
@media (prefers-reduced-motion: reduce) {
  .md-skel { animation: none; }
  .cta:hover { transform: none; }
}
</style>
