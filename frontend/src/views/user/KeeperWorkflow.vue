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

      <!-- ═══ Page header ═══ -->
      <header class="kw-head">
        <div class="kw-head-copy">
          <div class="kw-eyebrow" aria-hidden="true">
            <span class="kw-eyebrow-line"></span>
            <span>寄养工作台</span>
          </div>
          <h1 class="kw-title">照护师工作台</h1>
          <p class="kw-sub">
            {{ keeperInfo
              ? `${keeperInfo.merchant_name_wsh || '门店'} · ${keeperInfo.name_wsh || '照护师'}。接单状态与打卡会影响派单，请保持真实。`
              : '接单状态与打卡会影响派单，请保持真实。' }}
          </p>
        </div>
        <dl v-if="keeperInfo" class="kw-facts" aria-label="工作台概览">
          <div>
            <dd class="tabular">{{ pendingOrders.length }}</dd>
            <dt>待接单</dt>
          </div>
          <div>
            <dd class="tabular">{{ keeperInfo.current_pets_wsh ?? 0 }} / {{ keeperInfo.max_pets_wsh ?? 0 }}</dd>
            <dt>在照护</dt>
          </div>
          <div>
            <dd class="tabular">{{ keeperInfo.completion_rate_wsh ?? '-' }}%</dd>
            <dt>完成率</dt>
          </div>
        </dl>
      </header>

      <!-- ═══════════════════════════════════════════
           01 · 今日
           ═══════════════════════════════════════════ -->
      <section class="kw-section" aria-label="今日状态">
        <header class="kw-sec-head">
          <div class="kw-head-copy">
            <p class="kw-eyebrow">
              <span class="kw-idx">01</span>
              <span class="kw-line" aria-hidden="true"></span>
              <span>今日</span>
            </p>
            <h2 class="kw-sec-title">今日状态</h2>
            <p class="kw-sec-desc">接单状态与考勤打卡都会实时同步给门店。</p>
          </div>
        </header>

        <div class="kw-today-grid">
          <div v-if="keeperInfo" class="kw-card kw-status-card">
            <p class="kw-card-label">接单状态</p>
            <p class="kw-status-badge">
              <span :class="['badge', getStatusBadge(KeeperOnlineStatus, keeperInfo.status_wsh)]">
                {{ getStatusLabel(KeeperOnlineStatus, keeperInfo.status_wsh) }}
              </span>
            </p>
            <p class="kw-status-note">状态会随商家开店、关店自动切换，忙碌仍按当前接单量计算。离线期间不会收到新的派单，已接单的服务仍需按约定完成。</p>
          </div>

          <KeeperAttendancePanel v-if="keeperInfo?.id_wsh" class="kw-att-panel" />
        </div>
      </section>

      <!-- ═══════════════════════════════════════════
           02 · Workspace
           ═══════════════════════════════════════════ -->
      <section class="kw-section" aria-label="工作台">
        <header class="kw-sec-head">
          <div class="kw-head-copy">
            <p class="kw-eyebrow">
              <span class="kw-idx">02</span>
              <span class="kw-line" aria-hidden="true"></span>
              <span>{{ { cert: 'Credentials', orders: 'Tasks', records: 'Daily', chat: 'Chat', profile: 'Profile' }[activeTab] || 'Workspace' }}</span>
            </p>
            <h2 class="kw-sec-title">{{ tabs.find(t => t.key === activeTab)?.label || '工作台' }}</h2>
            <p class="kw-sec-desc">{{ { cert: '提交照护资质与从业信息，通过门店审核后即可开始接单。', orders: '处理待接订单，跟进在照护服务的收宠、开工与完成等每个环节。', records: '记录每日照护动态与照片，家长可实时查看毛孩子的进展。', chat: '与家长即时沟通，确认服务细节、反馈与临时需求。', profile: '维护你的公开资料与头像，保持信息真实可联系。' }[activeTab] || '管理你的照护师工作台。' }}</p>
          </div>
        </header>

        <div class="kw-toolbar" role="group" aria-label="工作台栏目">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="['kw-chip', activeTab === tab.key ? 'active' : '']"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
          <button type="button" class="kw-chip kw-chip-ghost" @click="goKeeperProfile">
            <AppIcon><User /></AppIcon>
            <span>我的资料</span>
          </button>
        </div>

        <div class="kw-content">
          <KeeperCertTab
            v-if="activeTab === 'cert'"
            :merchants="merchants"
            :keeperInfo="keeperInfo"
            @submit="submitCertification"
          />

          <KeeperOrdersTab
            v-if="activeTab === 'orders'"
            :pendingOrders="pendingOrders"
            :activeOrders="myOrders"
            :loading="orderLoading"
            :completingId="completingId"
            :subTab="orderSubTab"
            :receiving="receiving"
            :startUploading="startUploading"
            :processingOrderNo="processingOrderNo"
            :modalKey="orderModalKey"
            :now-ms="nowMs"
            @update:subTab="orderSubTab = $event"
            @accept="acceptOrder"
            @reject="rejectOrder"
            @receive="handleReceive"
            @start="handleStart"
            @complete="completeService"
            @select-order="selectOrder"
          />

          <KeeperDailyTab
            v-if="activeTab === 'records'"
            :orderId="selectedOrderId"
            :myOrders="myOrders"
            :dailyStatus="dailyStatus"
            :careRecords="careRecords"
            :recordUploading="recordUploading"
            :submitKey="recordSubmitKey"
            @update:orderId="onSelectedOrderChange"
            @upload-timeline="submitRecord"
            @refresh="loadSelectedOrderData"
          />

          <section v-if="activeTab === 'chat'" class="kw-chat">
            <div class="kw-chat-head">
              <p class="kw-card-label">订单聊天</p>
              <select v-model="selectedChatOrderId" class="kw-select" @change="loadConversation">
                <option value="">请选择订单</option>
                <option v-for="order in myOrders" :key="order.id_wsh" :value="order.id_wsh">
                  {{ order.order_no_wsh }} - {{ order.pet_name_wsh || order.service_name_wsh }}
                </option>
              </select>
            </div>

            <div v-if="selectedChatOrderId" class="kw-chat-box">
              <div v-if="conversation.length === 0" class="kw-chat-empty">暂无消息，等待对方回复。</div>
              <div
                v-for="message in conversation"
                :key="message.id_wsh"
                :class="['kw-msg', message.from_user_id_wsh === authStore.user?.id_wsh ? 'is-me' : '']"
              >
                <p class="kw-msg-text">{{ message.content_wsh }}</p>
                <small class="kw-msg-time">{{ formatDateTime(message.created_at_wsh) }}</small>
              </div>
            </div>

            <div v-if="selectedChatOrderId" class="kw-chat-input">
              <input v-model="chatText" class="kw-input" placeholder="和对方沟通..." @keyup.enter="sendMessage">
              <button type="button" class="cta cta-primary" @click="sendMessage">发送</button>
            </div>
          </section>

          <KeeperProfileTab
            v-if="activeTab === 'profile'"
            :profile="profile"
            :uploading="keeperAvatarUploading"
            @save="updateProfile"
            @avatar-upload="uploadKeeperAvatar"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { KeeperOnlineStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import KeeperProfileTab from '@/components/keeper/KeeperProfileTab.vue'
import KeeperCertTab from '@/components/keeper/KeeperCertTab.vue'
import KeeperAttendancePanel from '@/components/keeper/KeeperAttendancePanel.vue'
import KeeperOrdersTab from '@/components/keeper/KeeperOrdersTab.vue'
import KeeperDailyTab from '@/components/keeper/KeeperDailyTab.vue'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, hasExpiredPaymentTimeout } from '@/utils/orderPaymentTimeout'
import { formatDateTime as utilFormatDateTime } from '@/utils/format'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const activeTab = ref('cert')
const orderSubTab = ref('pending')
const orderLoading = ref(false)
const pendingOrders = ref([])
const myOrders = ref([])
const keeperInfo = ref(null)
const merchants = ref([])
const selectedOrderId = ref('')
const selectedChatOrderId = ref('')
const careRecords = ref([])
const dailyStatus = ref(null)
const conversation = ref([])
const chatText = ref('')
const recordUploading = ref(false)
const keeperAvatarUploading = ref(false)
const completingId = ref(null)
const processingOrderNo = ref('')
const receiving = ref(false)
const startUploading = ref(false)
const orderModalKey = ref(0)
const recordSubmitKey = ref(0)
const nowMs = ref(Date.now())
let countdownTimer = null
let nextPaymentTimeoutRefreshAt = 0

