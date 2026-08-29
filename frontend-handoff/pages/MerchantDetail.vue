<template>
  <div class="merchant-detail-page">
    <LoadingSpinner v-if="loading" text="加载商家信息..." />

    <div v-else-if="merchant" class="detail-grid">
      <main class="detail-main">
        <section class="panel">
          <div class="merchant-head">
            <div class="merchant-avatar">{{ merchant.name_wsh?.charAt(0) || '商' }}</div>
            <div class="merchant-title">
              <h1>{{ merchant.name_wsh }}</h1>
              <div class="badge-row">
                <span :class="['badge', statusBadge]">{{ statusLabel }}</span>
                <span :class="['badge', getStatusBadge(MerchantStoreMode, merchant.store_mode_wsh)]">
                  {{ getStatusLabel(MerchantStoreMode, merchant.store_mode_wsh) }}
                </span>
                <span :class="['badge', getStatusBadge(MerchantStoreStatus, merchant.store_status_wsh)]">
                  {{ getStatusLabel(MerchantStoreStatus, merchant.store_status_wsh) }}
                </span>
                <span class="rating-star">★ {{ merchant.rating_wsh ?? '-' }}</span>
              </div>
            </div>
            <div class="head-actions">
              <button
                class="btn btn-sm"
                :class="isFavorited ? 'btn-primary' : 'btn-outline'"
                :disabled="favoriteLoading || favoriteToggling"
                @click="handleToggleFavorite"
              >
                <el-icon><Star /></el-icon>
                <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
              </button>
              <button class="btn btn-primary" @click="goToOwnerHome">联系商家</button>
            </div>
          </div>

          <dl class="info-grid">
            <div>
              <dt>店主</dt>
              <dd class="owner-cell">
                <img v-if="merchant.owner_avatar_wsh" :src="merchant.owner_avatar_wsh" class="owner-avatar" alt="店主头像">
                <span v-else class="owner-avatar owner-avatar-fallback">{{ (merchant.owner_name_wsh || '店').charAt(0) }}</span>
                <button v-if="ownerHomeId" class="link-btn" @click="goToOwnerHome">{{ merchant.owner_name_wsh || '-' }}</button>
                <span v-else>{{ merchant.owner_name_wsh || '-' }}</span>
              </dd>
            </div>
            <div>
              <dt>联系电话</dt>
              <dd>{{ merchant.phone_wsh || '-' }}</dd>
            </div>
            <div>
              <dt>地址</dt>
              <dd>{{ merchant.address_wsh || '-' }}</dd>
            </div>
            <div v-if="merchant.created_at_wsh">
              <dt>入驻时间</dt>
              <dd>{{ formatDate(merchant.created_at_wsh) }}</dd>
            </div>
            <div>
              <dt>评分</dt>
              <dd>{{ merchant.rating_wsh ?? '-' }}</dd>
            </div>
          </dl>

          <div v-if="merchant.description_wsh" class="desc-block">
            <h3>商家简介</h3>
            <p>{{ merchant.description_wsh }}</p>
          </div>

          <div v-if="qualifications.length > 0" class="desc-block">
            <h3>资质证明</h3>
            <div class="qual-grid">
              <div v-for="q in qualifications" :key="q.id_wsh" class="qual-item">
                <div v-if="qualificationUrls(q.file_url_wsh).length" class="qual-images">
                  <img
                    v-for="url in qualificationUrls(q.file_url_wsh)"
                    :key="url"
                    :src="url"
                    :alt="q.title_wsh"
                  >
                </div>
                <span>{{ q.title_wsh }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <h3 class="section-title">营业时间</h3>
          <div v-if="hours.length > 0" class="hours-list">
            <div v-for="h in hours" :key="h.day_of_week_wsh" class="hour-row">
              <span class="hour-day">{{ dayLabels[h.day_of_week_wsh] || `周${h.day_of_week_wsh}` }}</span>
              <span v-if="Number(h.is_closed_wsh) === 1" class="hour-closed">休息</span>
              <span v-else class="hour-time">{{ formatTime(h.open_time_wsh) }} - {{ formatTime(h.close_time_wsh) }}</span>
            </div>
          </div>
          <p v-else class="text-muted">暂无营业时间</p>
        </section>

        <section class="panel">
          <h3 class="section-title">服务项目 ({{ services.length }})</h3>
          <div v-if="services.length > 0" class="service-list">
            <div v-for="svc in services" :key="svc.id_wsh" class="service-item">
              <div class="service-info">
                <img v-if="firstImage(svc.images_wsh)" :src="firstImage(svc.images_wsh)" class="service-img" :alt="svc.name_wsh">
                <div class="service-body">
                  <strong>{{ svc.name_wsh }}</strong>
                  <span class="text-muted">{{ svc.category_name_wsh || svc.type_wsh }}</span>
                  <p v-if="svc.description_wsh" class="text-muted">{{ svc.description_wsh }}</p>
                </div>
              </div>
              <div class="service-action">
                <span class="price">&yen;{{ money(svc.price_wsh) }}<small>/{{ unitLabel(svc.unit_wsh) || '次' }}</small></span>
                <button class="btn btn-sm btn-primary" :disabled="bookDisabled" @click="bookService(svc)">
                  {{ isOpen ? '预约' : '休息中·可预约' }}
                </button>
              </div>
            </div>
          </div>
          <EmptyState v-else title="暂无服务" description="该商家还没有上架服务项目。" />
        </section>

        <section class="panel">
          <h3 class="section-title">用户评价 ({{ ratings.length }})</h3>
          <div v-if="ratings.length > 0" class="rating-list">
            <div v-for="r in ratings" :key="r.id_wsh" class="rating-item">
              <div class="rating-header">
                <strong>用户 #{{ r.user_id_wsh }}</strong>
                <span class="stars">{{ renderStars(r.score_wsh) }}</span>
                <span class="text-muted">{{ formatDate(r.created_at_wsh) }}</span>
              </div>
              <p v-if="r.content_wsh">{{ r.content_wsh }}</p>
              <div v-if="r.reply_wsh" class="rating-reply">
                <span class="text-muted">商家回复：</span>{{ r.reply_wsh }}
              </div>
            </div>
          </div>
          <EmptyState v-else title="暂无评价" description="还没有用户留下评价。" />
        </section>
      </main>

      <aside class="detail-sidebar">
        <section class="panel">
          <h4>快捷操作</h4>
          <button class="btn btn-primary btn-block" @click="goToOwnerHome">联系商家</button>
          <button class="btn btn-outline btn-block" @click="goBack">返回列表</button>
        </section>
      </aside>
    </div>

    <EmptyState v-else title="商家不存在" description="没有找到该商家的信息。">
      <button class="btn btn-primary" @click="goBack">返回列表</button>
    </EmptyState>
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
.merchant-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
}
.detail-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  padding: 20px;
}
.merchant-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}
.merchant-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
}
.merchant-title {
  flex: 1;
  min-width: 0;
}
.merchant-title h1 {
  margin: 0;
  font-size: 22px;
}
.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  align-items: center;
}
.rating-star {
  color: #f59e0b;
  font-weight: 600;
}
.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin: 0;
}
.info-grid dt {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.info-grid dd {
  margin: 4px 0 0;
}
.owner-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.owner-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.owner-avatar-fallback {
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.link-btn {
  background: none;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  padding: 0;
  font-size: inherit;
  text-decoration: underline;
}
.desc-block {
  margin-top: 16px;
}
.desc-block h3,
.section-title {
  margin: 0 0 8px;
}
.desc-block p {
  margin: 0;
  color: var(--color-muted-foreground);
  line-height: 1.6;
}
.qual-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.qual-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}
.qual-images {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(72px, 1fr));
  gap: 6px;
  width: 100%;
  max-width: 220px;
}
.qual-images img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
}
.qual-item span {
  font-size: 12px;
  color: var(--color-muted-foreground);
}
.hours-list {
  display: grid;
  gap: 8px;
}
.hour-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}
.hour-day {
  font-weight: 600;
}
.hour-closed {
  color: var(--color-danger);
}
.hour-time,
.text-muted {
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.service-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.service-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
}
.service-info {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex: 1;
  min-width: 0;
}
.service-img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.service-body {
  min-width: 0;
}
.service-body strong,
.service-body span {
  display: block;
}
.service-body span {
  font-size: 12px;
}
.service-body p {
  margin: 4px 0 0;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.service-action {
  text-align: right;
  flex-shrink: 0;
}
.price {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-destructive);
  margin-bottom: 8px;
}
.price small {
  font-size: 12px;
  font-weight: 400;
}
.rating-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.rating-item {
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
}
.rating-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.stars {
  color: #f59e0b;
}
.rating-reply {
  margin-top: 6px;
  padding: 8px;
  background: var(--color-muted);
  border-radius: 6px;
  font-size: 13px;
}
.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.btn-block {
  width: 100%;
}
@media (max-width: 760px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .merchant-head {
    align-items: flex-start;
    flex-direction: column;
  }
  .head-actions {
    justify-content: flex-start;
  }
  .service-item {
    align-items: stretch;
    flex-direction: column;
  }
  .service-action {
    text-align: left;
  }
}
</style>
