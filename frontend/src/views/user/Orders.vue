<template>
  <div class="od-page">
    <div class="od-shell">
      <nav class="od-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="od-crumb-link">首页</router-link>
        <span class="od-crumb-sep" aria-hidden="true">›</span>
        <span class="od-crumb-here">我的订单</span>
      </nav>

      <header class="od-head">
        <div class="od-head-copy">
          <p class="od-eyebrow" aria-hidden="true">
            <span class="od-eyebrow-word">订单中心</span>
            <span class="od-eyebrow-line" />
            <span>订单中心</span>
          </p>
          <h1 class="od-title">我的订单</h1>
          <p class="od-sub">
            {{ summaryText }}<template v-if="!loading"><span v-if="pendingCount > 0">，其中 <strong class="od-pend">{{ pendingCount }} 张待付款</strong>，请留意支付时限。</span><template v-else>。</template></template>
          </p>
        </div>
        <button class="od-refresh" type="button" :disabled="loading" @click="loadOrders">
          <AppIcon class="od-refresh-icon" aria-hidden="true"><Refresh /></AppIcon>
          刷新订单
        </button>
      </header>

      <section v-if="activeStay && !loading" class="od-active" :aria-label="`${activeStay.pet_name_wsh || '宠物'} 正在寄养中`">
        <div class="od-active-media">
          <MediaWithFallback :src="activeStayCover(activeStay)" :alt="`${activeStay.pet_name_wsh || '宠物'} 的寄养实拍`" :placeholder="''" fallback-label="暂无实拍" class="od-active-img" />
        </div>
        <div class="od-active-copy">
          <div class="od-active-min">
            <p class="od-active-eyebrow"><span class="od-active-dot" aria-hidden="true" />{{ activeStayLabel(activeStay) }}</p>
            <h2 class="od-active-title">{{ activeStay.pet_name_wsh || '宠物' }}正在{{ activeStay.merchant_name_wsh || '门店' }}</h2>
            <p class="od-active-meta">
              <span class="od-meta-truncate">{{ activeStay.service_name_wsh || '宠物寄养服务' }}</span>
              <span v-if="activeStay.keeper_name_wsh" class="od-meta-item">{{ activeStay.keeper_name_wsh }}</span>
              <span v-if="activeStay.delivered_at_wsh" class="od-meta-item od-tabular">{{ formatDateTime(activeStay.delivered_at_wsh, { format: 'compact' }) }} 入住</span>
            </p>
          </div>
          <div class="od-active-btm">
            <div class="od-active-progress-head">
              <p class="od-active-days">第 <strong class="od-tabular">{{ stayElapsed }}</strong> 天 / 共 <strong class="od-tabular">{{ stayTotal }}</strong> 天</p>
              <button class="od-active-link" type="button" @click="handleViewDetail(activeStay)">
                查看服务动态 <span class="od-active-arrow" aria-hidden="true">→</span>
              </button>
            </div>
            <div class="od-active-track" aria-hidden="true"><span class="od-active-fill" :style="{ width: `${stayRatio * 100}%` }" /></div>
          </div>
        </div>
      </section>

      <div class="od-controls">
        <div class="od-tabs-area">
          <div class="od-tabs" role="tablist" aria-label="订单分类">
            <button
              v-for="group in GROUPS"
              :key="group.key"
              type="button"
              role="tab"
              class="od-tab"
              :class="{ active: activeTab === group.key }"
              :aria-selected="activeTab === group.key"
              :disabled="loading"
              @click="selectGroup(group.key)"
            >
              <span class="od-tab-label" :class="{ strong: activeTab === group.key }">{{ group.label }}</span>
              <span class="od-tab-count" :class="{ on: activeTab === group.key }">{{ groupCount(group.key) }}</span>
              <span v-if="activeTab === group.key" class="od-tab-indicator" aria-hidden="true" />
            </button>
          </div>
          <div v-if="activeGroupSubStatuses.length" class="od-subchips">
            <span class="od-subchips-label">细分状态</span>
            <button class="od-chip" :class="{ active: !activeStatus }" type="button" :aria-pressed="!activeStatus" @click="activeStatus = ''">不限</button>
            <button
              v-for="entry in activeGroupSubStatuses"
              :key="entry.key"
              class="od-chip"
              :class="{ active: activeStatus === entry.key }"
              type="button"
              :aria-pressed="activeStatus === entry.key"
              @click="activeStatus = activeStatus === entry.key ? '' : entry.key"
            >{{ entry.label }}</button>
          </div>
        </div>

        <div class="od-toolbar">
          <div class="od-search">
            <AppIcon class="od-search-icon" aria-hidden="true"><Search /></AppIcon>
            <input
              v-model.trim="searchQuery"
              type="search"
              :disabled="loading"
              placeholder="搜索订单号、宠物、门店"
              aria-label="搜索订单"
              autocomplete="off"
            >
            <button v-if="searchQuery" type="button" class="od-clear" aria-label="清除搜索" @click="searchQuery = ''">✕</button>
          </div>
          <label class="od-sort">
            <AppIcon class="od-sort-icon" aria-hidden="true"><Sort /></AppIcon>
            <span class="od-sort-sr">排序方式</span>
            <select v-model="sortKey" :disabled="loading" class="od-sort-select">
              <option v-for="opt in SORT_OPTIONS" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
            <AppIcon class="od-sort-chev" aria-hidden="true"><ArrowDown /></AppIcon>
          </label>
          <button v-if="hasFilters" type="button" class="od-reset" @click="clearFilters">
            <AppIcon aria-hidden="true"><Close /></AppIcon>
            重置
          </button>
        </div>
      </div>

      <section class="od-list-area" aria-label="订单列表" :aria-busy="loading">
        <template v-if="loading && !orders.length">
          <div v-for="i in 3" :key="i" class="od-skeleton" />
        </template>

        <div v-else-if="loadError && !orders.length" class="od-state od-state-error">
          <div class="od-state-icon od-state-icon-error">⚠</div>
          <h3>订单加载失败</h3>
          <p>{{ loadError }}</p>
          <div class="od-state-actions">
            <button class="od-btn od-btn-primary" type="button" @click="loadOrders">重新加载</button>
          </div>
        </div>

        <div v-else-if="!orders.length" class="od-state">
          <div class="od-state-icon">🐾</div>
          <h3>还没有订单</h3>
          <p>去看看有哪些寄养与照护方案，为你的宠物找到合适的照护伙伴。下单后的进度、实拍与日报都会出现在这里。</p>
          <div class="od-state-actions">
            <router-link to="/services" class="od-btn od-btn-primary">浏览照护服务</router-link>
            <router-link to="/" class="od-btn">了解服务流程</router-link>
          </div>
        </div>

        <div v-else-if="!filteredOrders.length" class="od-state">
          <div class="od-state-icon">🔍</div>
          <h3>没有符合条件的订单</h3>
          <p>换个关键词，或者回到「全部」看看你的完整订单记录。</p>
          <div class="od-state-actions">
            <button class="od-btn" type="button" @click="clearFilters">重置筛选</button>
          </div>
        </div>

        <template v-else>
          <div class="od-list">
            <OrderCard
              v-for="o in filteredOrders"
              :key="`${viewKey}-${o.id_wsh}`"
              :order="o"
              :now-ms="nowMs"
              :processing="processingOrderId === o.id_wsh"
              @cancel="showCancel = o"
              @pay="method => handlePay(o, method)"
              @deliver="handleDeliver(o)"
              @review="openReview(o)"
              @tip="showTip = o"
              @viewDetail="handleViewDetail(o)"
            />
          </div>
          <p class="od-footer-note">已显示全部 <span class="od-tabular">{{ filteredOrders.length }}</span> 张订单</p>
        </template>
      </section>
    </div>

    <TipDialog :visible="!!showTip" :order="showTip" @close="showTip = null" @tipped="onTipped" />
    <ReviewDialog :visible="!!showReview" :order="showReview" :done-types="reviewedDims" @close="showReview = null" @reviewed="onReviewed" />
    <CancelOrderDialog
      :visible="!!showCancel"
      :order="showCancel"
      :loading="showCancel && processingOrderId === showCancel.id_wsh"
      @close="showCancel = null"
      @cancel="handleCancel"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Close, Refresh, Search, Sort } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import OrderCard from '@/components/order/OrderCard.vue'
