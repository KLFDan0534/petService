<template>
  <div>
    <PageHero :title="service?.name_wsh || '服务详情'" subtitle="查看服务信息、商家和看护员" />

    <LoadingSpinner v-if="loading" text="加载服务信息..." />

    <div v-else-if="service" class="detail-container">
      <div class="detail-main">
        <section class="card">
          <div class="service-hero">
            <div v-if="firstImage" class="service-cover" :style="{ backgroundImage: `url(${firstImage})` }"></div>
            <div v-else class="service-cover placeholder">服</div>

            <div class="service-hero-info">
              <span class="badge badge-info">{{ categoryStore.getCategoryName(service.category_id_wsh) }}</span>
              <h1>{{ service.name_wsh }}</h1>
              <div class="service-price">¥{{ money(service.price_wsh) }} <span>/ {{ service.unit_wsh || '次' }}</span></div>
              <p class="service-desc">{{ service.description_wsh || '暂无服务描述' }}</p>
              <div class="service-actions">
                <FavoriteToggleButton
                  class="btn-sm"
                  :target-id="service.id_wsh"
                  :target-type="FAVORITE_TARGET_TYPES.SERVICE"
                />
                <button class="btn btn-primary" @click="createOrder">立即预约</button>
              </div>
            </div>
          </div>
        </section>

        <section v-if="merchant" class="card">
          <h3 class="section-title">商家信息</h3>
          <div class="provider-card" @click="goToMerchant(merchant.id_wsh)">
            <div class="provider-avatar">{{ merchant.name_wsh?.charAt(0) || '商' }}</div>
            <div class="provider-info">
              <strong>{{ merchant.name_wsh }}</strong>
              <span class="text-muted">{{ merchant.address_wsh || '-' }}</span>
              <span v-if="service.merchant_rating_wsh" class="text-muted rating-summary">
                ★ {{ Number(service.merchant_rating_wsh).toFixed(1) }}（{{ service.merchant_rating_count_wsh || 0 }} 条）
              </span>
            </div>
            <span class="link-arrow">&rsaquo;</span>
          </div>

          <div v-if="keepers.length > 0" class="keepers-sub">
            <h4>看护员（{{ keepers.length }}）</h4>
            <div class="keeper-mini-list">
              <div v-for="k in keepers" :key="k.id_wsh" class="keeper-mini-item" @click="goToKeeper(k.id_wsh)">
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
          <button class="btn btn-primary btn-block" @click="createOrder">立即预约</button>
          <button v-if="merchant" class="btn btn-outline btn-block" @click="goToMerchant(merchant.id_wsh)">
            查看商家
          </button>
          <button class="btn btn-outline btn-block" @click="goBack">返回列表</button>
        </div>
      </aside>
    </div>

    <EmptyState v-else title="服务不存在" description="找不到该服务信息">
      <button class="btn btn-primary" @click="goBack">返回首页</button>
    </EmptyState>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getService } from '@/api/service'
import { getRatings } from '@/api/rating'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useCategoryStore } from '@/stores/category'
import * as merchantService from '@/services/merchantService'
import * as keeperService from '@/services/keeperService'
import PageHero from '@/components/common/PageHero.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FavoriteToggleButton from '@/components/common/FavoriteToggleButton.vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const categoryStore = useCategoryStore()

const service = ref(null)
const ratings = ref([])
const merchantRatings = ref([])
const merchantRatingsLoading = ref(false)
const merchantRatingsError = ref(false)
const ratingsError = ref(false)
const merchant = ref(null)
const keepers = ref([])
const loading = ref(true)

const firstImage = computed(() => {
  if (!service.value?.images_wsh) return ''
  return service.value.images_wsh.split(',')[0].trim()
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

async function loadDetail() {
  loading.value = true
  try {
    const [svcRes, ratingRes] = await Promise.all([
      getService(route.params.id),
      getRatings({ targetId: route.params.id, targetType: 'service' }),
    ])
    if (svcRes.code === 200) {
      service.value = svcRes.data
      if (svcRes.data.merchant_id_wsh) {
        loadProviderInfo(svcRes.data.merchant_id_wsh)
        loadMerchantRatings(svcRes.data.merchant_id_wsh)
      }
    }
    if (ratingRes.code === 200) {
      ratings.value = ratingRes.data || []
    } else {
      ratingsError.value = true
    }
  } catch (error) {
    appStore.addToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadMerchantRatings(merchantId) {
  merchantRatingsLoading.value = true
  merchantRatingsError.value = false
  try {
    const res = await getRatings({ targetId: merchantId, targetType: 'merchant' })
    if (res.code === 200) {
      merchantRatings.value = res.data || []
    } else {
      merchantRatingsError.value = true
    }
  } catch {
    merchantRatingsError.value = true
  } finally {
    merchantRatingsLoading.value = false
  }
}

async function loadProviderInfo(merchantId) {
  try {
    const [m, k] = await Promise.all([
      merchantService.getById(merchantId),
      keeperService.getByMerchant(merchantId),
    ])
    merchant.value = m
    keepers.value = k || []
  } catch {
    merchant.value = null
    keepers.value = []
  }
}

async function createOrder() {
  if (!service.value) return
  const query = {
    create: 'true',
    serviceId: service.value.id_wsh,
  }

  if (!authStore.isLoggedIn) {
    appStore.loginRedirectPath = `/orders?${new URLSearchParams({
      create: 'true',
      serviceId: String(service.value.id_wsh),
    }).toString()}`
    appStore.showLoginPrompt = true
    return
  }

  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!ok) return

  router.push({ path: '/orders', query })
}

function goToMerchant(merchantId) {
  router.push(`/merchants/${merchantId}`)
}

function goToKeeper(keeperId) {
  router.push(`/keepers/${keeperId}`)
}

function goBack() {
  router.push('/dashboard')
}

onMounted(() => {
  loadDetail()
  categoryStore.loadCategories()
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

.service-cover {
  width: 360px;
  min-height: 240px;
  background-size: cover;
  background-position: center;
  background-color: var(--color-muted);
  border-radius: 12px;
  flex-shrink: 0;
}

.service-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  color: var(--color-primary);
  font-weight: 700;
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

  .service-cover {
    width: 100%;
    min-height: 200px;
  }

  .review-date {
    margin-left: 0;
  }
}
</style>
