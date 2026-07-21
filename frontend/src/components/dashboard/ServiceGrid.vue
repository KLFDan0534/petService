<template>
  <div v-if="featured.length" class="pricing-grid">
    <div
      v-for="s in featured"
      :key="s.id_wsh"
      class="pricing-card featured"
      @click="goDetail(s.id_wsh)"
    >
      <div v-if="s.firstImage" class="pricing-card-img" :style="{ backgroundImage: `url(${s.firstImage})` }"></div>
      <div class="price">¥{{ s.price_wsh }} <span>/ {{ s.unit_wsh }}</span></div>
      <h3>{{ s.name_wsh }}</h3>
      <p class="service-desc">{{ s.description_wsh }}</p>
      <div class="service-tags">
        <span class="badge badge-info">{{ categoryStore.getCategoryName(s.category_id_wsh) }}</span>
      </div>
      <div class="service-card-footer featured-footer">
        <div class="service-actions">
          <FavoriteToggleButton
            class="btn-sm"
            :target-id="s.id_wsh"
            :target-type="FAVORITE_TARGET_TYPES.SERVICE"
            :show-text="false"
            aria-label="收藏服务"
          />
          <button class="btn btn-primary btn-sm" @click.stop="createOrder(s.id_wsh, s.name_wsh, s.price_wsh)">立即预约</button>
        </div>
      </div>
    </div>
  </div>

  <h2 class="section-title">全部服务</h2>
  <div v-if="withImages.length === 0" class="empty-state">
    <div class="icon">服</div>
    <h3>暂无服务</h3>
    <p>敬请期待</p>
  </div>
  <div v-else class="service-grid">
    <div
      v-for="s in withImages"
      :key="s.id_wsh"
      class="service-card"
      @click="goDetail(s.id_wsh)"
    >
      <div v-if="s.firstImage" class="service-card-img" :style="{ backgroundImage: `url(${s.firstImage})` }"></div>
      <div v-else class="service-card-img icon-placeholder">服</div>
      <div class="service-card-body">
        <h3>{{ s.name_wsh }}</h3>
        <p>{{ s.description_wsh }}</p>
        <div class="service-tags">
          <span class="badge badge-info">{{ categoryStore.getCategoryName(s.category_id_wsh) }}</span>
        </div>
        <div class="service-card-footer">
          <span class="service-price">¥{{ s.price_wsh }} <span>/ {{ s.unit_wsh }}</span></span>
          <div class="service-actions">
            <FavoriteToggleButton
              class="btn-sm"
              :target-id="s.id_wsh"
              :target-type="FAVORITE_TARGET_TYPES.SERVICE"
              :show-text="false"
              aria-label="收藏服务"
            />
            <button class="btn btn-sm btn-primary" @click.stop="createOrder(s.id_wsh, s.name_wsh, s.price_wsh)">预约</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useCategoryStore } from '@/stores/category'
import FavoriteToggleButton from '@/components/common/FavoriteToggleButton.vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'

const props = defineProps({
  services: { type: Array, default: () => [] },
})

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const categoryStore = useCategoryStore()

const withImages = computed(() => props.services.map(service => ({
  ...service,
  firstImage: service.images_wsh ? service.images_wsh.split(',')[0].trim() : '',
})))

const featured = computed(() => withImages.value.slice(0, 3))

function goDetail(serviceId) {
  router.push(`/services/${serviceId}`)
}

async function createOrder(serviceId, serviceName, price) {
  const query = { create: 'true', serviceId, serviceName, price }
  if (authStore.isLoggedIn) {
    const ok = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
    if (!ok) return
  }
  router.push({ path: '/orders', query })
}
</script>

<style scoped>
.section-title {
  margin: 8px 0 16px;
  font-size: 22px;
  line-height: 1.3;
}

.pricing-grid {
  margin: 0;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.pricing-card,
.service-card {
  border: 1px solid var(--color-border);
  overflow: hidden;
  text-align: left;
}

.pricing-card {
  padding: 0;
}

.pricing-card.featured {
  transform: none;
}

.pricing-card.featured:hover {
  transform: translateY(-1px);
}

.pricing-card-img {
  width: 100%;
  aspect-ratio: 16 / 9;
  height: auto;
  background-size: cover;
  background-position: center;
  border-radius: 0;
  margin-bottom: 0;
}

.pricing-card .price,
.pricing-card h3,
.pricing-card .service-desc,
.pricing-card .service-tags,
.pricing-card .service-card-footer {
  margin-left: 18px;
  margin-right: 18px;
}

.pricing-card .price {
  margin-top: 16px;
  font-size: 26px;
}

.pricing-card h3 {
  margin-top: 8px;
  font-size: 18px;
  color: var(--color-foreground);
}

.service-desc {
  margin: 12px 0;
  color: var(--color-muted-foreground);
  font-size: 14px;
  line-height: 1.6;
}

.service-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 8px 0 12px;
}

.service-card-img {
  width: 100%;
  aspect-ratio: 16 / 10;
  height: auto;
  background-size: cover;
  background-position: center;
  border-radius: 0;
}

.icon-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: var(--color-muted);
}

.service-card-body {
  padding: 16px;
}

.service-card-body h3 {
  margin-bottom: 8px;
  font-size: 17px;
  color: var(--color-foreground);
}

.service-card-body p {
  font-size: 13px;
  color: var(--color-muted-foreground);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.service-card-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.featured-footer {
  justify-content: flex-end;
  margin-bottom: 18px;
}

.service-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.service-price {
  font-weight: 700;
  font-size: 18px;
  color: var(--color-primary);
}

.service-price span {
  font-size: 13px;
  font-weight: 400;
}

@media (max-width: 768px) {
  .pricing-grid,
  .service-grid {
    grid-template-columns: 1fr;
  }
}
</style>
