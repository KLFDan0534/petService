<template>
  <div class="fv-page">
    <div class="fv-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="fv-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="fv-crumb-link">首页</router-link>
        <span class="fv-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="fv-crumb-link">个人中心</router-link>
        <span class="fv-crumb-sep" aria-hidden="true">›</span>
        <span class="fv-crumb-here">我的收藏</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="fv-head">
        <div class="fv-head-copy">
          <div class="fv-eyebrow" aria-hidden="true">
            <span class="fv-eyebrow-line"></span>
            <span>收藏</span>
          </div>
          <h1 class="fv-title">我的收藏</h1>
          <p class="fv-sub">收藏的服务、门店与照护师会保留在这里，下次预约时可以直接从收藏进入。</p>
        </div>
        <dl class="fv-facts" aria-label="收藏概览">
          <div class="fv-fact">
            <dd>{{ loading ? '--' : total }}</dd>
            <dt>收藏总数</dt>
          </div>
        </dl>
      </header>

      <!-- ═══ 01 · 收藏内容 ═══ -->
      <section class="fv-section" aria-label="收藏内容">
        <header class="fv-sec-head">
          <div class="fv-head-copy">
            <p class="fv-eyebrow fv-sec-eyebrow">
              <span class="fv-idx">01</span>
              <span class="fv-line" aria-hidden="true"></span>
              <span>收藏</span>
            </p>
            <h2 class="fv-sec-title">收藏内容</h2>
            <p class="fv-sec-desc">按类型筛选，取消收藏后可随时重新添加。</p>
          </div>
        </header>

        <!-- type tabs -->
        <div class="fv-tabs" role="tablist" aria-label="收藏类型">
          <button
            type="button"
            role="tab"
            :aria-selected="activeType === 'all'"
            class="fv-chip"
            :class="{ active: activeType === 'all' }"
            @click="activeType = 'all'; handleTabChange()"
          >
            全部
          </button>
          <button
            v-for="type in favoriteTypes"
            :key="type.code_wsh"
            type="button"
            role="tab"
            :aria-selected="activeType === type.code_wsh"
            class="fv-chip"
            :class="{ active: activeType === type.code_wsh }"
            @click="activeType = type.code_wsh; handleTabChange()"
          >
            {{ type.label_wsh }}
          </button>
        </div>

        <!-- loading -->
        <template v-if="loading">
          <div class="fv-grid"><div v-for="i in 3" :key="i" class="fv-skeleton" /></div>
        </template>

        <!-- empty -->
        <div v-else-if="!items.length" class="fv-empty">
          <p class="fv-empty-title">{{ activeType === 'all' ? '还没有收藏' : '这个分类下还没有收藏' }}</p>
          <p class="fv-empty-desc">浏览服务或门店时点收藏，之后就能在这里快速找回。</p>
          <div class="fv-empty-actions">
            <router-link to="/services" class="cta cta-primary">去发现服务</router-link>
            <button v-if="activeType !== 'all'" type="button" class="cta cta-outline" @click="activeType = 'all'; handleTabChange()">查看全部</button>
          </div>
        </div>

        <!-- grid -->
        <template v-else>
          <div class="fv-grid">
            <article v-for="item in items" :key="item.id_wsh" class="fv-card" :class="{ 'is-removing': removingId === item.id_wsh }">
              <button type="button" class="fv-media" :aria-label="item.title_wsh || '查看详情'" @click="openDetail(item)">
                <img v-if="item.image_url_wsh" :src="item.image_url_wsh" :alt="item.title_wsh || '收藏内容'">
                <span v-else class="fv-media-fallback">{{ getTypeInitial(item.target_type_wsh) }}</span>
              </button>

              <div class="fv-body">
                <div class="fv-tags">
                  <span class="tag-pill">{{ item.target_type_label_wsh || item.target_type_wsh || '收藏' }}</span>
                  <span v-if="item.created_at_wsh" class="fv-date">收藏于 {{ formatDate(item.created_at_wsh) }}</span>
                </div>
                <h3 class="fv-name">
                  <button type="button" class="fv-name-btn" @click="openDetail(item)">{{ item.title_wsh || '未命名' }}</button>
                </h3>
                <p v-if="item.description_wsh" class="fv-desc">{{ item.description_wsh }}</p>
                <dl v-if="item.primary_info_wsh || item.secondary_info_wsh" class="fv-info">
                  <dd v-if="item.primary_info_wsh">{{ item.primary_info_wsh }}</dd>
                  <dd v-if="item.secondary_info_wsh">{{ item.secondary_info_wsh }}</dd>
                </dl>
                <div class="fv-foot">
                  <p v-if="item.amount_wsh != null" class="fv-price">
                    <span class="fv-yen">¥</span>{{ formatAmount(item.amount_wsh) }}
                    <span v-if="item.amount_suffix_wsh" class="fv-suffix">{{ item.amount_suffix_wsh }}</span>
                  </p>
                  <button v-else type="button" class="fv-view" @click="openDetail(item)">查看</button>
                  <button
                    type="button"
                    class="cta cta-outline fv-remove"
                    :disabled="removingId === item.id_wsh"
                    @click="removeFavorite(item)"
                  >
                    {{ removingId === item.id_wsh ? '处理中...' : '取消收藏' }}
                  </button>
                </div>
              </div>
            </article>
          </div>

          <!-- pagination -->
          <div v-if="!loading && total > pageSize" class="fv-pagination">
            <p class="fv-total">共 <b>{{ total }}</b> 条收藏</p>
            <div class="fv-pages">
              <button type="button" class="fv-page-btn" :disabled="currentPage <= 1" @click="handlePageChange(currentPage - 1)">‹</button>
              <template v-for="n in Math.max(1, Math.ceil(total / pageSize))" :key="n">
                <button type="button" class="fv-page-btn" :class="{ active: currentPage === n }" @click="handlePageChange(n)">{{ n }}</button>
              </template>
              <button
                type="button"
                class="fv-page-btn"
                :disabled="currentPage >= Math.max(1, Math.ceil(total / pageSize))"
                @click="handlePageChange(currentPage + 1)"
              >›</button>
            </div>
            <label class="fv-size-label">
              每页
              <select class="fv-size" :value="pageSize" @change="handleSizeChange(Number($event.target.value))">
                <option :value="6">6</option>
                <option :value="12">12</option>
                <option :value="24">24</option>
              </select>
            </label>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { getPage, getTypes, toggle } from '@/services/favoriteService'
