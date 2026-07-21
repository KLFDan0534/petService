<template>
  <div class="dashboard-page">
    <div v-if="banners.length" class="page-hero banner-hero">
      <div
        class="banner-track"
        :class="{ 'no-transition': !transitioning }"
        :style="{ transform: `translateX(-${activeSlide * 100}%)` }"
        @click="goBannerLink"
      >
        <div v-for="(s, i) in slides" :key="i" class="banner-slide">
          <div v-if="s.type === 'welcome'" class="slide-welcome">
            <h1>首页</h1>
            <p>{{ s.text }}</p>
          </div>
          <template v-else>
            <img :src="s.image_url_wsh" :alt="s.title_wsh">
            <div class="banner-label">{{ s.title_wsh }}</div>
          </template>
        </div>
      </div>
            <button v-if="slides.length > 1" class="banner-prev" aria-label="上一张轮播图" @click.stop="prevSlide">&lsaquo;</button>
            <button v-if="slides.length > 1" class="banner-next" aria-label="下一张轮播图" @click.stop="nextSlide">&rsaquo;</button>
      <div class="banner-dots">
        <button
          v-for="(s, i) in slides"
          :key="i"
          :class="{ active: activeSlide === i }"
          :aria-label="`切换到第 ${i + 1} 张`"
          @click.stop="goSlide(i)"
        ></button>
      </div>
    </div>
    <PageHero v-else title="首页" subtitle="选择适合您爱宠的服务" />
    <p class="welcome-line">{{ authStore.isLoggedIn ? '欢迎回来' : '欢迎来到宠物寄养平台' }}</p>

    <div v-if="loading" class="loading">
      <LoadingSpinner />
    </div>

    <template v-else>
<!--      <div v-if="authStore.isLoggedIn" class="stat-grid dashboard-stats">
        <div class="stat-card">
          <div class="stat-value">{{ stats.pets_wsh || 0 }}</div>
          <div class="stat-label">我的宠物</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.active_orders_wsh || 0 }}</div>
          <div class="stat-label">进行中订单</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.completed_orders_wsh || 0 }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.total_spent_wsh || 0 }}</div>
          <div class="stat-label">总消费</div>
        </div>
      </div>-->

      <div v-if="featured.length" class="pricing-grid">
        <div
          v-for="s in featured"
          :key="s.id_wsh"
          class="pricing-card featured"
          role="button"
          tabindex="0"
          @click="goDetail(s.id_wsh)"
          @keydown.enter.prevent="goDetail(s.id_wsh)"
          @keydown.space.prevent="goDetail(s.id_wsh)"
        >
          <div v-if="s.firstImage" class="pricing-card-img" :style="{ backgroundImage: `url(${s.firstImage})` }"></div>
          <div class="price">¥{{ s.price_wsh }} <span>/ {{ s.unit_wsh }}</span></div>
          <h3>{{ s.name_wsh }}</h3>
          <p class="service-desc">{{ s.description_wsh }}</p>
          <div class="service-tags">
            <span class="badge badge-info">{{ typeLabels[s.type_wsh] || s.type_wsh || '服务' }}</span>
          </div>
          <button class="btn btn-primary" @click.stop="createOrder(s.id_wsh, s.name_wsh, s.price_wsh)">立即预约</button>
        </div>
      </div>

      <h2 class="section-title">全部服务</h2>
      <div v-if="withImages.length === 0" class="empty-state">
        <div class="icon">宠</div>
        <h3>暂无服务</h3>
        <p>敬请期待</p>
      </div>
      <div v-else class="service-grid">
        <div
          v-for="s in withImages"
          :key="s.id_wsh"
          class="service-card"
          role="button"
          tabindex="0"
          @click="goDetail(s.id_wsh)"
          @keydown.enter.prevent="goDetail(s.id_wsh)"
          @keydown.space.prevent="goDetail(s.id_wsh)"
        >
          <div v-if="s.firstImage" class="service-card-img" :style="{ backgroundImage: `url(${s.firstImage})` }"></div>
          <div v-else class="service-card-img icon-placeholder">宠</div>
          <div class="service-card-body">
            <h3>{{ s.name_wsh }}</h3>
            <p>{{ s.description_wsh }}</p>
            <div class="service-tags">
              <span class="badge badge-info">{{ typeLabels[s.type_wsh] || s.type_wsh || '服务' }}</span>
            </div>
            <div class="service-card-footer">
              <span class="service-price">¥{{ s.price_wsh }} <span>/ {{ s.unit_wsh }}</span></span>
              <button class="btn btn-sm btn-primary" @click.stop="createOrder(s.id_wsh, s.name_wsh, s.price_wsh)">预约</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'
import PageHero from '@/components/common/PageHero.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const stats = ref({})
const services = ref([])
const loading = ref(true)
const banners = ref([])
const activeSlide = ref(0)
const transitioning = ref(true)
let bannerTimer = null

const slides = computed(() => {
  const list = [{ type: 'welcome', text: '欢迎来到宠物寄养平台' }]
  banners.value.forEach(b => list.push({ type: 'banner', ...b }))
  return list
})

function changeSlide(index) {
  index = (index + slides.value.length) % slides.value.length
  const wrap = Math.abs(index - activeSlide.value) > 1
  if (wrap) {
    transitioning.value = false
    activeSlide.value = index
    requestAnimationFrame(() => {
      requestAnimationFrame(() => { transitioning.value = true })
    })
  } else {
    activeSlide.value = index
  }
}

