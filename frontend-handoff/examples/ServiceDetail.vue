<template>
  <div class="svc-detail">
    <div class="svc-shell">
      <template v-if="loading">
        <div class="d-skeleton" aria-label="服务加载中">
          <div class="d-skel-media" />
          <div class="d-skel-info" />
        </div>
      </template>

      <div v-else-if="loadError" class="d-empty">
        <h2>{{ loadError.title }}</h2>
        <p>{{ loadError.description }}</p>
        <button class="r-btn r-btn-outline" type="button" @click="goToCatalog">返回服务列表</button>
      </div>

      <template v-else-if="service">
        <nav class="d-crumb" aria-label="面包屑">
          <a class="d-back" role="button" tabindex="0" @click="goToCatalog" @keydown.enter.prevent="goToCatalog">
            <span class="d-back-arrow" aria-hidden="true">←</span>
            返回全部服务
          </a>
        </nav>

        <header class="d-hero">
          <div class="d-media">
            <div class="d-frame">
              <MediaWithFallback
                :src="activeMediaUrl"
                :alt="`${service.name_wsh} 主图`"
                placeholder="暂无图片"
              />
            </div>
            <div v-if="mediaList.length > 1" class="d-thumbs">
              <button
                v-for="(media, index) in mediaList"
                :key="media.url_wsh || index"
                type="button"
                class="d-thumb"
                :class="{ active: index === activeIndex }"
                :aria-label="`查看第 ${index + 1} 张图片`"
                @click="setActive(index)"
              >
                <MediaWithFallback :src="media.url_wsh" :alt="`第 ${index + 1} 张图片`" />
              </button>
            </div>
          </div>

          <div class="d-info">
            <span class="d-cat">{{ service.category_name_wsh || '未分类' }}</span>
            <h1 class="d-title text-break">{{ service.name_wsh }}</h1>
            <p class="d-desc text-break">{{ service.description_wsh || '暂无服务描述' }}</p>

            <div v-if="service.service_rating_count_wsh" class="d-rating">
              ★ {{ Number(service.service_rating_wsh).toFixed(1) }}（{{ service.service_rating_count_wsh }} 条评价）
            </div>

            <div class="d-price-row">
              <p class="d-price">
                <span class="d-currency">¥</span>
                <span class="d-amount">{{ money(service.price_wsh) }}</span>
                <span class="d-unit">/ {{ unitLabel(service.unit_wsh) || '次' }}</span>
              </p>
            </div>

            <p v-if="bookableReasonText" class="d-notice">{{ bookableReasonText }}</p>

            <div class="d-actions">
              <button ref="bookButtonEl" type="button" class="r-btn r-btn-primary" @click="createOrder">
                立即预约
                <span class="r-arrow" aria-hidden="true">→</span>
              </button>
              <FavoriteToggleButton
                class="btn-sm"
                :target-id="service.id_wsh"
                :target-type="FAVORITE_TARGET_TYPES.SERVICE"
              />
            </div>
          </div>
        </header>

        <section v-if="service.merchant_name_wsh" class="d-card">
          <h2 class="d-card-title">商家信息</h2>
          <div
            class="d-provider"
            role="link"
            tabindex="0"
            :aria-label="`查看商家 ${service.merchant_name_wsh}`"
            @click="goToMerchant(service.merchant_id_wsh)"
            @keydown.enter.prevent="goToMerchant(service.merchant_id_wsh)"
            @keydown.space.prevent="goToMerchant(service.merchant_id_wsh)"
          >
            <div class="d-avatar">{{ service.merchant_name_wsh.charAt(0) }}</div>
            <div class="d-provider-info">
              <strong>{{ service.merchant_name_wsh }}</strong>
              <span>{{ merchant?.address_wsh || '-' }}</span>
              <span v-if="service.service_rating_count_wsh">★ {{ Number(service.service_rating_wsh).toFixed(1) }}（{{ service.service_rating_count_wsh }} 条）</span>
            </div>
            <span class="d-arrow" aria-hidden="true">›</span>
          </div>

          <div v-if="keepers.length" class="d-keepers">
            <h3>看护员（{{ keepers.length }}）</h3>
            <div class="d-keeper-list">
              <div
                v-for="k in keepers"
                :key="k.id_wsh"
                class="d-keeper"
                role="link"
                tabindex="0"
                :aria-label="`查看看护员 ${k.name_wsh}`"
                @click="goToKeeper(k.id_wsh)"
                @keydown.enter.prevent="goToKeeper(k.id_wsh)"
                @keydown.space.prevent="goToKeeper(k.id_wsh)"
              >
                <img v-if="k.avatar_wsh" :src="k.avatar_wsh" class="d-keeper-avatar">
                <div v-else class="d-keeper-avatar d-keeper-ph">{{ k.name_wsh?.charAt(0) || '看' }}</div>
                <div class="d-keeper-info">
                  <strong>{{ k.name_wsh }}</strong>
                  <span>{{ k.experience_years_wsh || 0 }}年经验</span>
                </div>
                <span class="d-arrow" aria-hidden="true">›</span>
              </div>
            </div>
          </div>
        </section>

        <section class="d-card">
          <h2 class="d-card-title">商家评价 ({{ merchantRatings.length }})</h2>
          <p v-if="merchantRatingsError" class="d-empty-line">商家评价加载失败</p>
          <p v-else-if="merchantRatingsLoading" class="d-empty-line">加载中...</p>
          <p v-else-if="merchantRatings.length === 0" class="d-empty-line">暂无商家评价</p>
          <div v-else class="d-reviews">
            <article v-for="r in merchantRatings" :key="r.id_wsh" class="d-review">
              <header>
                <span class="d-review-user">用户 #{{ r.user_id_wsh }}</span>
                <span class="d-review-stars">★ {{ r.score_wsh }}</span>
                <time>{{ formatDate(r.created_at_wsh) }}</time>
              </header>
              <p>{{ r.content_wsh }}</p>
            </article>
          </div>
        </section>

        <section class="d-card">
          <h2 class="d-card-title">用户评价 ({{ ratings.length }})</h2>
          <p v-if="ratingsError" class="d-empty-line">评价加载失败</p>
          <p v-else-if="ratings.length === 0" class="d-empty-line">暂无评价</p>
          <div v-else class="d-reviews">
            <article v-for="r in ratings" :key="r.id_wsh" class="d-review">
              <header>
                <span class="d-review-user">用户 #{{ r.user_id_wsh }}</span>
                <span class="d-review-stars">★ {{ r.score_wsh }}</span>
                <time>{{ formatDate(r.created_at_wsh) }}</time>
              </header>
              <p>{{ r.content_wsh }}</p>
              <div v-if="r.reply_wsh" class="d-reply"><strong>商家回复：</strong>{{ r.reply_wsh }}</div>
            </article>
          </div>
        </section>
      </template>
    </div>

    <CreateOrderDialog
      :visible="showBookingDialog"
      :initial-service-id="String(service?.id_wsh || route.params.id || '')"
      @close="closeBookingDialog"
      @created="onBookingCreated"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getServiceDetail } from '@/api/service'