import { formatLocalDate, formatMoney } from '@/utils/format'
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
  return formatLocalDate(value, '')
}

function formatAmount(value) {
  const num = Number(value)
  return Number.isNaN(num) ? String(value ?? '') : formatMoney(num)
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
.fv-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.fv-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.fv-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.fv-crumb-link { color: var(--ref-muted); text-decoration: none; }
.fv-crumb-link:hover { color: var(--ref-ink); }
.fv-crumb-sep { color: var(--ref-line); }
.fv-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.fv-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.fv-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.fv-idx { font-variant-numeric: tabular-nums; }
.fv-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.fv-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.fv-head-copy { min-width: 0; }
.fv-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.fv-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}

/* ═══ Facts ═══ */
.fv-facts { margin: 0; }
.fv-fact {
  min-width: 150px;
  padding: 18px 24px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.fv-fact:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lift);
}
.fv-fact dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.fv-fact dt {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ref-muted);
}

/* ═══ Section ═══ */
.fv-section { margin-top: 56px; }
.fv-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.fv-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.fv-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.fv-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Type tabs ═══ */
.fv-tabs {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}
.fv-tabs::-webkit-scrollbar { display: none; }
.fv-chip {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.fv-chip:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  background: var(--ref-sand);
}
.fv-chip.active {
  background: var(--ref-ink);
  border-color: var(--ref-ink);
  color: var(--ref-cream);
}

