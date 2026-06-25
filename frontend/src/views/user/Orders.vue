<template>
  <div class="orders-page">
    <PageHero title="我的订单" subtitle="下单、送达、沟通、报告、打赏与评价" />

    <div class="toolbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['btn', activeTab === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
      <button class="btn btn-outline btn-sm" style="margin-left:auto" @click="loadOrders" :disabled="loading">刷新</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredOrders.length === 0" class="empty-state">
      <div class="icon">📋</div>
      <h3>暂无订单</h3>
      <p>选择服务后即可创建寄养订单</p>
      <router-link to="/dashboard" class="btn btn-primary">浏览服务</router-link>
    </div>

<!--    订单列表-->
    <div v-else class="order-list">
      <article v-for="order in filteredOrders" :key="order.id_wsh" class="card order-card">
        <header class="order-card__header">
          <div>
            <div class="order-no">订单号：{{ order.order_no_wsh }}</div>
            <h3>{{ order.service_name_wsh || '寄养服务' }}</h3>
          </div>
          <span :class="['badge', statusBadge(order.status_wsh)]">{{ statusMap[order.status_wsh] || order.status_wsh }}</span>
        </header>

        <div class="order-card__grid">
          <div><span>宠物</span>{{ order.pet_name_wsh || '#' + order.pet_id_wsh }}</div>
          <div><span>商家</span>{{ order.merchant_name_wsh || '#' + order.merchant_id_wsh }}</div>
          <div><span>看护人</span>{{ order.keeper_name_wsh || '#' + order.keeper_id_wsh }}</div>
          <div><span>服务时间</span>{{ order.start_date_wsh }} 至 {{ order.end_date_wsh }}</div>
          <div><span>送达地点</span>{{ order.delivery_address_wsh || order.merchant_address_wsh || '-' }}</div>
          <div><span>送养/开始时间</span>{{ formatDateTime(order.delivery_time_wsh) }}</div>
          <div><span>接回时间</span>{{ formatDateTime(order.pickup_time_wsh) }}</div>
          <div><span>交接码</span>{{ order.handover_code_wsh || '-' }}</div>
          <div><span>实付金额</span>￥{{ order.final_amount_wsh || order.total_amount_wsh || 0 }}</div>
        </div>

        <p v-if="order.remark_wsh" class="order-remark">{{ order.remark_wsh }}</p>

        <footer class="order-actions">
          <button class="btn btn-sm btn-outline" @click="openDetail(order)">详情</button>
          <button v-if="order.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="payOrder(order)">付款</button>
          <button v-if="order.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="cancelOrder(order)">取消</button>
          <button v-if="order.status_wsh === 'confirmed'" class="btn btn-sm btn-primary" @click="markDelivered(order)">已送达</button>
          <button v-if="order.status_wsh === 'completed'" class="btn btn-sm btn-primary" @click="openReview(order)">评价</button>
          <button v-if="order.status_wsh === 'completed'" class="btn btn-sm btn-success" @click="openTip(order)">打赏</button>
        </footer>
      </article>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @mousedown.self="cancelCreate">
      <div class="modal order-modal">
        <h2>创建订单</h2>
        <div class="form-grid">
          <label>
            宠物
            <select v-model="form.pet_id_wsh" class="form-control">
              <option value="">请选择宠物</option>
              <option v-for="pet in pets" :key="pet.id_wsh" :value="pet.id_wsh">{{ pet.name_wsh }} {{ pet.breed_wsh ? `(${pet.breed_wsh})` : '' }}</option>
            </select>
          </label>
          <label>
            商家
            <select v-model="form.merchant_id_wsh" class="form-control" @change="onMerchantChange">
              <option value="">请选择商家</option>
              <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">{{ merchant.name_wsh }}</option>
            </select>
          </label>
          <label>
            看护人
            <select v-model="form.keeper_id_wsh" class="form-control" :disabled="!form.merchant_id_wsh">
              <option value="">请选择看护人</option>
              <option v-for="keeper in keepers" :key="keeper.id_wsh" :value="keeper.id_wsh">
                {{ keeper.name_wsh }} ￥{{ keeper.price_per_day_wsh }}/天 · 当前 {{ keeper.current_pets_wsh || 0 }}/{{ keeper.max_pets_wsh || '-' }}
              </option>
            </select>
          </label>
          <label>
            宠物送到哪里
            <input v-model="form.delivery_address_wsh" class="form-control" placeholder="默认使用商家地址">
          </label>
          <label>
            送养/开始时间
            <input v-model="form.delivery_time_wsh" type="datetime-local" class="form-control" :min="minDateTime">
          </label>
          <label>
            接回时间
            <input v-model="form.pickup_time_wsh" type="datetime-local" class="form-control" :min="pickupMinDateTime">
          </label>
          <label class="form-grid__wide">
            备注
            <textarea v-model="form.remark_wsh" class="form-control" rows="3" placeholder="饮食、过敏、用药、性格等"></textarea>
          </label>
        </div>
        <div class="estimate">预计金额：￥{{ estimatedAmount }}</div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="cancelCreate">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="submitting" @click="submitOrder">{{ submitting ? '提交中...' : '确认下单' }}</button>
        </div>
      </div>
    </div>

    <div v-if="detailOrder" class="modal-overlay" @mousedown.self="closeDetail">
      <div class="modal detail-modal">
        <div class="detail-header">
          <div>
            <h2>{{ detailOrder.service_name_wsh || '订单详情' }}</h2>
            <p>{{ detailOrder.order_no_wsh }}</p>
          </div>
          <span :class="['badge', statusBadge(detailOrder.status_wsh)]">{{ statusMap[detailOrder.status_wsh] || detailOrder.status_wsh }}</span>
        </div>

        <div class="detail-tabs">
          <button v-for="tab in detailTabs" :key="tab.key" :class="['btn', detailTab === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="detailTab = tab.key">
            {{ tab.label }}
          </button>
        </div>

        <section v-if="detailTab === 'overview'" class="detail-section">
          <div class="order-card__grid">
            <div><span>宠物</span>{{ detailOrder.pet_name_wsh || '#' + detailOrder.pet_id_wsh }}</div>
            <div><span>看护人</span>{{ detailOrder.keeper_name_wsh || '-' }}</div>
            <div><span>商家地址</span>{{ detailOrder.merchant_address_wsh || '-' }}</div>
            <div><span>送达地址</span>{{ detailOrder.delivery_address_wsh || '-' }}</div>
            <div><span>送养时间</span>{{ formatDateTime(detailOrder.delivery_time_wsh) }}</div>
            <div><span>交接码</span>{{ detailOrder.handover_code_wsh || '-' }}</div>
            <div><span>接收时间</span>{{ formatDateTime(detailOrder.received_at_wsh) }}</div>
            <div><span>开始培养</span>{{ formatDateTime(detailOrder.started_at_wsh) }}</div>
            <div><span>完成时间</span>{{ formatDateTime(detailOrder.completed_at_wsh) }}</div>
          </div>
          <div v-if="detailOrder.start_photo_wsh" class="start-photo">
            <span>开始照片</span>
            <img :src="detailOrder.start_photo_wsh" alt="开始培养照片">
          </div>
        </section>

        <section v-if="detailTab === 'timeline'" class="detail-section">
          <div v-if="dailyStatus" class="daily-status">
            <strong>每日上传：</strong>
            已上传 {{ dailyStatus.uploaded_days_wsh?.length || 0 }}/{{ dailyStatus.required_days_wsh?.length || 0 }}
            <span v-if="dailyStatus.missing_days_wsh?.length"> · 缺少 {{ dailyStatus.missing_days_wsh.join('、') }}</span>
          </div>
          <div v-if="timeline.length === 0" class="empty-inline">暂无照护动态</div>
          <div v-for="item in timeline" :key="item.id_wsh" class="timeline-item">
            <div class="timeline-item__meta">{{ recordTypeMap[item.type_wsh] || item.type_wsh }} · {{ formatDateTime(item.record_time_wsh || item.created_at_wsh) }}</div>
            <p>{{ item.content_wsh }}</p>
            <div v-if="item.images_wsh" class="photo-preview-grid timeline-photos">
              <img v-for="url in parseImageUrls(item.images_wsh)" :key="url" :src="url" alt="动态照片">
            </div>
          </div>
        </section>

        <section v-if="detailTab === 'chat'" class="detail-section">
          <div class="chat-box">
            <div v-if="conversation.length === 0" class="empty-inline">暂无沟通消息</div>
            <div v-for="message in conversation" :key="message.id_wsh" :class="['chat-message', message.from_user_id_wsh === authStore.user?.id_wsh ? 'is-me' : '']">
              <p>{{ message.content_wsh }}</p>
              <small>{{ formatDateTime(message.created_at_wsh) }}</small>
            </div>
          </div>
          <div class="chat-input">
            <input v-model="chatText" class="form-control" placeholder="和看护人沟通订单细节">
            <button class="btn btn-primary btn-sm" @click="sendMessage">发送</button>
          </div>
        </section>

        <section v-if="detailTab === 'report'" class="detail-section">
          <button v-if="detailOrder.status_wsh === 'completed'" class="btn btn-primary btn-sm" @click="generateReport(detailOrder)">生成/刷新 AI 报告</button>
          <div v-if="reports.length === 0" class="empty-inline">暂无报告</div>
          <article v-for="report in reports" :key="report.id_wsh" class="report-item">
            <div class="timeline-item__meta">{{ report.type_wsh || 'report' }} · {{ formatDateTime(report.created_at_wsh) }}</div>
            <pre>{{ report.content_wsh }}</pre>
          </article>
        </section>

        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="closeDetail">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="tipOrder" class="modal-overlay" @mousedown.self="tipOrder = null">
      <div class="modal mini-modal">
        <h2>打赏服务</h2>
        <input v-model="tipForm.amount_wsh" type="number" min="0.01" step="0.01" class="form-control" placeholder="金额">
        <textarea v-model="tipForm.message_wsh" class="form-control" rows="3" placeholder="留言"></textarea>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="tipOrder = null">取消</button>
          <button class="btn btn-success btn-sm" @click="submitTip">确认打赏</button>
        </div>
      </div>
    </div>

    <div v-if="reviewOrder" class="modal-overlay" @mousedown.self="reviewOrder = null">
      <div class="modal mini-modal">
        <h2>评价服务</h2>
        <select v-model="reviewForm.score_wsh" class="form-control">
          <option :value="5">5 分</option>
          <option :value="4">4 分</option>
          <option :value="3">3 分</option>
          <option :value="2">2 分</option>
          <option :value="1">1 分</option>
        </select>
        <textarea v-model="reviewForm.content_wsh" class="form-control" rows="4" placeholder="说说这次服务体验"></textarea>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="reviewOrder = null">取消</button>
          <button class="btn btn-primary btn-sm" @click="submitReview">提交评价</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')
