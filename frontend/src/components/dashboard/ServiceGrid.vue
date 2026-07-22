<template>
  <div v-if="loading" class="catalog-grid" aria-label="服务加载中">
    <article v-for="index in 6" :key="index" class="service-card service-skeleton" aria-hidden="true">
      <div class="skeleton-media" />
      <div class="skeleton-body">
        <span />
        <span />
        <span />
      </div>
    </article>
  </div>

  <div v-else-if="normalizedServices.length" class="catalog-grid">
    <article v-for="service in normalizedServices" :key="service.id_wsh" class="service-card">
      <button
        class="service-media-button"
        type="button"
        :aria-label="`查看${service.name_wsh}详情`"
        @click="goDetail(service.id_wsh)"
      >
        <MediaWithFallback
          :src="service.firstImage"
          :alt="`${service.name_wsh}服务图片`"
          :placeholder="service.name_wsh"
        />
      </button>

      <div class="service-card-body">
        <div class="service-heading">
          <div>
            <span class="service-category">{{ categoryLabel(service) }}</span>
            <h3>{{ service.name_wsh }}</h3>
          </div>
          <div class="service-price">
            <strong>¥{{ formatMoney(service.price_wsh) }}</strong>
            <span>/ {{ service.unit_wsh || '次' }}</span>
          </div>
        </div>

        <p>{{ service.description_wsh || '查看服务详情并选择适合爱宠的照护方案。' }}</p>

        <div class="service-actions">
          <button class="detail-link" type="button" @click="goDetail(service.id_wsh)">
            查看详情
            <el-icon aria-hidden="true"><ArrowRight /></el-icon>
          </button>
          <div class="service-actions-end">
            <FavoriteToggleButton
              v-if="authStore.isLoggedIn"
              class="btn-sm"
              :target-id="service.id_wsh"
              :target-type="FAVORITE_TARGET_TYPES.SERVICE"
              :show-text="false"
              :aria-label="`收藏${service.name_wsh}`"
            />
            <button class="btn btn-primary btn-sm" type="button" @click="createOrder(service)">
              <el-icon aria-hidden="true"><Calendar /></el-icon>
              立即预约
            </button>
          </div>
        </div>
      </div>
    </article>
  </div>

  <div v-else class="empty-services">
    <el-icon aria-hidden="true"><Service /></el-icon>
    <h3>没有找到匹配的服务</h3>
    <p>调整关键词或服务分类后再试试。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Calendar, Service } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useCategoryStore } from '@/stores/category'
import FavoriteToggleButton from '@/components/common/FavoriteToggleButton.vue'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'

const props = defineProps({
  services: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const categoryStore = useCategoryStore()

const normalizedServices = computed(() => props.services.map(service => ({
  ...service,
  firstImage: String(service.images_wsh || '').split(',').map(url => url.trim()).find(Boolean) || '',
})))

function categoryLabel(service) {
  return service.category_name_wsh || categoryStore.getCategoryName(service.category_id_wsh) || '宠物服务'
}

function formatMoney(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '--'
  return amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function goDetail(serviceId) {
  router.push(`/services/${serviceId}`)
}

async function createOrder(service) {
  const query = {
    create: 'true',
    serviceId: service.id_wsh,
    serviceName: service.name_wsh,
    price: service.price_wsh,
  }

  if (!authStore.isLoggedIn) {
    appStore.loginRedirectPath = `/orders?${new URLSearchParams(query).toString()}`
    appStore.showLoginPrompt = true
    return
  }

  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!ok) return
  router.push({ path: '/orders', query })
}
</script>

<style scoped>
.catalog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.service-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  transition: border-color 180ms ease-out, box-shadow 180ms ease-out, transform 180ms ease-out;
}

.service-card:hover {
  border-color: color-mix(in srgb, var(--color-primary) 54%, var(--color-border));
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.service-media-button {
  width: 100%;
  aspect-ratio: 16 / 10;
  padding: 0;
  overflow: hidden;
  background: var(--color-muted);
  border: 0;
}

.service-media-button :deep(.media-image) {
  transition: transform 240ms ease-out;
}

.service-media-button:hover :deep(.media-image) {
  transform: scale(1.035);
}

.service-card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 20px;
}

.service-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.service-heading > div:first-child {
  min-width: 0;
}

.service-category {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.service-heading h3 {
  margin-top: 6px;
  color: var(--color-foreground);
  font-size: 19px;
  line-height: 1.3;
  text-wrap: balance;
}

.service-price {
  flex: 0 0 auto;
  color: var(--color-primary);
  text-align: right;
}

.service-price strong,
.service-price span {
  display: block;
}

.service-price strong {
  font-size: 20px;
  line-height: 1.2;
}

.service-price span {
  margin-top: 3px;
  color: var(--color-muted-foreground);
  font-size: 12px;
}

.service-card-body > p {
  display: -webkit-box;
  min-height: 72px;
  margin: 16px 0 20px;
  overflow: hidden;
  color: var(--color-muted-foreground);
  font-size: 14px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.service-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.service-actions-end {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 6px;
  padding: 0;
  color: var(--color-foreground);
  background: transparent;
  border: 0;
  font-size: 14px;
  font-weight: 700;
}

.detail-link:hover {
  color: var(--color-primary);
}

.empty-services {
  display: grid;
  min-height: 260px;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: 32px;
  color: var(--color-muted-foreground);
  background: var(--color-muted);
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  text-align: center;
}

.empty-services .el-icon {
  color: var(--color-primary);
  font-size: 36px;
}

.empty-services h3 {
  color: var(--color-foreground);
  font-size: 18px;
}

.service-skeleton {
  min-height: 430px;
}

.skeleton-media,
.skeleton-body span {
  background: linear-gradient(90deg, var(--color-muted) 25%, var(--color-card) 50%, var(--color-muted) 75%);
  background-size: 200% 100%;
  animation: catalog-shimmer 1.3s linear infinite;
}

.skeleton-media {
  aspect-ratio: 16 / 10;
}

.skeleton-body {
  display: grid;
  gap: 14px;
  padding: 20px;
}

.skeleton-body span {
  height: 14px;
  border-radius: 4px;
}

.skeleton-body span:nth-child(1) { width: 58%; }
.skeleton-body span:nth-child(3) { width: 76%; }

@keyframes catalog-shimmer {
  to { background-position: -200% 0; }
}

@media (max-width: 980px) {
  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .catalog-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .service-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .service-actions-end,
  .service-actions-end .btn {
    width: 100%;
  }

  .service-actions-end :deep(.favorite-toggle) {
    width: 44px;
    flex: 0 0 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .service-card,
  .service-media-button :deep(.media-image) {
    transition: none;
  }

  .service-skeleton .skeleton-media,
  .service-skeleton .skeleton-body span {
    animation: none;
  }
}
</style>
