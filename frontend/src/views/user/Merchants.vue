<template>
  <div class="ms-page">
    <div class="ms-shell">

      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ms-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ms-crumb-link">首页</router-link>
        <span class="ms-crumb-sep" aria-hidden="true">›</span>
        <span class="ms-crumb-here">门店</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="ms-head">
        <div class="ms-head-copy">
          <p class="ms-eyebrow">
            <span class="ms-idx">Our stores</span>
            <span class="ms-line" aria-hidden="true"></span>
            <span>门店</span>
          </p>
          <h1 class="ms-title">门店</h1>
          <p class="ms-desc">每家门店的资质、在职照护师与营业状态都在页面上写明。选门店前建议先看一眼资质核验。</p>
        </div>
        <div class="ms-head-side">
          <dl class="ms-facts">
            <div class="ms-fact">
              <dt>门店总数</dt>
              <dd>{{ merchants.length }}</dd>
            </div>
            <div class="ms-fact">
              <dt>营业中</dt>
              <dd>{{ openCount }}</dd>
            </div>
          </dl>
          <button type="button" class="cta cta-outline" :disabled="locating" @click="nearbySearch">
            <span v-if="locating" class="cta-spin" aria-hidden="true"></span>
            {{ nearby ? '查看全部门店' : '附近优先' }}
          </button>
        </div>
      </header>

      <!-- ═══ Toolbar：搜索 + 排序 + 筛选 ═══ -->
      <div class="ms-toolbar">
        <label class="ms-search">
          <span class="sr-only">搜索门店</span>
          <svg class="ms-search-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14Zm7 7a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z"/>
          </svg>
          <input
            v-model="keyword"
            class="ms-input ms-input-search"
            placeholder="搜索门店名称或地址"
          >
        </label>

        <div class="ms-chips" role="group" aria-label="排序方式">
          <button
            v-for="opt in SORTS"
            :key="opt.key"
            type="button"
            :aria-pressed="sort === opt.key"
            :class="['chip', { 'chip-active': sort === opt.key }]"
            @click="sort = opt.key"
          >{{ opt.label }}</button>
          <button
            type="button"
            :aria-pressed="openOnly"
            :class="['chip', { 'chip-brand': openOnly }]"
            @click="openOnly = !openOnly"
          >只看营业中</button>
        </div>

        <div class="ms-toolbar-spacer" aria-hidden="true"></div>

        <div class="ms-toolbar-actions">
          <label class="ms-radius">
            <span class="ms-radius-label">半径 (km)</span>
            <input id="ms-radius-input" v-model="radius" class="ms-input ms-input-sm" placeholder="默认 5km" @keyup.enter="nearbySearch" />
          </label>
          <button type="button" class="cta cta-outline cta-sm" @click="openRegister">注册商家</button>
          <button type="button" class="cta cta-outline cta-sm" @click="openMyMerchant">我的商家</button>
        </div>
      </div>

      <!-- ═══ Section title ═══ -->
      <section class="ms-section">
        <header class="ms-section-head">
          <div class="ms-head-copy">
            <p class="ms-eyebrow">
              <span class="ms-idx">01</span>
              <span class="ms-line" aria-hidden="true"></span>
              <span>Stores</span>
            </p>
            <h2 class="ms-section-title">{{ nearby ? '附近的门店' : '全部门店' }}</h2>
            <p class="ms-desc">
              {{ nearby ? '按你当前位置的直线距离排序，距离仅供参考。' : '支持按名称、地址或特色搜索。' }}
            </p>
          </div>
        </header>

        <div v-if="loading" class="ms-skeleton-grid">
          <div v-for="i in 3" :key="i" class="ms-skeleton"></div>
        </div>

        <div v-else-if="errorMsg" class="ms-empty">
          <p class="ms-empty-title">门店加载失败</p>
          <p class="ms-empty-desc">{{ errorMsg }}</p>
          <button type="button" class="cta cta-outline" @click="reload">重试</button>
        </div>

        <div v-else-if="visible.length" class="ms-grid">
          <article
            v-for="m in visible"
            :key="m.id_wsh"
            class="ms-card"
            role="button"
            tabindex="0"
            :aria-label="`查看 ${m.name_wsh || '门店'} 详情`"
            @click="viewMerchant(m.id_wsh)"
            @keydown.enter="viewMerchant(m.id_wsh)"
          >
            <div class="ms-media">
              <MediaWithFallback
                :src="m.business_license_wsh"
                :alt="`${m.name_wsh || '门店'}的照片`"
                :placeholder="m.name_wsh"
              />
              <span v-if="Number(m.store_status_wsh) === 1" class="ms-open-badge">
                <span class="ms-open-dot" aria-hidden="true"></span>
                {{ getStatusLabel(MerchantStoreStatus, m.store_status_wsh) }}
              </span>
              <span v-else class="ms-closed-badge">
                {{ getStatusLabel(MerchantStoreStatus, m.store_status_wsh) }}
              </span>
            </div>

            <div class="ms-body">
              <div class="ms-top">
                <h3 class="ms-name">{{ m.name_wsh || m.username_wsh || '未命名门店' }}</h3>
                <span :class="['badge', warmBadge(getStatusBadge(MerchantStatus, m.status_wsh))]">
                  {{ getStatusLabel(MerchantStatus, m.status_wsh) }}
                </span>
              </div>

              <div class="ms-meta">
                <span class="ms-rating">
                  <span class="ms-star" aria-hidden="true">★</span>
                  <b>{{ Number(m.rating_wsh || 0).toFixed(1) }}</b>
                </span>
                <span v-if="m.distance_wsh" class="ms-dist">{{ formatServiceDistance(m.distance_wsh) }}</span>
                <span v-if="m.address_wsh" class="ms-addr">{{ m.address_wsh }}</span>
              </div>

              <p v-if="m.description_wsh" class="ms-desc-text">{{ m.description_wsh }}</p>

              <div v-if="(m.qualifications_wsh || []).length" class="ms-quals">
                <span v-for="q in m.qualifications_wsh" :key="q.id_wsh" class="tag-pill">
                  {{ q.status_wsh === 'approved' ? '已认证' : '资质待审' }}
                </span>
              </div>

              <div class="ms-foot">
                <span class="ms-foot-note">查看门店详情</span>
                <span class="ms-arrow" aria-hidden="true">↗</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="ms-empty">
          <p class="ms-empty-title">没有符合条件的门店</p>
          <p class="ms-empty-desc">试试清空搜索词，或关掉「只看营业中」。</p>
          <button type="button" class="cta cta-outline" @click="resetFilter">清空筛选</button>
        </div>
      </section>
    </div>

    <!-- ═══════════════════════════════════════════
         注册 / 编辑商家
         ═══════════════════════════════════════════ -->
    <div v-if="showMerchantForm" class="dlg-overlay" @mousedown.self="showMerchantForm = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" :aria-label="editingMerchantId ? '编辑商家' : '注册商家'">
        <div class="dlg-head">
          <h3 class="dlg-title">{{ editingMerchantId ? '编辑商家' : '注册商家' }}</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="showMerchantForm = false">✕</button>
        </div>
        <form class="dlg-body" @submit.prevent="saveMerchant">
          <div class="dlg-field">
            <label class="dlg-label" for="mf-name">商家名称</label>
            <input id="mf-name" v-model="merchantForm.name_wsh" class="dlg-input" required placeholder="请输入商家名称" />
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="mf-desc">描述</label>
            <textarea id="mf-desc" v-model="merchantForm.description_wsh" class="dlg-input dlg-textarea" rows="3" placeholder="简要介绍门店服务"></textarea>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="mf-phone">联系电话</label>
            <input id="mf-phone" v-model="merchantForm.phone_wsh" class="dlg-input" placeholder="请输入联系电话" />
          </div>
          <div class="dlg-field">
            <label class="dlg-label">资质照片</label>
            <div class="dlg-upload">
              <input
                ref="merchantQualificationInput"
                type="file"
                accept="image/*"
                class="dlg-file"
                @change="uploadMerchantQualification"
              />
              <button type="button" class="cta cta-outline cta-sm" @click="merchantQualificationInput?.click()">
                {{ qualificationUploading ? '上传中...' : '上传资质' }}
              </button>
              <span class="dlg-upload-state">{{ merchantForm.qualification_image_wsh ? '已上传资质' : '未上传资质' }}</span>
            </div>
          </div>
          <div class="dlg-field">
            <label class="dlg-label">地址</label>
            <AmapAddressPicker
              v-model="merchantForm.address_wsh"
              v-model:latitude="merchantForm.latitude_wsh"
              v-model:longitude="merchantForm.longitude_wsh"
              placeholder="搜索地址或点选位置"
            />
          </div>
          <div class="dlg-foot">
            <button type="button" class="dlg-cancel" @click="showMerchantForm = false">取消</button>
            <button type="submit" class="cta cta-primary">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getMerchants, getMyMerchant, getNearbyMerchants, createMerchant, updateMerchant } from '@/api/merchant'
