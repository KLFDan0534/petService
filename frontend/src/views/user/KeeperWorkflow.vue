<template>
  <div class="keeper-page">
    <PageHero title="看护工作台" subtitle="接单、接收宠物、每日上传动态、沟通并完成服务" />

    <div class="toolbar">
      <button v-for="tab in tabs" :key="tab.key" :class="['btn', activeTab === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </div>

    <section v-if="activeTab === 'cert'" class="card section-card">
      <h3>看护资质</h3>
      <div v-if="keeperInfo" class="profile-summary">
        <strong>{{ keeperInfo.name_wsh }}</strong>
        <span>从业 {{ keeperInfo.experience_years_wsh || 0 }} 年 · ￥{{ keeperInfo.price_per_day_wsh }}/天 · 容量 {{ keeperInfo.current_pets_wsh || 0 }}/{{ keeperInfo.max_pets_wsh || '-' }}</span>
      </div>
      <div v-else class="form-grid">
        <label>
          所属商家
          <select v-model="selectedMerchantId" class="form-control">
            <option value="">请选择商家</option>
            <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">{{ merchant.name_wsh }}</option>
          </select>
        </label>
        <label>姓名<input v-model="certForm.name_wsh" class="form-control"></label>
        <label>手机<input v-model="certForm.phone_wsh" class="form-control"></label>
        <label>从业年限<input v-model.number="certForm.experience_years_wsh" type="number" class="form-control"></label>
        <label>日收费<input v-model.number="certForm.price_per_day_wsh" type="number" class="form-control"></label>
        <label>最大接单数<input v-model.number="certForm.max_pets_wsh" type="number" class="form-control"></label>
        <label class="form-grid__wide">介绍<textarea v-model="certForm.bio_wsh" class="form-control" rows="3"></textarea></label>
        <button class="btn btn-primary" :disabled="!selectedMerchantId" @click="submitCertification">提交认证</button>
      </div>
    </section>

    <section v-if="activeTab === 'orders'">
      <div class="sub-toolbar">
        <button :class="['btn', orderSubTab === 'pending' ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="orderSubTab = 'pending'">待处理</button>
        <button :class="['btn', orderSubTab === 'active' ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="orderSubTab = 'active'">我的订单</button>
      </div>

      <div v-if="orderLoading" class="loading">加载中...</div>
      <div v-else-if="visibleOrders.length === 0" class="empty-state">
        <div class="icon">📦</div>
        <h3>暂无订单</h3>
      </div>
      <div v-else class="order-list">
        <article v-for="order in visibleOrders" :key="order.id_wsh" class="card order-card">
          <header class="order-card__header">
            <div>
              <div class="order-no">{{ order.order_no_wsh }}</div>
              <h3>{{ order.service_name_wsh || '寄养订单' }}</h3>
            </div>
            <span :class="['badge', statusBadge(order.status_wsh)]">{{ statusMap[order.status_wsh] || order.status_wsh }}</span>
          </header>
          <div class="order-grid">
            <div><span>宠物</span>{{ order.pet_name_wsh || '#' + order.pet_id_wsh }}</div>
            <div><span>主人</span>{{ order.owner_name_wsh || '#' + order.owner_id_wsh }}</div>
            <div><span>服务时间</span>{{ order.start_date_wsh }} 至 {{ order.end_date_wsh }}</div>
            <div><span>送达地点</span>{{ order.delivery_address_wsh || order.merchant_address_wsh || '-' }}</div>
            <div><span>送养时间</span>{{ formatDateTime(order.delivery_time_wsh) }}</div>
            <div><span>接收截止</span>{{ formatDateTime(order.receiver_available_end_wsh) }}</div>
          </div>
          <footer class="order-actions">
            <button v-if="['pending','paid'].includes(order.status_wsh)" class="btn btn-success btn-sm" @click="acceptOrder(order)">接单</button>
            <button v-if="['pending','paid'].includes(order.status_wsh)" class="btn btn-danger btn-sm" @click="rejectOrder(order)">拒单</button>
            <button v-if="['confirmed','delivered'].includes(order.status_wsh)" class="btn btn-primary btn-sm" @click="openReceiveModal(order)">确认接收</button>
            <button v-if="order.status_wsh === 'received'" class="btn btn-primary btn-sm" @click="openStartModal(order)">开始培养</button>
            <button v-if="order.status_wsh === 'in_progress'" class="btn btn-success btn-sm" :disabled="completingId === order.id_wsh" @click="completeService(order)">完成订单</button>
            <button class="btn btn-outline btn-sm" @click="selectOrder(order)">履约记录</button>
          </footer>
        </article>
      </div>
    </section>

    <section v-if="activeTab === 'records'" class="records-grid">
      <aside class="card section-card">
        <h3>选择订单</h3>
        <select v-model="selectedOrderId" class="form-control" @change="onSelectedOrderChange">
          <option value="">请选择订单</option>
          <option v-for="order in myOrders" :key="order.id_wsh" :value="order.id_wsh">
            {{ order.service_name_wsh || '订单#' + order.id_wsh }} · {{ statusMap[order.status_wsh] || order.status_wsh }}
          </option>
        </select>
        <div v-if="dailyStatus" class="daily-card">
          <strong>每日上传进度</strong>
          <p>已上传 {{ dailyStatus.uploaded_days_wsh?.length || 0 }}/{{ dailyStatus.required_days_wsh?.length || 0 }}</p>
          <p v-if="dailyStatus.missing_days_wsh?.length">缺少：{{ dailyStatus.missing_days_wsh.join('、') }}</p>
        </div>
      </aside>

      <main v-if="selectedOrderId" class="card section-card">
        <h3>上传今日动态</h3>
        <div class="record-tabs">
          <button v-for="tab in recordTabs" :key="tab.key" :class="['btn', recordType === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="recordType = tab.key">
            {{ tab.label }}
          </button>
        </div>
        <textarea v-model="recordForm.content_wsh" class="form-control" rows="4" :placeholder="recordPlaceholder"></textarea>
        <div class="upload-field">
          <input ref="recordFileInput" type="file" accept="image/*" multiple style="display:none" @change="onRecordFilesChange">
          <button class="btn btn-outline btn-sm" @click="recordFileInput?.click()">选择照片</button>
          <span>{{ recordFiles.length ? `已选择 ${recordFiles.length} 张` : '未选择照片' }}</span>
        </div>
        <div v-if="recordFilePreviews.length" class="photo-preview-grid">
          <img v-for="preview in recordFilePreviews" :key="preview" :src="preview" alt="动态照片预览">
        </div>
        <div class="order-actions">
          <button class="btn btn-primary btn-sm" :disabled="recordUploading" @click="submitRecord">{{ recordUploading ? '上传中...' : '提交动态' }}</button>
          <button class="btn btn-outline btn-sm" @click="loadSelectedOrderData">刷新</button>
        </div>

        <div class="timeline">
          <div v-if="careRecords.length === 0" class="empty-inline">暂无动态</div>
          <article v-for="record in careRecords" :key="record.id_wsh" class="timeline-item">
            <div class="timeline-item__meta">{{ recordTypeMap[record.type_wsh] || record.type_wsh }} · {{ formatDateTime(record.record_time_wsh || record.created_at_wsh) }}</div>
            <p>{{ record.content_wsh }}</p>
            <div v-if="record.images_wsh" class="photo-preview-grid timeline-photos">
              <img v-for="url in parseImageUrls(record.images_wsh)" :key="url" :src="url" alt="动态照片">
            </div>
          </article>
        </div>
      </main>
    </section>

    <section v-if="activeTab === 'chat'" class="card section-card">
      <h3>订单聊天</h3>
      <select v-model="selectedChatOrderId" class="form-control" @change="loadConversation">
        <option value="">请选择订单</option>
        <option v-for="order in myOrders" :key="order.id_wsh" :value="order.id_wsh">{{ order.order_no_wsh }} · {{ order.pet_name_wsh || order.service_name_wsh }}</option>
      </select>
      <div v-if="selectedChatOrderId" class="chat-box">
        <div v-if="conversation.length === 0" class="empty-inline">暂无消息</div>
        <div v-for="message in conversation" :key="message.id_wsh" :class="['chat-message', message.from_user_id_wsh === authStore.user?.id_wsh ? 'is-me' : '']">
          <p>{{ message.content_wsh }}</p>
          <small>{{ formatDateTime(message.created_at_wsh) }}</small>
        </div>
      </div>
      <div v-if="selectedChatOrderId" class="chat-input">
        <input v-model="chatText" class="form-control" placeholder="和领养者/主人沟通">
        <button class="btn btn-primary btn-sm" @click="sendMessage">发送</button>
      </div>
    </section>

    <section v-if="activeTab === 'profile'" class="card section-card">
      <h3>我的资料</h3>
      <div class="form-grid">
        <label>昵称<input v-model="profileForm.nickname_wsh" class="form-control"></label>
        <label>邮箱<input v-model="profileForm.email_wsh" class="form-control"></label>
        <label>手机<input v-model="profileForm.phone_wsh" class="form-control"></label>
      </div>
      <button class="btn btn-primary" @click="updateProfile">保存资料</button>
    </section>

    <div v-if="receivingOrder" class="modal-overlay" @mousedown.self="closeReceiveModal">
      <div class="modal mini-modal">
        <h2>确认接收宠物</h2>
        <p class="modal-hint">{{ receivingOrder.order_no_wsh }} · {{ receivingOrder.pet_name_wsh || '宠物' }}</p>
        <input v-model="receiveForm.handover_code_wsh" class="form-control code-input" inputmode="numeric" maxlength="4" placeholder="输入4位交接码">
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="closeReceiveModal">取消</button>
          <button class="btn btn-primary btn-sm" @click="submitReceive">确认接收</button>
        </div>
      </div>
    </div>

    <div v-if="startingOrder" class="modal-overlay" @mousedown.self="closeStartModal">
      <div class="modal mini-modal">
        <h2>开始培养</h2>
        <p class="modal-hint">{{ startingOrder.order_no_wsh }} · {{ startingOrder.pet_name_wsh || '宠物' }}</p>
        <input ref="startPhotoInput" type="file" accept="image/*" style="display:none" @change="onStartPhotoChange">
        <button class="btn btn-outline btn-sm" @click="startPhotoInput?.click()">选择开始照片</button>
        <div v-if="startPhotoPreview" class="photo-preview-grid single">
          <img :src="startPhotoPreview" alt="开始照片预览">
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="closeStartModal">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="startUploading" @click="submitStartService">{{ startUploading ? '上传中...' : '开始培养' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()

const activeTab = ref('cert')
const orderSubTab = ref('pending')
const orderLoading = ref(false)
const pendingOrders = ref([])
const myOrders = ref([])
const keeperInfo = ref(null)
const merchants = ref([])
const selectedMerchantId = ref('')
const selectedOrderId = ref('')
const selectedChatOrderId = ref('')
const careRecords = ref([])
const dailyStatus = ref(null)
const conversation = ref([])
const chatText = ref('')
const recordType = ref('feed')
const receivingOrder = ref(null)
const startingOrder = ref(null)
const recordFileInput = ref(null)
const startPhotoInput = ref(null)
const recordFiles = ref([])
const recordFilePreviews = ref([])
const recordUploading = ref(false)
const startPhotoFile = ref(null)
const startPhotoPreview = ref('')
const startUploading = ref(false)
const completingId = ref(null)

const tabs = [
  { key: 'cert', label: '资质' },
  { key: 'orders', label: '订单' },
  { key: 'records', label: '每日动态' },
  { key: 'chat', label: '聊天' },
  { key: 'profile', label: '资料' },
]

const statusMap = {
  pending: '待付款',
  paid: '已支付',
  confirmed: '待送达',
  delivered: '已送达',
  received: '已接收',
  in_progress: '培养中',
  completed: '已完成',
  cancelled: '已取消',
}

const recordTabs = [
  { key: 'feed', label: '喂食' },
  { key: 'activity', label: '活动' },
  { key: 'medication', label: '用药' },
  { key: 'health', label: '健康' },
  { key: 'note', label: '日志' },
]

const recordTypeMap = { feed: '喂食', activity: '活动', medication: '用药', health: '健康', note: '日志' }

const profileForm = reactive({ nickname_wsh: '', email_wsh: '', phone_wsh: '' })
const certForm = reactive({ name_wsh: '', phone_wsh: '', experience_years_wsh: 3, price_per_day_wsh: 100, max_pets_wsh: 3, bio_wsh: '' })
const recordForm = reactive({ content_wsh: '' })
const receiveForm = reactive({ handover_code_wsh: '' })

const visibleOrders = computed(() => orderSubTab.value === 'pending' ? pendingOrders.value : myOrders.value)
const recordPlaceholder = computed(() => ({
  feed: '例如：上午 8 点喂食 100g，食欲正常',
  activity: '例如：下午散步 40 分钟，精神状态良好',
  medication: '例如：已按主人要求服药',
  health: '例如：体温正常，无异常情况',
  note: '例如：今日护理日志',
}[recordType.value]))

onMounted(async () => {
  await Promise.all([loadProfile(), loadKeeperInfo(), loadMerchants(), loadPendingOrders(), loadMyOrders()])
})

async function loadProfile() {
  const res = await authStore.apiGet('/api/users/me')
  if (res.code === 200 && res.data) {
    profileForm.nickname_wsh = res.data.nickname_wsh || ''
    profileForm.email_wsh = res.data.email_wsh || ''
    profileForm.phone_wsh = res.data.phone_wsh || ''
  }
}

async function updateProfile() {
  const res = await authStore.apiPut('/api/users/me', { ...profileForm })
  if (res.code === 200) appStore.addToast('资料已保存', 'success')
}

async function loadKeeperInfo() {
  const res = await authStore.apiGet('/api/keepers/me')
  if (res.code === 200 && res.data) keeperInfo.value = res.data
}

async function loadMerchants() {
  const res = await authStore.apiGet('/api/merchants')
  if (res.code === 200) merchants.value = res.data || []
}

async function submitCertification() {
  if (!selectedMerchantId.value) return
  const res = await authStore.apiPost('/api/keepers', { ...certForm, merchant_id_wsh: Number(selectedMerchantId.value), status_wsh: 1 })
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

async function acceptOrder(order) {
  const res = await authStore.apiPost('/api/orders/accept', { order_no_wsh: order.order_no_wsh })
  if (res.code === 200) {
    appStore.addToast('接单成功', 'success')
    await refreshOrders()
  }
}

async function rejectOrder(order) {
  const res = await authStore.apiPost('/api/orders/reject', { order_no_wsh: order.order_no_wsh })
  if (res.code === 200) {
    appStore.addToast('已拒单', 'success')
    await refreshOrders()
  }
}

function openReceiveModal(order) {
  receivingOrder.value = order
  receiveForm.handover_code_wsh = ''
}

function closeReceiveModal() {
  receivingOrder.value = null
  receiveForm.handover_code_wsh = ''
}

async function submitReceive() {
  if (!receivingOrder.value) return
  if (!/^\d{4}$/.test(receiveForm.handover_code_wsh.trim())) {
    appStore.addToast('请输入4位交接码', 'warning')
    return
  }
  const res = await authStore.apiPost('/api/orders/received', {
    order_no_wsh: receivingOrder.value.order_no_wsh,
    handover_code_wsh: receiveForm.handover_code_wsh.trim(),
  })
  if (res.code === 200) {
    appStore.addToast('已确认接收宠物', 'success')
    closeReceiveModal()
    await refreshOrders()
  }
}

function openStartModal(order) {
  startingOrder.value = order
  clearStartPhoto()
}

function closeStartModal() {
  startingOrder.value = null
  clearStartPhoto()
}

function onStartPhotoChange(event) {
  const file = event.target.files?.[0]
  startPhotoFile.value = file || null
  startPhotoPreview.value = file ? URL.createObjectURL(file) : ''
}

async function submitStartService() {
  if (!startingOrder.value) return
  if (!startPhotoFile.value) {
    appStore.addToast('请先上传一张宠物照片', 'warning')
    return
  }
  startUploading.value = true
  try {
    const formData = new FormData()
    formData.append('order_no_wsh', startingOrder.value.order_no_wsh)
    formData.append('file', startPhotoFile.value)
    const res = await authStore.apiPost('/api/orders/start/upload', formData)
    if (res.code === 200) {
      appStore.addToast('已开始培养', 'success')
      closeStartModal()
      await refreshOrders()
    }
  } finally {
    startUploading.value = false
  }
}

function clearStartPhoto() {
  startPhotoFile.value = null
  startPhotoPreview.value = ''
  if (startPhotoInput.value) startPhotoInput.value.value = ''
}

async function completeService(order) {
  completingId.value = order.id_wsh
  try {
    const res = await authStore.apiPost('/api/orders/complete', { order_no_wsh: order.order_no_wsh })
    if (res.code === 200) {
      appStore.addToast('订单已完成，AI 报告将自动生成', 'success')
    }
    await refreshOrders()
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  } finally {
    completingId.value = null
  }
}

async function selectOrder(order) {
  activeTab.value = 'records'
  selectedOrderId.value = String(order.id_wsh)
  await loadSelectedOrderData()
}

async function onSelectedOrderChange() {
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

function onRecordFilesChange(event) {
  const files = Array.from(event.target.files || [])
  recordFiles.value = files
  recordFilePreviews.value = files.map(file => URL.createObjectURL(file))
}

async function submitRecord() {
  if (!selectedOrderId.value || !recordForm.content_wsh.trim()) {
    appStore.addToast('请填写动态内容', 'warning')
    return
  }
  if (recordFiles.value.length === 0) {
    appStore.addToast('请上传至少一张宠物照片', 'warning')
    return
  }
  recordUploading.value = true
  try {
    const formData = new FormData()
    formData.append('type_wsh', recordType.value)
    formData.append('content_wsh', recordForm.content_wsh)
    recordFiles.value.forEach(file => formData.append('files', file))
    const res = await authStore.apiPost(`/api/order-fulfillments/${selectedOrderId.value}/timeline/upload`, formData)
    if (res.code === 200) {
      recordForm.content_wsh = ''
      clearRecordFiles()
      appStore.addToast('动态已上传', 'success')
      await loadSelectedOrderData()
    }
  } finally {
    recordUploading.value = false
  }
}

function clearRecordFiles() {
  recordFiles.value = []
  recordFilePreviews.value = []
  if (recordFileInput.value) recordFileInput.value.value = ''
}

async function loadConversation() {
  if (!selectedChatOrderId.value) return
  const res = await authStore.apiGet(`/api/order-fulfillments/${selectedChatOrderId.value}/conversation`)
  if (res.code === 200) conversation.value = res.data || []
}

async function sendMessage() {
  if (!selectedChatOrderId.value || !chatText.value.trim()) return
  const res = await authStore.apiPost(`/api/order-fulfillments/${selectedChatOrderId.value}/conversation`, { content_wsh: chatText.value })
  if (res.code === 200) {
    chatText.value = ''
    await loadConversation()
  }
}

function statusBadge(status) {
  return {
    pending: 'badge-warning',
    paid: 'badge-info',
    confirmed: 'badge-info',
    delivered: 'badge-primary',
    received: 'badge-primary',
    in_progress: 'badge-info',
    completed: 'badge-success',
    cancelled: 'badge-danger',
  }[status] || 'badge-info'
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function parseImageUrls(value) {
  return String(value || '').split(',').map(url => url.trim()).filter(Boolean)
}

function timeOnly(value) {
  if (!value) return '-'
  return new Date(value).toLocaleTimeString()
}
</script>

<style scoped>
.toolbar, .sub-toolbar, .record-tabs, .order-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.toolbar { margin-bottom: 24px; }
.sub-toolbar { margin-bottom: 16px; }
.section-card { padding: 22px; display: grid; gap: 14px; }
.profile-summary { display: grid; gap: 4px; color: var(--color-muted-foreground); }
.profile-summary strong { color: var(--color-foreground); }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.form-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); }
.form-grid__wide { grid-column: 1 / -1; }
.order-list { display: grid; gap: 14px; }
.order-card { padding: 18px; }
.order-card__header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.order-card__header h3 { margin: 4px 0 0; font-size: 18px; }
.order-no { color: var(--color-muted-foreground); font-size: 12px; }
.order-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin: 14px 0; font-size: 13px; }
.order-grid span { display: block; margin-bottom: 3px; color: var(--color-muted-foreground); font-size: 12px; }
.records-grid { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 16px; align-items: start; }
.daily-card { border: 1px solid var(--color-border); border-radius: 6px; padding: 12px; background: var(--color-muted); }
.daily-card p { margin: 6px 0 0; font-size: 13px; }
.upload-field { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--color-muted-foreground); font-size: 13px; }
.photo-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
.photo-preview-grid.single { max-width: 180px; }
.photo-preview-grid img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 6px; border: 1px solid var(--color-border); background: var(--color-muted); }
.timeline-photos { margin-top: 8px; max-width: 420px; }
.timeline { display: grid; gap: 10px; margin-top: 8px; }
.timeline-item { border: 1px solid var(--color-border); border-radius: 6px; padding: 12px; }
.timeline-item p { margin: 6px 0; }
.timeline-item small, .timeline-item__meta { color: var(--color-muted-foreground); font-size: 12px; word-break: break-all; }
.mini-modal { max-width: 420px; display: grid; gap: 12px; }
.modal-hint { margin: 0; color: var(--color-muted-foreground); font-size: 13px; }
.code-input { max-width: 180px; font-size: 22px; letter-spacing: 4px; text-align: center; }
.chat-box { display: grid; gap: 8px; max-height: 360px; overflow: auto; }
.chat-message { max-width: 76%; padding: 10px 12px; border-radius: 6px; background: var(--color-muted); }
.chat-message.is-me { justify-self: end; background: var(--color-primary); color: #fff; }
.chat-message p { margin: 0; }
.chat-input { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.empty-inline { color: var(--color-muted-foreground); font-size: 13px; }
@media (max-width: 760px) {
  .records-grid { grid-template-columns: 1fr; }
  .order-card__header { display: grid; }
  .chat-input { grid-template-columns: 1fr; }
}
</style>