const profile = reactive({ nickname_wsh: '', avatar_wsh: '' })

const tabs = [
  { key: 'cert', label: '资质' },
  { key: 'orders', label: '订单' },
  { key: 'records', label: '日常动态' },
  { key: 'chat', label: '聊天' },
  { key: 'profile', label: '资料' },
]

onMounted(async () => {
  countdownTimer = window.setInterval(() => {
    nowMs.value = Date.now()
    refreshExpiredPaymentOrders()
  }, 1000)
  await Promise.all([loadProfile(), loadKeeperInfo(), loadMerchants(), loadPendingOrders(), loadMyOrders()])
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function loadProfile() {
  const res = await authStore.apiGet('/api/users/me')
  if (res.code === 200 && res.data) {
    profile.nickname_wsh = res.data.nickname_wsh || ''
    profile.avatar_wsh = keeperInfo.value?.avatar_wsh || res.data.avatar_wsh || ''
  }
}

async function updateProfile(nickname) {
  const res = await authStore.apiPut('/api/users/me', { nickname_wsh: nickname })
  if (res.code === 200 && keeperInfo.value?.id_wsh && profile.avatar_wsh) {
    await authStore.apiPut(`/api/keepers/${keeperInfo.value.id_wsh}`, { avatar_wsh: profile.avatar_wsh })
    keeperInfo.value = { ...keeperInfo.value, avatar_wsh: profile.avatar_wsh }
  }
  if (res.code === 200) appStore.addToast('资料已保存', 'success')
}

async function loadKeeperInfo() {
  const res = await authStore.apiGet('/api/keepers/me')
  if (res.code === 200 && res.data) {
    keeperInfo.value = res.data
    profile.avatar_wsh = res.data.avatar_wsh || profile.avatar_wsh
  }
}

async function loadMerchants() {
  const res = await authStore.apiGet('/api/merchants')
  if (res.code === 200) merchants.value = res.data || []
}

async function submitCertification(form) {
  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.APPLY_KEEPER, { authStore, appStore, router })
  if (!ok) return
  const res = await authStore.apiPost('/api/keepers', { ...form, status_wsh: 1 })
  if (res.code === 200) {
    keeperInfo.value = res.data
    appStore.addToast('认证已提交', 'success')
  }
}

async function loadPendingOrders() {
  orderLoading.value = true
  try {
    const res = await authStore.apiGet('/api/orders/pending')
    if (res.code === 200) pendingOrders.value = res.data || []
  } finally {
    orderLoading.value = false
  }
}

async function loadMyOrders() {
  const res = await authStore.apiGet('/api/orders/my-keeper')
  if (res.code === 200) myOrders.value = res.data || []
}

async function refreshOrders() {
  await Promise.all([loadPendingOrders(), loadMyOrders()])
}

function refreshExpiredPaymentOrders() {
  if (
    orderLoading.value
    || nowMs.value < nextPaymentTimeoutRefreshAt
    || !hasExpiredPaymentTimeout(pendingOrders.value, nowMs.value)
  ) {
    return
  }
  nextPaymentTimeoutRefreshAt = nowMs.value + PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS
  void refreshOrders()
}

async function acceptOrder(order) {
  if (!order?.order_no_wsh || processingOrderNo.value) return
  processingOrderNo.value = order.order_no_wsh
  try {
    const res = await authStore.apiPost('/api/orders/accept', { order_no_wsh: order.order_no_wsh })
    if (res.code === 200) {
      appStore.addToast('接单成功', 'success')
      await refreshOrders()
    } else {
      appStore.addToast(res.message || '接单失败', 'error')
      await refreshOrders()
    }
  } catch (e) {
    appStore.addToast(e?.message || '接单失败', 'error')
    await refreshOrders()
  } finally {
    processingOrderNo.value = ''
  }
}

async function rejectOrder(order) {
  if (!order?.order_no_wsh || processingOrderNo.value) return
  processingOrderNo.value = order.order_no_wsh
  try {
    const res = await authStore.apiPost('/api/orders/reject', { order_no_wsh: order.order_no_wsh })
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

async function handleReceive(orderNo, handoverCode) {
  receiving.value = true
  try {
    const location = await getCurrentAddress()
    const res = await authStore.apiPost('/api/orders/received', {
      order_no_wsh: orderNo,
      handover_code_wsh: handoverCode,
      received_address_wsh: location.address_wsh,
    })
    if (res.code === 200) {
      appStore.addToast('已确认收宠', 'success')
      orderModalKey.value++
      await refreshOrders()
    }
  } catch (error) {
    appStore.addToast(error.message || '请先定位后再确认收宠', 'warning')
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
    const res = await authStore.apiPost('/api/orders/start/upload', formData)
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

async function completeService(order) {
  if (!order?.order_no_wsh || processingOrderNo.value) return
  processingOrderNo.value = order.order_no_wsh
  completingId.value = order.id_wsh
  try {
    const res = await authStore.apiPost('/api/orders/complete', { order_no_wsh: order.order_no_wsh })
    if (res.code === 200) appStore.addToast('订单已完成，AI 报告将自动生成', 'success')
    else appStore.addToast(res.message || '操作失败', 'error')
    await refreshOrders()
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  } finally {
    completingId.value = null
    processingOrderNo.value = ''
  }
}

async function selectOrder(order) {
  activeTab.value = 'records'
  selectedOrderId.value = String(order.id_wsh)
  await loadSelectedOrderData()
}

async function onSelectedOrderChange(val) {
  selectedOrderId.value = val
  await loadSelectedOrderData()
}

async function loadSelectedOrderData() {
  if (!selectedOrderId.value) return
  const [recordsRes, statusRes] = await Promise.all([
    authStore.apiGet(`/api/order-fulfillments/${selectedOrderId.value}/timeline`),
    authStore.apiGet(`/api/order-fulfillments/${selectedOrderId.value}/daily-status`),
  ])
  if (recordsRes.code === 200) careRecords.value = recordsRes.data || []
  if (statusRes.code === 200) dailyStatus.value = statusRes.data
}

async function submitRecord(data) {
  recordUploading.value = true
  try {
    const formData = new FormData()
    formData.append('type_wsh', data.type)
    formData.append('content_wsh', data.content)
    data.files.forEach(file => formData.append('files', file))
    const res = await authStore.apiPost(`/api/order-fulfillments/${data.orderId}/timeline/upload`, formData)
    if (res.code === 200) {
      appStore.addToast('动态已上传', 'success')
      recordSubmitKey.value++
      await loadSelectedOrderData()
    }
  } finally {
    recordUploading.value = false
  }
}

async function uploadKeeperAvatar(file) {
  keeperAvatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await authStore.apiPost('/api/files/upload?directory=keepers', formData)
    if (res.code === 200 && res.data?.url_wsh) {
      profile.avatar_wsh = res.data.url_wsh
      appStore.addToast('头像上传成功', 'success')
    }
  } finally {
    keeperAvatarUploading.value = false
  }
}

async function loadConversation() {
  if (!selectedChatOrderId.value) return
  const res = await authStore.apiGet(`/api/order-fulfillments/${selectedChatOrderId.value}/conversation`)
  if (res.code === 200) conversation.value = res.data || []
}

async function sendMessage() {
  if (!selectedChatOrderId.value || !chatText.value.trim()) return
  const res = await authStore.apiPost(`/api/order-fulfillments/${selectedChatOrderId.value}/conversation`, {
    content_wsh: chatText.value,
  })
  if (res.code === 200) {
    chatText.value = ''
    await loadConversation()
  }
}

function formatDateTime(value) {
  return utilFormatDateTime(value, { fallback: '-' })
}

function goKeeperProfile() {
  router.push('/keeper/profile')
}
</script>

<style scoped>
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

/* ═══ Breadcrumb ═══ */
.kw-crumb {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.kw-crumb-link { color: var(--ref-muted); text-decoration: none; }
.kw-crumb-link:hover { color: var(--ref-ink); }
.kw-crumb-sep { color: var(--ref-line); }
.kw-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Page header ═══ */
.kw-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 32px; padding: 22px 0 8px;
}
.kw-head-copy { min-width: 0; }
.kw-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.kw-eyebrow-line { width: 24px; height: 1px; background: var(--ref-line); }
.kw-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(28px, 3.6vw, 38px); font-weight: 500; line-height: 1.15;
  letter-spacing: -0.01em; color: var(--ref-ink);
}
.kw-sub { margin: 12px 0 0; max-width: 620px; font-size: 13px; line-height: 1.8; color: var(--ref-ink-soft); opacity: 0.85; }
.kw-facts {
  display: flex; gap: 0; flex-shrink: 0; margin: 0;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); overflow: hidden; background: var(--ref-surface);
}
.kw-facts div { padding: 14px 22px; text-align: center; border-left: 1px solid var(--ref-line); }
.kw-facts div:first-child { border-left: 0; }
.kw-facts dd { margin: 0; font-family: var(--ref-font-display); font-size: 22px; font-weight: 400; line-height: 1; color: var(--ref-ink); font-variant-numeric: tabular-nums; }
.kw-facts dt { margin: 8px 0 0; font-size: 11px; letter-spacing: 0.14em; color: var(--ref-muted); }

/* ═══ Sections ═══ */
.kw-section { margin-top: 48px; }
.kw-sec-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 12px 24px; }
.kw-idx { font-variant-numeric: tabular-nums; }
.kw-line { width: 24px; height: 1px; background: var(--ref-line); }
.kw-sec-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.kw-sec-desc { margin: 8px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }

/* ═══ 今日 ═══ */
.kw-today-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr); gap: 16px; margin-top: 20px; align-items: stretch; }
.kw-card {
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}
.kw-status-card { padding: 24px; }
.kw-card-label { margin: 0; font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted); }
.kw-status-badge { margin: 14px 0 0; }
.kw-status-note { margin: 16px 0 0; padding-top: 14px; border-top: 1px solid var(--ref-line); font-size: 12px; line-height: 1.8; color: var(--ref-muted); }