import { uploadFileToDirectory } from '@/api/file'
import AmapAddressPicker from '@/components/common/AmapAddressPicker.vue'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { MerchantStatus, MerchantStoreStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { formatServiceDistance } from '@/composables/useServiceDistance'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const SORTS = [
  { key: 'rating', label: '评分优先' },
  { key: 'distance', label: '距离优先' },
  { key: 'orders', label: '服务单量' },
]

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const merchants = ref([])
const loading = ref(true)
const locating = ref(false)
const errorMsg = ref('')
const nearby = ref(false)
const radius = ref('')
const keyword = ref('')
const sort = ref('rating')
const openOnly = ref(false)
const qualificationUploading = ref(false)
const showMerchantForm = ref(false)
const editingMerchantId = ref(null)
const merchantQualificationInput = ref(null)

const merchantForm = reactive({
  name_wsh: '',
  description_wsh: '',
  phone_wsh: '',
  address_wsh: '',
  latitude_wsh: null,
  longitude_wsh: null,
  qualification_image_wsh: '',
  business_license_wsh: '',
})

/** 营业中门店数量（展示用） */
const openCount = computed(() => merchants.value.filter(m => Number(m.store_status_wsh) === 1).length)

/** 关键词 + 只看营业中 过滤（对齐 React useMerchants.visible） */
const filtered = computed(() => {
  let list = merchants.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(m => {
      const hay = [m.name_wsh, m.address_wsh, m.description_wsh].filter(Boolean).join(' ').toLowerCase()
      return hay.includes(kw)
    })
  }
  if (openOnly.value) list = list.filter(m => Number(m.store_status_wsh) === 1)
  return list
})

