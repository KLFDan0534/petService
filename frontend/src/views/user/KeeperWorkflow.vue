<template>
  <div class="kw-page">
    <div class="kw-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="kw-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="kw-crumb-link">首页</router-link>
        <span class="kw-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="kw-crumb-link">个人中心</router-link>
        <span class="kw-crumb-sep" aria-hidden="true">›</span>
        <span class="kw-crumb-here">照护师工作台</span>
      </nav>

      <!-- ═══ 紧凑标题：标题 + 身份行（门店 · 姓名 + 接单状态 + 打卡） ═══ -->
      <header class="kw-head">
        <h1 class="kw-title">照护师工作台</h1>
        <div class="kw-identity">
          <span class="kw-identity-text">{{ identityText }}</span>
          <span
            v-if="keeperInfo"
            :class="['badge', 'kw-status-pill', getStatusBadge(KeeperOnlineStatus, keeperInfo.status_wsh)]"
          >{{ getStatusLabel(KeeperOnlineStatus, keeperInfo.status_wsh) }}</span>
          <button
            v-if="keeperInfo?.id_wsh"
            type="button"
            class="kw-att-btn"
            :title="attendanceButtonText"
            @click="attendanceVisible = true"
          >{{ attendanceButtonText }}</button>
        </div>
      </header>

      <!-- 未入驻：只保留申请入口，不在这里做资质表单 -->
      <div v-if="!keeperInfo" class="kw-card kw-onboard">
        <p class="kw-card-label">还没有照护师档案</p>
        <p class="kw-onboard-text">提交资质与从业信息，通过门店审核后就能在这里接单。</p>
        <router-link to="/keeper-apply" class="cta cta-primary">去申请成为照护师</router-link>
      </div>

      <!-- ═══ 桌面端左侧导航 + 右侧单内容区 ═══ -->
      <div v-else class="kw-layout">
        <nav class="kw-nav" aria-label="工作台导航">
          <button
            v-for="section in sections"
            :key="section.key"
            type="button"
            :class="['kw-nav-item', activeSection === section.key ? 'is-active' : '']"
            :aria-current="activeSection === section.key ? 'page' : undefined"
            @click="activeSection = section.key"
          >
            <span>{{ section.label }}</span>
            <span v-if="section.count" class="kw-nav-count">{{ section.count }}</span>
          </button>
        </nav>

        <div class="kw-main">
          <!-- 可抢订单 -->
          <section
            v-if="activeSection === 'pool'"
            class="kw-block kw-block--pool"
            aria-labelledby="kw-pool-title"
          >
            <header class="kw-block-head">
              <div>
                <h2 id="kw-pool-title" class="kw-block-title">
                  可抢订单
                  <span v-if="poolOrders.length" class="kw-block-count">{{ poolOrders.length }}</span>
                </h2>
                <p class="kw-block-desc">新订单支付后进入这里，先抢先得；30 分钟无人接单将由系统自动分配。</p>
              </div>
            </header>

            <KeeperOrdersTab
              subTab="pool"
              :poolOrders="poolOrders"
              :loading="orderLoading"
              :processingOrderNo="processingOrderNo"
              :now-ms="nowMs"
              @claim="claimOrder"
            />

            <div v-if="poolHasMore" class="kw-load-more">
              <button
                class="btn btn-sm btn-outline"
                type="button"
                :disabled="poolLoadingMore"
                @click="loadMorePoolOrders"
              >
                {{ poolLoadingMore ? '加载中…' : '加载更多可抢订单' }}
              </button>
            </div>
          </section>

          <!-- 我的订单 -->
          <section
            v-else-if="activeSection === 'orders'"
            class="kw-block kw-block--mine"
            aria-labelledby="kw-mine-title"
            :aria-busy="myStatusLoading ? 'true' : 'false'"
          >
            <header class="kw-block-head">
              <div>
                <h2 id="kw-mine-title" class="kw-block-title">我的订单</h2>
                <p class="kw-block-desc">接收、开始培养与确认接走都在订单卡片上完成。</p>
              </div>
            </header>

            <div class="kw-status-tabs" role="tablist" aria-label="我的订单状态分类">
              <button
                v-for="tab in ORDER_STATUS_TABS"
                :key="tab.key || 'all'"
                type="button"
                role="tab"
                :data-status="tab.key"
                :aria-selected="activeOrderStatus === tab.key"
                :class="['kw-status-tab', activeOrderStatus === tab.key ? 'is-active' : '']"
                @click="selectOrderStatus(tab.key)"
              >{{ tab.label }}</button>
            </div>

            <!-- 分类请求期间只给一行固定高度提示：列表保持上一份结果，不会闪空态也不会跳高度 -->
            <p class="kw-status-hint" :class="{ 'is-busy': myStatusLoading }" role="status" aria-live="polite">
              <span v-if="myStatusLoading">正在筛选订单…</span>
            </p>

            <KeeperOrdersTab
              subTab="active"
              :activeOrders="myOrders"
              :loading="orderLoading"
              :completingId="completingId"
              :receiving="receiving"
              :startUploading="startUploading"
              :processingOrderNo="processingOrderNo"
              :modalKey="orderModalKey"
              :now-ms="nowMs"
              @reject="rejectOrder"
              @receive="handleReceive"
              @start="handleStart"
              @checkout="handleCheckout"
            />

            <div v-if="myHasMore" class="kw-load-more">
              <button
                class="btn btn-sm btn-outline"
                type="button"
                :disabled="myLoadingMore || myStatusLoading"
                @click="loadMoreMyOrders"
              >
                {{ myLoadingMore ? '加载中…' : '加载更多订单' }}
              </button>
            </div>
          </section>

          <!-- 订单工作区：订单摘要列表 + 同一订单内的对话/动态切换 -->
          <section
            v-else-if="activeSection === 'workspace'"
            class="kw-block kw-block--workspace"
            aria-labelledby="kw-workspace-title"
          >
            <header class="kw-block-head">
              <div>
                <h2 id="kw-workspace-title" class="kw-block-title">订单工作区</h2>
                <p class="kw-block-desc">选择一笔订单，在同一界面里和主人沟通、上传照护动态。</p>
              </div>
            </header>

            <div class="kw-workspace">
              <div class="kw-ws-list" :aria-busy="workspaceStatusLoading ? 'true' : 'false'">
                <div class="kw-status-tabs" role="tablist" aria-label="工作区订单状态分类">
                  <button
                    v-for="tab in WORKSPACE_STATUS_TABS"
                    :key="tab.key"
                    type="button"
                    role="tab"
                    :data-status="tab.key"
                    :aria-selected="activeWorkspaceStatus === tab.key"
                    :class="['kw-status-tab', activeWorkspaceStatus === tab.key ? 'is-active' : '']"
                    @click="selectWorkspaceStatus(tab.key)"
                  >{{ tab.label }}</button>
                </div>

                <!-- 等待新分类时保留上一份列表与右侧详情，只多一行固定高度提示 -->
                <p class="kw-status-hint" :class="{ 'is-busy': workspaceStatusLoading }" role="status" aria-live="polite">
                  <span v-if="workspaceStatusLoading">正在筛选订单…</span>
                </p>

                <p v-if="sortedWorkspaceOrders.length === 0" class="kw-empty-inline">当前分类暂无订单</p>
                <button
                  v-for="order in sortedWorkspaceOrders"
                  :key="order.id_wsh"
                  type="button"
                  :data-order-id="order.id_wsh"
                  :class="['kw-ws-card', selectedOrderId === String(order.id_wsh) ? 'is-active' : '']"
                  @click="selectOrder(order)"
                >
                  <span class="kw-ws-card-head">
                    <span class="kw-order-pick-no">{{ order.order_no_wsh }}</span>
                    <span class="kw-ws-card-tags">
                      <span
                        v-if="unreadCount(order) > 0"
                        class="kw-unread-badge"
                        :aria-label="`${unreadCount(order)} 条未读消息`"
                      >{{ unreadBadgeText(order) }}</span>
                      <span class="kw-order-pick-status">{{ orderStatusLabel(order) }}</span>
                    </span>
                  </span>
                  <span class="kw-order-pick-name">{{ orderTitle(order) }}</span>
                  <span class="kw-order-pick-sub">{{ orderPeriod(order) }}</span>
                  <span class="kw-ws-summary">
                    <span class="kw-ws-summary-row">最近消息：<b>{{ chatSummaryText(order) }}</b></span>
                    <span class="kw-ws-summary-row">动态进度：<b>{{ dailySummaryText(order) }}</b></span>
                  </span>
                </button>

                <div v-if="workspaceHasMore" class="kw-load-more">
                  <button
                    class="btn btn-sm btn-outline"
                    type="button"
                    :disabled="workspaceLoadingMore || workspaceStatusLoading"
                    @click="loadMoreWorkspaceOrders"
                  >
                    {{ workspaceLoadingMore ? '加载中…' : '加载更多订单' }}
                  </button>
                </div>
              </div>

              <div class="kw-ws-detail">
                <template v-if="selectedOrder">
                  <header class="kw-ws-detail-head">
                    <div>
                      <p class="kw-ws-detail-no">{{ selectedOrder.order_no_wsh }}</p>
                      <h3 class="kw-ws-detail-title">{{ orderTitle(selectedOrder) }}</h3>
                      <p class="kw-ws-detail-sub">
                        {{ orderStatusLabel(selectedOrder) }} · {{ orderPeriod(selectedOrder) }}
                      </p>
                    </div>
                    <button type="button" class="kw-ws-back" @click="clearSelectedOrder">更换订单</button>
                  </header>

                  <div class="kw-seg" role="tablist" aria-label="订单详情切换">
                    <button
                      type="button"
                      role="tab"
                      data-detail-tab="chat"
                      :aria-selected="detailTab === 'chat'"
                      :class="['kw-seg-btn', detailTab === 'chat' ? 'is-active' : '']"
                      @click="detailTab = 'chat'"
                    >对话</button>
                    <button
                      type="button"
                      role="tab"
                      data-detail-tab="daily"
                      :aria-selected="detailTab === 'daily'"
                      :class="['kw-seg-btn', detailTab === 'daily' ? 'is-active' : '']"
                      @click="detailTab = 'daily'"
                    >照护动态</button>
                  </div>

                  <div v-if="detailTab === 'chat'" class="kw-chat">
                    <div class="kw-chat-box">
                      <div v-if="conversation.length === 0" class="kw-chat-empty">暂无消息，先和主人打个招呼。</div>
                      <div
                        v-for="message in conversation"
                        :key="message.id_wsh"
                        :class="['kw-msg', message.from_user_id_wsh === authStore.user?.id_wsh ? 'is-me' : '']"
                      >
                        <p class="kw-msg-text">{{ message.content_wsh }}</p>
                        <small class="kw-msg-time">{{ formatDateTime(message.created_at_wsh) }}</small>
                      </div>
                    </div>

                    <div class="kw-chat-input">
                      <input
                        v-model="chatText"
                        data-testid="chat-input"
                        class="kw-input"
                        placeholder="和主人沟通…"
                        @keyup.enter="sendMessage"
                      >
                      <button type="button" data-testid="chat-send" class="cta cta-primary" @click="sendMessage">发送</button>
                    </div>
                  </div>

                  <KeeperDailyTab
                    v-else
                    order-locked
                    :orderId="selectedOrderId"
                    :myOrders="workspaceOrders"
                    :dailyStatus="dailyStatus"
                    :careRecords="careRecords"
                    :recordUploading="recordUploading"
                    :submitKey="recordSubmitKey"
                    @upload-timeline="submitRecord"
                    @refresh="refreshSelectedOrderData"
                  />
                </template>
                <p v-else class="kw-empty-inline">从左侧选择一笔订单，即可沟通和上传动态。</p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>

    <!-- 考勤只保留紧凑入口，点开才加载面板 -->
    <AppDialog
      :visible="attendanceVisible"
      size="md"
      title="今日考勤"
      description="上班与下班打卡会记录到所属门店。"
      @close="attendanceVisible = false"
    >
      <KeeperAttendancePanel v-if="keeperInfo?.id_wsh" @status-change="handleAttendanceStatusChange" />
    </AppDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getMyKeeperInfo } from '@/api/keeper'
