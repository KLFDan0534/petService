<template>
  <div class="order-detail-page">
    <div v-if="store.loading" class="loading">加载中...</div>

    <div v-else-if="store.error" class="empty-state">
      <div class="icon">!</div>
      <h3>加载失败</h3>
      <p>{{ store.error }}</p>
      <button class="btn btn-primary" type="button" @click="init">重新加载</button>
    </div>

    <div v-else-if="store.order" class="detail-shell">
      <button class="btn btn-outline btn-sm back-btn" type="button" @click="goBack">← 返回订单</button>

      <section class="detail-hero">
        <div class="hero-main">
          <span class="order-no">{{ store.order.order_no_wsh || `#${store.order.id_wsh}` }}</span>
          <h1>{{ store.order.service_name_wsh || '宠物寄养订单' }}</h1>
          <p>
            {{ formatDate(store.order.start_date_wsh) }} 至 {{ formatDate(store.order.end_date_wsh) }}
            <span v-if="orderBillingText"> · {{ orderBillingText }}</span>
          </p>
        </div>
        <div class="hero-side">
          <span :class="['badge', statusBadge(store.order.status_wsh)]">{{ statusLabel(store.order.status_wsh) }}</span>
          <strong>¥{{ money(store.order.final_amount_wsh || store.order.total_amount_wsh) }}</strong>
          <small>实付金额</small>
          <small v-if="store.order.discount_wsh && Number(store.order.discount_wsh) > 0" class="discount-note">
            已优惠 ¥{{ money(store.order.discount_wsh) }}
          </small>
          <button
            v-if="canCreateComplaint"
            class="btn btn-outline btn-sm complaint-link"
            type="button"
            @click="openComplaint"
          >
            发起投诉
          </button>
        </div>
      </section>

      <div class="quick-grid">
        <div>
          <span>商家</span>
          <strong>{{ store.order.merchant_name_wsh || '-' }}</strong>
        </div>
        <div>
          <span>看护人</span>
          <strong>{{ store.order.keeper_name_wsh || '-' }}</strong>
        </div>
        <div>
          <span>送达时间</span>
          <strong>{{ formatTime(store.order.delivery_time_wsh) }}</strong>
        </div>
        <div>
          <span>接回时间</span>
          <strong>{{ formatTime(store.order.pickup_time_wsh) }}</strong>
        </div>
      </div>

      <div class="detail-layout">
        <main class="detail-main">
          <nav class="detail-tabs" aria-label="订单详情标签">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :class="['btn', 'btn-sm', activeTab === tab.key ? 'btn-primary' : 'btn-outline']"
              type="button"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </nav>

          <OrderOverviewTab v-if="activeTab === 'overview'" :order="store.order" />
          <OrderTimelineTab
            v-else-if="activeTab === 'timeline'"
            :items="store.timeline"
            :daily-status="store.dailyStatus"
            :loading="store.timelineLoading"
            :has-more="store.timelineHasMore"
            @load-more="store.loadMoreTimeline(orderId)"
          />
          <OrderChatTab
            v-else-if="activeTab === 'chat'"
            :messages="store.messages"
            :loading="store.messageLoading"
            :sending="messageSending"
            :send-error="messageSendError"
            :order-id="orderId"
            @send="handleSendMessage"
            @refresh="store.fetchMessages(orderId)"
          />
          <OrderReportTab
            v-else
            :reports="store.reports"
            :loading="store.reportLoading"
            :order="store.order"
            @regenerate="handleRegenerate"
          />
        </main>

        <aside class="detail-sidebar">
          <OrderSidebar :order="store.order" :processing="processingOrder" @pay="handlePay" @cancel="handleCancel" />
        </aside>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="icon">?</div>
      <h3>订单不存在</h3>
      <router-link to="/orders" class="btn btn-primary">返回订单</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { useOrderDetailStore } from '@/stores/orderDetail'
import { sendMessage, sendMessageWithFile } from '@/api/orderDetail'
import OrderOverviewTab from '@/components/order/detail/OrderOverviewTab.vue'
import OrderTimelineTab from '@/components/order/detail/OrderTimelineTab.vue'
import OrderChatTab from '@/components/order/detail/OrderChatTab.vue'
import OrderReportTab from '@/components/order/detail/OrderReportTab.vue'
import OrderSidebar from '@/components/order/detail/OrderSidebar.vue'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, isPaymentTimeoutExpired } from '@/utils/orderPaymentTimeout'
import { billingText } from '@/domain/BookingUnit'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const store = useOrderDetailStore()
const orderId = ref(Number(route.params.id))
const activeTab = ref(route.query.tab === 'chat' ? 'chat' : 'overview')
const processingOrder = ref(false)
const messageSending = ref(false)
const messageSendError = ref('')
let chatEventSource = null
let chatReconnectTimer = null
let chatReconnectDelay = 1000
let paymentTimeoutTimer = null
let nextPaymentTimeoutRefreshAt = 0