/* ═══ Grid ═══ */
.fv-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 22px;
}

/* ═══ Skeleton ═══ */
.fv-skeleton {
  height: 320px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: fv-shimmer 1.3s linear infinite;
}

/* ═══ Empty ═══ */
.fv-empty {
  margin-top: 22px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  text-align: center;
}
.fv-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.fv-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.fv-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-control);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Favorite card ═══ */
.fv-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s;
}
.fv-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-lift);
}
.fv-card.is-removing { opacity: 0.6; }
.fv-media {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  padding: 0;
  background: var(--ref-sand);
  cursor: pointer;
  border: none;
}
.fv-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}
.fv-card:hover .fv-media img { transform: scale(1.05); }
.fv-media-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-family: var(--ref-font-display);
  font-size: 34px;
  color: var(--ref-brand);
}
.fv-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 18px;
}
.fv-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  min-width: 0;
}
.tag-pill {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: var(--radius-inline);
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
}
.fv-date {
  font-size: 11px;
  color: var(--ref-muted);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  margin-left: auto;
  overflow: hidden;
  text-overflow: ellipsis;
}
.fv-name { margin: 12px 0 0; min-width: 0; }
.fv-name-btn {
  display: block;
  max-width: 100%;
  padding: 0;
  background: none;
  border: none;
  font-family: var(--ref-font-display);
  font-size: 19px;
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  text-align: left;
  cursor: pointer;
  overflow-wrap: anywhere;
  transition: color 0.15s;
}
.fv-name-btn:hover { color: var(--ref-brand); }
.fv-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fv-info {
  margin: 12px 0 0;
  display: grid;
  gap: 4px;
  min-width: 0;
}
.fv-info dd {
  margin: 0;
  font-size: 11.5px;
  color: var(--ref-muted);
  overflow: hidden;
  overflow-wrap: anywhere;
}
.fv-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 12px;
  margin-top: auto;
  padding-top: 16px;
}
.fv-price {
  margin: 0;
  min-width: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}
.fv-yen { font-size: 0.6em; color: var(--ref-muted); vertical-align: top; }
.fv-suffix { margin-left: 4px; font-family: var(--font-body); font-size: 11px; color: var(--ref-muted); white-space: nowrap; }
.fv-view {
  padding: 0;
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-brand);
  cursor: pointer;
  transition: color 0.15s;
}
.fv-view:hover { color: var(--ref-brand-deep); }
.fv-remove { height: 36px; padding: 0 14px; font-size: 12.5px; flex: 0 0 auto; margin-left: auto; }

/* ═══ Pagination ═══ */
.fv-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 28px;
}
.fv-total { margin: 0; font-size: 12px; color: var(--ref-muted); }
.fv-total b { color: var(--ref-ink-soft); font-variant-numeric: tabular-nums; }
.fv-pages { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; justify-content: center; }
.fv-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.fv-page-btn:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  background: var(--ref-sand);
  color: var(--ref-ink);
}
.fv-page-btn.active {
  background: var(--ref-brand);
  border-color: var(--ref-brand);
  color: #fff;
}
.fv-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.fv-size-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ref-muted);
}
.fv-size {
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13px;
  cursor: pointer;
}

/* ═══ Animations ═══ */
@keyframes fv-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .fv-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 560px) {
  .fv-shell { padding: 0 16px; }
  .fv-head { padding: 30px 0 22px; }
  .fv-sub { font-size: 13.5px; }
  .fv-fact { min-width: 0; width: 100%; }
  .fv-section { margin-top: 44px; }
  .fv-grid { grid-template-columns: 1fr; }
  .fv-card { flex-direction: row; }
  .fv-media { width: 42%; aspect-ratio: auto; min-height: 160px; }
  .fv-media-fallback { font-size: 26px; }
}
@media (prefers-reduced-motion: reduce) {
  .fv-skeleton { animation: none; }
}
</style>
