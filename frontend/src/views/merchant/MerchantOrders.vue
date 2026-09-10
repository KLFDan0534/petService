<template>
  <div class="merchant-orders-page">
    <div class="page-head">
      <div>
        <h2>订单管理</h2>
        <p>查看当前商户收到的寄养服务订单，按状态跟进履约进度。</p>
      </div>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading" @click="loadOrders">
        刷新
      </button>
    </div>

    <section class="summary-grid" aria-label="订单概览">
      <div class="summary-item">
        <span>全部订单</span>
        <strong>{{ orders.length }}</strong>
      </div>
      <div class="summary-item">
        <span>待接单</span>
        <strong>{{ countByStatus('paid') }}</strong>
      </div>
      <div class="summary-item">
        <span>服务中</span>
        <strong>{{ activeCount }}</strong>
      </div>
      <div class="summary-item">
        <span>已完成</span>
        <strong>{{ countByStatus('completed') }}</strong>
      </div>
    </section>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['btn', 'btn-sm', activeTab === tab.key ? 'btn-primary' : 'btn-outline']"
        type="button"
        @click="activeTab = tab.key"
      >
        <span>{{ tab.label }}</span>
        <span class="tab-count">{{ countByTab(tab.key) }}</span>
      </button>
    </div>

    <div v-if="loading" class="state-block">订单加载中...</div>
    <div v-else-if="error" class="state-block state-block--error">
      <strong>订单加载失败</strong>
      <p>{{ error }}</p>
      <button class="btn btn-sm btn-primary" type="button" @click="loadOrders">重新加载</button>
    </div>
    <div v-else-if="filteredOrders.length === 0" class="state-block">
      当前筛选下暂无订单。
    </div>

    <div v-else class="orders-list">
      <article v-for="order in filteredOrders" :key="order.id_wsh || order.order_no_wsh" class="order-card">
        <header class="order-card__head">
          <div class="order-title">
            <span class="order-no">{{ order.order_no_wsh || `#${order.id_wsh}` }}</span>
            <h3>{{ order.service_name_wsh || '寄养服务订单' }}</h3>
          </div>
          <span :class="['badge', statusBadge(order.status_wsh)]">
            {{ statusLabel(order.status_wsh) }}
          </span>
        </header>
        <div v-if="showPaymentCountdown(order)" class="payment-countdown">
          <span>支付剩余</span>
          <strong>{{ paymentCountdownText(order) }}</strong>
          <small>未支付订单暂不能接单</small>
        </div>

        <dl class="order-facts">
          <div>
            <dt>用户</dt>
            <dd>{{ order.owner_name_wsh || '-' }}</dd>
          </div>
          <div>
            <dt>宠物</dt>
            <dd>{{ order.pet_name_wsh || `#${order.pet_id_wsh || '-'}` }}</dd>
          </div>
          <div>
            <dt>看护人</dt>
            <dd>{{ order.keeper_name_wsh || `#${order.keeper_id_wsh || '-'}` }}</dd>
          </div>
          <div>
            <dt>服务日期</dt>
            <dd>{{ formatDate(order.start_date_wsh) }} 至 {{ formatDate(order.end_date_wsh) }}</dd>
          </div>
          <div>
            <dt>计费</dt>
            <dd>{{ billingText(order) }}</dd>
          </div>
          <div>
            <dt>下单时间</dt>
            <dd>{{ formatDateTime(order.created_at_wsh, { fallback: '-' }) }}</dd>
          </div>
        </dl>

        <div class="money-row">
          <span>实付金额</span>
          <strong>￥{{ money(order.final_amount_wsh || order.total_amount_wsh) }}</strong>
          <small v-if="order.price_per_day_wsh">￥{{ money(order.price_per_day_wsh) }}/天</small>
        </div>

        <p v-if="order.remark_wsh" class="remark">{{ order.remark_wsh }}</p>

        <footer class="order-actions">
          <button class="btn btn-sm btn-outline" type="button" @click="goDetail(order)">
            查看详情
          </button>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { getMerchantOrders } from '@/api/order'
import { OrderStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { formatDate, formatDateTime, formatMoney as money } from '@/utils/format'
import { billingText } from '@/domain/BookingUnit'
import {
  PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS,
  formatPaymentTimeoutRemaining,
  getPaymentTimeoutRemaining,
  isPaymentTimeoutExpired,
} from '@/utils/orderPaymentTimeout'

const router = useRouter()
const appStore = useAppStore()

const orders = ref([])
const loading = ref(false)
const error = ref('')
const activeTab = ref('all')
const nowMs = ref(Date.now())
let countdownTimer = null
let nextPaymentTimeoutRefreshAt = 0

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待支付' },
  { key: 'paid', label: '待接单' },
  { key: 'active', label: '服务中' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
  { key: 'refund', label: '退款' },
]

const tabStatusMap = {
  pending: ['pending'],
  paid: ['paid'],
  active: ['confirmed', 'delivered', 'received', 'in_progress'],
  completed: ['completed'],
  cancelled: ['cancelled'],
  refund: ['refunding', 'refunded'],
}

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  const statuses = tabStatusMap[activeTab.value] || [activeTab.value]
  return orders.value.filter(order => statuses.includes(orderStatus(order)))
})

