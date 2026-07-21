<template>
  <div class="favorites-page">
    <PageHero title="我的收藏" subtitle="按类型查看和管理你收藏的商家、寄养员和服务" />

    <section class="favorites-shell">
      <el-tabs v-model="activeType" class="favorite-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane
          v-for="type in favoriteTypes"
          :key="type.code_wsh"
          :label="type.label_wsh"
          :name="type.code_wsh"
        />
      </el-tabs>

      <div v-if="loading" class="loading-wrap">
        <LoadingSpinner text="加载收藏中..." />
      </div>

      <EmptyState
        v-else-if="items.length === 0"
        title="暂无收藏"
        icon="⭐"
        description="去浏览并收藏你喜欢的商家、寄养员和服务"
      >
        <router-link to="/dashboard" class="btn btn-primary">去首页看看</router-link>
      </EmptyState>

      <div v-else class="favorite-list">
        <article
          v-for="item in items"
          :key="item.id_wsh"
          class="favorite-card"
          @click="openDetail(item)"
        >
          <div class="favorite-thumb">
            <img v-if="item.image_url_wsh" :src="item.image_url_wsh" :alt="item.title_wsh">
            <div v-else class="favorite-thumb-fallback">{{ getTypeInitial(item.target_type_wsh) }}</div>
          </div>

          <div class="favorite-body">
            <div class="favorite-head">
              <span class="badge badge-info">{{ item.target_type_label_wsh }}</span>
              <span class="favorite-date">{{ formatDate(item.created_at_wsh) }}</span>
            </div>
            <h3>{{ item.title_wsh }}</h3>
            <p>{{ item.description_wsh || '暂无简介' }}</p>
            <div class="favorite-meta">
              <span v-if="item.primary_info_wsh">{{ item.primary_info_wsh }}</span>
              <span v-if="item.secondary_info_wsh">{{ item.secondary_info_wsh }}</span>
              <span v-if="item.amount_wsh != null">{{ formatAmount(item.amount_wsh) }}{{ item.amount_suffix_wsh || '' }}</span>
            </div>
          </div>

          <div class="favorite-actions" @click.stop>
            <button class="btn btn-outline btn-sm" @click="openDetail(item)">查看</button>
            <button
              class="btn btn-danger btn-sm"
              :disabled="removingId === item.id_wsh"
              @click="removeFavorite(item)"
            >
              {{ removingId === item.id_wsh ? '处理中...' : '取消收藏' }}
            </button>
          </div>
        </article>
      </div>

      <div v-if="!loading && total > pageSize" class="favorite-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[6, 12, 24]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { getPage, getTypes, toggle } from '@/services/favoriteService'
import {
  DEFAULT_FAVORITE_PAGE_SIZE,
  FAVORITE_TARGET_TYPES,
  getFavoriteTargetTypeLabel,
  normalizeFavoriteTargetType,
} from '@/constants/favorite'

const router = useRouter()
const appStore = useAppStore()

const favoriteTypes = ref([])
const activeType = ref('all')
const items = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(DEFAULT_FAVORITE_PAGE_SIZE)
const loading = ref(true)
const removingId = ref(null)

const selectedType = computed(() => {
  return activeType.value === 'all' ? '' : normalizeFavoriteTargetType(activeType.value)
})

async function loadTypes() {
  try {
    const data = await getTypes()
    favoriteTypes.value = Array.isArray(data) ? data : []
  } catch {
    favoriteTypes.value = [
      { code_wsh: FAVORITE_TARGET_TYPES.MERCHANT, label_wsh: getFavoriteTargetTypeLabel(FAVORITE_TARGET_TYPES.MERCHANT) },
      { code_wsh: FAVORITE_TARGET_TYPES.KEEPER, label_wsh: getFavoriteTargetTypeLabel(FAVORITE_TARGET_TYPES.KEEPER) },
      { code_wsh: FAVORITE_TARGET_TYPES.SERVICE, label_wsh: getFavoriteTargetTypeLabel(FAVORITE_TARGET_TYPES.SERVICE) },
    ]
  }
}

async function loadFavorites(pageNo = currentPage.value) {
  loading.value = true
  try {
    const params = {
      page: pageNo,
      size: pageSize.value,
    }
    if (selectedType.value) {
      params.target_type_wsh = selectedType.value
    }
    const data = await getPage(params)
    items.value = data?.list || []
    total.value = Number(data?.total || 0)
    currentPage.value = Number(data?.page || pageNo)
    pageSize.value = Number(data?.size || pageSize.value)
  } catch {
    items.value = []
    total.value = 0
    appStore.addToast('加载收藏失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  currentPage.value = 1
  loadFavorites(1)
}

function handlePageChange(pageNo) {
  currentPage.value = pageNo
  loadFavorites(pageNo)
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadFavorites(1)
}

function openDetail(item) {
  if (item?.detail_url_wsh) {
    router.push(item.detail_url_wsh)
  }
}

function formatDate(value) {
  if (!value) return ''
  try {
    return new Date(value).toLocaleDateString('zh-CN')
  } catch {
    return String(value)
  }
}

function formatAmount(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return String(value ?? '')
  return num.toFixed(2)
}

function getTypeInitial(type) {
  const label = getFavoriteTargetTypeLabel(type)
  return label ? label.charAt(0) : '⭐'
}

async function removeFavorite(item) {
  if (!item || removingId.value === item.id_wsh) return
  removingId.value = item.id_wsh
  try {
    const nextTotal = Math.max(total.value - 1, 0)
    const nextPages = Math.max(1, Math.ceil(nextTotal / pageSize.value))
    await toggle(item.target_id_wsh, item.target_type_wsh)
    appStore.addToast('已取消收藏', 'success')
    await loadFavorites(Math.min(currentPage.value, nextPages))
  } catch {
    appStore.addToast('取消收藏失败', 'error')
  } finally {
    removingId.value = null
  }
}

onMounted(async () => {
  await Promise.all([loadTypes(), loadFavorites(1)])
})
</script>

<style scoped>
.favorites-shell {
  padding: 0 4px 8px;
}

.favorite-tabs {
  margin-bottom: 12px;
}

.loading-wrap {
  padding: 48px 0;
}

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-card {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) auto;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.favorite-card:hover {
  background: var(--color-muted);
  border-color: var(--color-primary);
}

.favorite-thumb {
  width: 110px;
  height: 110px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.favorite-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.favorite-thumb-fallback {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
}

.favorite-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.favorite-body h3 {
  margin: 0;
  font-size: 18px;
}

.favorite-body p {
  margin: 0;
  color: var(--color-muted-foreground);
  line-height: 1.6;
}

.favorite-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.favorite-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
  align-items: flex-end;
  min-width: 120px;
}

.favorite-date {
  color: var(--color-muted-foreground);
  font-size: 12px;
}

.favorite-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 760px) {
  .favorite-card {
    grid-template-columns: 1fr;
  }

  .favorite-thumb {
    width: 100%;
    height: 180px;
  }

  .favorite-actions {
    align-items: flex-start;
    flex-direction: row;
    flex-wrap: wrap;
    min-width: 0;
  }

  .favorite-pagination {
    justify-content: center;
  }
}
</style>