/** 排序 */
const visible = computed(() => {
  const list = [...filtered.value]
  const sorters = {
    rating: (a, b) => Number(b.rating_wsh || 0) - Number(a.rating_wsh || 0),
    distance: (a, b) => (a.distance_wsh ?? 1e9) - (b.distance_wsh ?? 1e9),
    orders: (a, b) => Number(b.order_count_wsh || 0) - Number(a.order_count_wsh || 0),
  }
  return list.sort(sorters[sort.value] || sorters.rating)
})

/** 全局 badge-* 色调 → 暖色编辑风色调 */
const WARM_TONE = {
  'badge-success': 'badge-active',
  'badge-warning': 'badge-action',
  'badge-secondary': 'badge-closed',
  'badge-info': 'badge-queued',
  'badge-primary': 'badge-done',
  'badge-danger': 'badge-danger',
  'badge-error': 'badge-danger',
}
function warmBadge(globalClass) {
  return WARM_TONE[globalClass] || 'badge-queued'
}

async function loadMerchants() {
  loading.value = true
  errorMsg.value = ''
  nearby.value = false
  try {
    const r = await getMerchants()
    if (r.code === 200) merchants.value = r.data || []
    else errorMsg.value = r.message || r.msg || '加载失败'
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '门店服务暂时无法连接'
  } finally {
    loading.value = false
  }
}