import TipDialog from '@/components/order/TipDialog.vue'
import ReviewDialog from '@/components/order/ReviewDialog.vue'
import CancelOrderDialog from '@/components/order/CancelOrderDialog.vue'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import { getOrders, cancelOrder as apiCancelOrder, confirmDelivered } from '@/api/order'
import { getServices } from '@/api/service'
import { createPayment, executePayment } from '@/api/payment'
import { createTip } from '@/api/wallet'
import { createRating, getMyRatingsByOrder } from '@/api/rating'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, hasExpiredPaymentTimeout } from '@/utils/orderPaymentTimeout'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const appStore = useAppStore()

const orders = ref([])
const loading = ref(true)
const loadError = ref('')
const activeTab = ref('all')
const activeStatus = ref('')
const searchQuery = ref('')
const sortKey = ref('recent')
const showTip = ref(null)
const showReview = ref(null)
const showCancel = ref(null)
const reviewedDims = ref([])
const nowMs = ref(Date.now())
const processingOrderId = ref(null)
let countdownTimer = null
let nextPaymentTimeoutRefreshAt = 0

const GROUPS = [
  { key: 'all', label: '全部', statuses: [] },
  { key: 'todo', label: '待处理', statuses: ['pending', 'paid', 'confirmed'] },
  { key: 'active', label: '寄养中', statuses: ['delivered', 'received', 'in_progress'] },
  { key: 'completed', label: '已完成', statuses: ['completed'] },
  { key: 'closed', label: '已关闭', statuses: ['cancelled', 'refunding', 'refunded'] },
]
const STATUS_LABELS = {
  pending: '待付款', paid: '已支付', confirmed: '待送达', delivered: '已送达',
  received: '已接收', in_progress: '服务中', completed: '已完成',
  cancelled: '已取消', refunding: '退款中', refunded: '已退款',
}
const SORT_OPTIONS = [
  { key: 'recent', label: '最近下单' },
  { key: 'schedule', label: '服务时间' },
  { key: 'amount', label: '金额从高到低' },
]