const activeCount = computed(() =>
  ['confirmed', 'delivered', 'received', 'in_progress'].reduce((sum, status) => sum + countByStatus(status), 0)
)

onMounted(() => {
  countdownTimer = window.setInterval(() => {
    nowMs.value = Date.now()
    refreshExpiredPaymentOrders()
  }, 1000)
  loadOrders()
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const res = await getMerchantOrders()
    if (res.code !== 200) {
      throw new Error(res.message || '接口返回异常')
    }
    const data = res.data
    orders.value = (Array.isArray(data) ? data : (data?.list || [])).map(order => ({
      ...order,
      status_wsh: orderStatus(order),
    }))
  } catch (err) {
    error.value = err?.message || '无法获取商家订单'
    appStore.addToast(error.value, 'error')
  } finally {
    loading.value = false
  }
}

function refreshExpiredPaymentOrders() {
  if (loading.value || nowMs.value < nextPaymentTimeoutRefreshAt) {
    return
  }
  const hasExpired = orders.value.some(order => orderStatus(order) === 'pending' && isPaymentTimeoutExpired(order, nowMs.value))
  if (!hasExpired) {
    return
  }
  nextPaymentTimeoutRefreshAt = nowMs.value + PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS
  void loadOrders()
}

function countByStatus(status) {
  return orders.value.filter(order => orderStatus(order) === status).length
}

function countByTab(key) {
  if (key === 'all') return orders.value.length
  const statuses = tabStatusMap[key] || [key]
  return orders.value.filter(order => statuses.includes(orderStatus(order))).length
}

function orderStatus(order) {
  const raw = String(order?.status_wsh ?? order?.status ?? '').trim()
  const normalized = raw.toLowerCase()
  const aliasMap = {
    '待付款': 'pending',
    '待支付': 'pending',
    '已支付': 'paid',
    '待接单': 'paid',
    '已确认': 'confirmed',
    '待送达': 'confirmed',
    '已送达': 'delivered',
    '已接收': 'received',
    '服务中': 'in_progress',
    '进行中': 'in_progress',
    '已完成': 'completed',
    '已取消': 'cancelled',
    '退款中': 'refunding',
    '已退款': 'refunded',
  }
  return aliasMap[raw] || normalized
}

function statusLabel(status) {
  return getStatusLabel(OrderStatus, String(status || '').trim().toLowerCase())
}

function statusBadge(status) {
  return getStatusBadge(OrderStatus, String(status || '').trim().toLowerCase())
}

function showPaymentCountdown(order) {
  return orderStatus(order) === 'pending' && getPaymentTimeoutRemaining(order, nowMs.value) != null
}

function paymentCountdownText(order) {
  return formatPaymentTimeoutRemaining(getPaymentTimeoutRemaining(order, nowMs.value))
}

function goDetail(order) {
  if (!order?.id_wsh) return
  router.push({ name: 'OrderDetail', params: { id: order.id_wsh } })
}
</script>

<style scoped>
.merchant-orders-page {
  display: grid;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.page-head > div {
  min-width: 0;
}

.page-head h2 {
  margin: 0;
  font-size: 18px;
}

.page-head p {
  margin: 2px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

.summary-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-inline);
  background: var(--color-card);
  padding: 8px 12px;
  min-width: 0;
}

.summary-item span {
  display: block;
  color: var(--color-muted-foreground);
  font-size: 11px;
}

.summary-item strong {
  display: block;
  margin-top: 2px;
  font-size: 16px;
  line-height: 1;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

.tabs .btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 1 auto;
  max-width: 100%;
  white-space: nowrap;
}

.tab-count {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.08);
  font-size: 12px;
  line-height: 20px;
}

.btn-primary .tab-count {
  background: rgba(255, 255, 255, 0.22);
}

.state-block {
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  color: var(--color-muted-foreground);
  background: var(--color-card);
}

.state-block--error {
  color: var(--color-destructive);
}

.state-block--error p {
  margin: 8px 0 14px;
}

.orders-list {
  display: grid;
  gap: 8px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.order-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-inline);
  background: var(--color-card);
  padding: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}

.order-card__head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
  min-width: 0;
}

.order-title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
}

.order-title h3 {
  margin: 2px 0 0;
  font-size: 14px;
  line-height: 1.3;
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.order-no {
  display: block;
  color: var(--color-muted-foreground);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  max-width: 100%;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.order-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px 12px;
  margin-top: 8px;
  min-width: 0;
}

.order-facts div {
  display: flex;
  gap: 4px;
  align-items: baseline;
  min-width: 0;
}

.order-facts dt {
  color: var(--color-muted-foreground);
  font-size: 11px;
  flex-shrink: 0;
}

.order-facts dd {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
  font-size: 13px;
}

.money-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: var(--radius-inline);
  background: var(--color-muted);
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.money-row span,
.money-row small {
  color: var(--color-muted-foreground);
  font-size: 12px;
}

.money-row strong {
  color: var(--color-destructive);
  font-size: 16px;
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.remark {
  margin: 8px 0 0;
  color: var(--color-muted-foreground);
  white-space: pre-wrap;
  font-size: 13px;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border);
}

@media (max-width: 760px) {
  .page-head {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .money-row {
    flex-wrap: wrap;
  }
}
</style>