import { unitLabel } from '@/domain/BookingUnit'
import { getRatings } from '@/api/rating'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import * as merchantService from '@/services/merchantService'
import * as keeperService from '@/services/keeperService'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import FavoriteToggleButton from '@/components/common/FavoriteToggleButton.vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const service = ref(null)
const loading = ref(true)
const loadError = ref(null)
const showBookingDialog = ref(false)
const bookButtonEl = ref(null)
const mediaList = ref([])
const activeIndex = ref(0)
const ratings = ref([])
const merchantRatings = ref([])
const merchantRatingsLoading = ref(false)
const merchantRatingsError = ref(false)
const ratingsError = ref(false)
const merchant = ref(null)
const keepers = ref([])
let requestSeq = 0

const activeMediaUrl = computed(() => mediaList.value[activeIndex.value]?.url_wsh || '')

const BOOKABLE_REASON_TEXT = {
  FUTURE_BOOKING_DISABLED: '该商家暂未开放未来预约，暂不支持在线预约',
  UNSUPPORTED_SERVICE_UNIT: '该服务计费方式暂不支持在线预约，请线下联系商家',
}

const bookableReasonText = computed(() => {
  if (service.value?.bookable_wsh === false) {
    return BOOKABLE_REASON_TEXT[service.value.bookable_reason_wsh]
      || service.value.bookable_reason_wsh
      || '该服务暂不支持在线预约'
  }
  return ''
})

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value) {
  if (!value) return ''
  try {
    return new Date(value).toLocaleDateString('zh-CN')
  } catch {
    return String(value)
  }
}