const typeLabels = {
  boarding: '寄养',
  grooming: '美容',
  training: '训练',
  walk: '遛弯',
  medical: '医疗',
}

const withImages = computed(() => services.value.map(service => ({
  ...service,
  firstImage: service.images_wsh ? service.images_wsh.split(',')[0].trim() : '',
})))

const featured = computed(() => withImages.value.slice(0, 3))

function normalizeServices(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

async function loadBanners() {
  try {
    const r = await request.get('/api/notices/active', { params: { type: 'banner' } })
    if (r.data.code === 200) {
      if (bannerTimer) { clearInterval(bannerTimer); bannerTimer = null }
      banners.value = r.data.data || []
      if (banners.value.length) {
        bannerTimer = setInterval(() => changeSlide(activeSlide.value + 1), 6000)
      }
    }
  } catch (e) {}
}

async function loadDashboard() {
  loading.value = true
  try {
    const statsRequest = authStore.isLoggedIn
      ? request.get('/statistics/user')
      : Promise.resolve({ data: { code: 200, data: {} } })
    const serviceRequest = request.get('/services')

    const [statsResponse, servicesResponse] = await Promise.all([statsRequest, serviceRequest])
    if (statsResponse.data.code === 200) stats.value = statsResponse.data.data || {}
    if (servicesResponse.data.code === 200) services.value = normalizeServices(servicesResponse.data.data)
  } catch (e) {
    services.value = []
    appStore.addToast('加载首页数据失败', 'error')
  } finally {
    loading.value = false
  }
  loadBanners()
}

function goBannerLink() {
  const s = slides.value[activeSlide.value]
  if (s?.type === 'banner' && s.link_url_wsh) window.open(s.link_url_wsh, '_blank')
}

function prevSlide() { changeSlide(activeSlide.value - 1) }

function nextSlide() { changeSlide(activeSlide.value + 1) }

function goSlide(i) { changeSlide(i) }

function goDetail(serviceId) {
  router.push(`/services/${serviceId}`)
}

function createOrder(serviceId, serviceName, price) {
  router.push({ path: '/orders', query: { create: 'true', serviceId, serviceName, price } })
}

onMounted(() => {
  loadDashboard()
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

function onVisibilityChange() {
  if (document.visibilityState === 'visible') loadBanners()
}
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
}

.banner-hero {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  padding: 0;
  height: 200px;
}

.banner-track {
  display: flex;
  height: 100%;
  transition: transform 0.35s ease;
}

.banner-track.no-transition {
  transition: none;
}

.banner-slide {
  min-width: 100%;
  height: 100%;
  position: relative;
}

.banner-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.slide-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  color: var(--color-on-primary);
}

.slide-welcome h1 {
  font-size: 28px;
  margin-bottom: 4px;
}

.slide-welcome p {
  color: var(--color-on-primary);
  opacity: 0.72;
}

.banner-label {
  position: absolute;
  right: 16px;
  bottom: 40px;
  max-width: min(420px, calc(100% - 32px));
  background: rgba(15, 23, 42, 0.68);
  color: #fff;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 700;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255,255,255,0.16);
}

.banner-prev,
.banner-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(15, 23, 42, 0.42);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.18);
  font-size: 24px;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  cursor: pointer;
  line-height: 34px;
  text-align: center;
  z-index: 2;
}

.banner-prev { left: 12px; }
.banner-next { right: 12px; }

.banner-prev:hover,
.banner-next:hover {
  background: rgba(15, 23, 42, 0.72);
}

.banner-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.banner-dots button {
  width: 8px;
  height: 8px;
  min-height: 8px;
  padding: 0;
  border-radius: var(--radius-full);
  background: rgba(255,255,255,0.55);
  cursor: pointer;
}

.banner-dots button.active {
  width: 18px;
  background: #fff;
}

.dashboard-stats {
  margin-bottom: 28px;
}

.welcome-line {
  margin: -18px 0 24px;
  text-align: center;
  color: var(--color-muted-foreground);
}

.section-title {
  margin: 32px 0 16px;
  font-size: 20px;
  color: var(--color-foreground);
}

.pricing-card,
.service-card {
  cursor: pointer;
}

.pricing-card:focus-visible,
.service-card:focus-visible {
  outline: 3px solid var(--color-ring);
  outline-offset: 3px;
}

.pricing-card-img {
  width: 100%;
  height: 180px;
  background-size: cover;
  background-position: center;
  border-radius: var(--radius-md);
  margin-bottom: 14px;
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
  justify-content: center;
  margin: 8px 0 12px;
}

.service-card-img {
  width: 100%;
  height: 160px;
  background-size: cover;
  background-position: center;
  border-radius: var(--radius-md);
}

.icon-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
  background: var(--color-muted);
  color: var(--color-primary);
}

.service-card-body {
  padding: 14px 2px 0;
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
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.service-price {
  font-weight: 700;
  font-size: 18px;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.service-price span {
  font-size: 13px;
  font-weight: 400;
  color: var(--color-muted-foreground);
}

@media (max-width: 768px) {
  .banner-hero {
    height: 180px;
  }

  .pricing-grid,
  .service-grid {
    grid-template-columns: 1fr;
  }

  .service-card-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
