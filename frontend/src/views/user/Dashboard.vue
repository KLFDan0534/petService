<template>
  <div class="dashboard-page">
    <section class="landing-hero" aria-labelledby="home-hero-title">
      <MediaWithFallback
        class="hero-media"
        :src="heroImage"
        alt="宠物在专业照护环境中的服务现场"
        loading="eager"
      />
      <div class="hero-scrim" aria-hidden="true"></div>

      <div class="hero-inner">
        <div class="hero-copy">
          <div class="hero-trust">
            <el-icon aria-hidden="true"><CircleCheckFilled /></el-icon>
            <span>把每一次托付，都交给值得信赖的人</span>
          </div>
          <h1 id="home-hero-title">安心托付，快乐相伴</h1>
          <p>
            从寄养、美容到日常陪伴，给爱宠一套看得见进度、找得到伙伴的照护体验。
          </p>
          <div class="hero-actions">
            <button class="btn btn-primary hero-primary" type="button" @click="startBooking">
              <el-icon aria-hidden="true"><Calendar /></el-icon>
              立即预约
            </button>
            <button class="btn hero-secondary" type="button" @click="scrollToServices">
              浏览服务
              <el-icon aria-hidden="true"><ArrowRight /></el-icon>
            </button>
          </div>

          <dl class="hero-stats" aria-label="平台服务概览">
            <div>
              <dt>{{ serviceCountDisplay }}</dt>
              <dd>服务方案</dd>
            </div>
            <div>
              <dt>{{ providerCountDisplay }}</dt>
              <dd>服务伙伴</dd>
            </div>
            <div>
              <dt>{{ averageRatingDisplay }}</dt>
              <dd>用户评分</dd>
            </div>
          </dl>
        </div>

        <div class="hero-booking-preview" aria-label="快速预约入口">
          <div class="hero-preview-icon"><el-icon aria-hidden="true"><MagicStick /></el-icon></div>
          <div>
            <strong>今天想让爱宠享受什么？</strong>
            <span>{{ selectedService?.name_wsh || '从服务方案中选择' }}</span>
          </div>
          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

