<template>
  <div class="svc-page">
    <div class="svc-shell">
      <nav class="s-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="s-crumb-link">首页</router-link>
        <span class="s-crumb-sep" aria-hidden="true">›</span>
        <span class="s-crumb-here">全部服务</span>
      </nav>

      <section class="s-hero">
        <div class="s-hero-copy">
          <div class="s-eyebrow" aria-hidden="true">
            <span class="s-eyebrow-line" />
            <span>Services</span>
          </div>
          <h1 class="s-title">为你的爱宠，<br class="s-br">找到合适的照护服务</h1>
          <p class="s-sub">从日常洗护、遛宠陪伴到寄养与行为训练，按分类浏览门店在售方案，价格与档期实时同步，选定后可直接进入预约。</p>
          <dl class="s-stats" aria-label="服务概览">
            <div>
              <dd>{{ loading ? '--' : services.length }}</dd>
              <dt>可预约方案</dt>
            </div>
            <div class="s-stat-bordered">
              <dd>{{ loading ? '--' : categories.length }}</dd>
              <dt>服务分类</dt>
            </div>
            <div class="s-stat-bordered">
              <dd>{{ loading ? '--' : merchantCount }}</dd>
              <dt>覆盖门店</dt>
            </div>
          </dl>
        </div>

        <div class="s-hero-media">
          <div class="s-hero-frame">
            <MediaWithFallback v-if="!loading" :src="heroImage" :alt="'照护服务'">
            </MediaWithFallback>
            <div v-else class="s-hero-skel" />
          </div>
        </div>
      </section>

      <BannerCarousel v-if="banners.length" class="s-banner" :banners="banners" />

      <section class="s-catalogue" aria-labelledby="catalogue-title">
        <header class="s-cat-head">
          <div class="s-eyebrow" aria-hidden="true">
            <span class="s-eyebrow-line" />
            <span>Catalogue</span>
          </div>
          <h2 id="catalogue-title" class="s-cat-title">按分类浏览</h2>
          <p class="s-cat-note">选择分类，下方方案将同步更新</p>
        </header>

        <div class="s-rail" role="group" aria-label="服务分类">
          <button
            type="button"
            class="s-chip"
            :class="{ active: selectedCategory === '' }"
            @click="selectedCategory = ''"
          >
            全部 <span class="s-chip-count">{{ loading ? '--' : services.length }}</span>
          </button>
          <button
            v-for="category in categories"
            :key="category"
            type="button"
            class="s-chip"
            :class="{ active: selectedCategory === category }"
            @click="selectedCategory = category"
          >
            {{ category }} <span class="s-chip-count">{{ categoryCounts[category] ?? 0 }}</span>
          </button>
        </div>

        <div class="s-toolbar" role="search" aria-label="筛选预约服务">
          <div class="s-search">
            <el-icon class="s-search-icon" aria-hidden="true"><Search /></el-icon>
            <input
              v-model.trim="searchQuery"
              type="search"
              placeholder="搜索服务名称或内容"
              aria-label="搜索服务"
              autocomplete="off"
            >
            <button v-if="searchQuery" type="button" class="s-clear" aria-label="清除搜索" @click="searchQuery = ''">✕</button>
          </div>
          <button v-if="hasFilters" type="button" class="s-reset" @click="resetFilters">
            ✕ 清除筛选
          </button>
        </div>

        <div class="s-results-head">
          <div>
            <h2 id="service-results-title" class="s-cat-title">
              {{ selectedCategory || '全部照护方案' }}
            </h2>
            <p class="s-results-note" aria-live="polite">
              {{ loading ? '正在加载方案' : `共 ${filteredServices.length} 套方案 · 价格与档期实时同步门店` }}
            </p>
          </div>
        </div>

        <ServiceGrid :services="filteredServices" :loading="loading" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getActiveNotices } from '@/api/notice'
import { getServices } from '@/api/service'
import { getMerchants } from '@/api/merchant'
import { useAppStore } from '@/stores/app'
import { useCategoryStore } from '@/stores/category'
import BannerCarousel from '@/components/dashboard/BannerCarousel.vue'
import ServiceGrid from '@/components/dashboard/ServiceGrid.vue'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import { useServiceDistance } from '@/composables/useServiceDistance'

const appStore = useAppStore()
const categoryStore = useCategoryStore()
const { attachDistances } = useServiceDistance()
const services = ref([])
const banners = ref([])
const merchants = ref([])
const loading = ref(true)
const searchQuery = ref('')
const selectedCategory = ref('')

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

function categoryLabel(service) {
  return service.category_name_wsh || categoryStore.getCategoryName(service.category_id_wsh)
}