const activeGroup = computed(() => GROUPS.find(g => g.key === activeTab.value) || GROUPS[0])

function groupCount(groupKey) {
  const group = GROUPS.find(g => g.key === groupKey)
  if (groupKey === 'all') return orders.value.length
  if (!group || !group.statuses.length) return 0
  return orders.value.filter(order => group.statuses.includes(order.status_wsh)).length
}

const pendingCount = computed(() => orders.value.filter(order => order.status_wsh === 'pending').length)

const activeGroupSubStatuses = computed(() => {
  const statuses = activeGroup.value.statuses
  if (!statuses.length) return []
  const present = statuses.filter(key => orders.value.some(order => order.status_wsh === key))
  return present.length > 1 ? present.map(key => ({ key, label: STATUS_LABELS[key] })) : []
})

const activeStay = computed(() => {
  if (loading.value) return null
  return orders.value.find(order => ['in_progress', 'received', 'delivered'].includes(order.status_wsh)) || null
})

const stayTotal = computed(() => {
  const o = activeStay.value
  if (!o) return 1
  return Math.max(nightsBetween(o.start_date_wsh, o.end_date_wsh), 1)
})
const stayElapsed = computed(() => {
  const o = activeStay.value
  if (!o) return 1
  const start = o.start_date_wsh ? new Date(o.start_date_wsh).getTime() : NaN
  if (Number.isNaN(start)) return 1
  const elapsed = Math.ceil((Date.now() - start) / 86400000)
  return clamp(elapsed, 1, stayTotal.value)
})
const stayRatio = computed(() => clamp(stayElapsed.value / stayTotal.value, 0.06, 1))

const filteredOrders = computed(() => {
  let list = orders.value
  const statuses = activeGroup.value.statuses
  if (statuses.length) list = list.filter(order => statuses.includes(order.status_wsh))
  if (activeStatus.value) list = list.filter(order => order.status_wsh === activeStatus.value)
  const keyword = searchQuery.value.trim().toLocaleLowerCase()
  if (keyword) {
    list = list.filter(order => {
      const haystack = [
        order.order_no_wsh,
        order.service_name_wsh,
        order.merchant_name_wsh,
        order.keeper_name_wsh,
        order.pet_name_wsh,
        order.pet_breed_wsh,
      ].filter(Boolean)
      return haystack.some(field => String(field).toLocaleLowerCase().includes(keyword))
    })
  }
  const sorted = [...list]
  if (sortKey.value === 'recent') {
    sorted.sort((a, b) => time(b.created_at_wsh) - time(a.created_at_wsh) || (Number(b.id_wsh) || 0) - (Number(a.id_wsh) || 0))
  } else if (sortKey.value === 'schedule') {
    sorted.sort((a, b) => time(a.start_date_wsh) - time(b.start_date_wsh))
  } else {
    sorted.sort((a, b) => orderAmount(b) - orderAmount(a))
  }
  return sorted
})

