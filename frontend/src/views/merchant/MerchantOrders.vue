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
        <strong>{{ totalCount }}</strong>
      </div>
      <div class="summary-item">
        <span>已支付待处理</span>
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

    <div class="page-size-row">
      <label>
        <span>每页</span>
        <select v-model.number="pageSize" :disabled="loading" @change="changePageSize">
          <option v-for="n in pageSizes" :key="n" :value="n">{{ n }} 条</option>
        </select>
      </label>
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
        <!-- 待支付的单子顶上挂个倒计时，时间一到后端会把订单关掉 -->
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
            <dd>{{ order.keeper_name_wsh || (order.keeper_id_wsh ? `#${order.keeper_id_wsh}` : '待分配') }}</dd>
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
          <small v-if="order.unit_price_wsh">￥{{ money(order.unit_price_wsh) }}/{{ unitLabel(order.billing_unit_wsh) }}</small>
        </div>

        <p v-if="order.remark_wsh" class="remark">{{ order.remark_wsh }}</p>

        <!-- 待分配订单可以就地指派；其余动作仍进详情页做 -->
        <footer class="order-actions">
          <template v-if="isReassignable(order)">
            <select v-model="assignSelection[assignKey(order)]" class="assign-select">
              <option value="">改派给…</option>
              <option
                v-for="keeper in reassignCandidates(order)"
                :key="keeper.id_wsh"
                :value="String(keeper.id_wsh)"
              >
                {{ keeper.name_wsh }}（在住 {{ keeper.current_pets_wsh || 0 }}/{{ keeper.max_pets_wsh || '-' }}）
              </option>
            </select>
            <button
              class="btn btn-sm btn-primary"
              type="button"
              :disabled="assigningOrderNo === order.order_no_wsh || !assignSelection[assignKey(order)]"
              @click="handleAssign(order)"
            >{{ assigningOrderNo === order.order_no_wsh ? '改派中...' : '改派' }}</button>
          </template>
          <template v-else-if="isUnassigned(order)">
            <select v-model="assignSelection[assignKey(order)]" class="assign-select">
              <option value="">选择看护人</option>
              <option v-for="keeper in assignableKeepers" :key="keeper.id_wsh" :value="String(keeper.id_wsh)">
                {{ keeper.name_wsh }}（在住 {{ keeper.current_pets_wsh || 0 }}/{{ keeper.max_pets_wsh || '-' }}）
              </option>
            </select>
            <button
              class="btn btn-sm btn-primary"
              type="button"
              :disabled="assigningOrderNo === order.order_no_wsh || !assignSelection[assignKey(order)]"
              @click="handleAssign(order)"
            >{{ assigningOrderNo === order.order_no_wsh ? '指派中...' : '指派' }}</button>
          </template>
          <button class="btn btn-sm btn-outline" type="button" @click="goDetail(order)">
            查看详情
          </button>
        </footer>
      </article>
    </div>

    <div v-if="!loading && !error" class="scroll-pager">
      <span>已加载 {{ filteredOrders.length }} / 共 {{ totalCount }} 单</span>
      <button
        v-if="hasMore"
        class="btn btn-sm btn-primary"
        type="button"
        :disabled="loadingMore"
        @click="loadMore"
      >
        {{ loadingMore ? '加载中…' : '加载更多' }}
      </button>
      <span v-else>已经到底了</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { assignOrder, scrollMerchantOrders, getMerchantOrderStats } from '@/api/order'
