<template>
  <div class="orders-page">
    <PageHero title="我的订单" subtitle="查看寄养进度、交接信息、沟通记录和服务报告" />

    <section class="summary-band" aria-label="订单概览">
      <div class="summary-item">
        <span>全部订单</span>
        <strong>{{ orders.length }}</strong>
      </div>
      <div class="summary-item">
        <span>待处理</span>
        <strong>{{ statusCount('pending') + statusCount('paid') + statusCount('confirmed') }}</strong>
      </div>
      <div class="summary-item">
        <span>服务中</span>
        <strong>{{ statusCount('delivered') + statusCount('received') + statusCount('in_progress') }}</strong>
      </div>
      <div class="summary-item">
        <span>已完成</span>
        <strong>{{ statusCount('completed') }}</strong>
      </div>
    </section>

    <div class="toolbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['btn', activeTab === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        type="button"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
      <button class="btn btn-outline btn-sm refresh-btn" type="button" @click="loadOrders" :disabled="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredOrders.length === 0" class="empty-state">
      <div class="icon">📋</div>
      <h3>暂无订单</h3>
      <p>选择寄养服务后，可以在这里查看订单进度和交接信息。</p>
      <router-link to="/dashboard" class="btn btn-primary">浏览服务</router-link>
    </div>

    <div v-else class="order-list">
      <OrderCard v-for="o in filteredOrders" :key="o.id_wsh" :order="o" :now-ms="nowMs" :processing="processingOrderId === o.id_wsh" @cancel="handleCancel(o)" @pay="method => handlePay(o, method)" @deliver="handleDeliver(o)" @review="openReview(o)" @tip="showTip = o" @viewDetail="handleViewDetail(o)" />
    </div>

    <button class="btn btn-primary" @click="showCreateDialog = true" style="margin: 16px 0">创建订单</button>

    <CreateOrderDialog
      :visible="showCreateDialog"
      :initial-service-name="createServiceName"
      :initial-service-id="createServiceId"
      :initial-price="createPrice"
      :initial-merchant-id="createMerchantId"
      :initial-keeper-id="createKeeperId"
      @close="showCreateDialog = false; router.replace({ query: {} })"
      @created="onOrderCreated"
    />

    <TipDialog :visible="!!showTip" :order="showTip" @close="showTip = null" @tipped="onTipped" />

    <ReviewDialog :visible="!!showReview" :order="showReview" :done-types="reviewedDims" @close="showReview = null" @reviewed="onReviewed" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import OrderCard from '@/components/order/OrderCard.vue'
import CreateOrderDialog from '@/components/order/CreateOrderDialog.vue'
import TipDialog from '@/components/order/TipDialog.vue'
import ReviewDialog from '@/components/order/ReviewDialog.vue'
import { getOrders, cancelOrder as apiCancelOrder, confirmDelivered } from '@/api/order'
import { createPayment, executePayment } from '@/api/payment'
import { createTip } from '@/api/wallet'
import { createRating, getMyRatingsByOrder } from '@/api/rating'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, hasExpiredPaymentTimeout } from '@/utils/orderPaymentTimeout'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')
const showCreateDialog = ref(false)
const showTip = ref(null)
const showReview = ref(null)
const reviewedDims = ref([])
const createServiceName = ref('')
const createServiceId = ref('')
const createPrice = ref('')
const createMerchantId = ref('')
const createKeeperId = ref('')
const nowMs = ref(Date.now())
const processingOrderId = ref(null)
let countdownTimer = null
let nextPaymentTimeoutRefreshAt = 0

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待付款' },
  { key: 'paid', label: '已支付' },
  { key: 'confirmed', label: '待送达' },
  { key: 'delivered', label: '已送达' },
  { key: 'received', label: '已接收' },
  { key: 'in_progress', label: '服务中' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
]

const filteredOrders = computed(() => activeTab.value === 'all'
  ? orders.value
  : orders.value.filter(order => order.status_wsh === activeTab.value))

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
  if (route.query.create === 'true') openCreateFromQuery(route.query)
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

watch(() => route.query, query => {
  if (query.create === 'true' && !showCreateDialog.value) openCreateFromQuery(query)
})

async function loadOrders() {
  loading.value = true
  try {
    const res = await getOrders()
    if (res.code === 200) orders.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    appStore.addToast(e?.message || '订单加载失败', 'error')
  } finally {
    loading.value = false
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

async function openCreateFromQuery(query) {
  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!ok) return
  createServiceName.value = query.serviceName || ''
  createServiceId.value = query.serviceId || ''
  createPrice.value = query.price || ''
  createMerchantId.value = query.merchantId || ''
  createKeeperId.value = query.keeperId || ''
  showCreateDialog.value = true
}

function onOrderCreated(data) {
  appStore.addToast(`下单成功，交接码：${data?.handover_code_wsh || '待生成'}`, 'success')
  showCreateDialog.value = false
  router.replace({ query: {} })
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

function statusCount(status) {
  return orders.value.filter(order => order.status_wsh === status).length
}
</script>

<style scoped>
.orders-page { display: block; }

.summary-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.summary-item {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 14px 16px;
}
.summary-item span {
  display: block;
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.summary-item strong {
  display: block;
  margin-top: 4px;
  font-size: 24px;
  line-height: 1;
}

.toolbar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.refresh-btn { margin-left: auto; }
.order-list { display: grid; gap: 16px; }

@media (max-width: 760px) {
  .summary-band { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .refresh-btn { margin-left: 0; }
}

@media (max-width: 520px) {
  .summary-band { grid-template-columns: 1fr; }
}
</style>