const hasFilters = computed(() => Boolean(searchQuery.value) || Boolean(activeStatus.value) || activeTab.value !== 'all' || sortKey.value !== 'recent')
const viewKey = computed(() => `${activeTab.value}-${activeStatus.value}-${sortKey.value}`)

const summaryText = computed(() => {
  if (loading.value) return '正在同步你的订单记录…'
  if (!orders.value.length) return '下单后，订单进度、实拍与费用都会集中在这里。'
  const parts = [`共 ${orders.value.length} 张订单`]
  if (groupCount('active')) parts.push(`${groupCount('active')} 张正在寄养`)
  if (groupCount('completed')) parts.push(`${groupCount('completed')} 张已完成`)
  return parts.join('，')
})

function selectGroup(key) {
  activeTab.value = key
  activeStatus.value = ''
}
function clearFilters() {
  activeTab.value = 'all'
  activeStatus.value = ''
  searchQuery.value = ''
  sortKey.value = 'recent'
}

async function openReview(order) {
  showReview.value = order
  reviewedDims.value = []
  try {
    const res = await getMyRatingsByOrder(order.id_wsh)
    if (res.code === 200 && Array.isArray(res.data)) {
      reviewedDims.value = res.data.map(item => item.target_type_wsh).filter(Boolean)
    }
  } catch (e) { /* 已评价状态获取失败不阻塞评价入口 */ }
}

