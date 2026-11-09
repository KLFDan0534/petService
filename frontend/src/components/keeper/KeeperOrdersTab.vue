<template>
  <section>
    <div class="sub-toolbar">
      <button :class="['btn', subTab === 'pool' ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="$emit('update:subTab', 'pool')">
        待抢单 <span v-if="poolOrders.length" class="tab-count">{{ poolOrders.length }}</span>
      </button>
      <button :class="['btn', subTab === 'active' ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="$emit('update:subTab', 'active')">我的订单</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="visibleOrders.length === 0" class="empty-state">
      <div class="icon">📦</div>
      <h3>{{ emptyText }}</h3>
      <p v-if="subTab === 'pool'" class="empty-hint">新订单支付后会进入这里，先抢先得；超过 30 分钟无人接单，系统会自动分配。</p>
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
          <template v-if="subTab === 'pool'">
            <button
              class="btn btn-primary btn-sm"
              :disabled="processingOrderNo === order.order_no_wsh"
              @click="$emit('claim', order)"
            >{{ processingOrderNo === order.order_no_wsh ? '抢单中...' : '抢单' }}</button>
            <button class="btn btn-outline btn-sm" @click="$emit('select-order', order)">履约记录</button>
          </template>
          <!-- 非池子：按订单状态给出接单/拒单/履约动作。整块用 v-else 包住，
               否则「拒单」会接在「接单」的 v-else-if 链上，永远渲染不出来 -->
          <template v-else>
            <button v-if="order.status_wsh === 'paid'" class="btn btn-danger btn-sm" :disabled="processingOrderNo === order.order_no_wsh" @click="$emit('reject', order)">拒单</button>
            <span v-else-if="order.status_wsh === 'pending'" class="order-waiting-note">待付款，暂不能接单</span>
            <button v-if="order.status_wsh === 'confirmed'" class="btn btn-primary btn-sm" @click="openReceiveModal(order)">确认接收</button>
            <button v-if="order.status_wsh === 'received'" class="btn btn-primary btn-sm" @click="openStartModal(order)">开始培养</button>
            <template v-if="order.status_wsh === 'in_progress'">
              <!-- 接走已确认但还没付补费：不重复弹码，等主人支付 -->
              <span v-if="order.checkout_confirmed_at_wsh" class="order-waiting-note">
                待主人补费 {{ overtimeText(order) }}
              </span>
              <button v-else class="btn btn-success btn-sm" :disabled="completingId === order.id_wsh || processingOrderNo === order.order_no_wsh" @click="openCheckoutModal(order)">确认接走</button>
            </template>
            <button class="btn btn-outline btn-sm" @click="$emit('select-order', order)">履约记录</button>
          </template>
        </footer>
      </article>
    </div>

    <AppDialog :visible="receivingOrder" :width="420" :title="'确认接收宠物'" @close="closeReceiveModal">
      <div class="dialog-body">
        <p class="modal-hint">{{ receivingOrder.order_no_wsh }} · {{ receivingOrder.pet_name_wsh || '宠物' }}</p>
        <input v-model="receiveForm.handover_code_wsh" class="form-control code-input" inputmode="numeric" maxlength="4" placeholder="输入4位交接码">
        <p class="modal-hint" :style="{ color: receiveForm.received_latitude_wsh ? '#2e7d32' : '#c62828' }">
          <template v-if="locating">正在获取当前位置…</template>
          <template v-else-if="receiveForm.received_latitude_wsh">已获取当前位置，可确认接收</template>
          <template v-else>未获取到定位，请允许浏览器定位后重试</template>
          <button
            v-if="!locating"
            type="button"
            style="margin-left:6px;background:none;border:0;padding:0;color:inherit;text-decoration:underline;cursor:pointer"
            @click="locateForReceive"
          >重新定位</button>
        </p>
        <label v-if="isDevBuild" class="modal-hint" style="display:flex;align-items:center;gap:6px;cursor:pointer">
          <input v-model="receiveForm.skip_distance_check_wsh" type="checkbox">
          调试：跳过距离校验
        </label>
      </div>
      <template #footer>
        <button class="btn btn-secondary btn-sm" @click="closeReceiveModal">取消</button>
        <button class="btn btn-primary btn-sm" :disabled="receiving" @click="submitReceive">{{ receiving ? '提交中...' : '确认接收' }}</button>
      </template>
    </AppDialog>

    <AppDialog :visible="startingOrder" :width="420" :title="'开始培养'" @close="closeStartModal">
      <div class="dialog-body">
        <p class="modal-hint">{{ startingOrder.order_no_wsh }} · {{ startingOrder.pet_name_wsh || '宠物' }}</p>
        <input ref="startPhotoInput" type="file" accept="image/*" style="display:none" @change="onStartPhotoChange">
        <button class="btn btn-outline btn-sm" @click="startPhotoInput?.click()">选择开始照片</button>
        <div v-if="startPhotoPreview" class="photo-preview-grid single">
          <img :src="startPhotoPreview" alt="开始照片预览">
        </div>
      </div>
      <template #footer>
        <button class="btn btn-secondary btn-sm" @click="closeStartModal">取消</button>
        <button class="btn btn-primary btn-sm" :disabled="startUploading" @click="submitStart">{{ startUploading ? '上传中...' : '开始培养' }}</button>
      </template>
    </AppDialog>

    <!-- 确认接走：必须由主人出示独立 4 位接走码（送达码不能用于接走） -->
    <AppDialog :visible="checkoutOrder" :width="420" :title="'确认接走'" @close="closeCheckoutModal">
      <div class="dialog-body">
        <p class="modal-hint">{{ checkoutOrder.order_no_wsh }} · {{ checkoutOrder.pet_name_wsh || '宠物' }}</p>
        <input
          v-model="checkoutForm.pickup_code_wsh"
          data-testid="pickup-code-input"
          class="form-control code-input"
          inputmode="numeric"
          maxlength="4"
          placeholder="输入主人出示的4位接走码"
        >
        <p class="modal-hint">
          接走时间与晚接费由服务端在此刻冻结；如需补费，主人支付后订单才完成。
        </p>
        <p v-if="checkoutError" class="modal-hint" style="color:#c62828">{{ checkoutError }}</p>
      </div>
      <template #footer>
        <button class="btn btn-secondary btn-sm" @click="closeCheckoutModal">取消</button>
        <button class="btn btn-primary btn-sm" :disabled="completing" @click="submitCheckout">
          {{ completing ? '提交中...' : '确认接走' }}
        </button>
      </template>
    </AppDialog>
  </section>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { formatPaymentTimeoutRemaining, getPaymentTimeoutRemaining } from '@/utils/orderPaymentTimeout'
