<template>
  <div>
    <div v-if="showNoticeDialog && currentNotice" class="notice-modal-overlay">
      <div class="notice-modal" role="dialog" aria-modal="true" aria-labelledby="notice-dialog-title">
        <div class="notice-modal-header">
          <span class="notice-badge">公告</span>
          <span class="notice-date">{{ formatDate(currentNotice.created_at_wsh) }}</span>
        </div>
        <h2 id="notice-dialog-title">{{ currentNotice.title_wsh }}</h2>
        <p>{{ currentNotice.content_wsh }}</p>
        <button class="btn btn-primary notice-confirm" @click="acknowledgeNotice">我知道了</button>
      </div>
    </div>

    <div v-if="banners.length" class="page-hero banner-hero">
      <div class="banner-track" :class="{ 'no-transition': !transitioning }" :style="{ transform: `translateX(-${activeSlide * 100}%)` }" @click="goBannerLink">
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
      <button v-if="slides.length > 1" class="banner-prev" @click.stop="prevSlide">‹</button>
      <button v-if="slides.length > 1" class="banner-next" @click.stop="nextSlide">›</button>
      <div class="banner-dots">
        <span v-for="(s, i) in slides" :key="i" :class="{ active: activeSlide === i }" @click.stop="goSlide(i)"></span>
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
        <div v-for="s in featured" :key="s.id_wsh" class="pricing-card featured" @click="goDetail(s.id_wsh)" style="cursor:pointer">
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
        <div v-for="s in withImages" :key="s.id_wsh" class="service-card" @click="goDetail(s.id_wsh)" style="cursor:pointer">
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
const noticeQueue = ref([])
const currentNotice = ref(null)
const showNoticeDialog = ref(false)
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

async function loadUnreadNotices() {
  try {
    const response = authStore.isLoggedIn
      ? await request.get('/api/notices/unread')
      : await request.get('/api/notices/active', { params: { type: 'notice' } })

    if (response.data.code !== 200) return
    let notices = response.data.data || []
    if (!authStore.isLoggedIn) {
      notices = notices.filter(n => !localStorage.getItem(publicNoticeKey(n.id_wsh)))
    }
    noticeQueue.value = notices
    showNextNotice()
  } catch (e) {}
}

function showNextNotice() {
  currentNotice.value = noticeQueue.value.shift() || null
  showNoticeDialog.value = !!currentNotice.value
}

function publicNoticeKey(id) {
  return `notice_read_public_${id}`
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString()
}

async function acknowledgeNotice() {
  const notice = currentNotice.value
  if (!notice) return
  try {
    if (authStore.isLoggedIn) {
      await request.post(`/api/notices/${notice.id_wsh}/read`, {})
    } else {
      localStorage.setItem(publicNoticeKey(notice.id_wsh), '1')
    }
  } catch (e) {
    if (!authStore.isLoggedIn) localStorage.setItem(publicNoticeKey(notice.id_wsh), '1')
  } finally {
    showNextNotice()
  }
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
  loadUnreadNotices()
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
.banner-hero {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  padding: 0;
  height: 200px;
}

.notice-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.45);
}

.notice-modal {
  width: min(440px, 100%);
  border-radius: 12px;
  background: var(--color-card);
  color: var(--color-card-foreground);
  padding: 24px;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24);
}

.notice-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.notice-badge {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.notice-date {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.notice-modal h2 {
  margin: 0 0 12px;
  font-size: 22px;
}

.notice-modal p {
  max-height: 45vh;
  overflow: auto;
  margin: 0;
  color: var(--color-muted-foreground);
  line-height: 1.7;
  white-space: pre-wrap;
}

.notice-confirm {
  width: 100%;
  margin-top: 20px;
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
  background: var(--color-primary);
  color: #fff;
}

.slide-welcome h1 {
  font-size: 28px;
  margin-bottom: 4px;
}

.slide-welcome p {
  color: var(--color-muted-foreground);
}

.banner-label {
  position: absolute;
  right: 16px;
  bottom: 40px;
  background: linear-gradient(135deg, rgba(0,0,0,0.6), rgba(0,0,0,0.3));
  color: #fff;
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255,255,255,0.15);
}

.banner-prev,
.banner-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0,0,0,0.3);
  color: #fff;
  border: none;
  font-size: 24px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  line-height: 36px;
  text-align: center;
  z-index: 2;
}

.banner-prev { left: 12px; }
.banner-next { right: 12px; }

.banner-prev:hover,
.banner-next:hover {
  background: rgba(0,0,0,0.6);
}

.banner-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.banner-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
}

.banner-dots span.active {
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
}

.pricing-card-img {
  width: 100%;
  height: 180px;
  background-size: cover;
  background-position: center;
  border-radius: 12px 12px 0 0;
  margin-bottom: 12px;
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
  height: 160px;
  background-size: cover;
  background-position: center;
  border-radius: 12px 12px 0 0;
}

.icon-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: var(--color-muted);
}

.service-card-body {
  padding: 12px 16px 16px;
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
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
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
</style>