const tabs = [
  { key: 'overview', label: '概览' },
  { key: 'timeline', label: '服务动态' },
  { key: 'chat', label: '在线聊天' },
  { key: 'report', label: 'AI报告' },
]

const canCreateComplaint = computed(() => {
  return Number(store.order?.owner_id_wsh) === Number(authStore.user?.id_wsh)
})

const statusMap = {
  pending: '待付款',
  paid: '已支付',
  confirmed: '待送达',
  delivered: '已送达',
  received: '已接收',
  in_progress: '服务中',
  completed: '已完成',
  cancelled: '已取消',
  refunding: '退款中',
  refunded: '已退款',
}

function statusLabel(status) {
  return statusMap[status] || status || '-'
}

function statusBadge(status) {
  return {
    pending: 'badge-warning',
    paid: 'badge-info',
    confirmed: 'badge-info',
    delivered: 'badge-info',
    received: 'badge-info',
    in_progress: 'badge-info',
    completed: 'badge-success',
    cancelled: 'badge-danger',
    refunded: 'badge-success',
    refunding: 'badge-warning',
  }[status] || 'badge-info'
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/orders')
}

function openComplaint() {
  const order = store.order
  if (!order) return
  const targetType = order.keeper_id_wsh ? 'keeper' : 'merchant'
  const targetId = order.keeper_id_wsh || order.merchant_id_wsh || ''
  router.push({
    name: 'Complaints',
    query: {
      orderId: order.id_wsh,
      orderNo: order.order_no_wsh || '',
      targetType,
      targetId,
    },
  })
}

async function init() {
  store.$reset()
  messageSendError.value = ''
  await Promise.all([
    store.fetchOrderDetail(orderId.value),
    store.fetchTimeline(orderId.value),
    store.fetchMessages(orderId.value),
    store.fetchReports(orderId.value),
  ])
}

async function handleSendMessage({ content, file, done }) {
  if (messageSending.value) return
  const text = content?.trim() || ''
  if (!text && !file) return
  messageSending.value = true
  messageSendError.value = ''
  let success = false
  try {
    if (file) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('content_wsh', text)
      formData.append('type_wsh', 'image')
      const res = await sendMessageWithFile(orderId.value, formData)
      success = res.code === 200
      if (!success) messageSendError.value = res.message || '图片发送失败'
    } else {
      const res = await sendMessage(orderId.value, { content_wsh: text, type_wsh: 'text' })
      success = res.code === 200
      if (!success) messageSendError.value = res.message || '消息发送失败'
    }
    if (success) {
      done?.(true)
      await store.fetchMessages(orderId.value)
    } else {
      done?.(false)
      appStore.addToast(messageSendError.value, 'error')
    }
  } catch (error) {
    messageSendError.value = error?.message || '发送失败，请稍后重试'
    done?.(false)
    appStore.addToast(messageSendError.value, 'error')
  } finally {
    messageSending.value = false
  }
}

function openChatEventStream() {
  closeChatEventStream()
  const token = localStorage.getItem('token')
  if (!token || typeof EventSource === 'undefined') return
  chatEventSource = new EventSource(`/api/chat-events/stream?token=${encodeURIComponent(token)}`)
  chatEventSource.addEventListener('connected', () => {
    chatReconnectDelay = 1000
  })
  chatEventSource.addEventListener('chat-message', event => {
    try {
      const data = JSON.parse(event.data)
      if (Number(data.order_id_wsh) === Number(orderId.value)) {
        store.fetchMessages(orderId.value)
      }
    } catch {
      // Ignore malformed SSE events.
    }
  })
  chatEventSource.onerror = () => {
    closeChatEventStream()
    scheduleChatReconnect()
  }
}

function closeChatEventStream() {
  if (chatReconnectTimer) {
    window.clearTimeout(chatReconnectTimer)
    chatReconnectTimer = null
  }
  if (chatEventSource) {
    chatEventSource.close()
    chatEventSource = null
  }
}

function scheduleChatReconnect() {
  if (chatReconnectTimer) return
  const delay = chatReconnectDelay
  chatReconnectDelay = Math.min(chatReconnectDelay * 2, 15000)
  chatReconnectTimer = window.setTimeout(() => {
    chatReconnectTimer = null
    openChatEventStream()
  }, delay)
}