<!--    <BannerCarousel v-if="banners.length" class="home-campaigns" :banners="banners" />-->

    <section id="services" class="landing-section services-section" aria-labelledby="services-title">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Services</span>
          <h2 id="services-title">为爱宠准备的服务方案</h2>
        </div>
        <div class="section-heading-action">
          <p>按宠物的节奏选择服务，价格和单位清楚呈现，预约前再确认照护细节。</p>
          <router-link class="all-services-link" to="/services">
            查看全部 {{ services.length }} 项服务
            <el-icon aria-hidden="true"><ArrowRight /></el-icon>
          </router-link>
        </div>
      </div>

      <div v-if="loading" class="service-grid" aria-label="服务加载中">
        <article v-for="index in 6" :key="index" class="service-package skeleton-package" aria-hidden="true">
          <div class="skeleton-media"></div>
          <div class="skeleton-line skeleton-line-wide"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line skeleton-line-short"></div>
        </article>
      </div>

      <div v-else-if="services.length" class="service-grid">
        <article
          v-for="(service, index) in featuredServices"
          :key="service.id_wsh"
          class="service-package"
          :class="accentClass(index)"
        >
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
            <span class="service-icon" aria-hidden="true">
              <el-icon><component :is="serviceIcon(service)" /></el-icon>
            </span>
          </button>
          <div class="service-package-body">
            <div class="service-heading-row">
              <div>
                <span class="service-category">{{ categoryLabel(service) }}</span>
                <h3>{{ service.name_wsh }}</h3>
              </div>
              <div class="service-price">
                <strong>¥{{ formatMoney(service.price_wsh) }}</strong>
                <span>/ {{ service.unit_wsh || '次' }}</span>
              </div>
            </div>
            <p>{{ service.description_wsh || '为爱宠提供稳定、细致的日常照护。' }}</p>
            <div class="service-actions">
              <button class="text-link" type="button" @click="goDetail(service.id_wsh)">
                查看详情
                <el-icon aria-hidden="true"><ArrowRight /></el-icon>
              </button>
              <button class="btn btn-primary btn-sm" type="button" @click="createOrder(service)">
                <el-icon aria-hidden="true"><Calendar /></el-icon>
                预约
              </button>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-section">
        <el-icon aria-hidden="true"><Service /></el-icon>
        <h3>暂时没有可预约的服务</h3>
        <p>服务团队正在准备新的照护方案。</p>
      </div>
    </section>

    <section id="gallery" class="landing-band gallery-band" aria-labelledby="gallery-title">
      <div class="section-inner">
        <div class="section-heading">
          <div>
            <span class="section-kicker">Care moments</span>
            <h2 id="gallery-title">每一处细节，都值得被看见</h2>
          </div>
          <p>服务现场的真实图片来自平台已有服务资料，照护前先了解你将选择的环境。</p>
        </div>

        <div v-if="galleryImages.length" class="gallery-grid">
          <button
            v-for="(image, index) in galleryImages"
            :key="image.key"
            class="gallery-item"
            :class="{ 'gallery-item-featured': index === 0 }"
            type="button"
            :aria-label="`查看${image.service.name_wsh}服务图片`"
            @click="goDetail(image.service.id_wsh)"
          >
            <MediaWithFallback :src="image.url" :alt="`${image.service.name_wsh}服务现场`" :placeholder="image.service.name_wsh" />
            <span class="gallery-caption">
              <strong>{{ image.service.name_wsh }}</strong>
              <span>{{ categoryLabel(image.service) }}</span>
            </span>
          </button>
        </div>
        <div v-else class="empty-section empty-section-light">
          <el-icon aria-hidden="true"><Picture /></el-icon>
          <h3>服务图片正在整理</h3>
          <p>先从服务方案开始了解平台。</p>
        </div>
      </div>
    </section>

    <section id="testimonials" class="landing-section testimonials-section" aria-labelledby="testimonials-title">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Pet parents</span>
          <h2 id="testimonials-title">来自真实订单的反馈</h2>
        </div>
        <p>只展示平台已有评价，让每一句体验都能追溯到具体的服务伙伴。</p>
      </div>

      <div v-if="testimonialsLoading" class="testimonial-grid" aria-label="评价加载中">
        <article v-for="index in 3" :key="index" class="testimonial-card skeleton-testimonial" aria-hidden="true">
          <div class="skeleton-line skeleton-line-short"></div>
          <div class="skeleton-line skeleton-line-wide"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line skeleton-line-short"></div>
        </article>
      </div>

      <div v-else-if="testimonials.length" class="testimonial-grid">
        <article v-for="testimonial in testimonials" :key="testimonial.key" class="testimonial-card">
          <div class="rating-row" :aria-label="`${testimonial.score_wsh} 分评价`">
            <el-icon
              v-for="score in 5"
              :key="score"
              :class="{ active: score <= testimonial.score_wsh }"
              aria-hidden="true"
            >
              <StarFilled />
            </el-icon>
          </div>
          <blockquote>“{{ testimonial.content_wsh }}”</blockquote>
          <footer>
            <span class="testimonial-avatar" aria-hidden="true"><el-icon><UserFilled /></el-icon></span>
            <span>
              <strong>平台宠物主人</strong>
              <small>评价了 {{ testimonial.targetLabel }}</small>
            </span>
            <time v-if="testimonial.created_at_wsh" :datetime="testimonial.created_at_wsh">
              {{ formatDate(testimonial.created_at_wsh) }}
            </time>
          </footer>
        </article>
      </div>

      <div v-else class="empty-section">
        <el-icon aria-hidden="true"><ChatDotRound /></el-icon>
        <h3>首批体验反馈正在积累</h3>
        <p>完成一次服务后，欢迎回来分享爱宠的体验。</p>
      </div>
    </section>

    <section id="booking" class="landing-band booking-band" aria-labelledby="booking-title">
      <div class="section-inner booking-inner">
        <div class="booking-copy">
          <span class="section-kicker">Book a care day</span>
          <h2 id="booking-title">给爱宠安排下一次好时光</h2>
          <p>选定服务后，订单页面会继续补充宠物、商家、看护员和时间信息。</p>
          <div class="booking-proof-list">
            <span><el-icon aria-hidden="true"><CircleCheckFilled /></el-icon> 服务进度可查</span>
            <span><el-icon aria-hidden="true"><CircleCheckFilled /></el-icon> 评价记录可追溯</span>
            <span><el-icon aria-hidden="true"><CircleCheckFilled /></el-icon> 订单信息可回看</span>
          </div>
        </div>

        <form class="quick-booking" @submit.prevent="startBooking">
          <div class="quick-booking-header">
            <span>快速预约</span>
            <el-icon aria-hidden="true"><Calendar /></el-icon>
          </div>
          <label for="quick-service">选择服务</label>
          <select id="quick-service" v-model="selectedServiceId">
            <option value="" disabled>请选择服务方案</option>
            <option v-for="service in services" :key="service.id_wsh" :value="String(service.id_wsh)">
              {{ service.name_wsh }} · ¥{{ formatMoney(service.price_wsh) }}/{{ service.unit_wsh || '次' }}
            </option>
          </select>
          <div class="quick-booking-summary" aria-live="polite">
            <span>{{ selectedService?.description_wsh || '选择一个服务方案开始预约' }}</span>
            <strong v-if="selectedService">¥{{ formatMoney(selectedService.price_wsh) }}</strong>
          </div>
          <button class="btn btn-primary btn-block" type="submit" :disabled="!selectedService">
            <el-icon aria-hidden="true"><Calendar /></el-icon>
            开始预约
          </button>
        </form>
      </div>
    </section>

    <footer class="landing-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <router-link to="/dashboard" class="footer-logo">
            <span class="brand-mark"><el-icon aria-hidden="true"><House /></el-icon></span>
            <span>宠物寄养平台</span>
          </router-link>
          <p>让每一次托付，都有清晰的服务和温柔的回应。</p>
        </div>
        <nav class="footer-links" aria-label="平台导航">
          <router-link to="/services">预约服务</router-link>
          <router-link to="/merchants">附近商户</router-link>
          <router-link to="/orders">我的订单</router-link>
          <router-link to="/profile">个人中心</router-link>
        </nav>
        <p class="footer-meta">© {{ currentYear }} 宠物寄养平台</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Calendar,
  ChatDotRound,
  CircleCheckFilled,
  FirstAidKit,
  House,
  Location,
  MagicStick,
  Medal,
  Picture,
  Scissor,
  Service,
  Star,
  StarFilled,
  UserFilled,
} from '@element-plus/icons-vue'
import { getKeepers } from '@/api/keeper'
import { getMerchants } from '@/api/merchant'
import { getRatings } from '@/api/rating'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'
import BannerCarousel from '@/components/dashboard/BannerCarousel.vue'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const services = ref([])
const banners = ref([])
const testimonials = ref([])
const providers = ref([])
const merchants = ref([])
const loading = ref(true)
const testimonialsLoading = ref(true)
const selectedServiceId = ref('')