onMounted(async () => {
  countdownTimer = window.setInterval(() => {
    nowMs.value = Date.now()
    refreshExpiredPaymentOrders()
  }, 1000)
  await loadOrders()
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function loadOrders() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getOrders()
    if (res.code === 200) {
      const list = Array.isArray(res.data) ? res.data : []
      await enrichServiceImages(list)
      orders.value = list
    } else {
      loadError.value = res.message || '订单加载失败'
    }
  } catch (e) {
    loadError.value = e?.message || '订单加载失败，请检查网络连接后重试'
    appStore.addToast(e?.message || '订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function enrichServiceImages(orderList) {
  const missingIds = [...new Set(orderList.filter(o => !o.service_images_wsh && o.service_id_wsh).map(o => o.service_id_wsh))]
  if (missingIds.length === 0) return
  try {
    const svcRes = await getServices()
    if (svcRes.code !== 200 || !Array.isArray(svcRes.data)) return
    const svcMap = new Map()
    svcRes.data.forEach(s => { if (s.id_wsh) svcMap.set(s.id_wsh, s.images_wsh) })
    orderList.forEach(o => {
      if (!o.service_images_wsh && o.service_id_wsh) {
        const images = svcMap.get(o.service_id_wsh)
        if (images) o.service_images_wsh = images
      }
    })
  } catch { /* 补拉失败不阻塞订单列表展示 */ }
}

function refreshExpiredPaymentOrders() {
  if (loading.value || nowMs.value < nextPaymentTimeoutRefreshAt || !hasExpiredPaymentTimeout(orders.value, nowMs.value)) return
  nextPaymentTimeoutRefreshAt = nowMs.value + PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS
  void loadOrders()
}

function handleViewDetail(order) {
  if (!order?.id_wsh) return
  router.push({ name: 'OrderDetail', params: { id: order.id_wsh } })
}

async function handlePay(order, method = 'balance') {
  if (!order?.id_wsh || processingOrderId.value) return
  processingOrderId.value = order.id_wsh
  try {
    const createRes = await createPayment({ order_id_wsh: order.id_wsh, method_wsh: method })
    if (createRes.code !== 200 || !createRes.data?.pay_no_wsh) {
      appStore.addToast(createRes.message || '支付单创建失败', 'error')
      await loadOrders()
      return
    }
    const payRes = await executePayment({ pay_no_wsh: createRes.data.pay_no_wsh })
    if (payRes.code === 200) appStore.addToast('支付成功', 'success')
    await loadOrders()
  } catch (e) {
    appStore.addToast(e?.message || '支付失败', 'error')
    await loadOrders()
  } finally {
    processingOrderId.value = null
  }
}

async function handleCancel(order) {
  if (!order?.id_wsh || processingOrderId.value) return
  processingOrderId.value = order.id_wsh
  try {
    const res = await apiCancelOrder({ order_id_wsh: order.id_wsh })
    if (res.code === 200) appStore.addToast('订单已取消', 'success')
    await loadOrders()
  } catch (e) {
    appStore.addToast(e?.message || '取消失败', 'error')
    await loadOrders()
  } finally {
    processingOrderId.value = null
    showCancel.value = null
  }
}

async function handleDeliver(order) {
  if (!order?.id_wsh || processingOrderId.value) return
  processingOrderId.value = order.id_wsh
  let location
  try {
    location = await getCurrentAddress()
  } catch (error) {
    appStore.addToast(error.message || '请允许定位后再确认送达', 'warning')
    processingOrderId.value = null
    return
  }
  try {
    const res = await confirmDelivered({ order_no_wsh: order.order_no_wsh, delivered_address_wsh: location.address_wsh })
    if (res.code === 200) appStore.addToast('已确认送达', 'success')
    await loadOrders()
  } catch (e) {
    appStore.addToast(e?.message || '确认送达失败', 'error')
    await loadOrders()
  } finally {
    processingOrderId.value = null
  }
}

async function onTipped(orderId, amount, message) {
  try {
    const res = await createTip({ order_id_wsh: orderId, amount_wsh: Number(amount), message_wsh: message })
    if (res.code === 200) {
      appStore.addToast('打赏成功', 'success')
      showTip.value = null
      await loadOrders()
    }
  } catch (e) { appStore.addToast('打赏失败', 'error') }
}

async function onReviewed({ orderId, targetType, targetId, score, content }) {
  const order = showReview.value
  if (!order) return
  try {
    const res = await createRating({
      order_id_wsh: order.id_wsh,
      target_id_wsh: targetId,
      target_type_wsh: targetType,
      score_wsh: score,
      content_wsh: content,
    })
    if (res.code === 200) {
      appStore.addToast('评价成功', 'success')
      reviewedDims.value = [...new Set([...reviewedDims.value, targetType])]
      await loadOrders()
    }
  } catch (e) { appStore.addToast('评价失败', 'error') }
}

/* ── 工具 ── */
function activeStayLabel(order) {
  return STATUS_LABELS[order.status_wsh] || '寄养中'
}
function activeStayCover(order) {
  const images = parseImages(order.service_images_wsh)
  return images[0] || order.start_photo_wsh || ''
}
function parseImages(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean)
  if (typeof raw !== 'string') return []
  const trimmed = raw.trim()
  if (!trimmed) return []
  try {
    const parsed = JSON.parse(trimmed)
    if (Array.isArray(parsed)) return parsed.filter(Boolean)
    if (typeof parsed === 'string') return [parsed]
  } catch { /* fall through */ }
  return trimmed.split(',').map(s => s.trim()).filter(Boolean)
}
function nightsBetween(start, end) {
  if (!start || !end) return 0
  const a = new Date(start).getTime()
  const b = new Date(end).getTime()
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0
  return Math.round((b - a) / 86400000)
}
function clamp(value, min, max) { return Math.min(Math.max(value, min), max) }
function time(value) {
  const stamp = value ? new Date(value).getTime() : NaN
  return Number.isNaN(stamp) ? 0 : stamp
}
function orderAmount(order) {
  return Number(order.final_amount_wsh || order.total_amount_wsh || order.settlement_amount_wsh || 0) || 0
}
</script>

<style scoped>
.od-page { width: 100%; padding: 6px 0 72px; }
.od-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* Breadcrumb */
.od-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.od-crumb-link { color: var(--ref-muted); }
.od-crumb-link:hover { color: var(--ref-ink); }
.od-crumb-sep { color: var(--ref-line); }
.od-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow */
.od-eyebrow { display: flex; align-items: center; gap: 10px; }
.od-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.od-eyebrow > span { font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }
.od-eyebrow-word { letter-spacing: 0.28em; text-transform: none; }

/* Header */
.od-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 20px 24px; padding: 40px 0 30px; }
.od-head-copy { min-width: 0; }
.od-title {
  margin: 6px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.od-sub {
  margin: 14px 0 0;
  max-width: 640px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.od-pend { font-weight: 600; color: var(--ref-brand-deep); }
.od-refresh {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  padding: 0 16px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 150ms ease, background 150ms ease, color 150ms ease, transform 150ms ease;
}
.od-refresh:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 28%, transparent); color: var(--ref-ink); transform: translateY(-1px); }
.od-refresh:disabled { opacity: 0.5; cursor: not-allowed; }
.od-refresh-icon { font-size: 15px; }

