<template>
  <div class="sidebar-stack">
    <section class="card status-card">
      <span class="section-kicker">当前状态</span>
      <strong>{{ statusLabel(order.status_wsh) }}</strong>
      <p>{{ statusHint(order.status_wsh) }}</p>
      <div v-if="showPaymentCountdown" class="payment-countdown">
        <span>支付剩余</span>
        <strong>{{ paymentCountdownText }}</strong>
        <small>超时后订单将自动取消</small>
      </div>
    </section>

    <section class="card progress-card">
      <span class="section-kicker">服务进度</span>
      <ol class="progress-list">
        <li v-for="(step, index) in progressSteps" :key="step" :class="{ done: index <= progressStep }">
          <span>{{ index + 1 }}</span>
          {{ step }}
        </li>
      </ol>
    </section>

    <section class="card merchant-card">
      <span class="section-kicker">商家信息</span>
      <div class="merchant-line">
        <div class="merchant-avatar">{{ order.merchant_name_wsh?.charAt(0) || '商' }}</div>
        <div>
          <strong>{{ order.merchant_name_wsh || '-' }}</strong>
          <p>{{ order.merchant_phone_wsh || '-' }}</p>
        </div>
      </div>
    </section>

    <section class="sidebar-actions">
      <template v-if="order.status_wsh === 'pending'">
        <button class="btn btn-primary" type="button" :disabled="processing" @click="$emit('pay', 'balance')">
          余额支付
        </button>
        <button class="btn btn-danger" type="button" :disabled="processing" @click="$emit('cancel')">
          取消订单
        </button>
      </template>
      <button class="btn btn-outline" type="button" @click="contactService">联系客服</button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { formatPaymentTimeoutRemaining, getPaymentTimeoutRemaining } from '@/utils/orderPaymentTimeout'

const props = defineProps({
  order: { type: Object, required: true },
  processing: { type: Boolean, default: false },
})
defineEmits(['pay', 'cancel'])

const appStore = useAppStore()
const nowMs = ref(Date.now())
let countdownTimer = null

const progressSteps = ['创建订单', '支付完成', '宠物送达', '商家接收', '开始服务', '服务完成']

const statusMap = {
  pending: '待付款',
  paid: '已支付',
  confirmed: '待送达',
  delivered: '已送达',
  received: '已接收',
  in_progress: '服务中',
  completed: '已完成',
  cancelled: '已取消',
  refunding: '退款中',
  refunded: '已退款',
}

function statusLabel(status) {
  return statusMap[status] || status || '-'
}

function statusHint(status) {
  const map = {
    pending: '等待完成支付，支付后商家会继续处理。',
    paid: '订单已支付，等待商家确认。',
    confirmed: '商家已确认，请按约定时间送达宠物。',
    delivered: '宠物已送达，等待商家核验交接码。',
    received: '商家已接收宠物，可开始寄养服务。',
    in_progress: '寄养服务进行中，可查看服务动态。',
    completed: '服务已完成，平台托管款已结算给商家。',
    cancelled: '订单已取消。',
    refunding: '退款申请处理中。',
    refunded: '退款已完成。',
  }
  return map[status] || '订单状态已更新。'
}

const progressStep = computed(() => {
  const steps = {
    pending: 0,
    paid: 1,
    confirmed: 1,
    delivered: 2,
    received: 3,
    in_progress: 4,
    completed: 5,
  }
  return steps[props.order.status_wsh] ?? 0
})

const paymentRemaining = computed(() => getPaymentTimeoutRemaining(props.order, nowMs.value))
const showPaymentCountdown = computed(() => props.order.status_wsh === 'pending' && paymentRemaining.value != null)
const paymentCountdownText = computed(() => formatPaymentTimeoutRemaining(paymentRemaining.value))

onMounted(() => {
  countdownTimer = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

function contactService() {
  appStore.addToast('客服热线：400-888-8888', 'info')
}
</script>

<style scoped>
.sidebar-stack {
  position: sticky;
  top: 20px;
  display: grid;
  gap: 14px;
}
.card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  padding: 18px;
}
.section-kicker {
  display: block;
  color: var(--color-muted-foreground);
  font-size: 12px;
  margin-bottom: 6px;
}
.status-card strong {
  display: block;
  font-size: 24px;
  color: var(--color-primary);
}
.status-card p {
  margin: 8px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.5;
}
.payment-countdown {
  display: grid;
  gap: 4px;
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(245, 158, 11, .35);
  border-radius: 8px;
  background: rgba(245, 158, 11, .08);
}
.payment-countdown span,
.payment-countdown small {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.payment-countdown strong {
  color: var(--color-warning, #b45309);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 22px;
}
.progress-list {
  list-style: none;
  display: grid;
  gap: 10px;
}
.progress-list li {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.progress-list li span {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  font-size: 12px;
  font-weight: 700;
}
.progress-list li.done {
  color: var(--color-foreground);
  font-weight: 700;
}
.progress-list li.done span {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
.merchant-line {
  display: flex;
  gap: 12px;
  align-items: center;
}
.merchant-avatar {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  background: var(--color-muted);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  flex: 0 0 auto;
}
.merchant-line p {
  margin: 2px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.sidebar-actions {
  display: grid;
  gap: 10px;
}
.sidebar-actions .btn,
.payment-method {
  width: 100%;
}
.payment-method {
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0 10px;
  background: var(--color-card);
  color: var(--color-foreground);
}
@media (max-width: 900px) {
  .sidebar-stack {
    position: static;
  }
}
</style>