function openPaymentTimeoutWatcher() {
  closePaymentTimeoutWatcher()
  paymentTimeoutTimer = window.setInterval(() => {
    refreshExpiredPaymentOrder()
  }, 1000)
}

function closePaymentTimeoutWatcher() {
  if (paymentTimeoutTimer) {
    window.clearInterval(paymentTimeoutTimer)
    paymentTimeoutTimer = null
  }
}

function refreshExpiredPaymentOrder() {
  const order = store.order
  const now = Date.now()
  if (
    now < nextPaymentTimeoutRefreshAt
    || !order
    || store.loading
    || order.status_wsh !== 'pending'
    || !isPaymentTimeoutExpired(order, now)
  ) {
    return
  }
  nextPaymentTimeoutRefreshAt = now + PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS
  void store.fetchOrderDetail(orderId.value)
}

async function handlePay(method = 'balance') {
  if (processingOrder.value) return
  processingOrder.value = true
  try {
    const result = await store.pay(orderId.value, method)
    if (result.success) appStore.addToast('支付成功', 'success')
    else appStore.addToast(result.message || '支付失败', 'error')
  } catch (error) {
    appStore.addToast(error?.message || '支付失败', 'error')
    await store.fetchOrderDetail(orderId.value)
  } finally {
    processingOrder.value = false
  }
}

async function handleCancel() {
  if (processingOrder.value) return
  processingOrder.value = true
  try {
    const result = await store.cancel(orderId.value)
    if (result.success) appStore.addToast('订单已取消', 'success')
    else appStore.addToast(result.message || '取消失败', 'error')
  } catch (error) {
    appStore.addToast(error?.message || '取消失败', 'error')
    await store.fetchOrderDetail(orderId.value)
  } finally {
    processingOrder.value = false
  }
}

async function handleRegenerate() {
  const result = await store.regenerate(orderId.value)
  if (result?.code === 200) appStore.addToast('AI报告已重新生成', 'success')
  else appStore.addToast(result?.message || '生成失败', 'error')
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

const orderBillingText = computed(() => billingText(store.order ? {
  billing_unit_wsh: store.order.billing_unit_wsh,
  quantity_wsh: store.order.quantity_wsh,
  duration_minutes_wsh: store.order.duration_minutes_wsh,
  days_wsh: store.order.days_wsh,
} : null))

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

watch(() => route.params.id, (nextId) => {
  if (nextId) {
    orderId.value = Number(nextId)
    activeTab.value = route.query.tab === 'chat' ? 'chat' : 'overview'
    init()
    openChatEventStream()
  }
})

watch(activeTab, tab => {
  if (tab === 'chat' && orderId.value) {
    store.fetchMessages(orderId.value)
  }
})

onMounted(() => {
  init()
  openChatEventStream()
  openPaymentTimeoutWatcher()
})
onUnmounted(() => {
  closeChatEventStream()
  closePaymentTimeoutWatcher()
  store.$reset()
})
</script>

<style scoped>
.order-detail-page {
  min-height: 80vh;
}
.detail-shell {
  display: grid;
  gap: 18px;
}
.back-btn {
  justify-self: start;
}
.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: stretch;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  color: var(--color-on-primary);
  border-radius: var(--radius-xl);
  padding: 28px;
  box-shadow: var(--shadow-md);
}
.hero-main {
  min-width: 0;
}
.hero-main h1 {
  margin: 6px 0 8px;
  font-size: 28px;
  line-height: 1.25;
}
.hero-main p {
  margin: 0;
  opacity: .85;
}
.order-no {
  font-family: var(--font-mono);
  font-size: 12px;
  opacity: .8;
}
.hero-side {
  min-width: 170px;
  background: rgba(255,255,255,.24);
  border: 1px solid rgba(255,255,255,.38);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 4px;
}
.hero-side strong {
  font-size: 28px;
  line-height: 1.1;
}
.hero-side small {
  opacity: .75;
}
.discount-note {
  font-size: 11px;
}
.complaint-link {
  margin-top: 8px;
  background: rgba(255,255,255,.16);
  color: var(--color-on-primary);
  border-color: rgba(255,255,255,.56);
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.quick-grid > div {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  min-width: 0;
}
.quick-grid span {
  display: block;
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.quick-grid strong {
  display: block;
  margin-top: 3px;
  font-size: 14px;
  overflow-wrap: anywhere;
}
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
  align-items: start;
}
.detail-main {
  min-width: 0;
  display: grid;
  gap: 16px;
}
.detail-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-sidebar {
  min-width: 0;
}
@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .detail-hero {
    flex-direction: column;
    padding: 22px;
  }
  .hero-main h1 {
    font-size: 22px;
  }
  .hero-side {
    min-width: 0;
  }
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