function setActive(index) {
  if (index >= 0 && index < mediaList.value.length) {
    activeIndex.value = index
  }
}

async function loadDetail() {
  const seq = ++requestSeq
  loading.value = true
  loadError.value = null
  const id = route.params.id
  try {
    const [detailRes, ratingRes] = await Promise.all([
      getServiceDetail(id),
      getRatings({ targetId: id, targetType: 'service' }).catch(() => null),
    ])
    if (seq !== requestSeq) return
    if (detailRes.code === 200 && detailRes.data) {
      service.value = detailRes.data
      mediaList.value = detailRes.data.media_wsh || []
      const coverIndex = mediaList.value.findIndex(m => Number(m.is_cover_wsh) === 1)
      activeIndex.value = coverIndex >= 0 ? coverIndex : 0
      if (detailRes.data.bookable_wsh === false && bookableReasonText.value) {
        appStore.addToast(bookableReasonText.value, 'warning')
      }
      if (detailRes.data.merchant_id_wsh) {
        loadProviderInfo(detailRes.data.merchant_id_wsh, seq)
        loadMerchantRatings(detailRes.data.merchant_id_wsh, seq)
      }
    } else {
      service.value = null
      loadError.value = {
        title: '服务不可用',
        description: detailRes.message || '该服务不存在或已下架',
      }
    }
    if (ratingRes && ratingRes.code === 200) {
      ratings.value = ratingRes.data || []
    } else {
      ratingsError.value = true
    }
  } catch (error) {
    if (seq !== requestSeq) return
    service.value = null
    loadError.value = {
      title: '加载失败',
      description: '服务信息加载失败，请稍后重试',
    }
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

async function loadMerchantRatings(merchantId, seq) {
  merchantRatingsLoading.value = true
  merchantRatingsError.value = false
  try {
    const res = await getRatings({ targetId: merchantId, targetType: 'merchant' })
    if (seq !== requestSeq) return
    if (res.code === 200) {
      merchantRatings.value = res.data || []
    } else {
      merchantRatingsError.value = true
    }
  } catch {
    if (seq !== requestSeq) return
    merchantRatingsError.value = true
  } finally {
    if (seq === requestSeq) {
      merchantRatingsLoading.value = false
    }
  }
}

async function loadProviderInfo(merchantId, seq) {
  try {
    const [m, k] = await Promise.all([
      merchantService.getById(merchantId),
      keeperService.getByMerchant(merchantId),
    ])
    if (seq !== requestSeq) return
    merchant.value = m
    keepers.value = k || []
  } catch {
    if (seq !== requestSeq) return
    merchant.value = null
    keepers.value = []
  }
}

async function createOrder() {
  if (!service.value) return

  if (service.value.bookable_wsh === false) {
    appStore.addToast(bookableReasonText.value || '该服务暂不支持在线预约', 'warning')
    return
  }

  if (!authStore.isLoggedIn) {
    appStore.loginRedirectPath = `/services/${service.value.id_wsh}?book=1`
    appStore.showLoginPrompt = true
    return
  }

  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!ok) return

  showBookingDialog.value = true
}

function closeBookingDialog() {
  showBookingDialog.value = false
  bookButtonEl.value?.focus?.()
}

function onBookingCreated(data) {
  // 单笔：进入订单详情；批量（多宠物连续下单）：进入订单列表逐单支付
  if (data?.id_wsh) {
    router.push(`/orders/${data.id_wsh}`)
  } else if (Array.isArray(data?.orders) && data.orders.length > 0) {
    router.push('/orders')
  }
}

async function handleBookingResume() {
  const query = route.query || {}
  const keys = Object.keys(query)
  const isOneShotBooking = keys.length === 1 && keys[0] === 'book' && query.book === '1'
  if (!isOneShotBooking) return
  if (!authStore.isLoggedIn) {
    appStore.loginRedirectPath = `/services/${route.params.id}?book=1`
    appStore.showLoginPrompt = true
    router.replace({ path: route.path, query: {} })
    return
  }
  if (!service.value) return
  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!ok) return
  if (service.value.bookable_wsh === false) {
    appStore.addToast(bookableReasonText.value || '该服务暂不支持在线预约', 'warning')
    return
  }
  showBookingDialog.value = true
  router.replace({ path: route.path, query: {} })
}