const serviceTypeLabels = {
  BOARDING_STANDARD: '标准寄养',
  BOARDING_VIP: '品质寄养',
  GROOMING_BASIC: '美容护理',
  TRAINING_BASIC: '行为训练',
  WALK_STANDARD: '遛宠陪伴',
  MEDICAL_CHECKUP: '健康护理',
  boarding: '寄养服务',
  grooming: '美容服务',
  training: '训练服务',
  walk: '遛宠服务',
  medical: '医疗服务',
}

const iconByServiceType = {
  BOARDING_STANDARD: House,
  BOARDING_VIP: Star,
  GROOMING_BASIC: Scissor,
  TRAINING_BASIC: Medal,
  WALK_STANDARD: Location,
  MEDICAL_CHECKUP: FirstAidKit,
}

const selectedService = computed(() => services.value.find(service => String(service.id_wsh) === String(selectedServiceId.value)) || null)
const featuredServices = computed(() => services.value.slice(0, 6))
const galleryImages = computed(() => services.value
  .flatMap(service => service.imageUrls.map((url, imageIndex) => ({
    key: `${service.id_wsh}-${imageIndex}`,
    service,
    url,
  })))
  .slice(0, 6))
const heroImage = computed(() => galleryImages.value[0]?.url || '')
const serviceCountDisplay = computed(() => loading.value ? '--' : String(services.value.length))
const providerCountDisplay = computed(() => testimonialsLoading.value ? '--' : String(providers.value.length + merchants.value.length))
const averageRatingDisplay = computed(() => {
  const scores = testimonials.value.map(item => Number(item.score_wsh)).filter(Number.isFinite)
  if (!scores.length) return '暂无'
  return (scores.reduce((total, score) => total + score, 0) / scores.length).toFixed(1)
})
const currentYear = new Date().getFullYear()