function reload() {
  nearby.value ? loadNearby() : loadMerchants()
}

async function loadNearby() {
  loading.value = true
  errorMsg.value = ''
  locating.value = true
  try {
    const location = await getCurrentAddress()
    const r = await getNearbyMerchants({ lat: location.latitude_wsh, lng: location.longitude_wsh, radius: radius.value || undefined })
    if (r.code === 200) {
      merchants.value = r.data || []
      nearby.value = true
    } else {
      errorMsg.value = r.message || r.msg || '附近门店加载失败'
    }
  } catch (e) {
    errorMsg.value = e.message || '定位失败，无法搜索附近商家'
  } finally {
    locating.value = false
    loading.value = false
  }
}

function nearbySearch() {
  if (nearby.value) {
    loadMerchants()
  } else {
    loadNearby()
  }
}

function resetFilter() {
  keyword.value = ''
  openOnly.value = false
}

function resetSearch() {
  radius.value = ''
  loadMerchants()
}

async function openRegister() {
  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.APPLY_MERCHANT, { authStore, appStore, router })
  if (!ok) return
  editingMerchantId.value = null
  resetMerchantForm()
  showMerchantForm.value = true
}

async function openMyMerchant() {
  const r = await getMyMerchant()
  if (r.code === 200 && r.data) router.push(`/merchants/${r.data.id_wsh}`)
  else appStore.addToast('您还没有注册商家', 'info')
}

function viewMerchant(id) {
  router.push(`/merchants/${id}`)
}

function resetMerchantForm() {
  merchantForm.name_wsh = ''
  merchantForm.description_wsh = ''
  merchantForm.phone_wsh = ''
  merchantForm.address_wsh = ''
  merchantForm.latitude_wsh = null
  merchantForm.longitude_wsh = null
  merchantForm.qualification_image_wsh = ''
  merchantForm.business_license_wsh = ''
}

async function uploadMerchantQualification(event) {
  const file = event.target.files?.[0]
  if (!file) return
  qualificationUploading.value = true
  try {
    const data = new FormData()
    data.append('file', file)
    const r = await uploadFileToDirectory('qualifications/merchants', data)
    if (r.code === 200 && r.data?.url_wsh) {
      merchantForm.qualification_image_wsh = r.data.url_wsh
      merchantForm.business_license_wsh = r.data.url_wsh
      appStore.addToast('资质上传成功', 'success')
    }
  } finally {
    qualificationUploading.value = false
    event.target.value = ''
  }
}

async function saveMerchant() {
  if (!editingMerchantId.value) {
    const ok = await ensureProfileRequirement(PROFILE_ACTIONS.APPLY_MERCHANT, { authStore, appStore, router })
    if (!ok) return
  }
  if (!merchantForm.address_wsh || merchantForm.latitude_wsh == null || merchantForm.longitude_wsh == null) {
    appStore.addToast('请选择商家地址', 'warning')
    return
  }
  const r = editingMerchantId.value
    ? await updateMerchant(editingMerchantId.value, merchantForm)
    : await createMerchant(merchantForm)
  if (r.code === 200) {
    appStore.addToast(editingMerchantId.value ? '更新成功' : '注册成功', 'success')
    showMerchantForm.value = false
    loadMerchants()
  }
}

onMounted(loadMerchants)
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.ms-page {
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ms-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.ms-crumb {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.ms-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ms-crumb-link:hover { color: var(--ref-ink); }
.ms-crumb-sep { color: var(--ref-line); }
.ms-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex; align-items: center; gap: 6px;
  border-radius: var(--r-tag); border: 1px solid transparent;
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.badge-action { background: var(--ref-ink); color: var(--ref-cream); }
.badge-active { background: color-mix(in srgb, var(--ref-brand) 14%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand) 28%, transparent); }
.badge-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 22%, transparent); }
.badge-done { background: color-mix(in srgb, var(--ref-ink) 6%, transparent); color: var(--ref-ink-soft); }
.badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-danger { background: color-mix(in srgb, var(--ref-brand-deep) 10%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand-deep) 24%, transparent); }
.tag-pill {
  display: inline-flex; align-items: center;
  border-radius: var(--r-tag); border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; color: var(--ref-ink-soft);
}