function goToMerchant(merchantId) {
  router.push(`/merchants/${merchantId}`)
}

function goToKeeper(keeperId) {
  router.push(`/keepers/${keeperId}`)
}

function goToCatalog() {
  router.push('/services')
}

watch(() => route.params.id, () => {
  loadDetail()
})

onMounted(() => {
  loadDetail().then(() => handleBookingResume())
})
</script>

<style scoped>
.svc-detail {
  width: 100%;
  padding: 10px 0 64px;
}

.svc-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* Back link */
.d-crumb {
  padding: 6px 0 18px;
}
.d-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ref-ink-soft);
  font-size: 13px;
  cursor: pointer;
}
.d-back:hover {
  color: var(--ref-brand);
}
.d-back-arrow {
  transition: transform 150ms ease;
}
.d-back:hover .d-back-arrow {
  transform: translateX(-3px);
}

/* Hero */
.d-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 44px;
  align-items: start;
}
.d-frame {
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid var(--ref-line);
  background: var(--ref-sand);
}
.d-frame :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.d-thumbs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 12px;
}
.d-thumb {
  aspect-ratio: 1;
  padding: 0;
  overflow: hidden;
  border-radius: 14px;
  border: 2px solid transparent;
  background: var(--ref-sand);
  cursor: pointer;
}
.d-thumb.active {
  border-color: var(--ref-brand);
}
.d-thumb:focus-visible {
  outline: 2px solid var(--ref-brand);
  outline-offset: 2px;
}
.d-thumb :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.d-info {
  min-width: 0;
}
.d-cat {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border: 1px solid var(--ref-line);
  border-radius: 99px;
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12px;
}
.d-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
}
.d-desc {
  margin: 16px 0 0;
  font-size: 15px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 88%, transparent);
}
.d-rating {
  margin: 16px 0 0;
  font-size: 14px;
  color: var(--ref-brand);
}
.d-price-row {
  margin: 28px 0 0;
  padding-top: 22px;
  border-top: 1px solid var(--ref-line);
}
.d-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
}
.d-currency {
  font-size: 14px;
  color: var(--ref-muted);
}
.d-amount {
  font-family: var(--ref-font-display);
  font-size: 46px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.d-unit {
  font-size: 14px;
  color: var(--ref-muted);
}
.d-notice {
  margin: 18px 0 0;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 28%, transparent);
  background: color-mix(in srgb, var(--ref-brand) 10%, transparent);
  color: var(--ref-brand-deep);
  font-size: 13.5px;
  line-height: 1.6;
}
.d-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 28px;
}

/* Editorial button */
.r-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: transform 150ms ease, background 150ms ease, border-color 150ms ease, color 150ms ease, box-shadow 150ms ease;
}
.r-btn:hover {
  transform: translateY(-1px);
}
.r-btn-primary {
  min-height: 48px;
  padding: 0 22px;
  background: var(--ref-brand);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 12px 26px -14px color-mix(in srgb, var(--ref-brand) 80%, transparent);
}
.r-btn-primary:hover {
  background: var(--ref-brand-deep);
}
.r-btn-outline {
  min-height: 44px;
  padding: 0 18px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  border: 1px solid var(--ref-line);
}
.r-btn-outline:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  background: var(--ref-sand);
}
.r-arrow {
  transition: transform 150ms ease;
}
.r-btn-primary:hover .r-arrow {
  transform: translateX(3px);
}