const showCreateModal = ref(false)
const submitting = ref(false)
const pets = ref([])
const merchants = ref([])
const keepers = ref([])
const serviceName = ref('')
const serviceId = ref('')
const price = ref('')
const detailOrder = ref(null)
const detailTab = ref('overview')
const timeline = ref([])
const dailyStatus = ref(null)
const conversation = ref([])
const reports = ref([])
const chatText = ref('')
const tipOrder = ref(null)
const reviewOrder = ref(null)
const nowTick = ref(Date.now())

const form = reactive({
  pet_id_wsh: '',
  merchant_id_wsh: '',
  keeper_id_wsh: '',
  delivery_address_wsh: '',
  delivery_time_wsh: '',
  pickup_time_wsh: '',
  remark_wsh: '',
})

const tipForm = reactive({ amount_wsh: '', message_wsh: '' })
const reviewForm = reactive({ score_wsh: 5, content_wsh: '' })

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待付款' },
  { key: 'paid', label: '已支付' },
  { key: 'confirmed', label: '待送达' },
  { key: 'delivered', label: '已送达' },
  { key: 'received', label: '已接收' },
  { key: 'in_progress', label: '培养中' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
]

const detailTabs = [
  { key: 'overview', label: '概览' },
  { key: 'timeline', label: '动态' },
  { key: 'chat', label: '聊天' },
  { key: 'report', label: '报告' },
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
  refunding: '退款中',
  refunded: '已退款',
}

