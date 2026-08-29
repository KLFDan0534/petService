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
          <div class="od-eyebrow" aria-hidden="true">
            <span class="od-eyebrow-line" />
            <span>Order Center</span>
          </div>
          <h1 class="od-title">我的订单</h1>
          <p class="od-sub">{{ summaryText }}</p>
        </div>
        <button class="od-refresh" type="button" :disabled="loading" @click="loadOrders">
          <el-icon class="od-refresh-icon" aria-hidden="true"><Refresh /></el-icon>
          刷新订单
        </button>
      </header>

      <section class="od-summary" aria-label="订单概览">
        <div class="od-stat">
          <dd>{{ loading ? '--' : orders.length }}</dd>
          <dt>全部订单</dt>
        </div>
        <div class="od-stat">
          <dd>{{ loading ? '--' : groupCount('todo') }}</dd>
          <dt>待处理</dt>
        </div>
        <div class="od-stat">
          <dd>{{ loading ? '--' : groupCount('active') }}</dd>
          <dt>寄养中</dt>
        </div>
        <div class="od-stat">
          <dd>{{ loading ? '--' : groupCount('completed') }}</dd>
          <dt>已完成</dt>
        </div>
      </section>

      <section v-if="activeStay" class="od-active" aria-label="正在寄养">
        <div class="od-active-copy">
          <span class="od-active-pulse" aria-hidden="true" />
          <div class="od-active-text">
            <p class="od-active-label">有一单正在寄养中</p>
            <p class="od-active-name">{{ activeStay.service_name_wsh || '宠物寄养服务' }}</p>
            <p class="od-active-meta">{{ activeStay.pet_name_wsh || '宠物' }} · {{ activeStay.merchant_name_wsh || '商家' }} · {{ orderNoText(activeStay) }}</p>
          </div>
        </div>
        <button class="od-active-btn" type="button" @click="handleViewDetail(activeStay)">
          查看服务动态 <span aria-hidden="true">↗</span>
        </button>
      </section>

      <div class="od-controls">
        <div class="od-tabs" role="group" aria-label="订单筛选">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="od-chip"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.key !== 'all'" class="od-chip-count">{{ groupCount(tab.key) }}</span>
          </button>
        </div>
        <div class="od-search">
          <el-icon class="od-search-icon" aria-hidden="true"><Search /></el-icon>
          <input
            v-model.trim="searchQuery"
            type="search"
            placeholder="搜索订单号 / 服务 / 商家 / 宠物"
            aria-label="搜索订单"
            autocomplete="off"
          >
          <button v-if="searchQuery" type="button" class="od-clear" aria-label="清除搜索" @click="searchQuery = ''">✕</button>
        </div>
      </div>

      <section class="od-list-area" aria-label="订单列表" :aria-busy="loading">
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="od-skeleton" />
        </template>

        <div v-else-if="!orders.length" class="od-empty">
          <div class="od-empty-icon" aria-hidden="true">📋</div>
          <h3>暂无订单</h3>
          <p>选择寄养服务后，可以在这里查看订单进度和交接信息。</p>
          <router-link to="/services" class="od-empty-btn">浏览服务</router-link>
        </div>

        <div v-else-if="!filteredOrders.length" class="od-empty">
          <h3>没有匹配的订单</h3>
          <p>试试更换关键词或切换筛选分组。</p>
          <button type="button" class="od-empty-btn" @click="clearFilters">清除筛选</button>
        </div>

        <template v-else>
          <div class="od-list">
            <OrderCard
              v-for="o in filteredOrders"
              :key="o.id_wsh"
              :order="o"
              :now-ms="nowMs"
              :processing="processingOrderId === o.id_wsh"
              @cancel="handleCancel(o)"
              @pay="method => handlePay(o, method)"
              @deliver="handleDeliver(o)"
              @review="openReview(o)"
              @tip="showTip = o"
              @viewDetail="handleViewDetail(o)"
            />
          </div>
          <p class="od-footer-note">
            已显示全部 <span class="od-num">{{ filteredOrders.length }}</span> 张订单
          </p>
        </template>
      </section>
    </div>

    <TipDialog :visible="!!showTip" :order="showTip" @close="showTip = null" @tipped="onTipped" />
    <ReviewDialog :visible="!!showReview" :order="showReview" :done-types="reviewedDims" @close="showReview = null" @reviewed="onReviewed" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import OrderCard from '@/components/order/OrderCard.vue'
import TipDialog from '@/components/order/TipDialog.vue'
import ReviewDialog from '@/components/order/ReviewDialog.vue'
import { getOrders, cancelOrder as apiCancelOrder, confirmDelivered } from '@/api/order'
import { getServices } from '@/api/service'
import { createPayment, executePayment } from '@/api/payment'
import { createTip } from '@/api/wallet'
import { createRating, getMyRatingsByOrder } from '@/api/rating'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, hasExpiredPaymentTimeout } from '@/utils/orderPaymentTimeout'

const router = useRouter()
const appStore = useAppStore()

