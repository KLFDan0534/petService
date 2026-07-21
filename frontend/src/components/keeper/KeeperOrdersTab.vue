<template>
  <section>
    <div class="sub-toolbar">
      <button :class="['btn', subTab === 'pending' ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="$emit('update:subTab', 'pending')">待处理</button>
      <button :class="['btn', subTab === 'active' ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="$emit('update:subTab', 'active')">我的订单</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
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
        <div v-if="showPaymentCountdown(order)" class="payment-countdown">
          <span>支付剩余</span>
          <strong>{{ paymentCountdownText(order) }}</strong>
          <small>未支付订单暂不能接单</small>
        </div>
        <div class="order-grid">
          <div><span>宠物</span>{{ order.pet_name_wsh || '#' + order.pet_id_wsh }}</div>
          <div><span>主人</span>{{ order.owner_name_wsh || '#' + order.owner_id_wsh }}</div>
          <div><span>服务时间</span>{{ order.start_date_wsh }} 至 {{ order.end_date_wsh }}</div>
          <div><span>送达地点</span>{{ order.delivery_address_wsh || order.merchant_address_wsh || '-' }}</div>
          <div><span>送养时间</span>{{ formatDateTime(order.delivery_time_wsh) }}</div>
          <div><span>接收截止</span>{{ formatDateTime(order.receiver_available_end_wsh) }}</div>
        </div>
        <footer class="order-actions">
          <button v-if="order.status_wsh === 'paid'" class="btn btn-success btn-sm" :disabled="processingOrderNo === order.order_no_wsh" @click="$emit('accept', order)">接单</button>
          <button v-if="order.status_wsh === 'paid'" class="btn btn-danger btn-sm" :disabled="processingOrderNo === order.order_no_wsh" @click="$emit('reject', order)">拒单</button>
          <span v-else-if="order.status_wsh === 'pending'" class="order-waiting-note">待付款，暂不能接单</span>
          <button v-if="order.status_wsh === 'delivered'" class="btn btn-primary btn-sm" @click="openReceiveModal(order)">确认接收</button>
          <button v-if="order.status_wsh === 'received'" class="btn btn-primary btn-sm" @click="openStartModal(order)">开始培养</button>
          <button v-if="order.status_wsh === 'in_progress'" class="btn btn-success btn-sm" :disabled="completingId === order.id_wsh || processingOrderNo === order.order_no_wsh" @click="$emit('complete', order)">完成订单</button>
          <button class="btn btn-outline btn-sm" @click="$emit('select-order', order)">履约记录</button>
        </footer>
      </article>
    </div>

    <div v-if="receivingOrder" class="modal-overlay" @mousedown.self="closeReceiveModal">
      <div class="modal mini-modal">
        <h2>确认接收宠物</h2>
        <p class="modal-hint">{{ receivingOrder.order_no_wsh }} · {{ receivingOrder.pet_name_wsh || '宠物' }}</p>
        <input v-model="receiveForm.handover_code_wsh" class="form-control code-input" inputmode="numeric" maxlength="4" placeholder="输入4位交接码">
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="closeReceiveModal">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="receiving" @click="submitReceive">{{ receiving ? '定位中...' : '确认接收' }}</button>
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
          <button class="btn btn-primary btn-sm" :disabled="startUploading" @click="submitStart">{{ startUploading ? '上传中...' : '开始培养' }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { formatPaymentTimeoutRemaining, getPaymentTimeoutRemaining } from '@/utils/orderPaymentTimeout'

const props = defineProps({
  pendingOrders: { type: Array, default: () => [] },
  activeOrders: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  completingId: { type: [Number, String], default: null },
  subTab: { type: String, default: 'pending' },
  receiving: { type: Boolean, default: false },
  startUploading: { type: Boolean, default: false },
  processingOrderNo: { type: String, default: '' },
  modalKey: { type: Number, default: 0 },
  nowMs: { type: Number, default: () => Date.now() },
})
const emit = defineEmits(['accept', 'reject', 'receive', 'start', 'complete', 'select-order', 'update:subTab'])

const statusMap = {
  pending: '待付款', paid: '已支付', confirmed: '待送达',
  delivered: '已送达', received: '已接收', in_progress: '培养中',
  completed: '已完成', cancelled: '已取消',
}

const visibleOrders = computed(() => props.subTab === 'pending' ? props.pendingOrders : props.activeOrders)

watch(() => props.modalKey, () => {
  receivingOrder.value = null
  startingOrder.value = null
  receiveForm.handover_code_wsh = ''
  clearStartPhoto()
})

const receivingOrder = ref(null)
const startingOrder = ref(null)
const receiveForm = reactive({ handover_code_wsh: '' })
const startPhotoInput = ref(null)
const startPhotoFile = ref(null)
const startPhotoPreview = ref('')

onUnmounted(() => {
  revokeStartPhotoPreview()
})

function openReceiveModal(order) {
  receivingOrder.value = order
  receiveForm.handover_code_wsh = ''
}

function closeReceiveModal() {
  receivingOrder.value = null
  receiveForm.handover_code_wsh = ''
}

function submitReceive() {
  if (!receivingOrder.value) return
  if (!/^\d{4}$/.test(receiveForm.handover_code_wsh.trim())) return
  emit('receive', receivingOrder.value.order_no_wsh, receiveForm.handover_code_wsh.trim())
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
  revokeStartPhotoPreview()
  startPhotoFile.value = file || null
  startPhotoPreview.value = file ? URL.createObjectURL(file) : ''
}

function clearStartPhoto() {
  revokeStartPhotoPreview()
  startPhotoFile.value = null
  startPhotoPreview.value = ''
  if (startPhotoInput.value) startPhotoInput.value.value = ''
}

function revokeStartPhotoPreview() {
  if (startPhotoPreview.value) {
    URL.revokeObjectURL(startPhotoPreview.value)
    startPhotoPreview.value = ''
  }
}

function submitStart() {
  if (!startingOrder.value || !startPhotoFile.value) return
  emit('start', startingOrder.value.order_no_wsh, startPhotoFile.value)
}

function statusBadge(status) {
  return {
    pending: 'badge-warning', paid: 'badge-info', confirmed: 'badge-info',
    delivered: 'badge-primary', received: 'badge-primary',
    in_progress: 'badge-info', completed: 'badge-success',
    cancelled: 'badge-danger',
  }[status] || 'badge-info'
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function showPaymentCountdown(order) {
  return order?.status_wsh === 'pending' && getPaymentTimeoutRemaining(order, props.nowMs) != null
}

function paymentCountdownText(order) {
  return formatPaymentTimeoutRemaining(getPaymentTimeoutRemaining(order, props.nowMs))
}
</script>

<style scoped>
.sub-toolbar, .order-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.sub-toolbar { margin-bottom: 16px; }
.order-list { display: grid; gap: 14px; }
.order-card { padding: 18px; }
.order-card__header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.order-card__header h3 { margin: 4px 0 0; font-size: 18px; }
.order-no { color: var(--color-muted-foreground); font-size: 12px; }
.order-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin: 14px 0; font-size: 13px; }
.order-grid span { display: block; margin-bottom: 3px; color: var(--color-muted-foreground); font-size: 12px; }
.order-waiting-note { color: var(--color-muted-foreground); font-size: 13px; align-self: center; }
.payment-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding: 8px 10px;
  border: 1px solid rgba(245, 158, 11, .35);
  border-radius: 8px;
  background: rgba(245, 158, 11, .08);
  font-size: 13px;
}
.payment-countdown span,
.payment-countdown small { color: var(--color-muted-foreground); }
.payment-countdown strong {
  color: var(--color-warning, #b45309);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}
.mini-modal { max-width: 420px; display: grid; gap: 12px; }
.modal-hint { margin: 0; color: var(--color-muted-foreground); font-size: 13px; }
.code-input { max-width: 180px; font-size: 22px; letter-spacing: 4px; text-align: center; }
.photo-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
.photo-preview-grid.single { max-width: 180px; }
.photo-preview-grid img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 6px; border: 1px solid var(--color-border); background: var(--color-muted); }
.loading, .empty-state { text-align: center; padding: 40px 20px; }
</style>
