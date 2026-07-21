<template>
  <div class="keeper-page">
    <PageHero
      title="看护工作台"
      subtitle="接单、收宠、日常记录和沟通"
    />

    <div v-if="keeperInfo" class="status-bar">
      <div class="status-label">当前状态</div>
      <span :class="['badge', getStatusBadge(KeeperOnlineStatus, keeperInfo.status_wsh)]">
        {{ getStatusLabel(KeeperOnlineStatus, keeperInfo.status_wsh) }}
      </span>
      <span class="status-hint">状态会随商家开店、关店自动切换，忙碌仍按当前接单量计算。</span>
    </div>

    <KeeperAttendancePanel v-if="keeperInfo?.id_wsh" />

    <div class="toolbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['btn', activeTab === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
      <button class="btn btn-outline btn-sm" @click="goKeeperProfile">
        <el-icon><User /></el-icon>
        <span>我的资料</span>
      </button>
    </div>

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

    <section v-if="activeTab === 'chat'" class="card section-card">
      <h3>订单聊天</h3>
      <select v-model="selectedChatOrderId" class="form-control" @change="loadConversation">
        <option value="">请选择订单</option>
        <option v-for="order in myOrders" :key="order.id_wsh" :value="order.id_wsh">
          {{ order.order_no_wsh }} - {{ order.pet_name_wsh || order.service_name_wsh }}
        </option>
      </select>
      <div v-if="selectedChatOrderId" class="chat-box">
        <div v-if="conversation.length === 0" class="empty-inline">暂无消息</div>
        <div
          v-for="message in conversation"
          :key="message.id_wsh"
          :class="['chat-message', message.from_user_id_wsh === authStore.user?.id_wsh ? 'is-me' : '']"
        >
          <p>{{ message.content_wsh }}</p>
          <small>{{ formatDateTime(message.created_at_wsh) }}</small>
        </div>
      </div>
      <div v-if="selectedChatOrderId" class="chat-input">
        <input v-model="chatText" class="form-control" placeholder="和对方沟通...">
        <button class="btn btn-primary btn-sm" @click="sendMessage">发送</button>
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
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { KeeperOnlineStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import PageHero from '@/components/common/PageHero.vue'
import KeeperProfileTab from '@/components/keeper/KeeperProfileTab.vue'
import KeeperCertTab from '@/components/keeper/KeeperCertTab.vue'
import KeeperAttendancePanel from '@/components/keeper/KeeperAttendancePanel.vue'
import KeeperOrdersTab from '@/components/keeper/KeeperOrdersTab.vue'
import KeeperDailyTab from '@/components/keeper/KeeperDailyTab.vue'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, hasExpiredPaymentTimeout } from '@/utils/orderPaymentTimeout'

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
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function goKeeperProfile() {
  router.push('/keeper/profile')
}
</script>

<style scoped>
.status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #dee0e3;
  border-radius: 6px;
}
.status-label {
  font-size: 13px;
  font-weight: 500;
  color: #646a73;
}
.status-hint {
  font-size: 12px;
  color: #8f959e;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}
.section-card {
  padding: 22px;
  display: grid;
  gap: 14px;
}
.chat-box {
  display: grid;
  gap: 8px;
  max-height: 360px;
  overflow: auto;
}
.chat-message {
  max-width: 76%;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--color-muted);
}
.chat-message.is-me {
  justify-self: end;
  background: var(--color-primary);
  color: #fff;
}
.chat-message p {
  margin: 0;
}
.chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}
.empty-inline {
  color: var(--color-muted-foreground);
  font-size: 13px;
}
@media (max-width: 760px) {
  .chat-input {
    grid-template-columns: 1fr;
  }
}
</style>