/* ═══ CTA ═══ */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }
.cta-sm { height: 36px; padding: 0 14px; font-size: 12.5px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-spin {
  width: 13px; height: 13px; border-radius: 50%;
  border: 2px solid color-mix(in srgb, currentColor 30%, transparent);
  border-top-color: currentColor; animation: ms-rot 0.7s linear infinite;
}

/* ═══ Header ═══ */
.ms-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 40px; padding: 20px 0 8px;
}
.ms-head-copy { min-width: 0; }
.ms-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.ms-idx { font-variant-numeric: tabular-nums; }
.ms-line { width: 24px; height: 1px; background: var(--ref-line); }
.ms-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.ms-desc { margin: 10px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.ms-head-side { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 16px; }
.ms-facts {
  display: flex; gap: 0; margin: 0;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); overflow: hidden;
}
.ms-fact { padding: 14px 22px; border-left: 1px solid var(--ref-line); }
.ms-fact:first-child { border-left: 0; }
.ms-fact dt { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.ms-fact dd {
  margin: 6px 0 0; font-family: var(--ref-font-display);
  font-size: 26px; font-weight: 400; line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
}

/* ═══ Toolbar ═══ */
.ms-toolbar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 12px 16px;
  margin-top: 18px; padding: 14px 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
}
.ms-search { position: relative; min-width: 0; flex: 1 1 220px; max-width: 340px; }
.ms-search-icon {
  position: absolute; left: 12px; top: 50%; transform: translateY(-50%);
  width: 15px; height: 15px; color: var(--ref-muted); pointer-events: none;
}
.ms-input {
  height: 40px; padding: 0 12px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 13px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ms-input-search { width: 100%; padding-left: 36px; }
.ms-input-sm { width: 132px; }
.ms-input::placeholder { color: var(--ref-muted); }
.ms-input:focus { outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent); }
.ms-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  display: inline-flex; align-items: center;
  border-radius: 999px; border: 1px solid var(--ref-line);
  background: var(--ref-surface); color: var(--ref-ink-soft);
  padding: 8px 14px; font-size: 11px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); background: color-mix(in srgb, var(--ref-sand) 60%, transparent); }
.chip-active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }
.chip-brand { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.chip-brand:hover { border-color: var(--ref-brand); background: var(--ref-brand-deep); color: #fff; }
.ms-toolbar-spacer { flex: 1; }
.ms-toolbar-actions { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 10px; }
.ms-radius { display: flex; flex-direction: column; gap: 6px; }
.ms-radius-label { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }

/* ═══ Section ═══ */
.ms-section { margin-top: 40px; }
.ms-section-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 16px 24px; }
.ms-section-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}