import { getCurrentAttendance } from '@/api/attendance'
import {
  scrollPoolOrders,
  scrollKeeperOrders,
  claimOrder as claimOrderApi,
  rejectOrder as rejectOrderApi,
  markReceived,
  startService,
  completeOrder,
  getTimeline,
  getDailyStatus,
  uploadTimeline,
  getConversation,
  sendMessage as sendMessageApi,
  markConversationRead,
} from '@/api/order'
import { getUnreadMessages } from '@/api/chat'
import { KeeperOnlineStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import AppDialog from '@/components/common/AppDialog.vue'
import KeeperAttendancePanel from '@/components/keeper/KeeperAttendancePanel.vue'
import KeeperOrdersTab from '@/components/keeper/KeeperOrdersTab.vue'
import KeeperDailyTab from '@/components/keeper/KeeperDailyTab.vue'
import { formatDateTime as utilFormatDateTime } from '@/utils/format'

const authStore = useAuthStore()
const appStore = useAppStore()

const orderLoading = ref(false)
// 待分配订单池：已支付但还没有负责人的订单，看护者可主动抢
const poolOrders = ref([])
// 待分配池的 SSE 连接：新单入池 / 被接走时实时刷新，不用手动刷新页面
let orderEventSource = null
let poolRefreshTimer = null
let sseReconnectTimer = null
let sseReconnectAttempts = 0
let sseClosedByAuth = false
// 最多重连 5 次（2s/4s/8s/16s/30s），超过就停下，避免 token 失效后无限 401
const SSE_MAX_RECONNECT_ATTEMPTS = 5
const myOrders = ref([])
// 订单工作区独立的一份列表：与「我的订单」各自分类、各自游标，互不覆盖
const workspaceOrders = ref([])
const keeperInfo = ref(null)
const selectedOrderId = ref('')
const detailTab = ref('chat')
const careRecords = ref([])
const dailyStatus = ref(null)
const conversation = ref([])
const chatText = ref('')
// 当前这条「还没下班」的考勤记录，决定页头按钮是上班还是下班打卡
const currentAttendance = ref(null)
// 对话摘要 / 每日上传进度都按订单 id 缓存：进入视图即可预览，点选时直接复用，不重复请求
const chatSummaryCache = ref({})
const chatSummaryLoading = ref({})
const dailyStatusCache = ref({})
// 每笔订单的未读消息：{ [orderId]: { count, at, text } }。
// 一次 /api/chat/unread 拿全量未读再按 order_id 聚合，比逐单拉会话省几十个请求；
// 新消息由 chat-events 事件流实时累加，红点与「顶到最前」都靠它。
const unreadByOrder = ref({})
// 视图首屏最多预取 8 笔订单的摘要，避免一次性打满接口
const SUMMARY_PREFETCH_LIMIT = 8
const recordUploading = ref(false)
const completingId = ref(null)
const processingOrderNo = ref('')
const receiving = ref(false)
const startUploading = ref(false)
const orderModalKey = ref(0)
const recordSubmitKey = ref(0)
const nowMs = ref(Date.now())
let countdownTimer = null

// 单内容区：一次只渲染一个分区，默认「可抢订单」
const activeSection = ref('pool')
const attendanceVisible = ref(false)

// 「我的订单」与「订单工作区」各自维护状态分类：两处 tab 互不影响。
// 只暴露服务端能过滤的真实状态 key；后端 /orders/keeper/scroll 的 status 支持单值或逗号多值，
// 这里用单值，切换即按该分类重新取第一页。
const ORDER_STATUS_TABS = [
  { key: '', label: '全部' },
  { key: 'confirmed', label: '待接收' },
  { key: 'received', label: '已接收' },
  { key: 'in_progress', label: '服务中' },
  { key: 'completed', label: '已完成' },
  { key: 'refunded', label: '已退款' },
]
// 工作区分类：按「服务中 → 待接收 → 已接收 → 其他」推进，已完成与已退款合并成「其他」。
// 「其他」用逗号多值下推到服务端 status，前端不做本地筛选，否则只筛到已加载的一页。
const WORKSPACE_STATUS_TABS = [
  { key: 'in_progress', label: '服务中' },
  { key: 'confirmed', label: '待接收' },
  { key: 'received', label: '已接收' },
  { key: 'completed,refunded', label: '其他' },
]
const activeOrderStatus = ref('')
const activeWorkspaceStatus = ref('in_progress')

// 工作区选中的订单始终来自工作区当前分类的第一页/已加载页，分类切换后不在列表里就自动取消选中
const selectedOrder = computed(() => workspaceOrders.value.find(
  order => String(order.id_wsh) === selectedOrderId.value,
) || null)

// 状态推进顺序：服务中(0) → 待接收(1) → 已接收(2) → 其他(3)
const WORKSPACE_STATUS_RANK = { in_progress: 0, confirmed: 1, received: 2 }

function workspaceStatusRank(order) {
  const rank = WORKSPACE_STATUS_RANK[order?.status_wsh]
  return rank === undefined ? 3 : rank
}

function unreadOf(order) {
  return unreadByOrder.value[String(order?.id_wsh ?? '')] || null
}

function unreadCount(order) {
  return unreadOf(order)?.count || 0
}

function unreadBadgeText(order) {
  const count = unreadCount(order)
  return count > 99 ? '99+' : String(count)
}

/**
 * 工作区列表排序（第一性原理：先看谁在等我，再看单子走到哪一步）：
 * 1. 有未读消息的订单置顶，未读越新越靠前 —— 对方刚发来消息，这笔就该第一个被看到；
 * 2. 其余按状态推进顺序：服务中 → 待接收 → 已接收 → 其他；
 * 3. 同一优先级保持服务端游标顺序（created_at 倒序），Array.sort 稳定，不会打乱分页结果。
 */
const sortedWorkspaceOrders = computed(() => [...workspaceOrders.value].sort((left, right) => {
  const leftUnread = unreadCount(left)
  const rightUnread = unreadCount(right)
  if ((leftUnread > 0) !== (rightUnread > 0)) return leftUnread > 0 ? -1 : 1
  if (leftUnread > 0 && rightUnread > 0) {
    const leftAt = timeOf(unreadOf(left)?.at)
    const rightAt = timeOf(unreadOf(right)?.at)
    if (leftAt !== rightAt) return rightAt - leftAt
  }
  return workspaceStatusRank(left) - workspaceStatusRank(right)
}))

/** 时间戳兜底：非法或缺失一律按 0 处理，避免 NaN 参与比较 */
function timeOf(value) {
  const stamp = value ? new Date(value).getTime() : NaN
  return Number.isNaN(stamp) ? 0 : stamp
}

const identityText = computed(() => (keeperInfo.value
  ? `${keeperInfo.value.merchant_name_wsh || '门店'} · ${keeperInfo.value.name_wsh || '照护师'}`
  : '接单状态与打卡会影响派单，请保持真实。'))

// 按钮直接说明「现在的考勤状态 + 点下去会做什么」，避免只写「打卡」看不出区别
const attendanceButtonText = computed(() => (currentAttendance.value
  ? '已打卡上班 · 下班打卡'
  : '未打卡 · 上班打卡'))

const sections = computed(() => [
  { key: 'pool', label: '可抢订单', count: poolOrders.value.length },
  { key: 'orders', label: '我的订单', count: myOrders.value.length },
  { key: 'workspace', label: '订单工作区', count: 0 },
])

// 只有进入工作区才加载自己的第一页并预取摘要：对话与动态各最多 8 笔，避免一次打满接口。
// 已经加载过就复用现有列表与游标，切分区回来不会把分页重置；每次进来都补一次未读数。
watch(activeSection, (section) => {
  if (section !== 'workspace') return
  loadWorkspaceUnread()
  if (workspaceLoaded.value) hydrateWorkspaceSummaries(workspaceOrders.value)
  else loadWorkspaceOrders()
})

onMounted(async () => {
  countdownTimer = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
  // 待抢单池必须首屏就加载：否则新看护者进来看不到任何可抢订单，也没有任何操作能触发刷新
  await Promise.all([loadKeeperInfo(), loadMyOrders(), loadPoolOrders(), loadCurrentAttendance()])
  connectOrderEvents()
  connectChatEvents()
  loadWorkspaceUnread()
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  if (poolRefreshTimer) clearTimeout(poolRefreshTimer)
  if (orderEventSource) {
    orderEventSource.close()
    orderEventSource = null
  }
  if (sseReconnectTimer) {
    clearTimeout(sseReconnectTimer)
    sseReconnectTimer = null
  }
  if (chatEventSource) {
    chatEventSource.close()
    chatEventSource = null
  }
  if (chatReconnectTimer) {
    clearTimeout(chatReconnectTimer)
    chatReconnectTimer = null
  }
})

async function loadKeeperInfo() {
  const res = await getMyKeeperInfo()
  if (res.code === 200 && res.data) {
    keeperInfo.value = res.data
  }
}

/** 首屏并行读取当前考勤：有未下班的记录才算「已打卡上班」 */
async function loadCurrentAttendance() {
  try {
    const res = await getCurrentAttendance()
    if (res?.code === 200) currentAttendance.value = res.data || null
  } catch (e) {
    // 考勤读取失败不阻塞工作台，按钮保持「未打卡」的默认状态
    currentAttendance.value = null
  }
}

/** 考勤面板加载完成 / 签到签退成功后回传最新状态，避免页头按钮文案陈旧 */
function handleAttendanceStatusChange(status) {
  currentAttendance.value = status || null
}

/**
 * 订阅订单事件流。
 * 后端在「订单支付且无负责人」时推 pool-new，在「被抢/被指派/无人可分配取消」时推 pool-removed。
 * 只做刷新与提示，不改业务数据；断线由 EventSource 自己重连。
 */
function connectOrderEvents() {
  if (orderEventSource || !authStore.token || sseClosedByAuth) return
  try {
    orderEventSource = new EventSource(
      `/api/order-events/stream?token=${encodeURIComponent(authStore.token)}`)
  } catch (e) {
    orderEventSource = null
    scheduleSseReconnect()
    return
  }
  orderEventSource.onopen = () => {
    sseReconnectAttempts = 0
  }
  orderEventSource.addEventListener('pool-new', () => {
    schedulePoolRefresh()
    appStore.addToast('有新的待抢订单', 'info')
  })
  orderEventSource.addEventListener('pool-removed', () => {
    schedulePoolRefresh()
  })
  orderEventSource.onerror = () => {
    // 浏览器原生 EventSource 失败后会每 ~3 秒无限重连；token 过期时就是无限 401。
    // 这里改成「有限次退避重连」，避免把服务端打成 401 洪流。
    if (orderEventSource) {
      orderEventSource.close()
      orderEventSource = null
    }
    scheduleSseReconnect()
  }
}

/** 有限次指数退避重连；超过上限就不再重试，等用户手动刷新页面 */
function scheduleSseReconnect() {
  if (sseReconnectTimer || sseClosedByAuth) return
  if (sseReconnectAttempts >= SSE_MAX_RECONNECT_ATTEMPTS) {
    sseClosedByAuth = true
    return
  }
  const delay = Math.min(30000, 2000 * 2 ** sseReconnectAttempts)
  sseReconnectAttempts += 1
  sseReconnectTimer = setTimeout(() => {
    sseReconnectTimer = null
    connectOrderEvents()
  }, delay)
}

/** 合并短时间内的多次事件，避免连续弹单时打出十几个请求 */
function schedulePoolRefresh() {
  if (poolRefreshTimer) clearTimeout(poolRefreshTimer)
  poolRefreshTimer = setTimeout(() => {
    poolRefreshTimer = null
    loadPoolOrders()
  }, 300)
}

// ── 订单聊天事件流：对方发来消息时累加未读并把该订单顶到最前 ──
// 与订单事件流各自一条连接、各自退避；token 失效时由 sseClosedByAuth 统一叫停，不再无限 401。
let chatEventSource = null
let chatReconnectTimer = null
let chatReconnectAttempts = 0

/** 订阅聊天事件；只在需要实时红点时才建连，页面卸载时关闭 */
function connectChatEvents() {
  if (chatEventSource || !authStore.token || sseClosedByAuth) return
  try {
    chatEventSource = new EventSource(
      `/api/chat-events/stream?token=${encodeURIComponent(authStore.token)}`)
  } catch (e) {
    chatEventSource = null
    scheduleChatReconnect()
    return
  }
  chatEventSource.onopen = () => {
    chatReconnectAttempts = 0
  }
  chatEventSource.addEventListener('chat-message', handleChatMessageEvent)
  chatEventSource.onerror = () => {
    if (chatEventSource) {
      chatEventSource.close()
      chatEventSource = null
    }
    scheduleChatReconnect()
  }
}

/** 有限次指数退避重连，与订单事件流同一套上限 */
function scheduleChatReconnect() {
  if (chatReconnectTimer || sseClosedByAuth) return
  if (chatReconnectAttempts >= SSE_MAX_RECONNECT_ATTEMPTS) {
    sseClosedByAuth = true
    return
  }
  const delay = Math.min(30000, 2000 * 2 ** chatReconnectAttempts)
  chatReconnectAttempts += 1
  chatReconnectTimer = setTimeout(() => {
    chatReconnectTimer = null
    connectChatEvents()
  }, delay)
}

/**
 * 收到一条订单聊天消息：
 * - 只处理发给自己的（自己发出的消息不产生未读）；
 * - 正在看这笔订单时直接并入会话并标已读，不亮红点；
 * - 否则累加未读，sortedWorkspaceOrders 会把它排到最前面。
 */
function handleChatMessageEvent(event) {
  let message = null
  try {
    message = JSON.parse(event.data)
  } catch (e) {
    return
  }
  if (!message?.order_id_wsh) return
  const key = String(message.order_id_wsh)
  const myUserId = String(authStore.user?.id_wsh ?? '')
  if (myUserId && String(message.to_user_id_wsh) !== myUserId) return
  // 摘要跟着一起更新，卡片上的「最近消息」不会落后于红点
  chatSummaryCache.value = {
    ...chatSummaryCache.value,
    [key]: { text: messageText(message), at: message.created_at_wsh || '' },
  }
  if (selectedOrderId.value === key) {
    if (!conversation.value.some(item => String(item.id_wsh) === String(message.id_wsh))) {
      conversation.value = [...conversation.value, message]
    }
    if (unreadByOrder.value[key]) markOrderRead(key)
    return
  }
  const previous = unreadByOrder.value[key]
  unreadByOrder.value = {
    ...unreadByOrder.value,
    [key]: {
      count: (previous?.count || 0) + 1,
      at: message.created_at_wsh || '',
      text: messageText(message),
    },
  }
}

/**
 * 拉一次全量未读消息，按订单聚合出未读数与最新未读时间。
 * 服务端已按 created_at 倒序返回，所以每笔订单第一次出现的那条就是最新未读。
 */
async function loadWorkspaceUnread() {
  try {
    const res = await getUnreadMessages()
    if (res?.code !== 200) return
    const myUserId = String(authStore.user?.id_wsh ?? '')
    const next = {}
    const list = Array.isArray(res.data) ? res.data : []
    list.forEach((message) => {
      if (message?.order_id_wsh == null) return
      if (myUserId && String(message.to_user_id_wsh) !== myUserId) return
      const key = String(message.order_id_wsh)
      const previous = next[key]
      const at = message.created_at_wsh || ''
      next[key] = {
        count: (previous?.count || 0) + 1,
        at: !previous || timeOf(at) >= timeOf(previous.at) ? at : previous.at,
        text: previous ? previous.text : messageText(message),
      }
    })
    unreadByOrder.value = next
  } catch (e) {
    // 未读数读取失败不阻塞列表：红点最多晚一次刷新，不影响订单与对话本身
  }
}

/** 点开订单即视为已读：本地红点立即消失，同时通知服务端，下次进来不再亮 */
async function markOrderRead(orderId) {
  const key = String(orderId)
  if (Object.prototype.hasOwnProperty.call(unreadByOrder.value, key)) {
    const next = { ...unreadByOrder.value }
    delete next[key]
    unreadByOrder.value = next
  }
  try {
    await markConversationRead(key)
  } catch (e) {
    // 标已读失败不影响正在进行的对话读取，下一次进入工作区会重新对齐
  }
}

/** 消息正文：没有文字就按图片消息展示，与摘要口径一致 */
function messageText(message) {
  const content = String(message?.content_wsh ?? '').trim()
  if (content) return content
  if (message?.type_wsh === 'image' || message?.file_url_wsh) return '图片消息'
  return '暂无对话'
}

// 分页状态：抢单池、我的订单、订单工作区各自维护游标
const poolCursor = ref('')
const poolHasMore = ref(false)
const poolLoadingMore = ref(false)
const myCursor = ref('')
const myHasMore = ref(false)
const myLoadingMore = ref(false)
const workspaceCursor = ref('')
const workspaceHasMore = ref(false)
const workspaceLoadingMore = ref(false)
const workspaceLoaded = ref(false)
// 分类切换只做「可见但不占位」的加载反馈：列表与游标等新数据回来再一起替换
const myStatusLoading = ref(false)
const workspaceStatusLoading = ref(false)
// 分类请求序号：只有最新一次请求能写列表，慢响应不会把新分类覆盖回去
let myStatusSeq = 0
let workspaceStatusSeq = 0
// 抢单池一次 50 条；我的订单 / 工作区固定 20 条一页（后端只支持 20/50/100），配合状态分类逐页加载
const POOL_PAGE_SIZE = 50
const ORDER_PAGE_SIZE = 20

/** 接口的 list_wsh 不保证一定是数组，统一在这里兜底，避免整页渲染崩掉 */
function asOrderList(payload) {
  return Array.isArray(payload?.list_wsh) ? payload.list_wsh : []
}

function orderKey(order) {
  return String(order?.id_wsh ?? order?.order_no_wsh ?? '')
}

/** 追加分页时按订单 id 去重，防止翻页期间数据漂移导致同一笔订单重复出现 */
function mergeOrders(existing, incoming) {
  const merged = [...existing]
  const seen = new Set(existing.map(orderKey).filter(Boolean))
  incoming.forEach((order) => {
    const key = orderKey(order)
    if (key && seen.has(key)) return
    if (key) seen.add(key)
    merged.push(order)
  })
  return merged
}

/** 列表变化后按当前分区补齐摘要：工作区里对话与动态都要展示 */
function hydrateWorkspaceSummaries(orders) {
  if (!orders?.length) return
  loadChatSummaries(orders)
  loadDailySummaries(orders)
}

async function loadPoolOrders() {
  try {
    const res = await scrollPoolOrders({ size: POOL_PAGE_SIZE })
    if (res.code === 200) {
      const payload = res.data || {}
      poolOrders.value = Array.isArray(payload.list_wsh) ? payload.list_wsh : []
      poolCursor.value = payload.next_cursor_wsh || ''
      poolHasMore.value = Boolean(payload.has_more_wsh)
    }
  } catch (e) {
    poolOrders.value = []
  }
}

async function loadMorePoolOrders() {
  if (poolLoadingMore.value || !poolHasMore.value) return
  poolLoadingMore.value = true
  try {
    const res = await scrollPoolOrders({ size: POOL_PAGE_SIZE, cursor: poolCursor.value })
    if (res.code === 200) {
      const payload = res.data || {}
      poolOrders.value = [...poolOrders.value, ...asOrderList(payload)]
      poolCursor.value = payload.next_cursor_wsh || ''
      poolHasMore.value = Boolean(payload.has_more_wsh)
    }
  } catch (e) {
    // 加载更多失败不打断当前操作，用户可再次点击
  } finally {
    poolLoadingMore.value = false
  }
}

/**
 * 取一页订单（首屏 / 切分类 / 刷新共用）。
 * 失败返回 null，由调用方决定是「保留上一份结果」还是「清空」，避免分类请求失败时闪空态。
 */
async function fetchOrderPage(status) {
  const params = { size: ORDER_PAGE_SIZE }
  if (status) params.status = status
  try {
    const res = await scrollKeeperOrders(params)
    if (res?.code === 200) {
      const payload = res.data || {}
      return {
        list: asOrderList(payload),
        cursor: payload.next_cursor_wsh || '',
        hasMore: Boolean(payload.has_more_wsh),
      }
    }
  } catch (e) {
    // 网络异常按失败处理，由调用方决定保留还是清空
  }
  return null
}

/**
 * 「我的订单」切换状态分类：请求期间保留上一份列表与高度，只显示加载提示。
 * activeOrderStatus 始终与列表保持一致，只有成功时才和列表、游标同批提交；
 * 失败时列表与分类都不动，因此不需要回滚（避免 A→B→C 失败时退回到列表里没有的 B）。
 */
async function selectOrderStatus(status) {
  if (activeOrderStatus.value === status) return
  myStatusLoading.value = true
  const seq = ++myStatusSeq
  const page = await fetchOrderPage(status)
  // 期间又切了分类或触发了刷新：丢弃这次响应，避免旧分类覆盖新分类
  if (seq !== myStatusSeq) return
  myStatusLoading.value = false
  if (!page) {
    // 失败：分类与列表都保持切换前的样子，明确提示即可
    appStore.addToast('订单筛选失败，已保留上一次结果', 'error')
    return
  }
  // 分类与数据同批提交：看到「已完成」被选中时，列表已经是那一页
  activeOrderStatus.value = status
  myOrders.value = page.list
  myCursor.value = page.cursor
  myHasMore.value = page.hasMore
}

async function loadMyOrders() {
  const seq = ++myStatusSeq
  const page = await fetchOrderPage(activeOrderStatus.value)
  if (seq !== myStatusSeq) return
  myStatusLoading.value = false
  if (page) {
    // 首屏/刷新时列表、游标、是否还有下一页必须一起重置，否则会拿旧游标继续翻
    myOrders.value = page.list
    myCursor.value = page.cursor
    myHasMore.value = page.hasMore
    return
  }
  // 刷新失败时清空列表，避免把上一次的订单和游标混着用
  myOrders.value = []
  myCursor.value = ''
  myHasMore.value = false
}

async function loadMoreMyOrders() {
  // 分类切换在途时禁止翻页：否则会拿旧游标配新分类去请求
  if (myLoadingMore.value || myStatusLoading.value || !myHasMore.value) return
  const seq = myStatusSeq
  myLoadingMore.value = true
  try {
    // 加载更多必须带上当前分类，否则会把别的状态订单混进当前列表
    const params = { size: ORDER_PAGE_SIZE, cursor: myCursor.value }
    if (activeOrderStatus.value) params.status = activeOrderStatus.value
    const res = await scrollKeeperOrders(params)
    // 期间切过分类：这一页属于旧分类，直接丢弃
    if (seq !== myStatusSeq) return
    if (res?.code === 200) {
      const payload = res.data || {}
      myOrders.value = mergeOrders(myOrders.value, asOrderList(payload))
      myCursor.value = payload.next_cursor_wsh || ''
      myHasMore.value = Boolean(payload.has_more_wsh)
    }
  } catch (e) {
    // 加载更多失败不打断当前操作，用户可再次点击
  } finally {
    myLoadingMore.value = false
  }
}

/**
 * 「订单工作区」切换状态分类：独立请求，不影响「我的订单」。
 * 请求期间保留旧列表与右侧详情（不闪空态、不跳高度），activeWorkspaceStatus 只在成功时
 * 与列表、游标同批提交；失败时都不动，只在成功后才清理已不在新结果里的已选订单。
 */
async function selectWorkspaceStatus(status) {
  if (activeWorkspaceStatus.value === status) return
  workspaceStatusLoading.value = true
  const seq = ++workspaceStatusSeq
  const page = await fetchOrderPage(status)
  // 期间又切了分类或重新进入工作区：丢弃这次响应
  if (seq !== workspaceStatusSeq) return
  workspaceStatusLoading.value = false
  if (!page) {
    appStore.addToast('订单筛选失败，已保留上一次结果', 'error')
    return
  }
  // 分类与数据同批提交，避免「选中了新分类但列表还是旧分类」
  activeWorkspaceStatus.value = status
  workspaceOrders.value = page.list
  workspaceCursor.value = page.cursor
  workspaceHasMore.value = page.hasMore
  workspaceLoaded.value = true
  // 更新成功后才清理已选订单：等待期间右侧详情保持原样
  if (selectedOrderId.value && !selectedOrder.value) selectedOrderId.value = ''
  hydrateWorkspaceSummaries(workspaceOrders.value)
}

async function loadWorkspaceOrders() {
  const seq = ++workspaceStatusSeq
  const page = await fetchOrderPage(activeWorkspaceStatus.value)
  if (seq !== workspaceStatusSeq) return
  workspaceStatusLoading.value = false
  if (page) {
    workspaceOrders.value = page.list
    workspaceCursor.value = page.cursor
    workspaceHasMore.value = page.hasMore
    workspaceLoaded.value = true
    // 选中的订单不在当前分类页时，右侧详情不能继续显示过期订单
    if (selectedOrderId.value && !selectedOrder.value) selectedOrderId.value = ''
    hydrateWorkspaceSummaries(workspaceOrders.value)
    return
  }
  // 加载失败时清空，避免旧订单与旧游标混用
  workspaceOrders.value = []
  workspaceCursor.value = ''
  workspaceHasMore.value = false
  workspaceLoaded.value = false
  selectedOrderId.value = ''
}

async function loadMoreWorkspaceOrders() {
  // 分类切换在途时禁止翻页：否则会拿旧游标配新分类去请求
  if (workspaceLoadingMore.value || workspaceStatusLoading.value || !workspaceHasMore.value) return
  const seq = workspaceStatusSeq
  workspaceLoadingMore.value = true
  try {
    // 游标与分类都用工作区自己的，不借用「我的订单」的状态
    const params = { size: ORDER_PAGE_SIZE, cursor: workspaceCursor.value }
    if (activeWorkspaceStatus.value) params.status = activeWorkspaceStatus.value
    const res = await scrollKeeperOrders(params)
    // 期间切过分类：这一页属于旧分类，直接丢弃
    if (seq !== workspaceStatusSeq) return
    if (res?.code === 200) {
      const payload = res.data || {}
      const incoming = asOrderList(payload)
      const known = new Set(workspaceOrders.value.map(orderKey).filter(Boolean))
      const added = incoming.filter((order) => {
        const key = orderKey(order)
        return !key || !known.has(key)
      })
      workspaceOrders.value = mergeOrders(workspaceOrders.value, incoming)
      workspaceCursor.value = payload.next_cursor_wsh || ''
      workspaceHasMore.value = Boolean(payload.has_more_wsh)
      // 给刚追加的订单补摘要（每批最多 8 笔），已缓存的会跳过
      if (added.length) hydrateWorkspaceSummaries(added)
    }
  } catch (e) {
    // 加载更多失败不打断当前操作，用户可再次点击
  } finally {
    workspaceLoadingMore.value = false
  }
}

async function refreshOrders() {
  await Promise.all([loadMyOrders(), loadPoolOrders()])
}

/**
 * 抢单：把待分配订单接到自己名下。
 * 并发下可能被别的看护者抢先，失败时刷新列表让用户看到最新池子。
 */
async function claimOrder(order) {
  if (!order?.order_no_wsh || processingOrderNo.value) return
  processingOrderNo.value = order.order_no_wsh
  try {
    const res = await claimOrderApi({ order_no_wsh: order.order_no_wsh })
    if (res.code === 200) {
      // 抢到后订单会出现在「我的订单」里，刷新即可看到，不再需要切页签
      appStore.addToast('抢单成功', 'success')
    }
    // 失败时不再额外 toast：request 拦截器已经统一提示，重复弹会出现两条一样的错误
    await refreshOrders()
  } catch (e) {
    // 400 的响应体里带着具体原因（排班已满 / 在休假 / 资质未过 / 已被接走），
    // 直接用 e.message 只会显示「Request failed with status code 400」，用户看不出为什么失败
    appStore.addToast(e?.response?.data?.message || e?.message || '抢单失败，请稍后重试', 'error')
    await refreshOrders()
  } finally {
    processingOrderNo.value = ''
  }
}

async function rejectOrder(order) {
  if (!order?.order_no_wsh || processingOrderNo.value) return
  processingOrderNo.value = order.order_no_wsh
  try {
    const res = await rejectOrderApi({ order_no_wsh: order.order_no_wsh })
    if (res.code === 200) {
      appStore.addToast('已拒单', 'success')
      await refreshOrders()
    } else {
      appStore.addToast(res.message || '拒单失败', 'error')
      await refreshOrders()
    }
  } catch (e) {
    appStore.addToast(e?.message || '拒单失败', 'error')
    await refreshOrders()
  } finally {
    processingOrderNo.value = ''
  }
}

async function handleReceive(payload) {
  receiving.value = true
  try {
    const res = await markReceived(payload)
    if (res.code === 200) {
      appStore.addToast('已确认收宠', 'success')
      orderModalKey.value++
      await refreshOrders()
    }
  } catch (error) {
    appStore.addToast(error.message || '确认收宠失败', 'error')
  } finally {
    receiving.value = false
  }
}

async function handleStart(orderNo, file) {
  startUploading.value = true
  try {
    const formData = new FormData()
    formData.append('order_no_wsh', orderNo)
    formData.append('file', file)
    const res = await startService(formData)
    if (res.code === 200) {
      appStore.addToast('已开始护理', 'success')
      orderModalKey.value++
      await refreshOrders()
    }
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  } finally {
    startUploading.value = false
  }
}

/**
 * 确认接走：提交独立接走码，服务端固定接走时刻并结算晚接费。
 * 0 费直接完成；有费返回补费单号，订单保持服务中，等主人余额支付后自动完成。
 */
async function handleCheckout(payload) {
  if (!payload?.order_no_wsh || processingOrderNo.value) return
  processingOrderNo.value = payload.order_no_wsh
  completingId.value = payload.order_no_wsh
  try {
    const res = await completeOrder(payload)
    if (res.code === 200) {
      const data = res.data || {}
      if (data.completed_wsh) {
        appStore.addToast('已确认接走，订单已完成', 'success')
      } else {
        const amount = Number(data.overtime_amount_wsh || 0)
        appStore.addToast(`已确认接走，待主人补费 ¥${amount.toFixed(2)}`, 'warning')
      }
      orderModalKey.value++
      await refreshOrders()
    } else {
      appStore.addToast(res.message || '确认接走失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || e?.message || '确认接走失败', 'error')
  } finally {
    completingId.value = null
    processingOrderNo.value = ''
  }
}

/**
 * 工作区选中订单：同一订单区内切换「对话 / 照护动态」，
 * 选中时一次取会话、时间线与每日进度，切换 tab 不再重复请求。
 */
function selectOrder(order) {
  const key = String(order?.id_wsh ?? '')
  if (!key) return
  selectedOrderId.value = key
  conversation.value = []
  chatText.value = ''
  careRecords.value = []
  // 预取过的进度直接复用，选中时不再重复请求
  dailyStatus.value = Object.prototype.hasOwnProperty.call(dailyStatusCache.value, key)
    ? dailyStatusCache.value[key]
    : null
  // 点开就把这笔订单的未读清掉：红点归零，服务端也一并标记已读
  markOrderRead(key)
  loadConversation()
  loadSelectedOrderData()
}

/** 更换订单：回到「先选一笔订单」的状态，右侧详情清空 */
function clearSelectedOrder() {
  selectedOrderId.value = ''
  conversation.value = []
  careRecords.value = []
  chatText.value = ''
}

/**
 * 加载选中订单的时间线与每日进度。
 * force=true 时（刷新 / 上传成功）强制重取进度并写回缓存，保证卡片摘要与详情同步。
 */
async function loadSelectedOrderData({ force = false } = {}) {
  const orderId = selectedOrderId.value
  if (!orderId) return
  const cached = Object.prototype.hasOwnProperty.call(dailyStatusCache.value, orderId)
  const [recordsRes, statusRes] = await Promise.all([
    getTimeline(orderId),
    force || !cached ? getDailyStatus(orderId) : Promise.resolve(null),
  ])
  if (recordsRes?.code === 200) {
    careRecords.value = Array.isArray(recordsRes.data) ? recordsRes.data : []
  }
  if (statusRes?.code === 200) {
    dailyStatusCache.value = { ...dailyStatusCache.value, [orderId]: statusRes.data || null }
  }
  if (Object.prototype.hasOwnProperty.call(dailyStatusCache.value, orderId)) {
    dailyStatus.value = dailyStatusCache.value[orderId]
  }
}

/** KeeperDailyTab 的「刷新」按钮：强制刷新当前订单的进度 */
function refreshSelectedOrderData() {
  return loadSelectedOrderData({ force: true })
}

async function submitRecord(data) {
  recordUploading.value = true
  try {
    const formData = new FormData()
    formData.append('type_wsh', data.type)
    formData.append('content_wsh', data.content)
    data.files.forEach(file => formData.append('files', file))
    const res = await uploadTimeline(data.orderId, formData)
    if (res.code === 200) {
      appStore.addToast('动态已上传', 'success')
      recordSubmitKey.value++
      await loadSelectedOrderData({ force: true })
    }
  } finally {
    recordUploading.value = false
  }
}

async function loadConversation() {
  if (!selectedOrderId.value) return
  const res = await getConversation(selectedOrderId.value)
  if (res.code === 200) {
    conversation.value = Array.isArray(res.data) ? res.data : []
    // 完整会话回来后同步摘要缓存，避免卡片还显示旧消息
    chatSummaryCache.value = {
      ...chatSummaryCache.value,
      [selectedOrderId.value]: summarizeMessages(conversation.value),
    }
  }
}

async function sendMessage() {
  if (!selectedOrderId.value || !chatText.value.trim()) return
  const res = await sendMessageApi(selectedOrderId.value, {
    content_wsh: chatText.value,
  })
  if (res.code === 200) {
    chatText.value = ''
    await loadConversation()
  }
}

/** 后端最新一页按 id 升序返回，所以最后一条就是最近消息 */
function summarizeMessages(messages) {
  const list = Array.isArray(messages) ? messages : []
  const last = list[list.length - 1]
  if (!last) return { text: '暂无对话', at: '' }
  const content = String(last.content_wsh ?? '').trim()
  if (content) return { text: content, at: last.created_at_wsh || '' }
  if (last.type_wsh === 'image' || last.file_url_wsh) {
    return { text: '图片消息', at: last.created_at_wsh || '' }
  }
  return { text: '暂无对话', at: last.created_at_wsh || '' }
}

/** 为当前已加载的订单并行预取会话摘要（最多 8 笔，已缓存/在途的跳过） */
async function loadChatSummaries(orders) {
  const targets = (orders || [])
    .filter(order => order?.id_wsh != null)
    .slice(0, SUMMARY_PREFETCH_LIMIT)
    .filter((order) => {
      const key = String(order.id_wsh)
      return !Object.prototype.hasOwnProperty.call(chatSummaryCache.value, key)
        && !chatSummaryLoading.value[key]
    })
  await Promise.all(targets.map(async (order) => {
    const key = String(order.id_wsh)
    chatSummaryLoading.value = { ...chatSummaryLoading.value, [key]: true }
    try {
      const res = await getConversation(key)
      const list = res?.code === 200 && Array.isArray(res.data) ? res.data : []
      chatSummaryCache.value = { ...chatSummaryCache.value, [key]: summarizeMessages(list) }
    } catch (e) {
      chatSummaryCache.value = { ...chatSummaryCache.value, [key]: { text: '暂无对话', at: '' } }
    } finally {
      const next = { ...chatSummaryLoading.value }
      delete next[key]
      chatSummaryLoading.value = next
    }
  }))
}

/** 为当前已加载的订单并行预取每日上传进度（最多 8 笔） */
async function loadDailySummaries(orders) {
  const targets = (orders || [])
    .filter(order => order?.id_wsh != null)
    .slice(0, SUMMARY_PREFETCH_LIMIT)
    .filter(order => !Object.prototype.hasOwnProperty.call(dailyStatusCache.value, String(order.id_wsh)))
  await Promise.all(targets.map(async (order) => {
    const key = String(order.id_wsh)
    try {
      const res = await getDailyStatus(key)
      if (res?.code === 200) {
        dailyStatusCache.value = { ...dailyStatusCache.value, [key]: res.data || null }
        if (selectedOrderId.value === key) dailyStatus.value = res.data || null
      }
    } catch (e) {
      // 单笔进度失败不影响其他订单的摘要
    }
  }))
}

const ORDER_STATUS_LABELS = {
  pending: '待付款', paid: '已支付', confirmed: '待接收', received: '已接收',
  in_progress: '培养中', completed: '已完成', cancelled: '已取消',
  refunding: '退款中', refunded: '已退款',
}

function orderStatusLabel(order) {
  return ORDER_STATUS_LABELS[order?.status_wsh] || '进行中'
}

function orderTitle(order) {
  const pet = order?.pet_name_wsh
  const service = order?.service_name_wsh
  if (pet && service) return `${pet} · ${service}`
  return pet || service || '订单'
}

function orderPeriod(order) {
  const start = order?.start_date_wsh
  const end = order?.end_date_wsh
  if (start && end) return `${start} 至 ${end}`
  return start || end || '服务时间待定'
}

function chatSummaryText(order) {
  const key = String(order?.id_wsh ?? '')
  const summary = chatSummaryCache.value[key]
  if (summary) return summary.text
  // 未读数里带着最新一条未读消息的正文：预取窗口之外的订单也能显示，不用再拉一次会话
  const unread = unreadByOrder.value[key]
  if (unread?.text) return unread.text
  return chatSummaryLoading.value[key] ? '读取对话…' : '摘要未加载'
}

function dailySummaryText(order) {
  const key = String(order?.id_wsh ?? '')
  if (!Object.prototype.hasOwnProperty.call(dailyStatusCache.value, key)) return '摘要未加载'
  const status = dailyStatusCache.value[key]
  const required = Array.isArray(status?.required_days_wsh) ? status.required_days_wsh.length : 0
  if (!required) return '暂无上传计划'
  const uploaded = Array.isArray(status?.uploaded_days_wsh) ? status.uploaded_days_wsh.length : 0
  let text = `已上传 ${uploaded}/${required} 天`
  const missing = Array.isArray(status?.missing_days_wsh) ? status.missing_days_wsh : []
  if (missing.length) {
    const shown = missing.slice(0, 2).join('、')
    text += ` · 缺 ${shown}${missing.length > 2 ? ` 等${missing.length}天` : ''}`
  }
  return text
}

function formatDateTime(value) {
  return utilFormatDateTime(value, { fallback: '-' })
}
</script>

<style scoped>
/* 订单列表底部的「加载更多」：游标分页下不显示页码，只保留继续加载 */
.kw-load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}

/* ═══════════════════════════════════════════════════════
   Editorial warm — uses shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode is handled globally.
   :deep() 统一覆盖 shared keeper 面板内部外观，不改组件逻辑。
   ═══════════════════════════════════════════════════════ */
.kw-page {
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  --r-frame: 26px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.kw-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* Breadcrumb */
.kw-crumb {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.kw-crumb-link { color: var(--ref-muted); text-decoration: none; }
.kw-crumb-link:hover { color: var(--ref-ink); }
.kw-crumb-sep { color: var(--ref-line); }
.kw-crumb-here { color: var(--ref-ink-soft); }

/* 紧凑页头：标题 + 身份行 */
.kw-head { padding: 14px 0 0; }
.kw-title {
  margin: 0; font-family: var(--ref-font-display);
  font-size: clamp(24px, 3vw, 30px); font-weight: 500; line-height: 1.2;
  letter-spacing: -0.01em; color: var(--ref-ink);
}
.kw-identity {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
  margin-top: 8px;
}
.kw-identity-text { font-size: 13px; color: var(--ref-ink-soft); }
.kw-status-pill { flex: none; border-radius: 999px; }
.kw-att-btn {
  display: inline-flex; align-items: center; height: 28px; padding: 0 12px;
  border: 1px solid var(--ref-line); border-radius: 999px;
  background: var(--ref-surface); color: var(--ref-ink-soft);
  font-size: 12px; cursor: pointer;
  transition: border-color 0.15s, color 0.15s, background 0.15s;
}
.kw-att-btn:hover { border-color: var(--ref-brand); color: var(--ref-brand-deep); }

/* 未入驻 */
.kw-card {
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}
.kw-onboard { margin-top: 20px; padding: 22px 24px; display: grid; gap: 10px; justify-items: start; }
.kw-card-label { margin: 0; font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted); }
.kw-onboard-text { margin: 0; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }

/* 左窄栏 + 主内容区 */
.kw-layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 32px;
  align-items: start;
  margin-top: 20px;
}
.kw-nav { display: flex; flex-direction: column; gap: 4px; }
.kw-nav-item {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  width: 100%; padding: 11px 14px;
  border: 1px solid transparent; border-radius: var(--r-btn);
  background: transparent; color: var(--ref-ink-soft);
  font-size: 13.5px; text-align: left; cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.kw-nav-item:hover { background: var(--ref-sand); color: var(--ref-ink); }
.kw-nav-item.is-active {
  background: var(--ref-surface); border-color: var(--ref-line);
  color: var(--ref-ink); font-weight: 500;
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}
.kw-nav-count {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 20px; height: 20px; padding: 0 6px; border-radius: 999px;
  background: var(--ref-brand); color: #fff; font-size: 11px;
  font-variant-numeric: tabular-nums;
}

/* 订单分区 */
.kw-block { min-width: 0; }
.kw-block-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 10px 24px; margin-bottom: 16px; }
.kw-block-title {
  margin: 0; display: flex; align-items: center; gap: 10px;
  font-family: var(--ref-font-display); font-size: 22px; font-weight: 400;
  line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.kw-block-count {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 22px; height: 22px; padding: 0 7px; border-radius: 999px;
  background: var(--ref-brand); color: #fff; font-size: 12px;
  font-family: var(--ref-font-sans); font-variant-numeric: tabular-nums;
}
.kw-block-desc { margin: 8px 0 0; max-width: 560px; font-size: 12.5px; line-height: 1.7; color: var(--ref-muted); }
.kw-block--pool {
  padding: 24px;
  border: 1px solid var(--ref-line); border-radius: var(--r-panel);
  background: color-mix(in srgb, var(--ref-cream) 65%, var(--ref-canvas));
}
.kw-empty-inline { margin: 0 0 12px; font-size: 13px; color: var(--ref-muted); }

/* 订单状态分类（我的订单 / 订单工作区共用） */
.kw-status-tabs { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 14px; }
.kw-status-tab {
  padding: 6px 12px; border: 1px solid var(--ref-line); border-radius: 999px;
  background: var(--ref-surface); color: var(--ref-ink-soft);
  font-size: 12.5px; cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}
.kw-status-tab:hover { border-color: var(--ref-brand); color: var(--ref-brand-deep); }
.kw-status-tab.is-active {
  background: var(--ref-brand); border-color: var(--ref-brand); color: #fff;
}
/* 分类请求中的提示行：始终占位，文字出现时不会把下面的列表往下挤 */
.kw-status-hint {
  margin: -6px 0 8px; min-height: 18px;
  font-size: 12px; line-height: 18px; color: var(--ref-muted);
}
.kw-status-hint.is-busy { color: var(--ref-brand-deep); }

/* 订单工作区：左侧摘要列表 + 右侧详情（窄屏堆叠） */
.kw-workspace {
  display: grid; grid-template-columns: minmax(0, 320px) minmax(0, 1fr);
  gap: 20px; align-items: start;
}
.kw-ws-list { display: grid; gap: 8px; min-width: 0; }
.kw-ws-card {
  display: grid; gap: 3px; width: 100%; padding: 12px 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); color: var(--ref-ink-soft);
  text-align: left; cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.kw-ws-card:hover { border-color: color-mix(in srgb, var(--ref-ink) 28%, transparent); }
.kw-ws-card.is-active {
  border-color: var(--ref-brand);
  background: color-mix(in srgb, var(--ref-brand) 8%, var(--ref-surface));
  color: var(--ref-ink);
}
.kw-ws-card-head {
  display: flex; align-items: center; justify-content: space-between; gap: 8px; width: 100%;
}
.kw-ws-card-tags { display: inline-flex; align-items: center; gap: 6px; flex: none; }
/* 未读红点：红底白字圆形，数字就是未读消息条数；>99 显示 99+ */
.kw-unread-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px;
  background: var(--color-danger, #dc2626); color: #fff;
  font-size: 11px; font-weight: 600; line-height: 1;
  font-variant-numeric: tabular-nums;
}
.kw-ws-summary { display: grid; gap: 2px; margin-top: 4px; font-size: 11.5px; color: var(--ref-muted); }
.kw-ws-summary-row { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kw-ws-summary-row b { font-weight: 500; color: var(--ref-ink-soft); }

.kw-ws-detail {
  min-width: 0; min-height: 280px; padding: 18px;
  border: 1px solid var(--ref-line); border-radius: var(--r-panel);
  background: var(--ref-surface);
}
.kw-ws-detail-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
  padding-bottom: 12px; border-bottom: 1px solid var(--ref-line);
}
.kw-ws-detail-no { margin: 0; font-size: 11.5px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.kw-ws-detail-title {
  margin: 4px 0 0; font-family: var(--ref-font-display); font-size: 17px; font-weight: 500; color: var(--ref-ink);
}
.kw-ws-detail-sub { margin: 4px 0 0; font-size: 12px; color: var(--ref-muted); }
.kw-ws-back {
  flex: none; height: 30px; padding: 0 12px;
  border: 1px solid var(--ref-line); border-radius: 999px;
  background: var(--ref-surface); color: var(--ref-ink-soft);
  font-size: 12px; cursor: pointer;
}
.kw-ws-back:hover { border-color: var(--ref-brand); color: var(--ref-brand-deep); }

/* 详情区「对话 / 照护动态」分段切换 */
.kw-seg {
  display: inline-flex; gap: 2px; margin: 14px 0;
  padding: 3px; border-radius: 999px; background: var(--ref-sand);
}
.kw-seg-btn {
  padding: 6px 16px; border: 0; border-radius: 999px;
  background: transparent; color: var(--ref-ink-soft);
  font-size: 12.5px; cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.kw-seg-btn.is-active {
  background: var(--ref-surface); color: var(--ref-ink); font-weight: 500;
  box-shadow: 0 1px 2px rgba(13, 33, 26, 0.08);
}

/* 订单卡上的状态与时间（工作区摘要卡复用） */
.kw-order-pick-no { font-size: 11.5px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.kw-order-pick-name { font-size: 13px; font-weight: 500; }
.kw-order-pick-status {
  flex: none; padding: 1px 7px; border-radius: 999px;
  background: color-mix(in srgb, var(--ref-brand) 10%, transparent);
  color: var(--ref-brand-deep); font-size: 10.5px; line-height: 1.6;
}
.kw-order-pick-sub { font-size: 11.5px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }

/* 对话 */
.kw-chat { display: grid; gap: 14px; }
.kw-input {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.kw-input:focus {
  outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.kw-chat-box { display: grid; gap: 10px; max-height: 380px; overflow-y: auto; padding-right: 4px; }
.kw-chat-empty { padding: 32px 0; text-align: center; font-size: 13px; color: var(--ref-muted); }
.kw-msg {
  max-width: 78%; justify-self: start;
  padding: 10px 14px; border-radius: 4px 14px 14px 14px;
  background: var(--ref-sand); color: var(--ref-ink-soft); font-size: 13px;
}
.kw-msg.is-me { justify-self: end; background: var(--ref-brand); color: #fff; border-radius: 14px 4px 14px 14px; }
.kw-msg-text { margin: 0; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.kw-msg-time { display: block; margin-top: 4px; font-size: 10.5px; opacity: 0.6; }
.kw-chat-input { display: grid; grid-template-columns: 1fr auto; gap: 10px; }

/* CTA */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }

/* Badges (page-level + shared panels) */
.badge,
:deep(.badge) {
  display: inline-flex; align-items: center; gap: 6px;
  border-radius: var(--r-tag); border: 1px solid transparent;
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.badge-success, :deep(.badge-success) { background: color-mix(in srgb, var(--ref-moss, #3f5347) 12%, transparent); color: var(--ref-moss, #3f5347); border-color: color-mix(in srgb, var(--ref-moss, #3f5347) 25%, transparent); }
.badge-warning, :deep(.badge-warning) { background: color-mix(in srgb, #b45309 10%, transparent); color: #b45309; border-color: color-mix(in srgb, #b45309 26%, transparent); }
.badge-danger, :deep(.badge-danger), .badge-error, :deep(.badge-error) { background: color-mix(in srgb, #b3402a 8%, transparent); color: color-mix(in srgb, #b3402a 92%, transparent); border-color: color-mix(in srgb, #b3402a 28%, transparent); }
.badge-info, :deep(.badge-info) { background: color-mix(in srgb, var(--ref-brand) 10%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand) 22%, transparent); }
.badge-primary, :deep(.badge-primary) { background: color-mix(in srgb, var(--ref-brand) 12%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand) 28%, transparent); }
.badge-secondary, :deep(.badge-secondary) { background: color-mix(in srgb, var(--ref-ink) 6%, transparent); color: var(--ref-ink-soft); border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent); }
.badge-disabled, :deep(.badge-disabled) { background: var(--ref-sand); color: var(--ref-muted); border-color: var(--ref-line); }

/* ═══════════════════════════════════════════════════════
   :deep() overrides for shared keeper panels
   ═══════════════════════════════════════════════════════ */

/* generic card inside panels */
:deep(.card) {
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}
:deep(.card:hover) { box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06); transform: none; }
:deep(.section-card) { padding: 22px; display: grid; gap: 14px; }
:deep(.section-card h3) { margin: 0; font-family: var(--ref-font-display); font-size: 17px; font-weight: 500; color: var(--ref-ink); }

/* buttons inside panels */
:deep(.btn) {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  min-height: 36px; padding: 8px 16px;
  border-radius: var(--r-btn); border: 1px solid transparent;
  font-size: 13px; font-weight: 500; cursor: pointer; white-space: nowrap;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
:deep(.btn-sm) { min-height: 34px; padding: 6px 14px; font-size: 12.5px; }
:deep(.btn-primary) { background: var(--ref-brand); color: #fff; box-shadow: none; }
:deep(.btn-primary:hover) { background: var(--ref-brand-deep); opacity: 1; transform: none; }
:deep(.btn-outline) { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
:deep(.btn-outline:hover) { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); }
:deep(.btn-secondary) { background: var(--ref-surface); color: var(--ref-brand-deep); border: 1px solid color-mix(in srgb, var(--ref-brand) 35%, transparent); }
:deep(.btn-secondary:hover) { background: color-mix(in srgb, var(--ref-brand) 10%, transparent); color: var(--ref-brand-deep); }
:deep(.btn-success) { background: var(--ref-moss, #3f5347); color: #fff; }
:deep(.btn-success:hover) { opacity: 0.88; transform: none; }
:deep(.btn-danger) { background: color-mix(in srgb, #b3402a 12%, transparent); color: #b3402a; border: 1px solid color-mix(in srgb, #b3402a 30%, transparent); }
:deep(.btn-danger:hover) { background: color-mix(in srgb, #b3402a 20%, transparent); opacity: 1; transform: none; }
:deep(.btn:disabled) { opacity: 0.5; cursor: not-allowed; }

/* form controls inside panels */
:deep(.form-control), :deep(.code-input) {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
:deep(.form-control:focus), :deep(.code-input:focus) {
  outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
:deep(.form-control) { height: auto; min-height: 42px; }
:deep(textarea.form-control) { padding: 10px 14px; resize: vertical; line-height: 1.6; }
:deep(.code-input) { max-width: 180px; font-size: 22px; letter-spacing: 4px; text-align: center; font-variant-numeric: tabular-nums; }

/* 动态上传与时间线 */
:deep(.upload-field) { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--ref-muted); font-size: 12.5px; }
/* 照片网格的列宽与最大宽度由各 keeper 组件自己定义，这里只补 warm 边框与圆角 */
:deep(.photo-preview-grid img) { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: var(--r-btn); border: 1px solid var(--ref-line); background: var(--ref-sand); }

/* attendance panel（只在对话框里出现） */
:deep(.attendance-panel) {
  display: grid; gap: 12px; padding: 4px 0 0; margin: 0;
  border: 0; background: transparent; box-shadow: none;
}
:deep(.attendance-main) { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
:deep(.attendance-main h3) { margin: 0; font-family: var(--ref-font-display); font-size: 17px; font-weight: 500; color: var(--ref-ink); }
:deep(.attendance-meta), :deep(.attendance-address) { margin: 6px 0 0; font-size: 12px; line-height: 1.6; color: var(--ref-muted); }
:deep(.attendance-records) { display: grid; gap: 6px; padding-top: 12px; border-top: 1px solid var(--ref-line); }
:deep(.attendance-record) { display: grid; grid-template-columns: 1fr 1fr 72px; gap: 8px; font-size: 12px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }

/* orders tab */
:deep(.order-list) { display: grid; gap: 14px; }
:deep(.order-card) { padding: 20px; display: grid; gap: 0; }
:deep(.order-card__header) { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
:deep(.order-card__header h3) { margin: 4px 0 0; font-size: 16px; font-family: var(--ref-font-display); font-weight: 500; color: var(--ref-ink); }
:deep(.order-no) { color: var(--ref-muted); font-size: 11.5px; }
:deep(.order-grid) { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px 16px; margin: 16px 0; font-size: 13px; color: var(--ref-ink); }
:deep(.order-grid span) { display: block; margin-bottom: 3px; color: var(--ref-muted); font-size: 11px; letter-spacing: 0.04em; }
:deep(.order-waiting-note) { color: var(--ref-muted); font-size: 12.5px; align-self: center; }
:deep(.order-actions) { display: flex; flex-wrap: wrap; gap: 8px; }
:deep(.payment-countdown) {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  margin-top: 12px; padding: 10px 12px;
  border: 1px solid color-mix(in srgb, #b45309 30%, transparent); border-radius: var(--r-btn);
  background: color-mix(in srgb, #b45309 6%, var(--ref-surface));
  font-size: 12.5px; color: var(--ref-ink-soft);
}
:deep(.payment-countdown span), :deep(.payment-countdown small) { color: var(--ref-muted); }
:deep(.payment-countdown strong) { color: #b45309; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-variant-numeric: tabular-nums; }

/* modals inside orders tab (receive / start) */
:deep(.modal-overlay) {
  position: fixed; inset: 0; z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  padding: 20px; background: rgba(10, 8, 6, 0.5); backdrop-filter: blur(2px);
  animation: kw-fade 0.15s ease;
}
:deep(.modal) {
  width: 100%; max-width: 420px; max-height: min(90vh, 720px); overflow-y: auto;
  padding: 24px; border-radius: var(--r-panel); background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: var(--shadow-pop);
  animation: kw-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
:deep(.modal h2) { margin: 0 0 8px; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
:deep(.modal-hint) { margin: 0 0 14px; color: var(--ref-muted); font-size: 12.5px; }
:deep(.mini-modal) { display: grid; gap: 14px; }
:deep(.modal-actions) { display: flex; flex-wrap: wrap; gap: 10px; justify-content: flex-end; margin-top: 6px; }

/* daily tab：列宽由 KeeperDailyTab 自己持有，页面不再覆盖，避免 locked 单列被压成 240–320px */
:deep(.daily-card) { border: 1px solid var(--ref-line); border-radius: var(--r-card); padding: 14px; background: color-mix(in srgb, var(--ref-cream) 50%, var(--ref-surface)); }
:deep(.daily-card strong) { font-size: 13px; color: var(--ref-ink); }
:deep(.daily-card p) { margin: 6px 0 0; font-size: 12.5px; color: var(--ref-ink-soft); }
:deep(.record-tabs) { display: flex; flex-wrap: wrap; gap: 8px; }
:deep(.timeline) { display: grid; gap: 10px; margin-top: 8px; }
:deep(.timeline-item) { border: 1px solid var(--ref-line); border-radius: var(--r-card); padding: 14px; background: var(--ref-surface); }
:deep(.timeline-item p) { margin: 6px 0; font-size: 13px; color: var(--ref-ink-soft); line-height: 1.6; }
:deep(.timeline-item small), :deep(.timeline-item__meta) { color: var(--ref-muted); font-size: 11.5px; word-break: break-all; }
:deep(.timeline-photos) { margin-top: 8px; max-width: 420px; }
:deep(.empty-inline) { color: var(--ref-muted); font-size: 13px; padding: 12px 0; }

/* empty / loading states inside panels */
:deep(.loading) { text-align: center; padding: 44px 20px; color: var(--ref-muted); font-size: 13px; }
:deep(.empty-state) { text-align: center; padding: 56px 24px; color: var(--ref-muted); }
:deep(.empty-state .icon) { font-size: 40px; margin-bottom: 14px; opacity: 0.7; }
:deep(.empty-state h3) { margin: 0 0 8px; font-family: var(--ref-font-display); font-size: 18px; font-weight: 500; color: var(--ref-ink); }
:deep(.empty-state p) { margin: 0; font-size: 13px; color: var(--ref-muted); }

/* Animations */
@keyframes kw-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes kw-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* Responsive：窄屏导航转横向，主体保持普通页面滚动 */
@media (max-width: 900px) {
  .kw-layout { grid-template-columns: 1fr; gap: 16px; margin-top: 16px; }
  .kw-workspace { grid-template-columns: 1fr; }
  .kw-nav {
    flex-direction: row; gap: 6px;
    overflow-x: auto; padding-bottom: 4px;
    scrollbar-width: thin;
  }
  .kw-nav-item { width: auto; flex: 0 0 auto; padding: 8px 14px; white-space: nowrap; }
}
@media (max-width: 640px) {
  :deep(.attendance-main) { align-items: flex-start; flex-direction: column; }
  :deep(.attendance-record) { grid-template-columns: 1fr; }
  :deep(.order-grid) { grid-template-columns: 1fr; }
}
@media (max-width: 520px) {
  .kw-shell { padding: 0 16px; }
  .kw-block--pool { padding: 18px; }
  .kw-chat-input { grid-template-columns: 1fr; }
  .kw-ws-detail { padding: 14px; }
}
@media (prefers-reduced-motion: reduce) {
  .kw-msg, .kw-card, :deep(.modal), :deep(.modal-overlay) { animation: none !important; transition: none; }
}
</style>