import { formatDateTime as utilFormatDateTime } from '@/utils/format'

// 生产构建不渲染调试开关：避免有人勾一下绕过 500 米交接围栏
const isDevBuild = import.meta.env.DEV

const props = defineProps({
  poolOrders: { type: Array, default: () => [] },
  activeOrders: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  completingId: { type: [Number, String], default: null },
  subTab: { type: String, default: 'pool' },
  receiving: { type: Boolean, default: false },
  startUploading: { type: Boolean, default: false },
  processingOrderNo: { type: String, default: '' },
  modalKey: { type: Number, default: 0 },
  nowMs: { type: Number, default: () => Date.now() },
})
const emit = defineEmits(['reject', 'claim', 'receive', 'start', 'checkout', 'select-order', 'update:subTab'])

const statusMap = {
  pending: '待付款', paid: '已支付',   confirmed: '待接收',
  received: '已接收', in_progress: '培养中',
  completed: '已完成', cancelled: '已取消',
}

const visibleOrders = computed(() => {
  if (props.subTab === 'pool') return props.poolOrders
  return props.activeOrders
})
const emptyText = computed(() => {
  if (props.subTab === 'pool') return '暂无可抢订单'
  return '暂无进行中的订单'
})

// 父组件每次要关弹窗就把 modalKey 加一，这里跟着把三个弹窗的状态都清干净
watch(() => props.modalKey, () => {
  receivingOrder.value = null
  startingOrder.value = null
  checkoutOrder.value = null
  receiveForm.handover_code_wsh = ''
  checkoutForm.pickup_code_wsh = ''
  checkoutError.value = ''
  clearStartPhoto()
})

const receivingOrder = ref(null)
const startingOrder = ref(null)
const checkoutOrder = ref(null)
const checkoutError = ref('')
const checkoutForm = reactive({ pickup_code_wsh: '' })
const completing = computed(() => checkoutOrder.value != null
  && props.completingId === checkoutOrder.value.id_wsh)
const receiveForm = reactive({
  handover_code_wsh: '',
  received_address_wsh: '',
  received_latitude_wsh: null,
  received_longitude_wsh: null,
  received_accuracy_wsh: null,
  skip_distance_check_wsh: false,
})
const locating = ref(false)
const startPhotoInput = ref(null)
const startPhotoFile = ref(null)
const startPhotoPreview = ref('')

// 离开页面时把预览地址还回去，不然这张图会一直挂在内存里
onUnmounted(() => {
  revokeStartPhotoPreview()
})