const recordTypeMap = { feed: '喂食', activity: '活动', medication: '用药', health: '健康', note: '日志' }

const filteredOrders = computed(() => activeTab.value === 'all'
  ? orders.value
  : orders.value.filter(order => order.status_wsh === activeTab.value))

const minDateTime = computed(() => toLocalDateTimeInput(new Date(nowTick.value)))

const pickupMinDateTime = computed(() => {
  if (!form.delivery_time_wsh) return minDateTime.value
  return toLocalDateTimeInput(new Date(new Date(form.delivery_time_wsh).getTime() + 600000))
})

const estimatedAmount = computed(() => {
  if (!form.delivery_time_wsh || !form.pickup_time_wsh) return '0.00'
  const start = new Date(form.delivery_time_wsh)
  const end = new Date(form.pickup_time_wsh)
  const days = Math.max(0, Math.ceil((end - start) / 86400000))
  const keeper = keepers.value.find(item => Number(item.id_wsh) === Number(form.keeper_id_wsh))
  const dayPrice = Number(keeper?.price_per_day_wsh || price.value || 0)
  return (days * dayPrice).toFixed(2)
})

onMounted(async () => {
  await loadOrders()
  if (route.query.create === 'true') openCreateFromQuery(route.query)
})

watch(() => route.query, query => {
  if (query.create === 'true' && !showCreateModal.value) openCreateFromQuery(query)
})