/* ═══ Toolbar ═══ */
.kw-toolbar { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.kw-chip {
  display: inline-flex; align-items: center; gap: 6px;
  height: 38px; padding: 0 18px; border-radius: var(--r-btn);
  background: var(--ref-surface); border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.kw-chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 28%, transparent); color: var(--ref-ink); }
.kw-chip.active { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.kw-chip-ghost { background: transparent; }
.kw-chip .el-icon { font-size: 15px; }

.kw-content { margin-top: 20px; }

/* ═══ Chat (page-level) ═══ */
.kw-chat {
  padding: 24px; display: grid; gap: 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
}
.kw-chat-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
.kw-select, .kw-input {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.kw-select { max-width: 360px; }
.kw-select:focus, .kw-input:focus {
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

/* ═══ CTA ═══ */
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

/* ═══ Badges (page-level + shared panels) ═══ */
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

/* form grid inside cert / profile tabs */
:deep(.form-grid) { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
:deep(.form-grid label) { display: grid; gap: 6px; font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
:deep(.form-grid__wide) { grid-column: 1 / -1; }
:deep(.hint) { font-size: 11.5px; color: var(--ref-muted); }
:deep(.profile-summary) { display: grid; gap: 8px; color: var(--ref-ink-soft); font-size: 13px; }
:deep(.profile-summary strong) { color: var(--ref-ink); font-family: var(--ref-font-display); font-size: 18px; font-weight: 500; }
:deep(.qualification-list) { display: flex; flex-wrap: wrap; gap: 6px; }
:deep(.upload-field) { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--ref-muted); font-size: 12.5px; }
:deep(.photo-preview-grid) { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
:deep(.photo-preview-grid.single) { max-width: 180px; }
:deep(.photo-preview-grid img) { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: var(--r-btn); border: 1px solid var(--ref-line); background: var(--ref-sand); }

/* attendance panel */
:deep(.attendance-panel) {
  height: 100%;
  display: grid; gap: 12px; padding: 24px; margin: 0;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}
:deep(.attendance-main) { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
:deep(.attendance-main h3) { margin: 0; font-family: var(--ref-font-display); font-size: 17px; font-weight: 500; color: var(--ref-ink); }
:deep(.attendance-meta), :deep(.attendance-address) { margin: 6px 0 0; font-size: 12px; line-height: 1.6; color: var(--ref-muted); }
:deep(.attendance-records) { display: grid; gap: 6px; padding-top: 12px; border-top: 1px solid var(--ref-line); }
:deep(.attendance-record) { display: grid; grid-template-columns: 1fr 1fr 72px; gap: 8px; font-size: 12px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }

/* orders tab */
:deep(.sub-toolbar) { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
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

/* daily tab */
:deep(.records-grid) { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 16px; align-items: start; }
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

/* ═══ Animations ═══ */
@keyframes kw-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes kw-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .kw-today-grid { grid-template-columns: 1fr; }
  .kw-facts { width: 100%; }
  .kw-facts div { flex: 1; }
}
@media (max-width: 760px) {
  .kw-head { align-items: flex-start; }
}
@media (max-width: 640px) {
  :deep(.attendance-main) { align-items: flex-start; flex-direction: column; }
  :deep(.attendance-record) { grid-template-columns: 1fr; }
  :deep(.records-grid) { grid-template-columns: 1fr; }
  :deep(.order-grid) { grid-template-columns: 1fr; }
}
@media (max-width: 520px) {
  .kw-shell { padding: 0 16px; }
  .kw-section { margin-top: 40px; }
  .kw-status-card { padding: 20px; }
  .kw-chat { padding: 18px; }
  .kw-chat-input { grid-template-columns: 1fr; }
  .kw-select { max-width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .kw-msg, .kw-card, .kw-chip, :deep(.modal), :deep(.modal-overlay) { animation: none !important; transition: none; }
}
</style>