const normalizedServiceType = service => service?.type_wsh || service?.type || ''

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

function parseImageUrls(value) {
  return String(value || '')
    .split(',')
    .map(url => url.trim())
    .filter(Boolean)
}

function hydrateServices(data) {
  services.value = normalizeList(data).map(service => ({
    ...service,
    imageUrls: parseImageUrls(service.images_wsh),
    firstImage: parseImageUrls(service.images_wsh)[0] || '',
  }))

  if (!selectedServiceId.value && services.value.length) {
    selectedServiceId.value = String(services.value[0].id_wsh)
  }
}

function categoryLabel(service) {
  return service?.category_name_wsh || serviceTypeLabels[normalizedServiceType(service)] || '照护服务'
}

function serviceIcon(service) {
  return iconByServiceType[normalizedServiceType(service)] || Service
}

function accentClass(index) {
  return ['accent-orange', 'accent-mint', 'accent-blue', 'accent-pink', 'accent-yellow', 'accent-purple'][index % 6]
}

function formatMoney(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '--'
  return amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('zh-CN')
}

async function loadDashboard() {
  loading.value = true
  try {
    const response = await request.get('/services')
    if (response.data.code === 200) hydrateServices(response.data.data)
  } catch (_) {
    services.value = []
    appStore.addToast('加载服务方案失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }

  void Promise.all([loadBanners(), loadCommunity()])
}

async function loadBanners() {
  try {
    const response = await request.get('/api/notices/active', { params: { type: 'banner' } })
    if (response.data.code !== 200) return
    banners.value = normalizeList(response.data.data)
  } catch (_) {
    banners.value = []
  }
}

async function loadCommunity() {
  testimonialsLoading.value = true
  try {
    const [merchantResponse, keeperResponse] = await Promise.all([getMerchants(), getKeepers()])
    merchants.value = normalizeList(merchantResponse?.data)
    providers.value = normalizeList(keeperResponse?.data)

    const serviceMerchantIds = new Set(services.value.map(service => service.merchant_id_wsh).filter(Boolean))
    const relevantMerchants = merchants.value
      .filter(merchant => !serviceMerchantIds.size || serviceMerchantIds.has(merchant.id_wsh))
      .slice(0, 3)
    const targets = [
      ...relevantMerchants.map(merchant => ({ id: merchant.id_wsh, type: 'merchant', label: merchant.name_wsh || '服务商家' })),
      ...providers.value.slice(0, 3).map(keeper => ({ id: keeper.id_wsh, type: 'keeper', label: keeper.name_wsh || '专业看护伙伴' })),
    ]

    const ratingResults = await Promise.allSettled(targets.map(async target => {
      const response = await getRatings({ targetId: target.id, targetType: target.type })
      if (response?.code !== 200) return []
      return normalizeList(response.data).map(rating => ({ ...rating, targetLabel: target.label, key: `${target.type}-${rating.id_wsh}` }))
    }))

    testimonials.value = ratingResults
      .flatMap(result => result.status === 'fulfilled' ? result.value : [])
      .filter(rating => rating.content_wsh && Number(rating.score_wsh) > 0)
      .sort((a, b) => new Date(b.created_at_wsh || 0) - new Date(a.created_at_wsh || 0))
      .slice(0, 3)
  } catch (_) {
    testimonials.value = []
    providers.value = []
    merchants.value = []
  } finally {
    testimonialsLoading.value = false
  }
}

function goDetail(serviceId) {
  router.push(`/services/${serviceId}`)
}

async function createOrder(service) {
  if (!service?.id_wsh) return
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

  const profileReady = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!profileReady) return
  router.push({ path: '/orders', query })
}

function startBooking() {
  if (selectedService.value) {
    void createOrder(selectedService.value)
    return
  }
  scrollToServices()
}

function scrollToServices() {
  const servicesElement = document.getElementById('services')
  if (!servicesElement) return
  const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  servicesElement.scrollIntoView({ behavior: reducedMotion ? 'auto' : 'smooth', block: 'start' })
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    void loadBanners()
  }
}

watch(() => services.value.length, () => {
  if (!selectedServiceId.value && services.value.length) {
    selectedServiceId.value = String(services.value[0].id_wsh)
  }
})

