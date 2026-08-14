<template>
  <div>
    <PageHero :title="service?.name_wsh || '服务详情'" subtitle="查看服务信息、商家和看护员" />

    <LoadingSpinner v-if="loading" text="加载服务信息..." />

    <div v-else-if="loadError" class="detail-container">
      <EmptyState :title="loadError.title" :description="loadError.description">
        <button class="btn btn-primary" @click="goToCatalog">返回服务列表</button>
      </EmptyState>
    </div>

    <div v-else-if="service" class="detail-container">
      <div class="detail-main">
        <section class="card">
          <div class="service-hero">
            <div class="gallery">
              <div class="gallery-main">
                <MediaWithFallback
                  :src="activeMediaUrl"
                  :alt="`${service.name_wsh} 主图`"
                  placeholder="暂无图片"
                />
              </div>
              <div v-if="mediaList.length > 1" class="gallery-thumbs">
                <button
                  v-for="(media, index) in mediaList"
                  :key="media.url_wsh || index"
                  type="button"
                  class="gallery-thumb"
                  :class="{ active: index === activeIndex }"
                  :aria-label="`查看第 ${index + 1} 张图片`"
                  :aria-current="index === activeIndex ? 'true' : undefined"
                  @click="setActive(index)"
                  @keydown.enter.prevent="setActive(index)"
                  @keydown.space.prevent="setActive(index)"
                >
                  <MediaWithFallback :src="media.url_wsh" :alt="`第 ${index + 1} 张图片`" />
                </button>
              </div>
            </div>

            <div class="service-hero-info">
              <span class="badge badge-info">{{ service.category_name_wsh || '未分类' }}</span>
              <h1 class="text-break">{{ service.name_wsh }}</h1>
              <div class="service-price">¥{{ money(service.price_wsh) }} <span>/ {{ service.unit_wsh || '次' }}</span></div>
              <p class="service-desc text-break">{{ service.description_wsh || '暂无服务描述' }}</p>
              <div v-if="service.service_rating_count_wsh" class="rating-summary">
                ★ {{ Number(service.service_rating_wsh).toFixed(1) }}（{{ service.service_rating_count_wsh }} 条）
              </div>
              <div class="service-actions">
                <FavoriteToggleButton
                  class="btn-sm"
                  :target-id="service.id_wsh"
                  :target-type="FAVORITE_TARGET_TYPES.SERVICE"
                />
                <button ref="bookButtonEl" class="btn btn-primary" @click="createOrder">立即预约</button>
              </div>
              <p v-if="service.bookable_wsh === false && bookableReasonText" class="bookable-reason">
                {{ bookableReasonText }}
              </p>
            </div>
          </div>
        </section>

        <section v-if="service.merchant_name_wsh" class="card">
          <h3 class="section-title">商家信息</h3>
          <div
            class="provider-card"
            role="link"
            tabindex="0"
            :aria-label="`查看商家 ${service.merchant_name_wsh}`"
            @click="goToMerchant(service.merchant_id_wsh)"
            @keydown.enter.prevent="goToMerchant(service.merchant_id_wsh)"
            @keydown.space.prevent="goToMerchant(service.merchant_id_wsh)"
          >
            <div class="provider-avatar">{{ service.merchant_name_wsh.charAt(0) }}</div>
            <div class="provider-info">
              <strong>{{ service.merchant_name_wsh }}</strong>
              <span class="text-muted">{{ merchant?.address_wsh || '-' }}</span>
              <span v-if="service.service_rating_count_wsh" class="text-muted rating-summary">
                ★ {{ Number(service.service_rating_wsh).toFixed(1) }}（{{ service.service_rating_count_wsh }} 条）
              </span>
            </div>
            <span class="link-arrow">&rsaquo;</span>
          </div>

          <div v-if="keepers.length > 0" class="keepers-sub">
            <h4>看护员（{{ keepers.length }}）</h4>
            <div class="keeper-mini-list">
              <div
                v-for="k in keepers"
                :key="k.id_wsh"
                class="keeper-mini-item"
                role="link"
                tabindex="0"
                :aria-label="`查看看护员 ${k.name_wsh}`"
                @click="goToKeeper(k.id_wsh)"
                @keydown.enter.prevent="goToKeeper(k.id_wsh)"
                @keydown.space.prevent="goToKeeper(k.id_wsh)"
              >
                <img v-if="k.avatar_wsh" :src="k.avatar_wsh" class="keeper-mini-avatar">
                <div v-else class="keeper-mini-avatar placeholder">{{ k.name_wsh?.charAt(0) || '看' }}</div>
                <div class="keeper-mini-info">
                  <strong>{{ k.name_wsh }}</strong>
                  <span class="text-muted">{{ k.experience_years_wsh || 0 }}年经验</span>
                </div>
                <span class="link-arrow">&rsaquo;</span>
              </div>
            </div>
          </div>
        </section>

        <section class="card">
          <h3 class="section-title">商家评价 ({{ merchantRatings.length }})</h3>
          <p v-if="merchantRatingsError" class="text-muted">商家评价加载失败</p>
          <div v-else-if="merchantRatingsLoading" class="text-muted">加载中...</div>
          <div v-else-if="merchantRatings.length === 0" class="empty-state">
            <p>暂无商家评价</p>
          </div>
          <div v-else class="review-list">
            <div v-for="r in merchantRatings" :key="r.id_wsh" class="review-card">
              <div class="review-header">
                <span class="review-user">用户 #{{ r.user_id_wsh }}</span>
                <span class="review-stars">★ {{ r.score_wsh }}</span>
                <span class="review-date">{{ formatDate(r.created_at_wsh) }}</span>
              </div>
              <p class="review-content">{{ r.content_wsh }}</p>
            </div>
          </div>
        </section>

        <section class="card">
          <h3 class="section-title">用户评价 ({{ ratings.length }})</h3>
          <p v-if="ratingsError" class="text-muted">评价加载失败</p>
          <div v-else-if="ratings.length === 0" class="empty-state">
            <p>暂无评价</p>
          </div>
          <div v-else class="review-list">
            <div v-for="r in ratings" :key="r.id_wsh" class="review-card">
              <div class="review-header">
                <span class="review-user">用户 #{{ r.user_id_wsh }}</span>
                <span class="review-stars">★ {{ r.score_wsh }}</span>
                <span class="review-date">{{ formatDate(r.created_at_wsh) }}</span>
              </div>
              <p class="review-content">{{ r.content_wsh }}</p>
              <div v-if="r.reply_wsh" class="review-reply">
                <strong>商家回复：</strong>{{ r.reply_wsh }}
              </div>
            </div>
          </div>
        </section>
      </div>

      <aside class="detail-sidebar">
        <div class="card">
          <h4>快捷操作</h4>
          <button ref="bookButtonEl" class="btn btn-primary btn-block" @click="createOrder">立即预约</button>
          <button v-if="service.merchant_name_wsh" class="btn btn-outline btn-block" @click="goToMerchant(service.merchant_id_wsh)">
            查看商家
          </button>
          <button class="btn btn-outline btn-block" @click="goToCatalog">返回列表</button>
        </div>
      </aside>
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
import { getRatings } from '@/api/rating'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import * as merchantService from '@/services/merchantService'
import * as keeperService from '@/services/keeperService'
import PageHero from '@/components/common/PageHero.vue'
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
  if (data?.id_wsh) {
    router.push(`/orders/${data.id_wsh}`)
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
.detail-container {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 0;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
}

.service-hero {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.gallery {
  width: 360px;
  flex-shrink: 0;
}

.gallery-main {
  width: 100%;
  aspect-ratio: 3 / 2;
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-muted);
}

.gallery-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.gallery-thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: var(--color-muted);
}