/* Cards */
.d-card {
  margin-top: 36px;
  padding: 28px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: 22px;
}
.d-card-title {
  margin: 0 0 18px;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.d-provider {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  cursor: pointer;
  transition: background 150ms ease;
}
.d-provider:focus-visible,
.d-keeper:focus-visible,
.d-back:focus-visible {
  outline: 2px solid var(--ref-brand);
  outline-offset: 2px;
}
.d-provider:hover {
  background: var(--ref-sand);
}
.d-avatar {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ref-brand);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}
.d-provider-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.d-provider-info strong {
  font-size: 15px;
  color: var(--ref-ink);
}
.d-provider-info span {
  font-size: 12.5px;
  color: var(--ref-muted);
}
.d-arrow {
  font-size: 22px;
  color: var(--ref-muted);
}
.d-keepers {
  margin-top: 20px;
  padding-left: 2px;
}
.d-keepers h3 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--ref-ink);
}
.d-keeper-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.d-keeper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 150ms ease;
}
.d-keeper:hover {
  background: var(--ref-sand);
}
.d-keeper-avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
}
.d-keeper-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ref-brand);
  color: #fff;
  font-size: 14px;
}
.d-keeper-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.d-keeper-info strong {
  font-size: 13px;
  color: var(--ref-ink);
}
.d-keeper-info span {
  font-size: 12px;
  color: var(--ref-muted);
}

/* Reviews */
.d-empty-line {
  color: var(--ref-muted);
  font-size: 14px;
}
.d-reviews {
  display: flex;
  flex-direction: column;
}
.d-review {
  padding: 16px 0;
  border-top: 1px solid var(--ref-line);
}
.d-review header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 8px;
  font-size: 13px;
}
.d-review-user {
  font-weight: 600;
  color: var(--ref-ink);
}
.d-review-stars {
  color: var(--ref-brand);
}
.d-review time {
  margin-left: auto;
  color: var(--ref-muted);
}
.d-review > p {
  font-size: 14px;
  line-height: 1.6;
  color: var(--ref-ink-soft);
}
.d-reply {
  margin-top: 10px;
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--ref-sand);
  font-size: 13px;
  line-height: 1.6;
  color: var(--ref-ink-soft);
}
.d-reply strong {
  color: var(--ref-ink);
}

/* Empty & skeleton */
.d-empty {
  padding: 80px 24px;
  text-align: center;
  color: var(--ref-muted);
}
.d-empty h2 {
  margin-bottom: 8px;
  font-family: var(--ref-font-display);
  font-size: 26px;
  color: var(--ref-ink);
}
.d-empty p {
  margin-bottom: 20px;
  font-size: 14px;
}
.d-skeleton {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 44px;
}
.d-skel-media {
  aspect-ratio: 16 / 10;
  border-radius: 20px;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: ref-shimmer 1.3s linear infinite;
}
.d-skel-info {
  height: 320px;
  border-radius: 20px;
  background: var(--ref-sand);
}
.text-break {
  overflow-wrap: anywhere;
  word-break: break-word;
}

@keyframes ref-shimmer {
  to { background-position: -200% 0; }
}

@media (max-width: 900px) {
  .d-hero {
    grid-template-columns: 1fr;
    gap: 28px;
  }
  .d-skeleton {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .svc-shell {
    padding: 0 16px;
  }
  .d-thumbs {
    gap: 8px;
  }
  .d-card {
    padding: 20px;
  }
  .d-amount {
    font-size: 38px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .r-btn,
  .d-back,
  .r-arrow {
    transition: none;
  }
  .d-skel-media,
  .d-skel-info {
    animation: none;
  }
}
</style>