onMounted(() => {
  void loadDashboard()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.dashboard-page {
  --landing-orange: #ff8c42;
  --landing-orange-deep: #d96b2b;
  --landing-mint: #06a982;
  --landing-blue: #3276c7;
  --landing-pink: #d64e7f;
  --landing-yellow: #bd7b08;
  --landing-purple: #7257bd;
  --landing-ink: #24343a;
  --landing-muted: #526268;
  --landing-surface: #ffffff;
  --landing-surface-alt: #fff5ed;
  --landing-border: #f2d5c2;
  --landing-clay-shadow: #f4c6a8;
  --landing-section-space: 88px;
  color: var(--landing-ink);
  font-family: 'Nunito', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  letter-spacing: 0;
}

:global(html[data-theme='dark'] .dashboard-page),
:global(html.dark .dashboard-page) {
  --landing-ink: #f5f7f8;
  --landing-muted: #c6d0d0;
  --landing-orange-deep: #ffb077;
  --landing-surface: #1a2021;
  --landing-surface-alt: #252a2c;
  --landing-border: #5a3c2e;
  --landing-clay-shadow: #4c2f25;
}

.landing-hero {
  position: relative;
  width: calc(100% + 48px);
  min-height: 620px;
  margin-left: -24px;
  overflow: hidden;
  isolation: isolate;
  background: #31454a;
  color: #fff;
}

.hero-media {
  position: absolute;
  inset: 0;
  z-index: -2;
}

.hero-scrim {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: rgba(18, 31, 34, 0.48);
}

.hero-media :deep(.media-placeholder) {
  color: #fff3e8;
  background: #9b5838;
}

.hero-media :deep(.media-placeholder .el-icon),
.hero-media :deep(.media-placeholder span) {
  display: none;
}

.hero-inner {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: min(1320px, calc(100% - 48px));
  min-height: 620px;
  margin: 0 auto;
  padding: 88px 0 44px;
}

.hero-copy {
  max-width: 680px;
}

.hero-trust,
.hero-booking-preview {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.hero-trust {
  color: #ffe2cc;
  font-size: 14px;
  font-weight: 800;
}

.hero-trust .el-icon {
  color: #7be0bd;
  font-size: 18px;
}

.hero-copy h1 {
  max-width: 680px;
  margin: 22px 0 16px;
  color: #fff;
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 56px;
  font-weight: 700;
  line-height: 1.12;
  text-wrap: balance;
}

.hero-copy > p {
  max-width: 590px;
  color: #f3f7f5;
  font-size: 18px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.hero-actions .btn {
  min-height: 48px;
}

.hero-primary {
  background: var(--landing-orange);
  color: #23150c;
  border: 2px solid #ffb17e;
  box-shadow: 4px 5px 0 rgba(0, 0, 0, 0.2);
}

.hero-primary:hover {
  background: #ff9d5d;
  color: #23150c;
  transform: translateY(-2px);
  box-shadow: 6px 7px 0 rgba(0, 0, 0, 0.22);
}

.hero-secondary {
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.65);
  background: transparent;
}

.hero-secondary:hover {
  color: #fff;
  border-color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin: 44px 0 0;
}

.hero-stats div {
  min-width: 130px;
  padding: 0 22px;
  border-left: 1px solid rgba(255, 255, 255, 0.35);
}

.hero-stats div:first-child {
  padding-left: 0;
  border-left: 0;
}

.hero-stats dt {
  color: #fff;
  font-size: 28px;
  font-weight: 900;
  line-height: 1.1;
}

.hero-stats dd {
  margin-top: 6px;
  color: #e6eeeb;
  font-size: 13px;
}

.hero-booking-preview {
  align-self: flex-end;
  max-width: 360px;
  padding: 15px 18px;
  color: var(--landing-ink);
  background: rgba(255, 255, 255, 0.94);
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 5px 6px 0 rgba(0, 0, 0, 0.18);
}

.hero-preview-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  color: #fff;
  background: var(--landing-orange);
  border-radius: 12px;
}

.hero-preview-icon .el-icon {
  font-size: 22px;
}

.hero-booking-preview div:nth-child(2) {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.hero-booking-preview strong {
  font-size: 14px;
}

.hero-booking-preview span {
  overflow: hidden;
  color: var(--landing-muted);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-booking-preview > .el-icon {
  margin-left: auto;
  color: var(--landing-orange-deep);
}

.home-campaigns {
  margin-top: 32px;
}

.section-kicker {
  color: var(--landing-orange-deep);
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.landing-section,
.landing-band {
  margin-top: var(--landing-section-space);
}

.landing-section {
  width: 100%;
}

.landing-band {
  width: calc(100% + 48px);
  margin-left: -24px;
}

.section-inner,
.booking-inner,
.footer-inner {
  width: min(1320px, calc(100% - 48px));
  margin: 0 auto;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 34px;
}

.section-heading h2 {
  margin-top: 8px;
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 36px;
  line-height: 1.2;
  text-wrap: balance;
}

.section-heading p {
  max-width: 430px;
  color: var(--landing-muted);
  font-size: 15px;
  line-height: 1.7;
}

.section-heading-action {
  display: flex;
  max-width: 430px;
  align-items: flex-start;
  flex-direction: column;
  gap: 12px;
}

.all-services-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  color: var(--landing-orange-deep);
  font-size: 14px;
  font-weight: 900;
}

.all-services-link:hover {
  color: var(--landing-blue);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.service-package {
  --package-accent: var(--landing-orange);
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--landing-surface);
  border: 2px solid var(--landing-border);
  border-radius: 20px;
  box-shadow: 6px 7px 0 var(--landing-clay-shadow), inset -2px -2px 8px rgba(44, 45, 45, 0.04);
  transition: transform 200ms ease-out, box-shadow 200ms ease-out, border-color 200ms ease-out;
}

.service-package:hover {
  border-color: var(--package-accent);
  transform: translate(-2px, -3px);
  box-shadow: 9px 10px 0 var(--landing-clay-shadow), inset -2px -2px 8px rgba(44, 45, 45, 0.04);
}

.accent-orange { --package-accent: var(--landing-orange); }
.accent-mint { --package-accent: var(--landing-mint); }
.accent-blue { --package-accent: var(--landing-blue); }
.accent-pink { --package-accent: var(--landing-pink); }
.accent-yellow { --package-accent: var(--landing-yellow); }
.accent-purple { --package-accent: var(--landing-purple); }

.service-media-button {
  position: relative;
  display: block;
  width: 100%;
  height: 206px;
  padding: 0;
  overflow: hidden;
  background: var(--color-muted);
  border: 0;
  text-align: left;
}

.service-media-button > .media-frame {
  transition: transform 240ms ease-out;
}

.service-media-button:hover > .media-frame {
  transform: scale(1.035);
}

.service-icon {
  position: absolute;
  right: 16px;
  bottom: 16px;
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: #fff;
  background: var(--package-accent);
  border: 2px solid rgba(255, 255, 255, 0.84);
  border-radius: 13px;
  box-shadow: 3px 4px 0 rgba(31, 36, 37, 0.16);
}

.service-icon .el-icon {
  font-size: 22px;
}

.service-package-body {
  display: flex;
  min-height: 220px;
  flex: 1;
  flex-direction: column;
  padding: 22px;
}

.service-heading-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.service-category {
  color: var(--package-accent);
  font-size: 12px;
  font-weight: 900;
}

.service-package h3 {
  margin-top: 5px;
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 22px;
  line-height: 1.25;
}

.service-price {
  flex: 0 0 auto;
  text-align: right;
}

.service-price strong {
  display: block;
  color: var(--package-accent);
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.service-price span {
  color: var(--landing-muted);
  font-size: 12px;
}

.service-package-body > p {
  display: -webkit-box;
  min-height: 68px;
  margin: 18px 0 20px;
  overflow: hidden;
  color: var(--landing-muted);
  font-size: 14px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.service-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
}

.btn-sm {
  min-height: 44px;
  padding: 9px 14px;
}

.btn-primary {
  background: var(--landing-orange);
  color: #28170e;
  border: 2px solid #ffb17e;
  box-shadow: 3px 4px 0 #f2c09f;
}

.btn-primary:hover:not(:disabled) {
  background: #ff9d5d;
  color: #28170e;
  transform: translateY(-2px);
  box-shadow: 5px 6px 0 #f2c09f;
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  box-shadow: none;
}

.text-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 5px;
  padding: 8px 0;
  color: var(--landing-orange-deep);
  font-size: 13px;
  font-weight: 900;
}

.text-link:hover {
  color: var(--landing-mint);
}

.text-link .el-icon {
  font-size: 16px;
  transition: transform 180ms ease-out;
}

.text-link:hover .el-icon {
  transform: translateX(3px);
}

.skeleton-package,
.skeleton-testimonial {
  min-height: 420px;
  padding: 0;
}

.skeleton-media {
  height: 206px;
  background: var(--color-muted);
}

.skeleton-line {
  width: 72%;
  height: 12px;
  margin: 20px 22px 0;
  background: var(--color-muted);
  border-radius: 8px;
}

.skeleton-line-wide { width: calc(100% - 44px); }
.skeleton-line-short { width: 42%; }

.empty-section {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  color: var(--landing-muted);
  text-align: center;
}

.empty-section > .el-icon {
  color: var(--landing-orange);
  font-size: 38px;
}

.empty-section h3 {
  color: var(--landing-ink);
  font-size: 18px;
}

.empty-section p {
  font-size: 14px;
}

.gallery-band {
  padding: 88px 0;
  background: var(--landing-surface-alt);
}

.gallery-grid {
  display: grid;
  grid-auto-rows: 178px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.gallery-item {
  position: relative;
  min-width: 0;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  background: var(--color-muted);
  border: 2px solid var(--landing-border);
  border-radius: 18px;
  text-align: left;
  transition: transform 200ms ease-out, border-color 200ms ease-out;
}

.gallery-item-featured {
  grid-column: span 2;
  grid-row: span 2;
}

.gallery-item:hover {
  border-color: var(--landing-orange);
  transform: translateY(-3px) rotate(-0.5deg);
}

.gallery-item > .media-frame {
  transition: transform 260ms ease-out;
}

.gallery-item:hover > .media-frame {
  transform: scale(1.05);
}

.gallery-caption {
  position: absolute;
  right: 12px;
  bottom: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  color: #fff;
  background: rgba(28, 38, 39, 0.78);
  border-radius: 11px;
}

.gallery-caption strong {
  font-size: 14px;
}

.gallery-caption span {
  color: #e1ebe8;
  font-size: 12px;
}

.empty-section-light {
  min-height: 250px;
}

.testimonials-section {
  padding-bottom: 16px;
}

.testimonial-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.testimonial-card {
  display: flex;
  min-width: 0;
  min-height: 258px;
  flex-direction: column;
  padding: 24px;
  background: var(--landing-surface);
  border: 2px solid var(--landing-border);
  border-radius: 20px;
  box-shadow: 5px 6px 0 var(--landing-clay-shadow);
}

.rating-row {
  display: flex;
  gap: 3px;
  color: #c8cfd0;
}

.rating-row .el-icon {
  font-size: 17px;
}

.rating-row .el-icon.active {
  color: #e39a18;
}

.testimonial-card blockquote {
  flex: 1;
  margin: 20px 0 28px;
  color: var(--landing-ink);
  font-size: 17px;
  line-height: 1.65;
}

.testimonial-card footer {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.testimonial-avatar {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  place-items: center;
  color: #fff;
  background: var(--landing-mint);
  border-radius: 13px;
}

.testimonial-avatar .el-icon {
  font-size: 20px;
}

.testimonial-card footer > span:nth-child(2) {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.testimonial-card footer strong {
  font-size: 13px;
}

.testimonial-card footer small,
.testimonial-card time {
  overflow: hidden;
  color: var(--landing-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skeleton-testimonial {
  min-height: 258px;
  padding-top: 24px;
}

.booking-band {
  padding: 80px 0;
  color: #fff;
  background: #24343a;
}

.booking-inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 440px);
  align-items: center;
  gap: 72px;
}

.booking-copy {
  max-width: 620px;
}

.booking-copy .section-kicker {
  color: #ffc18e;
}

.booking-copy h2 {
  margin: 12px 0 16px;
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 40px;
  line-height: 1.2;
  text-wrap: balance;
}

.booking-copy > p {
  max-width: 530px;
  color: #d9e5e1;
  font-size: 16px;
  line-height: 1.7;
}

.booking-proof-list {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 22px;
  margin-top: 28px;
}

.booking-proof-list span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #f1faf6;
  font-size: 13px;
  font-weight: 800;
}

.booking-proof-list .el-icon {
  color: #79dfbb;
  font-size: 17px;
}

.quick-booking {
  padding: 26px;
  color: var(--landing-ink);
  background: var(--landing-surface);
  border: 2px solid #f4c7a8;
  border-radius: 20px;
  box-shadow: 7px 8px 0 rgba(11, 18, 19, 0.26);
}

.quick-booking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
  color: var(--landing-orange-deep);
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 22px;
}

.quick-booking-header .el-icon {
  font-size: 22px;
}

.quick-booking label {
  display: block;
  margin-bottom: 8px;
  color: var(--landing-ink);
  font-size: 13px;
  font-weight: 900;
}

.quick-booking select {
  min-height: 48px;
  border-color: var(--landing-border);
}

.quick-booking-summary {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  min-height: 62px;
  margin: 16px 0;
  color: var(--landing-muted);
  font-size: 13px;
  line-height: 1.55;
}

.quick-booking-summary span {
  max-width: 300px;
}

.quick-booking-summary strong {
  flex: 0 0 auto;
  color: var(--landing-orange-deep);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.btn-block {
  width: 100%;
}

.landing-footer {
  width: calc(100% + 48px);
  margin-left: -24px;
  padding: 38px 0 30px;
  color: #d8e5e1;
  background: #182528;
}

.footer-inner {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto auto;
  align-items: center;
  gap: 32px;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #fff;
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 19px;
  font-weight: 700;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #21150d;
  background: var(--landing-orange);
  border-radius: 11px;
}

.footer-brand p {
  max-width: 310px;
  margin-top: 12px;
  color: #aebfbb;
  font-size: 13px;
  line-height: 1.6;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: end;
  gap: 8px 18px;
}

.footer-links a {
  min-height: 44px;
  padding: 10px 0;
  color: #d8e5e1;
  font-size: 13px;
  font-weight: 800;
}

.footer-links a:hover {
  color: #ffc18e;
}

.footer-meta {
  color: #8da09b;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .landing-hero,
  .hero-inner {
    min-height: 570px;
  }

  .hero-inner {
    padding-top: 72px;
  }

  .service-grid,
  .testimonial-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .booking-inner {
    gap: 40px;
  }

  .footer-inner {
    grid-template-columns: 1fr auto;
  }

  .footer-meta {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .dashboard-page {
    --landing-section-space: 64px;
  }

  .landing-hero,
  .hero-inner {
    min-height: 500px;
  }

  .hero-inner,
  .section-inner,
  .booking-inner,
  .footer-inner {
    width: min(100% - 32px, 1320px);
  }

  .landing-hero,
  .landing-band,
  .landing-footer {
    width: calc(100% + 32px);
    margin-left: -16px;
  }

  .hero-inner {
    padding: 56px 0 26px;
  }

  .hero-copy h1 {
    margin-top: 18px;
    font-size: 40px;
  }

  .hero-copy > p {
    max-width: 510px;
    font-size: 16px;
  }

  .hero-actions {
    margin-top: 24px;
  }

  .hero-stats {
    margin-top: 28px;
  }

  .hero-stats div {
    min-width: 92px;
    padding: 0 12px;
  }

  .hero-stats dt {
    font-size: 22px;
  }

  .hero-booking-preview {
    align-self: stretch;
    max-width: none;
  }

  .section-heading {
    display: block;
    margin-bottom: 24px;
  }

  .section-heading h2 {
    font-size: 30px;
  }

  .section-heading p {
    margin-top: 12px;
  }

  .section-heading-action {
    max-width: none;
  }

  .service-grid,
  .testimonial-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .gallery-band,
  .booking-band {
    padding: 64px 0;
  }

  .gallery-grid {
    grid-auto-rows: 142px;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .gallery-item-featured {
    grid-column: span 2;
    grid-row: span 2;
  }

  .gallery-caption {
    right: 8px;
    bottom: 8px;
    left: 8px;
    padding: 8px 9px;
  }

  .booking-inner {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .booking-copy h2 {
    font-size: 32px;
  }

  .quick-booking {
    padding: 22px;
  }

  .footer-inner {
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    gap: 20px;
  }

  .footer-links {
    justify-content: start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .service-package,
  .gallery-item,
  .service-media-button > .media-frame,
  .gallery-item > .media-frame,
  .text-link .el-icon {
    transition: none;
  }
}
</style>