.gallery-thumb.active {
  border-color: var(--color-primary);
}

.gallery-thumb:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.text-break {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.bookable-reason {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.rating-summary {
  margin: 8px 0 0;
}

.service-hero-info {
  flex: 1;
  min-width: 0;
}

.service-hero-info h1 {
  margin: 10px 0 8px;
  font-size: 24px;
}

.service-price {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 12px 0;
}

.service-price span {
  font-size: 14px;
  font-weight: 400;
  color: var(--color-muted-foreground);
}

.service-desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-muted-foreground);
  margin: 8px 0 16px;
}

.service-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.section-title {
  margin: 0 0 14px;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 32px 0;
  color: var(--color-muted-foreground);
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
}

.review-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.review-user {
  font-weight: 600;
}

.review-stars {
  color: #f59e0b;
}

.review-date {
  color: var(--color-muted-foreground);
  margin-left: auto;
}

.review-content {
  font-size: 14px;
  line-height: 1.5;
}

.review-reply {
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--color-muted);
  border-radius: 8px;
  font-size: 13px;
}

.provider-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.provider-card:focus-visible,
.keeper-mini-item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.provider-card:hover {
  background: var(--color-muted);
}

.provider-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.provider-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.provider-info strong {
  font-size: 15px;
}

.link-arrow {
  font-size: 22px;
  color: var(--color-muted-foreground);
}

.keepers-sub {
  margin-top: 12px;
  padding-left: 8px;
}

.keepers-sub h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.keeper-mini-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.keeper-mini-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.keeper-mini-item:hover {
  background: var(--color-muted);
}

.keeper-mini-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.keeper-mini-avatar.placeholder {
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.keeper-mini-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.keeper-mini-info strong {
  font-size: 13px;
}

.text-muted {
  color: var(--color-muted-foreground);
  font-size: 12px;
}

.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.btn-block {
  width: 100%;
}

@media (max-width: 960px) {
  .detail-container {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .service-hero {
    flex-direction: column;
  }

  .gallery {
    width: 100%;
  }

  .review-date {
    margin-left: 0;
  }
}
</style>