function firstImage(service) {
  return String(service?.images_wsh || '')
    .split(',')
    .map(url => url.trim())
    .find(Boolean) || ''
}

const categories = computed(() => {
  // 分类芯片、计数与筛选全部以服务自身的 category_name_wsh 为准（与参考设计一致）
  const seen = new Set()
  const list = []
  for (const service of services.value) {
    const label = categoryLabel(service)
    if (!label || label === '服务' || seen.has(label)) continue
    seen.add(label)
    list.push(label)
  }
  return list.sort((a, b) => a.localeCompare(b, 'zh-CN'))
})
const categoryCounts = computed(() => {
  const map = {}
  for (const service of services.value) {
    const key = categoryLabel(service)
    map[key] = (map[key] || 0) + 1
  }
  return map
})
const hasFilters = computed(() => Boolean(searchQuery.value || selectedCategory.value))
const merchantCount = computed(() => merchants.value.length)
const heroImage = computed(() => firstImage(services.value?.[0]) || '')
const filteredServices = computed(() => {
  const keyword = searchQuery.value.toLocaleLowerCase('zh-CN')
  return services.value.filter(service => {
    const category = categoryLabel(service)
    const searchableText = [service.name_wsh, service.description_wsh, category]
      .filter(Boolean)
      .join(' ')
      .toLocaleLowerCase('zh-CN')
    const matchesSearch = !keyword || searchableText.includes(keyword)
    const matchesCategory = !selectedCategory.value || category === selectedCategory.value
    return matchesSearch && matchesCategory
  })
})

function resetFilters() {
  searchQuery.value = ''
  selectedCategory.value = ''
}

async function loadServices() {
  loading.value = true
  try {
    const [serviceResponse, merchantResponse] = await Promise.all([getServices(), getMerchants()])
    if (serviceResponse.code === 200) services.value = normalizeList(serviceResponse.data)
    const merchantList = merchantResponse.code === 200 ? normalizeList(merchantResponse.data) : []
    merchants.value = merchantList
    // 服务卡片距离：用户位置 + 商家地址均走高德（不依赖本地经纬度）
    void attachDistances(services.value, merchantList)
  } catch (_) {
    services.value = []
    merchants.value = []
    appStore.addToast('加载预约服务失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

async function loadBanners() {
  try {
    const response = await getActiveNotices({ type: 'banner' })
    if (response.code === 200) banners.value = normalizeList(response.data)
  } catch (_) {
    banners.value = []
  }
}

onMounted(() => {
  void Promise.all([loadServices(), loadBanners(), categoryStore.loadCategories()])
})
</script>

<style scoped>
.svc-page {
  width: 100%;
  padding: 6px 0 72px;
}

.svc-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* Breadcrumb */
.s-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.s-crumb-link {
  color: var(--ref-muted);
}
.s-crumb-link:hover {
  color: var(--ref-ink);
}
.s-crumb-sep {
  color: var(--ref-line);
}
.s-crumb-here {
  color: var(--ref-ink-soft);
}

/* Eyebrow */
.s-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
}
.s-eyebrow-line {
  width: 32px;
  height: 1px;
  background: var(--ref-line);
}
.s-eyebrow > span:last-child {
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}

/* Hero */
.s-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 48px;
  align-items: center;
  padding: 48px 0 40px;
}
.s-title {
  margin: 20px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.s-br {
  display: block;
}
.s-sub {
  margin: 20px 0 0;
  max-width: 560px;
  font-size: 15px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.s-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0 28px;
  margin: 36px 0 0;
}
.s-stats div {
  display: flex;
  flex-direction: column;
}
.s-stat-bordered {
  border-left: 1px solid var(--ref-line);
  padding-left: 28px;
}
.s-stats dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.s-stats dt {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ref-muted);
}
.s-hero-frame {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 26px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
}
.s-hero-frame :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.s-hero-skel {
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: ref-shimmer 1.3s linear infinite;
}

/* Banner (ads) */
.s-banner {
  margin-top: 36px;
}

/* Catalogue */
.s-catalogue {
  margin-top: 56px;
}
.s-cat-head {
  padding-bottom: 24px;
}
.s-cat-title {
  margin: 14px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(24px, 3vw, 30px);
  line-height: 1.15;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
}
.s-cat-note {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ref-muted);
}

/* Category rail */
.s-rail {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}
.s-rail::-webkit-scrollbar {
  display: none;
}
.s-chip {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 16px;
  border-radius: 11px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.s-chip:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  background: var(--ref-sand);
}
.s-chip.active {
  background: var(--ref-ink);
  border-color: var(--ref-ink);
  color: var(--ref-cream);
}
.s-chip-count {
  font-size: 11px;
  opacity: 0.6;
  font-variant-numeric: tabular-nums;
}