import { getKeepersByMerchant } from '@/api/keeper'
import { OrderStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { formatDate, formatDateTime, formatMoney as money } from '@/utils/format'
import { billingText, unitLabel } from '@/domain/BookingUnit'
import {
  PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS,
  formatPaymentTimeoutRemaining,
  getPaymentTimeoutRemaining,
  isPaymentTimeoutExpired,
} from '@/utils/orderPaymentTimeout'

const router = useRouter()
const appStore = useAppStore()

// 订单列表，加上加载中、出错、正常三种页面状态
const orders = ref([])
const loading = ref(false)
const error = ref('')
const activeTab = ref('all')
const nowMs = ref(Date.now())   // 当前时间，倒计时靠它走

// 游标分页状态
const cursor = ref('')
const hasMore = ref(false)
const loadingMore = ref(false)
const pageSize = ref(20)
const pageSizes = [20, 50, 100]
// 服务端全量状态计数：页签数字不能在前端算，否则只覆盖已加载的一页
const statusCounts = ref({})
// 待分配订单的指派选择：order_no -> keeperId
const assignSelection = reactive({})
const assignableKeepers = ref([])
const assigningOrderNo = ref('')
let countdownTimer = null
let nextPaymentTimeoutRefreshAt = 0   // 下次允许重新拉列表的时间点

// 顶部页签，key 拿去和下面 tabStatusMap 里的状态分组对上
const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待支付' },
  { key: 'unassigned', label: '待分配' },
  { key: 'active', label: '服务中' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
  { key: 'refund', label: '退款' },
]

// 一个页签可能对着好几个后端状态，比如"服务中"把已确认一路到进行中都算进来
const tabStatusMap = {
  pending: ['pending'],
  paid: ['paid'],
  active: ['confirmed', 'received', 'in_progress'],
  completed: ['completed'],
  cancelled: ['cancelled'],
  refund: ['refunding', 'refunded'],
}

// 状态筛选（含「待分配」）已下推到服务端，当前页就是筛选结果
const filteredOrders = computed(() => orders.value)

// 概览里的"服务中"是这四种状态加起来的总数
const activeCount = computed(() =>
  ['confirmed', 'received', 'in_progress'].reduce((sum, status) => sum + countByStatus(status), 0)
)

// 每秒把当前时间往前推一格，支付倒计时才会走字，顺便看看有没有订单超时要刷新
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

// 拉商家收到的订单，顺手把每条的状态整理成统一的英文小写
async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const params = { size: pageSize.value }
    const status = currentStatusParam()
    if (status) params.status = status

    // 列表与计数并行：计数走服务端全量，页签数字才准
    const [listRes, statsRes] = await Promise.all([
      scrollMerchantOrders(params),
      getMerchantOrderStats(),
    ])
    if (listRes.code !== 200) {
      throw new Error(listRes.message || '接口返回异常')
    }
    const payload = listRes.data || {}
    orders.value = (Array.isArray(payload.list_wsh) ? payload.list_wsh : []).map(order => ({
      ...order,
      status_wsh: orderStatus(order),
    }))
    cursor.value = payload.next_cursor_wsh || ''
    hasMore.value = Boolean(payload.has_more_wsh)
    if (statsRes?.code === 200) {
      statusCounts.value = statsRes.data || {}
    }
    // 指派下拉需要本商家的在职看护人；没有待分配订单时不必查
    if (orders.value.some(isUnassigned)) {
      await loadAssignableKeepers()
    }
  } catch (err) {
    error.value = err?.message || '无法获取商家订单'
    appStore.addToast(error.value, 'error')
  } finally {
    loading.value = false
  }
}

// 当前页签对应的服务端状态参数
function currentStatusParam() {
  if (activeTab.value === 'all') return ''
  if (activeTab.value === 'unassigned') return 'unassigned'
  const statuses = tabStatusMap[activeTab.value] || [activeTab.value]
  return statuses.join(',')
}

// 加载下一页：追加
async function loadMore() {
  if (loadingMore.value || loading.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const params = { size: pageSize.value, cursor: cursor.value }
    const status = currentStatusParam()
    if (status) params.status = status

    const res = await scrollMerchantOrders(params)
    if (res.code === 200) {
      const payload = res.data || {}
      const list = (Array.isArray(payload.list_wsh) ? payload.list_wsh : []).map(order => ({
        ...order,
        status_wsh: orderStatus(order),
      }))
      orders.value = [...orders.value, ...list]
      cursor.value = payload.next_cursor_wsh || ''
      hasMore.value = Boolean(payload.has_more_wsh)
    }
  } catch (err) {
    appStore.addToast(err?.message || '加载更多失败', 'error')
  } finally {
    loadingMore.value = false
  }
}

// 切换每页条数：回第一页
function changePageSize() {
  loadOrders()
}

// 页签变化即重新查询（筛选已下推服务端）
watch(activeTab, () => {
  loadOrders()
})

// 有订单过了支付时限就重新拉一次列表，让后端把状态改掉
// 为什么还看一眼 nextPaymentTimeoutRefreshAt？因为每秒都打接口太费，隔一阵才真发
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

// 全部订单数：各状态计数之和，排除 unassigned（它是 paid 的子集，不能重复计）
const totalCount = computed(() =>
  Object.entries(statusCounts.value || {})
    .filter(([key]) => key !== 'unassigned')
    .reduce((sum, [, n]) => sum + (Number(n) || 0), 0)
)