/* Active stay 深色卡 */
.od-active {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 20px;
  background: #241C14;
  color: #F6EFE3;
  margin-top: 8px;
}
.od-active-media { width: 100%; height: 208px; flex-shrink: 0; }
.od-active-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.od-active-copy { display: flex; flex-direction: column; justify-content: space-between; gap: 24px; min-width: 0; flex: 1; padding: 24px 26px; }
.od-active-eyebrow { display: flex; align-items: center; gap: 8px; font-size: 12px; letter-spacing: 0.22em; text-transform: uppercase; color: rgba(246, 239, 227, 0.55); }
.od-active-dot { width: 6px; height: 6px; border-radius: 50%; background: #E0823F; }
.od-active-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 400;
  line-height: 1.15;
  letter-spacing: -0.01em;
  color: #F6EFE3;
}
.od-active-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 16px;
  margin-top: 10px;
  font-size: 13px;
  color: rgba(246, 239, 227, 0.65);
}
.od-meta-truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.od-meta-item { display: inline-flex; align-items: center; }
.od-tabular { font-variant-numeric: tabular-nums; }
.od-active-btm > * { margin: 0; }
.od-active-progress-head { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; }
.od-active-days { font-size: 13px; color: rgba(246, 239, 227, 0.75); }
.od-active-days strong { color: #F6EFE3; font-weight: 600; }
.od-active-link { display: inline-flex; align-items: center; gap: 6px; background: transparent; border: 0; cursor: pointer; font-size: 13px; font-weight: 500; color: #F6EFE3; padding: 0; }
.od-active-link .od-active-link-text, .od-active-link { border-bottom: 1px solid rgba(246, 239, 227, 0.3); padding-bottom: 2px; }
.od-active-link:hover { color: #E0823F; border-color: #E0823F; }
.od-active-arrow { display: inline-block; }
.od-active-link:hover .od-active-arrow { transform: translateX(4px); }
.od-active-track { margin-top: 12px; height: 4px; width: 100%; border-radius: var(--radius-pill); background: rgba(246, 239, 227, 0.15); overflow: hidden; }
.od-active-fill { display: block; height: 100%; border-radius: var(--radius-pill); background: #E0823F; transform-origin: left; }

/* Controls */
.od-controls { display: flex; flex-direction: column; gap: 18px; margin-top: 36px; }
.od-tabs-area { min-width: 0; }
.od-tabs {
  display: flex;
  align-items: flex-end;
  gap: 0;
  margin-bottom: -1px;
  overflow-x: auto;
  border-bottom: 1px solid var(--ref-line);
  scrollbar-width: none;
}
.od-tabs::-webkit-scrollbar { display: none; }
.od-tab {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  padding: 4px 14px 12px;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 13.5px;
  letter-spacing: -0.01em;
  color: var(--ref-muted);
  transition: color 150ms ease, opacity 150ms ease;
}
.od-tab:disabled { opacity: 0.5; cursor: not-allowed; }
.od-tab:hover { color: var(--ref-ink-soft); }
.od-tab.active { color: var(--ref-ink); }
.od-tab-label { line-height: 1; }
.od-tab-label.strong { font-weight: 600; }
.od-tab-count { font-size: 12px; font-variant-numeric: tabular-nums; color: color-mix(in srgb, var(--ref-muted) 70%, transparent); }
.od-tab-count.on { color: var(--ref-brand); }
.od-tab-indicator { position: absolute; left: 10px; right: 10px; bottom: -1px; height: 2px; border-radius: var(--radius-pill); background: var(--ref-brand); }
.od-subchips { display: flex; align-items: center; gap: 8px; padding-top: 16px; overflow-x: auto; scrollbar-width: none; }
.od-subchips::-webkit-scrollbar { display: none; }
.od-subchips-label { flex-shrink: 0; font-size: 12px; color: var(--ref-muted); }
.od-chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 13px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12px;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.od-chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); background: color-mix(in srgb, var(--ref-sand) 60%, transparent); }
.od-chip.active { border-color: var(--ref-ink); background: var(--ref-ink); color: var(--ref-cream); }

/* Toolbar */
.od-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.od-search { position: relative; flex: 1; min-width: 0; max-width: 320px; }
.od-search-icon { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--ref-muted); font-size: 15px; }
.od-search input {
  width: 100%;
  height: 40px;
  padding: 0 38px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13px;
  transition: border-color 150ms ease;
}
.od-search input::placeholder { color: var(--ref-muted); }
.od-search input:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); }
.od-search input:focus { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); outline: none; }
.od-search input:disabled { opacity: 0.6; cursor: not-allowed; }
.od-clear { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); width: 26px; height: 26px; border-radius: 8px; color: var(--ref-muted); background: transparent; font-size: 12px; cursor: pointer; }
.od-clear:hover { color: var(--ref-ink); background: var(--ref-sand); }
.od-sort { position: relative; display: flex; align-items: center; height: 40px; padding: 0 12px; border: 1px solid var(--ref-line); border-radius: 10px; background: var(--ref-surface); cursor: pointer; transition: border-color 150ms ease; }
.od-sort:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); }
.od-sort-icon { color: var(--ref-muted); font-size: 14px; }
.od-sort-sr { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
.od-sort-select { appearance: none; background: transparent; border: 0; padding: 0 18px 0 6px; font-size: 13px; color: var(--ref-ink); cursor: pointer; outline: none; }
.od-sort-select:disabled { opacity: 0.6; cursor: not-allowed; }
.od-sort-chev { position: absolute; right: 10px; color: var(--ref-muted); font-size: 14px; pointer-events: none; }
.od-reset { display: inline-flex; align-items: center; gap: 6px; height: 40px; padding: 0 10px; background: transparent; border: 0; cursor: pointer; font-size: 12.5px; color: var(--ref-ink-soft); transition: color 150ms ease; }
.od-reset:hover { color: var(--ref-brand); }

/* List */
.od-list-area { margin-top: 28px; }
.od-list { display: grid; gap: 18px; }
.od-footer-note { margin-top: 28px; text-align: center; font-size: 12px; color: var(--ref-muted); }
.od-footer-note .od-tabular { color: var(--ref-ink-soft); }

/* Skeleton */
.od-skeleton { height: 260px; border-radius: var(--radius-card); border: 1px solid var(--ref-line); background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%); background-size: 200% 100%; animation: od-shimmer 1.3s linear infinite; }

