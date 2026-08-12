<template>
  <div class="services-page">
    <section class="services-intro" aria-labelledby="services-page-title">
      <div>
        <span class="services-label">预约服务</span>
        <h1 id="services-page-title">为爱宠选择合适的照护</h1>
        <p>浏览平台全部可预约服务，比较服务内容与价格后进入详情，确认商家、看护员和时间。</p>
      </div>
      <dl class="services-overview" aria-label="服务概览">
        <div>
          <dt>{{ loading ? '--' : services.length }}</dt>
          <dd>可预约服务</dd>
        </div>
        <div>
          <dt>{{ loading ? '--' : categories.length }}</dt>
          <dd>服务分类</dd>
        </div>
      </dl>
    </section>

    <BannerCarousel v-if="banners.length" class="services-banner" :banners="banners" />

    <section class="service-browser" aria-labelledby="service-results-title">
      <div class="service-filters" role="search" aria-label="筛选预约服务">
        <label class="search-field">
          <span>搜索服务</span>
          <span class="input-shell">
            <el-icon aria-hidden="true"><Search /></el-icon>
            <input v-model.trim="searchQuery" type="search" placeholder="输入服务名称或内容" autocomplete="off">
          </span>
        </label>

        <label class="category-field">
          <span>服务分类</span>
          <select v-model="selectedCategory">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>

        <button v-if="hasFilters" class="btn btn-outline reset-button" type="button" @click="resetFilters">
          <el-icon aria-hidden="true"><Refresh /></el-icon>
          清除筛选
        </button>
      </div>

      <div class="results-heading">
        <div>
          <h2 id="service-results-title">全部服务</h2>
          <p>选择服务后可查看详细内容并开始预约。</p>
        </div>
        <strong aria-live="polite">{{ loading ? '正在加载' : `找到 ${filteredServices.length} 项服务` }}</strong>
      </div>

      <ServiceGrid :services="filteredServices" :loading="loading" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getActiveNotices } from '@/api/notice'
import { getServices } from '@/api/service'
import { useAppStore } from '@/stores/app'
import { useCategoryStore } from '@/stores/category'
import BannerCarousel from '@/components/dashboard/BannerCarousel.vue'
import ServiceGrid from '@/components/dashboard/ServiceGrid.vue'

const appStore = useAppStore()
const categoryStore = useCategoryStore()
const services = ref([])
const banners = ref([])
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

const categories = computed(() => {
  const fromStore = categoryStore.getChildren(0).map(cat => cat.name_wsh).filter(Boolean)
  if (fromStore.length) return fromStore
  return Array.from(new Set(services.value.map(categoryLabel))).filter(name => name !== '服务').sort((a, b) => a.localeCompare(b, 'zh-CN'))
})
const hasFilters = computed(() => Boolean(searchQuery.value || selectedCategory.value))
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
    const response = await getServices()
    if (response.code === 200) services.value = normalizeList(response.data)
  } catch (_) {
    services.value = []
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
.services-page {
  width: 100%;
  padding-bottom: 72px;
}

.services-intro {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 48px;
  padding: 42px 0 34px;
  border-bottom: 1px solid var(--color-border);
}

.services-label {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 800;
}

.services-intro h1 {
  margin-top: 10px;
  color: var(--color-foreground);
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 42px;
  line-height: 1.15;
  text-wrap: balance;
}

.services-intro p {
  max-width: 680px;
  margin-top: 16px;
  color: var(--color-muted-foreground);
  font-size: 16px;
  line-height: 1.75;
}

.services-overview {
  display: flex;
  gap: 0;
  margin: 0;
}

.services-overview div {
  min-width: 118px;
  padding: 0 22px;
  border-left: 1px solid var(--color-border);
}

.services-overview dt {
  color: var(--color-foreground);
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: 30px;
  font-weight: 800;
}

.services-overview dd {
  margin-top: 4px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.services-banner {
  margin-top: 32px;
}

.service-browser {
  padding-top: 52px;
}

.service-filters {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(180px, 240px) auto;
  align-items: end;
  gap: 16px;
  padding: 20px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.search-field,
.category-field {
  display: grid;
  gap: 7px;
  color: var(--color-foreground);
  font-size: 13px;
  font-weight: 700;
}

.input-shell {
  position: relative;
  display: block;
}

.input-shell .el-icon {
  position: absolute;
  top: 50%;
  left: 14px;
  color: var(--color-muted-foreground);
  font-size: 18px;
  transform: translateY(-50%);
}

.input-shell input,
.category-field select {
  width: 100%;
  min-height: 46px;
  color: var(--color-foreground);
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: 7px;
  font-size: 15px;
}

.input-shell input {
  padding: 10px 14px 10px 42px;
}

.category-field select {
  padding: 10px 38px 10px 12px;
}

.input-shell input:focus,
.category-field select:focus {
  border-color: var(--color-ring);
  outline: 3px solid color-mix(in srgb, var(--color-ring) 22%, transparent);
}

.reset-button {
  min-height: 46px;
}

.results-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin: 42px 0 24px;
}

.results-heading h2 {
  color: var(--color-foreground);
  font-size: 28px;
  line-height: 1.25;
}

.results-heading p {
  margin-top: 7px;
  color: var(--color-muted-foreground);
  font-size: 14px;
}

.results-heading > strong {
  color: var(--color-primary);
  font-size: 14px;
}

@media (max-width: 820px) {
  .services-intro {
    align-items: start;
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .services-overview div:first-child {
    padding-left: 0;
    border-left: 0;
  }

  .service-filters {
    grid-template-columns: 1fr 1fr;
  }

  .reset-button {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .services-page {
    padding-bottom: 48px;
  }

  .services-intro {
    padding-top: 28px;
  }

  .services-intro h1 {
    font-size: 34px;
  }

  .services-overview {
    width: 100%;
  }

  .services-overview div {
    min-width: 0;
    flex: 1;
    padding: 0 16px;
  }

  .service-browser {
    padding-top: 40px;
  }

  .service-filters {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .reset-button {
    grid-column: auto;
  }

  .results-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