/* Toolbar */
.s-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 28px;
}
.s-search {
  position: relative;
  flex: 1;
  min-width: 220px;
  max-width: 380px;
}
.s-search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ref-muted);
  font-size: 15px;
}
.s-search input {
  width: 100%;
  height: 44px;
  padding: 0 40px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}
.s-search input::placeholder {
  color: var(--ref-muted);
}
.s-search input:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent);
}
.s-search input:focus {
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  outline: none;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.s-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: var(--ref-muted);
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}
.s-clear:hover {
  color: var(--ref-ink);
  background: var(--ref-sand);
}
.s-reset {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 12px;
  border-radius: 11px;
  background: transparent;
  color: var(--ref-ink-soft);
  font-size: 13px;
  cursor: pointer;
}
.s-reset:hover {
  color: var(--ref-brand);
}

/* Results heading */
.s-results-head {
  margin: 44px 0 24px;
  padding-top: 28px;
  border-top: 1px solid var(--ref-line);
}
.s-results-head .s-cat-title {
  font-size: clamp(20px, 2.6vw, 26px);
}
.s-results-note {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ref-muted);
}

/* ===== 服务卡片（覆盖共享 ServiceGrid 的外观，保持功能不变）===== */
:deep(.service-card) {
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  box-shadow: none;
  transition: transform 200ms ease, border-color 200ms ease, box-shadow 200ms ease;
}
:deep(.service-card:hover) {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  transform: translateY(-4px);
}
:deep(.service-card .service-media-button) {
  aspect-ratio: 4 / 3;
  background: var(--ref-sand);
}
:deep(.service-card .service-media-button .media-image) {
  transition: transform 300ms ease;
}
:deep(.service-card .service-media-button:hover .media-image) {
  transform: scale(1.06);
}
:deep(.service-card .service-card-body) {
  padding: 20px;
}
:deep(.service-card .service-category) {
  color: var(--ref-brand);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
:deep(.service-card .service-heading h3) {
  margin-top: 6px;
  font-family: var(--ref-font-display);
  font-size: 20px;
  line-height: 1.3;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
:deep(.service-card .service-card-body > p) {
  color: var(--ref-ink-soft);
  font-size: 13.5px;
  line-height: 1.7;
}
:deep(.service-card .service-price strong) {
  color: var(--ref-ink);
  font-size: 22px;
  font-family: var(--ref-font-display);
  font-weight: 600;
}
:deep(.service-card .service-price span) {
  color: var(--ref-muted);
}
:deep(.service-card .service-actions) {
  border-top: 1px solid var(--ref-line);
}
:deep(.service-card .detail-link) {
  color: var(--ref-ink);
}
:deep(.service-card .detail-link:hover) {
  color: var(--ref-brand);
}
:deep(.service-card .service-actions .btn-primary) {
  background: var(--ref-brand);
  border-radius: 11px;
  color: #fff;
}
:deep(.service-card .service-actions .btn-primary:hover) {
  background: var(--ref-brand-deep);
  opacity: 1;
}
:deep(.service-card .service-merchant) {
  color: var(--ref-muted);
}
:deep(.service-card .service-distance) {
  background: color-mix(in srgb, var(--ref-brand) 12%, transparent);
  color: var(--ref-brand);
}
:deep(.empty-services) {
  background: var(--ref-surface);
  border: 1px dashed var(--ref-line);
  border-radius: 18px;
  color: var(--ref-muted);
}
:deep(.empty-services h3) {
  color: var(--ref-ink);
  font-family: var(--ref-font-display);
}
:deep(.empty-services .el-icon) {
  color: var(--ref-brand);
}
:deep(.service-skeleton .skeleton-media),
:deep(.service-skeleton .skeleton-body span) {
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
}

@keyframes ref-shimmer {
  to { background-position: -200% 0; }
}

@media (max-width: 900px) {
  .s-hero {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 34px 0 28px;
  }
  .s-br {
    display: none;
  }
}

@media (max-width: 560px) {
  .svc-shell {
    padding: 0 16px;
  }
  .s-sub {
    font-size: 14px;
  }
  .s-stats {
    gap: 0 20px;
  }
  .s-stat-bordered {
    padding-left: 20px;
  }
  .s-stats dd {
    font-size: 26px;
  }
  .s-catalogue {
    margin-top: 44px;
  }
  .s-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .s-search {
    max-width: none;
  }
  .s-reset {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .s-hero-skel {
    animation: none;
  }
}
</style>