/* States */
.od-state { padding: 64px 24px; border-radius: var(--radius-card); border: 1px solid var(--ref-line); background: var(--ref-surface); text-align: center; }
.od-state-error { border-color: color-mix(in srgb, #a03422 20%, transparent); background: color-mix(in srgb, #a03422 4%, var(--ref-surface)); }
.od-state-icon { display: flex; align-items: center; justify-content: center; width: 56px; height: 56px; margin: 0 auto; border-radius: 50%; background: var(--ref-sand); color: var(--ref-brand); font-size: 26px; }
.od-state-icon-error { background: var(--ref-surface); color: #a03422; }
.od-state h3 { margin: 20px 0 0; font-family: var(--ref-font-display); font-size: 22px; font-weight: 500; color: var(--ref-ink); }
.od-state p { max-width: 400px; margin: 10px auto 0; font-size: 13.5px; line-height: 1.7; color: var(--ref-muted); }
.od-state-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 26px; }
.od-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 18px;
  border-radius: 10px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease, transform 150ms ease;
}
.od-btn:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: var(--ref-sand); color: var(--ref-ink); transform: translateY(-1px); }
.od-btn-primary { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.od-btn-primary:hover { background: var(--ref-brand-deep); border-color: var(--ref-brand-deep); color: #fff; }

@keyframes od-shimmer { to { background-position: -200% 0; } }

@media (min-width: 860px) {
  .od-active { flex-direction: row; }
  .od-active-media { width: 268px; height: auto; }
  .od-active-copy { padding: 28px; }
}

@media (max-width: 720px) {
  .od-search { max-width: none; }
  .od-toolbar { flex-wrap: wrap; }
}

@media (max-width: 560px) {
  .od-shell { padding: 0 16px; }
  .od-head { padding: 30px 0 24px; }
  .od-sub { font-size: 13.5px; }
}

@media (prefers-reduced-motion: reduce) {
  .od-skeleton, .od-active-fill { animation: none; }
  .od-active-arrow { transform: none; }
}
</style>