async function openReceiveModal(order) {
  receivingOrder.value = order
  receiveForm.handover_code_wsh = ''
  receiveForm.skip_distance_check_wsh = false
  receiveForm.received_address_wsh = ''
  receiveForm.received_latitude_wsh = null
  receiveForm.received_longitude_wsh = null
  receiveForm.received_accuracy_wsh = null
  await locateForReceive()
}

// 打开弹窗就先取一次位置，失败不阻断——用户可以点"重新定位"或勾调试开关
async function locateForReceive() {
  locating.value = true
  try {
    const loc = await getCurrentAddress()
    receiveForm.received_latitude_wsh = loc?.latitude_wsh ?? null
    receiveForm.received_longitude_wsh = loc?.longitude_wsh ?? null
    receiveForm.received_accuracy_wsh = loc?.accuracy_wsh ?? null
    receiveForm.received_address_wsh = loc?.address_wsh ?? ''
  } catch (_) {
    receiveForm.received_latitude_wsh = null
    receiveForm.received_longitude_wsh = null
    receiveForm.received_accuracy_wsh = null
  } finally {
    locating.value = false
  }
}

function closeReceiveModal() {
  receivingOrder.value = null
  receiveForm.handover_code_wsh = ''
}

/** 待补费提示文案：金额全部来自后端，前端只做展示 */
function overtimeText(order) {
  const amount = Number(order?.overtime_amount_wsh || 0)
  const hours = Number(order?.overtime_hours_wsh || 0)
  return hours > 0 ? `¥${amount.toFixed(2)}（${hours} 小时）` : `¥${amount.toFixed(2)}`
}

function openCheckoutModal(order) {
  checkoutOrder.value = order
  checkoutForm.pickup_code_wsh = ''
  checkoutError.value = ''
}

function closeCheckoutModal() {
  checkoutOrder.value = null
  checkoutForm.pickup_code_wsh = ''
  checkoutError.value = ''
}

// 接走码必须是 4 位数字，格式不对直接返回，不发请求（避免把送达码/空值当接走码提交）
function submitCheckout() {
  if (!checkoutOrder.value) return
  const code = String(checkoutForm.pickup_code_wsh || '').trim()
  if (!/^\d{4}$/.test(code)) {
    checkoutError.value = '请输入 4 位数字接走码'
    return
  }
  checkoutError.value = ''
  emit('checkout', {
    order_no_wsh: checkoutOrder.value.order_no_wsh,
    pickup_code_wsh: code,
  })
}

// 交接码必须是 4 位数字，格式不对就直接返回，不发请求
function submitReceive() {
  if (!receivingOrder.value) return
  if (!/^\d{4}$/.test(receiveForm.handover_code_wsh.trim())) return
  emit('receive', {
    order_no_wsh: receivingOrder.value.order_no_wsh,
    handover_code_wsh: receiveForm.handover_code_wsh.trim(),
    received_address_wsh: receiveForm.received_address_wsh,
    received_latitude_wsh: receiveForm.received_latitude_wsh,
    received_longitude_wsh: receiveForm.received_longitude_wsh,
    received_accuracy_wsh: receiveForm.received_accuracy_wsh,
    skip_distance_check_wsh: receiveForm.skip_distance_check_wsh,
  })
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

// 换照片前先把上一张的预览地址还掉，否则每换一次就多留一份
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

// 状态对应哪个颜色的标签，没登记的状态统一按蓝色处理
function statusBadge(status) {
  return {
    pending: 'badge-warning', paid: 'badge-info',     confirmed: 'badge-info',
    received: 'badge-primary',
    in_progress: 'badge-info', completed: 'badge-success',
    cancelled: 'badge-danger',
  }[status] || 'badge-info'
}

function formatDateTime(value) {
  return utilFormatDateTime(value, { fallback: '-' })
}

// 只有待付款、并且付款倒计时还没走完的订单才显示这条提醒
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
/* 「待抢单」角标：与页签按钮同排，数字用小圆点强调 */
.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--color-danger, #d93025);
  color: #fff;
  font-size: 11px;
  line-height: 1;
}
.empty-hint {
  margin: 8px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
  max-width: 420px;
  text-align: center;
}
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
.dialog-body { display: grid; gap: 12px; }
.modal-hint { margin: 0; color: var(--color-muted-foreground); font-size: 13px; }
.code-input { max-width: 180px; font-size: 22px; letter-spacing: 4px; text-align: center; }
.photo-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
.photo-preview-grid.single { max-width: 180px; }
.photo-preview-grid img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: var(--radius-inline); border: 1px solid var(--color-border); background: var(--color-muted); }
.loading, .empty-state { text-align: center; padding: 40px 20px; }
</style>