const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')
const searchQuery = ref('')
const showTip = ref(null)
const showReview = ref(null)
const reviewedDims = ref([])
const nowMs = ref(Date.now())
const processingOrderId = ref(null)
let countdownTimer = null
let nextPaymentTimeoutRefreshAt = 0

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'todo', label: '待处理' },
  { key: 'active', label: '寄养中' },
  { key: 'completed', label: '已完成' },
  { key: 'closed', label: '已关闭' },
]

const GROUP_STATUSES = {
  todo: ['pending', 'paid', 'confirmed'],
  active: ['delivered', 'received', 'in_progress'],
  completed: ['completed'],
  closed: ['cancelled', 'refunding', 'refunded'],
}

function groupCount(groupKey) {
  const statuses = GROUP_STATUSES[groupKey]
  if (!statuses) return 0
  return orders.value.filter(order => statuses.includes(order.status_wsh)).length
}

const hasFilters = computed(() => Boolean(searchQuery.value || activeTab.value !== 'all'))

const activeStay = computed(() => {
  if (loading.value) return null
  return orders.value.find(order =>
    ['delivered', 'received', 'in_progress'].includes(order.status_wsh)) || null
})

const filteredOrders = computed(() => {
  let list = orders.value
  const group = activeTab.value
  if (group !== 'all') {
    const statuses = GROUP_STATUSES[group]
    list = list.filter(order => statuses.includes(order.status_wsh))
  }
  const keyword = searchQuery.value.toLocaleLowerCase('zh-CN')
  if (keyword) {
    list = list.filter(order => {
      const haystack = [
        order.order_no_wsh,
        order.service_name_wsh,
        order.merchant_name_wsh,
        order.keeper_name_wsh,
        order.pet_name_wsh,
        String(order.id_wsh || ''),
      ].filter(Boolean).join(' ').toLocaleLowerCase('zh-CN')
      return haystack.includes(keyword)
    })
  }
  return list
})

const summaryText = computed(() => {
  if (loading.value) return '正在同步你的订单记录…'
  if (!orders.value.length) return '下单后，订单进度、实拍与费用都会集中在这里。'
  const parts = [`共 ${orders.value.length} 张订单`]
  const active = groupCount('active')
  const completed = groupCount('completed')
  if (active) parts.push(`${active} 张正在寄养`)
  if (completed) parts.push(`${completed} 张已完成`)
  return parts.join('，') + '。'
})

function orderNoText(order) {
  return order.order_no_wsh || `#${order.id_wsh}`
}

function clearFilters() {
  activeTab.value = 'all'
  searchQuery.value = ''
}

async function openReview(order) {
  showReview.value = order
  reviewedDims.value = []
  try {
    const res = await getMyRatingsByOrder(order.id_wsh)
    if (res.code === 200 && Array.isArray(res.data)) {
      reviewedDims.value = res.data
        .map(item => item.target_type_wsh)
        .filter(Boolean)
    }
  } catch (e) {
    /* 已评价状态获取失败不阻塞评价入口 */
  }
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
  try {
    const res = await getOrders()
    if (res.code === 200) {
      const list = Array.isArray(res.data) ? res.data : []
      // 补拉服务图片：为没有 service_images_wsh 的订单从服务列表获取
      await enrichServiceImages(list)
      orders.value = list
    }
  } catch (e) {
    appStore.addToast(e?.message || '订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function enrichServiceImages(orderList) {
  // 找出缺少 service_images_wsh 的订单
  const missingIds = [...new Set(
    orderList
      .filter(o => !o.service_images_wsh && o.service_id_wsh)
      .map(o => o.service_id_wsh)
  )]
  if (missingIds.length === 0) return
  try {
    const svcRes = await getServices()
    if (svcRes.code !== 200 || !Array.isArray(svcRes.data)) return
    // 构建 service_id → images_wsh 映射
    const svcMap = new Map()
    svcRes.data.forEach(s => { if (s.id_wsh) svcMap.set(s.id_wsh, s.images_wsh) })
    // 回填到订单
    orderList.forEach(o => {
      if (!o.service_images_wsh && o.service_id_wsh) {
        const images = svcMap.get(o.service_id_wsh)
        if (images) o.service_images_wsh = images
      }
    })
  } catch {
    // 补拉失败不阻塞订单列表展示
  }
}

function refreshExpiredPaymentOrders() {
  if (
    loading.value
    || nowMs.value < nextPaymentTimeoutRefreshAt
    || !hasExpiredPaymentTimeout(orders.value, nowMs.value)
  ) {
    return
  }
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
    const res = await confirmDelivered({
      order_no_wsh: order.order_no_wsh,
      delivered_address_wsh: location.address_wsh,
    })
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
    const res = await createTip({
      order_id_wsh: orderId,
      amount_wsh: Number(amount),
      message_wsh: message,
    })
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
</script>

<style scoped>
.od-page {
  width: 100%;
  padding: 6px 0 72px;
}

.od-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* Breadcrumb */
.od-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.od-crumb-link { color: var(--ref-muted); }
.od-crumb-link:hover { color: var(--ref-ink); }
.od-crumb-sep { color: var(--ref-line); }
.od-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow */
.od-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
}
.od-eyebrow-line {
  width: 32px;
  height: 1px;
  background: var(--ref-line);
}
.od-eyebrow > span:last-child {
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}

/* Header */
.od-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 32px;
}
.od-head-copy { min-width: 0; }
.od-title {
  margin: 18px 0 0;
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
  max-width: 600px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.od-refresh {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 150ms ease, background 150ms ease, color 150ms ease, transform 150ms ease;
}
.od-refresh:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--ref-ink) 28%, transparent);
  color: var(--ref-ink);
  transform: translateY(-1px);
}
.od-refresh:disabled { opacity: 0.5; cursor: not-allowed; }
.od-refresh-icon { font-size: 15px; }