// 概览卡片要用，按状态数一下有几单（服务端全量口径）
function countByStatus(status) {
  return Number(statusCounts.value?.[status]) || 0
}

// 页签上的小数字，跟筛选走同一套服务端口径，不会出现数字和列表对不上
function countByTab(key) {
  if (key === 'all') return totalCount.value
  // 「待分配」= 已支付且无负责人，由后端单独统计并放进 unassigned 键
  if (key === 'unassigned') return Number(statusCounts.value?.unassigned) || 0
  const statuses = tabStatusMap[key] || [key]
  return statuses.reduce((sum, status) => sum + countByStatus(status), 0)
}

// 后端有时给英文有时给中文，这里统一翻成英文小写，后面判断就不用管来源了
/** 指派选择器的 key：优先用订单ID，避免订单号缺失时多张卡片共用同一个 key */
function assignKey(order) {
  return String(order.id_wsh || order.order_no_wsh || '')
}

/** 已支付但还没有负责人：需要商家指派或等待看护者抢单 */
function isUnassigned(order) {
  return orderStatus(order) === 'paid' && !order.keeper_id_wsh
}

/**
 * 已确认（已指派）且尚未开始服务的订单可以改派。
 * 看护者临时请假 / 排班冲突时，商家把订单转给另一位看护者，不必让用户退款重下。
 */
function isReassignable(order) {
  return orderStatus(order) === 'confirmed'
}

/** 改派候选人要排除当前负责人，否则等于没改 */
function reassignCandidates(order) {
  return assignableKeepers.value.filter(keeper => keeper.id_wsh !== order.keeper_id_wsh)
}

/** 加载本商家的在职看护人，供指派下拉使用 */
async function loadAssignableKeepers() {
  const merchantId = orders.value.find(order => order.merchant_id_wsh)?.merchant_id_wsh
  if (!merchantId) return
  try {
    const res = await getKeepersByMerchant(merchantId)
    const list = res.code === 200 && Array.isArray(res.data) ? res.data : []
    assignableKeepers.value = list
  } catch (e) {
    assignableKeepers.value = []
  }
}

async function handleAssign(order) {
  const key = assignKey(order)
  const keeperId = Number(assignSelection[key])
  if (!keeperId) {
    appStore.addToast('请先选择看护人', 'warning')
    return
  }
  assigningOrderNo.value = order.order_no_wsh
  try {
    const res = await assignOrder({ order_no_wsh: order.order_no_wsh, keeper_id_wsh: keeperId })
    if (res.code === 200) {
      appStore.addToast('指派成功', 'success')
      delete assignSelection[key]
      await loadOrders()
      return
    }
    appStore.addToast(res.message || '指派失败', 'error')
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || e?.message || '指派失败', 'error')
  } finally {
    assigningOrderNo.value = ''
  }
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
    '待接收': 'confirmed',
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

// 状态文案和颜色都去 constants 里查，别在页面里再抄一遍
function statusLabel(status) {
  return getStatusLabel(OrderStatus, String(status || '').trim().toLowerCase())
}

function statusBadge(status) {
  return getStatusBadge(OrderStatus, String(status || '').trim().toLowerCase())
}

// 只有待支付、而且还能算出剩余时间的订单才显示倒计时
function showPaymentCountdown(order) {
  return orderStatus(order) === 'pending' && getPaymentTimeoutRemaining(order, nowMs.value) != null
}

// 把剩余时间转成 12:03 这种字样
function paymentCountdownText(order) {
  return formatPaymentTimeoutRemaining(getPaymentTimeoutRemaining(order, nowMs.value))
}

// 这一页只负责看，接单、完成这些操作都去订单详情页做
function goDetail(order) {
  if (!order?.id_wsh) return
  router.push({ name: 'OrderDetail', params: { id: order.id_wsh } })
}
</script>

<style scoped>
/* 每页条数选择行 */
.page-size-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.page-size-row label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.page-size-row select {
  width: auto;
  min-width: 84px;
}

/* 列表底部：已加载条数 + 加载更多 */
.scroll-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 0 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

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
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border);
}

.assign-select {
  min-width: 150px;
  max-width: 220px;
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  /* 不写死背景色：跟随主题，避免暗色下浅字压白底 */
  background: var(--color-card, transparent);
  color: inherit;
  font-size: 13px;
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