/* ═══ Cards ═══ */
.ms-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 22px;
}
.ms-card {
  display: flex; flex-direction: column; overflow: hidden; cursor: pointer;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
  transition: transform 0.2s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.2s, border-color 0.2s;
}
.ms-card:hover, .ms-card:focus-visible {
  transform: translateY(-4px); outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 28px 60px -40px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.ms-media { position: relative; overflow: hidden; aspect-ratio: 16 / 9; background: var(--ref-sand); }
.ms-media :deep(.media-image) { transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.ms-card:hover .ms-media :deep(.media-image) { transform: scale(1.04); }
.ms-media :deep(.media-placeholder) { background: var(--ref-sand); }
.ms-media :deep(.media-placeholder .el-icon) { color: var(--ref-brand); }
.ms-media :deep(.media-placeholder span) { color: var(--ref-muted); }
.ms-open-badge {
  position: absolute; top: 12px; left: 12px;
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 10px; border-radius: var(--r-tag);
  background: var(--ref-brand); color: #fff; font-size: 11px; font-weight: 500;
}
.ms-open-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255, 255, 255, 0.85); }
.ms-closed-badge {
  position: absolute; top: 12px; left: 12px;
  display: inline-flex; align-items: center;
  padding: 5px 10px; border-radius: var(--r-tag);
  background: var(--ref-surface); color: var(--ref-muted);
  border: 1px solid var(--ref-line); font-size: 11px; font-weight: 500;
}
.ms-body { display: flex; flex-direction: column; flex: 1; padding: 18px 18px 16px; }
.ms-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.ms-name {
  min-width: 0; margin: 0; font-family: var(--ref-font-display);
  font-size: 20px; font-weight: 500; line-height: 1.25; letter-spacing: -0.01em; color: var(--ref-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  transition: color 0.15s;
}
.ms-card:hover .ms-name { color: var(--ref-brand); }
.ms-meta {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px 14px; margin-top: 12px;
  font-size: 12px; color: var(--ref-muted);
}
.ms-rating { display: inline-flex; align-items: center; gap: 5px; }
.ms-star { color: var(--ref-brand); font-size: 13px; }
.ms-rating b { color: var(--ref-ink); font-variant-numeric: tabular-nums; font-weight: 500; }
.ms-dist {
  padding: 2px 8px; border-radius: var(--r-tag);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  color: var(--ref-brand-deep); font-variant-numeric: tabular-nums;
}
.ms-addr { min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ms-desc-text {
  margin: 12px 0 0; font-size: 13px; line-height: 1.65; color: var(--ref-ink-soft); opacity: 0.85;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.ms-quals { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 14px; }
.ms-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  margin-top: auto; padding-top: 14px; border-top: 1px solid var(--ref-line);
}
.ms-foot-note { font-size: 12.5px; font-weight: 500; color: var(--ref-ink); transition: color 0.15s; }
.ms-arrow { font-size: 14px; color: var(--ref-muted); transition: transform 0.15s, color 0.15s; }
.ms-card:hover .ms-arrow { transform: translate(2px, -2px); color: var(--ref-brand); }

/* ═══ Loading / Empty ═══ */
.ms-skeleton-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 22px;
}
.ms-skeleton {
  height: 320px; border-radius: var(--r-card); border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: ms-shimmer 1.3s linear infinite;
}
.ms-empty {
  margin-top: 22px; padding: 56px 24px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.ms-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.ms-empty-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }
.ms-empty .cta { margin-top: 20px; }

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed; inset: 0; z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  padding: 20px; background: rgba(10, 8, 6, 0.5); backdrop-filter: blur(2px);
  animation: ms-fade 0.15s ease;
}
.dlg-panel {
  width: 100%; max-width: 480px; max-height: min(90vh, 760px); overflow-y: auto;
  border-radius: var(--r-panel); background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  animation: ms-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 0;
}
.dlg-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.dlg-close {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 8px; background: transparent; border: none;
  color: var(--ref-muted); font-size: 13px; cursor: pointer; transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-input {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-textarea { height: auto; padding: 12px 14px; resize: vertical; }
.dlg-input::placeholder { color: var(--ref-muted); }
.dlg-input:focus { outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent); }
.dlg-upload { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.dlg-file { display: none; }
.dlg-upload-state { font-size: 12px; color: var(--ref-muted); }
.dlg-foot {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  background: var(--ref-surface); border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══ Animations ═══ */
@keyframes ms-shimmer { to { background-position: -200% 0; } }
@keyframes ms-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes ms-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }
@keyframes ms-rot { to { transform: rotate(360deg); } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .ms-grid, .ms-skeleton-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .ms-shell { padding: 0 16px; }
  .ms-grid, .ms-skeleton-grid { grid-template-columns: 1fr; }
  .ms-facts { width: 100%; }
  .ms-fact { flex: 1; padding: 12px 16px; }
  .ms-toolbar-spacer { display: none; }
  .ms-toolbar-actions { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .ms-skeleton { animation: none; }
  .ms-card:hover { transform: none; }
  .ms-media :deep(.media-image) { transition: none; }
}
</style>