async function loadOrders() {
  loading.value = true
  try {
    const res = await authStore.apiGet('/api/orders')
    if (res.code === 200) orders.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function openCreateFromQuery(query) {
  serviceName.value = query.serviceName || ''
  serviceId.value = query.serviceId || ''
  price.value = query.price || ''
  await Promise.all([loadPets(), loadMerchants()])
  if (serviceId.value) {
    try {
      const r = await authStore.apiGet(`/api/services/${serviceId.value}`)
      if (r.code === 200 && r.data) {
        const svc = r.data
        serviceName.value = svc.name_wsh || serviceName.value
        const merchant = merchants.value.find(m => Number(m.id_wsh) === Number(svc.merchant_id_wsh))
        if (merchant) {
          form.merchant_id_wsh = merchant.id_wsh
          await loadKeepers(merchant.id_wsh)
        }
      }
    } catch (e) {}
  }
  showCreateModal.value = true
}

async function loadPets() {
  const res = await authStore.apiGet('/api/pets')
  if (res.code === 200) pets.value = res.data || []
}

async function loadMerchants() {
  const res = await authStore.apiGet('/api/merchants')
  if (res.code === 200) merchants.value = res.data || []
}

async function loadKeepers(merchantId) {
  const res = await authStore.apiGet(`/api/keepers/merchant/${merchantId}`)
  if (res.code === 200) keepers.value = res.data || []
}

function onMerchantChange() {
  form.keeper_id_wsh = ''
  keepers.value = []
  if (form.merchant_id_wsh) loadKeepers(form.merchant_id_wsh)
  if (serviceId.value) {
    serviceId.value = ''
    serviceName.value = ''
    price.value = ''
  }
}

function cancelCreate() {
  showCreateModal.value = false
  router.replace({ query: {} })
}

async function submitOrder() {
  nowTick.value = Date.now()
  if (!form.pet_id_wsh || !form.merchant_id_wsh || !form.keeper_id_wsh || !form.delivery_time_wsh || !form.pickup_time_wsh) {
    appStore.addToast('请补全宠物、商家、看护人、送养时间和接回时间', 'warning')
    return
  }
  const deliveryTime = new Date(form.delivery_time_wsh)
  const pickupTime = new Date(form.pickup_time_wsh)
  if (Number.isNaN(deliveryTime.getTime()) || Number.isNaN(pickupTime.getTime())) {
    appStore.addToast('请选择有效的送养和接回时间', 'warning')
    return
  }
  if (deliveryTime.getTime() < nowTick.value) {
    appStore.addToast('送养时间不能早于当前时间', 'warning')
    return
  }
  if (pickupTime.getTime() <= deliveryTime.getTime()) {
    appStore.addToast('接回时间必须晚于送养时间', 'warning')
    return
  }
  submitting.value = true
  try {
    const serviceDates = buildServiceDates(deliveryTime, pickupTime)
    const payload = {
      pet_id_wsh: Number(form.pet_id_wsh),
      merchant_id_wsh: Number(form.merchant_id_wsh),
      keeper_id_wsh: Number(form.keeper_id_wsh),
      service_id_wsh: serviceId.value ? Number(serviceId.value) : null,
      start_date_wsh: serviceDates.startDate,
      end_date_wsh: serviceDates.endDate,
      delivery_address_wsh: form.delivery_address_wsh,
      delivery_time_wsh: toApiDateTime(form.delivery_time_wsh),
      pickup_time_wsh: toApiDateTime(form.pickup_time_wsh),
      remark_wsh: form.remark_wsh,
    }
    const res = await authStore.apiPost('/api/orders', payload)
    if (res.code === 200) {
      const code = res.data?.handover_code_wsh
      appStore.addToast(`下单成功！交接码：${code || '未生成'}`, 'success')
      showCreateModal.value = false
      await router.replace({ query: {} })
      await loadOrders()
    }
  } finally {
    submitting.value = false
  }
}

async function payOrder(order) {
  const createRes = await authStore.apiPost('/api/payments/create', { order_id_wsh: order.id_wsh, method_wsh: 'online' })
  if (createRes.code !== 200) return
  const payRes = await authStore.apiPost('/api/payments/pay', { pay_no_wsh: createRes.data.pay_no_wsh })
  if (payRes.code === 200) {
    appStore.addToast('支付成功', 'success')
  }
  await loadOrders()
}

async function cancelOrder(order) {
  const res = await authStore.apiPost('/api/orders/cancel', { order_id_wsh: order.id_wsh })
  if (res.code === 200) {
    appStore.addToast('订单已取消', 'success')
  }
  await loadOrders()
}

// 标记送达
async function markDelivered(order) {
  const res = await authStore.apiPost('/api/orders/delivered', { order_no_wsh: order.order_no_wsh })
  if (res.code === 200) {
    appStore.addToast('已确认送达', 'success')
  }
  await loadOrders()
}

// 打开详情
async function openDetail(order) {
  const res = await authStore.apiGet(`/api/orders/${order.id_wsh}`)
  if (res.code === 200) detailOrder.value = res.data
  detailTab.value = 'overview'
  await Promise.all([loadTimeline(order.id_wsh), loadConversation(order.id_wsh), loadReports(order.id_wsh)])
}

function closeDetail() {
  detailOrder.value = null
  timeline.value = []
  dailyStatus.value = null
  conversation.value = []
  reports.value = []
}

async function loadTimeline(orderId) {
  const [timelineRes, statusRes] = await Promise.all([
    authStore.apiGet(`/api/order-fulfillments/${orderId}/timeline`),
    authStore.apiGet(`/api/order-fulfillments/${orderId}/daily-status`),
  ])
  if (timelineRes.code === 200) timeline.value = timelineRes.data || []
  if (statusRes.code === 200) dailyStatus.value = statusRes.data
}

async function loadConversation(orderId) {
  const res = await authStore.apiGet(`/api/order-fulfillments/${orderId}/conversation`)
  if (res.code === 200) conversation.value = res.data || []
}

async function sendMessage() {
  if (!chatText.value.trim() || !detailOrder.value) return
  const res = await authStore.apiPost(`/api/order-fulfillments/${detailOrder.value.id_wsh}/conversation`, { content_wsh: chatText.value })
  if (res.code === 200) {
    chatText.value = ''
    await loadConversation(detailOrder.value.id_wsh)
  }
}

async function loadReports(orderId) {
  const res = await authStore.apiGet(`/api/ai/reports/order/${orderId}`)
  if (res.code === 200) reports.value = res.data || []
}

async function generateReport(order) {
  const res = await authStore.apiPost('/api/ai/boarding-report', { order_id_wsh: order.id_wsh })
  if (res.code === 200) {
    appStore.addToast('AI 报告已生成', 'success')
    await loadReports(order.id_wsh)
  }
}

function openTip(order) {
  tipOrder.value = order
  tipForm.amount_wsh = ''
  tipForm.message_wsh = ''
}

async function submitTip() {
  if (!tipForm.amount_wsh || !tipOrder.value) return
  const res = await authStore.apiPost('/api/tips', {
    order_id_wsh: tipOrder.value.id_wsh,
    amount_wsh: Number(tipForm.amount_wsh),
    message_wsh: tipForm.message_wsh,
  })
  if (res.code === 200) {
    appStore.addToast('打赏成功', 'success')
    tipOrder.value = null
  }
}

function openReview(order) {
  reviewOrder.value = order
  reviewForm.score_wsh = 5
  reviewForm.content_wsh = ''
}

async function submitReview() {
  if (!reviewOrder.value) return
  const res = await authStore.apiPost('/api/ratings', {
    order_id_wsh: reviewOrder.value.id_wsh,
    target_id_wsh: reviewOrder.value.keeper_id_wsh,
    target_type_wsh: 'keeper',
    score_wsh: reviewForm.score_wsh,
    content_wsh: reviewForm.content_wsh,
  })
  if (res.code === 200) {
    appStore.addToast('评价成功', 'success')
    reviewOrder.value = null
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
    refunded: 'badge-success',
    refunding: 'badge-warning',
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

function buildServiceDates(deliveryTime, pickupTime) {
  return {
    startDate: toDateOnly(deliveryTime),
    endDate: toDateOnly(addDays(pickupTime, isSameDate(deliveryTime, pickupTime) ? 1 : 0)),
  }
}

function toApiDateTime(value) {
  return value ? `${value}:00` : null
}

function toLocalDateTimeInput(date) {
  const pad = value => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toDateOnly(date) {
  return toLocalDateTimeInput(date).slice(0, 10)
}

function addDays(date, days) {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

function isSameDate(first, second) {
  return toDateOnly(first) === toDateOnly(second)
}
</script>

<style scoped>
.orders-page { display: block; }
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.order-list { display: grid; gap: 16px; }
.order-card { padding: 18px; overflow-wrap: break-word; word-break: break-word; }
.order-card__header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.order-card__header h3 { margin: 4px 0 0; font-size: 18px; }
.order-no { color: var(--color-muted-foreground); font-size: 12px; }
.order-card__grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin-top: 14px; font-size: 13px; min-width: 0; overflow-wrap: break-word; word-break: break-word; }
.order-card__grid > div { min-width: 0; overflow-wrap: break-word; word-break: break-word; }
.order-card__grid span { display: block; margin-bottom: 3px; color: var(--color-muted-foreground); font-size: 12px; }
.order-remark { margin: 12px 0 0; color: var(--color-muted-foreground); }
.order-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.order-modal { max-width: 760px; }
.detail-modal { max-width: 860px; }
.mini-modal { max-width: 420px; display: grid; gap: 12px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-top: 16px; }
.form-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); }
.form-grid__wide { grid-column: 1 / -1; }
.estimate { margin-top: 14px; font-weight: 700; color: var(--color-primary); }
.detail-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.detail-header h2 { margin: 0; }
.detail-header p { margin: 4px 0 0; color: var(--color-muted-foreground); }
.detail-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin: 18px 0; }
.detail-section { display: grid; gap: 12px; }
.daily-status { padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 6px; background: var(--color-muted); font-size: 13px; }
.timeline-item, .report-item { padding: 12px; border: 1px solid var(--color-border); border-radius: 6px; }
.timeline-item p { margin: 6px 0 0; }
.timeline-item__meta { color: var(--color-muted-foreground); font-size: 12px; }
.photo-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
.photo-preview-grid img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 6px; border: 1px solid var(--color-border); background: var(--color-muted); }
.timeline-photos { margin-top: 8px; max-width: 420px; }
.start-photo { display: grid; gap: 6px; max-width: 220px; }
.start-photo span { color: var(--color-muted-foreground); font-size: 12px; }
.start-photo img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 6px; border: 1px solid var(--color-border); background: var(--color-muted); }
.report-item pre { white-space: pre-wrap; margin: 8px 0 0; font-family: inherit; }
.chat-box { display: grid; gap: 8px; max-height: 320px; overflow: auto; padding-right: 4px; }
.chat-message { max-width: 76%; padding: 10px 12px; border-radius: 6px; background: var(--color-muted); }
.chat-message.is-me { justify-self: end; background: var(--color-primary); color: #fff; }
.chat-message p { margin: 0; }
.chat-message small { opacity: .75; }
.chat-input { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.empty-inline { color: var(--color-muted-foreground); font-size: 13px; }
@media (max-width: 640px) {
  .order-card__header, .detail-header { display: grid; }
  .chat-input { grid-template-columns: 1fr; }
}
</style>