/* Summary */
.od-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 8px;
}
.od-stat {
  display: flex;
  flex-direction: column;
  padding: 22px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.od-stat:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.od-stat dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.od-stat dt {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ref-muted);
}

/* Active stay banner */
.od-active {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 20px;
  margin-top: 20px;
  padding: 16px 20px;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 26%, var(--ref-line));
  border-radius: 16px;
  background: color-mix(in srgb, var(--ref-brand) 7%, var(--ref-surface));
}
.od-active-copy {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}
.od-active-pulse {
  position: relative;
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--ref-brand);
}
.od-active-pulse::after {
  content: '';
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 55%, transparent);
  animation: od-pulse 2s ease-out infinite;
}
.od-active-text { min-width: 0; }
.od-active-label {
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ref-brand-deep);
}
.od-active-name {
  margin-top: 4px;
  font-family: var(--ref-font-display);
  font-size: 16px;
  font-weight: 500;
  color: var(--ref-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.od-active-meta {
  margin-top: 3px;
  font-size: 12px;
  color: var(--ref-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.od-active-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 16px;
  border-radius: 10px;
  background: var(--ref-brand);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, transform 150ms ease;
}
.od-active-btn:hover {
  background: var(--ref-brand-deep);
  transform: translateY(-1px);
}
@keyframes od-pulse {
  0% { opacity: 0.8; transform: scale(0.6); }
  70% { opacity: 0; transform: scale(1.4); }
  100% { opacity: 0; }
}

/* Controls: tabs + search */
.od-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 36px;
}
.od-tabs {
  display: flex;
  gap: 8px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}
.od-tabs::-webkit-scrollbar { display: none; }
.od-chip {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  height: 42px;
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
.od-chip:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  background: var(--ref-sand);
}
.od-chip.active {
  background: var(--ref-ink);
  border-color: var(--ref-ink);
  color: var(--ref-cream);
}
.od-chip-count {
  font-size: 11px;
  opacity: 0.6;
  font-variant-numeric: tabular-nums;
}
.od-search {
  position: relative;
  width: 240px;
  flex-shrink: 0;
}
.od-search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ref-muted);
  font-size: 15px;
}
.od-search input {
  width: 100%;
  height: 42px;
  padding: 0 40px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}
.od-search input::placeholder { color: var(--ref-muted); }
.od-search input:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); }
.od-search input:focus {
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  outline: none;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.od-clear {
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
.od-clear:hover {
  color: var(--ref-ink);
  background: var(--ref-sand);
}

/* List area */
.od-list-area { margin-top: 28px; }
.od-list { display: grid; gap: 18px; }

/* Skeleton */
.od-skeleton {
  height: 220px;
  border-radius: 18px;
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: od-shimmer 1.3s linear infinite;
}

/* Empty */
.od-empty {
  padding: 72px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  text-align: center;
}
.od-empty-icon { font-size: 40px; margin-bottom: 14px; }
.od-empty h3 {
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.od-empty p {
  margin-top: 10px;
  font-size: 14px;
  color: var(--ref-muted);
}
.od-empty-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  margin-top: 22px;
  padding: 0 22px;
  border-radius: 11px;
  background: var(--ref-brand);
  color: #fff;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, transform 150ms ease;
}
.od-empty-btn:hover {
  background: var(--ref-brand-deep);
  transform: translateY(-1px);
}

/* Footer note */
.od-footer-note {
  margin-top: 28px;
  text-align: center;
  font-size: 12px;
  color: var(--ref-muted);
}
.od-num {
  font-variant-numeric: tabular-nums;
  color: var(--ref-ink-soft);
}

@keyframes od-shimmer {
  to { background-position: -200% 0; }
}

@media (max-width: 900px) {
  .od-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 560px) {
  .od-shell { padding: 0 16px; }
  .od-head { padding: 30px 0 24px; }
  .od-sub { font-size: 13.5px; }
  .od-summary { gap: 10px; }
  .od-stat { padding: 18px 18px; }
  .od-stat dd { font-size: 26px; }
  .od-controls { align-items: stretch; }
  .od-search { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  .od-skeleton { animation: none; }
  .od-active-pulse::after { animation: none; }
}
</style>
