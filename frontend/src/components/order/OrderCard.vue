<template>
  <article class="order-card">
    <header class="order-card__header">
      <div class="order-title-group">
        <span class="order-no">{{ order.order_no_wsh || `#${order.id_wsh}` }}</span>
        <h3>{{ order.service_name_wsh || '宠物寄养服务' }}</h3>
      </div>
      <div class="order-card__badges">
        <span :class="['badge', statusBadge(order.status_wsh)]">
          {{ statusLabel(order.status_wsh) }}
        </span>
        <span v-if="order.status_wsh === 'completed' && authStore.isOwner" :class="['badge', order.has_feedback_wsh ? 'badge-success' : 'badge-warning']">
          {{ order.has_feedback_wsh ? '已评价' : '待反馈' }}
        </span>
      </div>
    </header>

    <div v-if="showPaymentCountdown" class="payment-countdown">
      <span>支付剩余</span>
      <strong>{{ paymentCountdownText }}</strong>
      <small>超时后订单将自动取消</small>
    </div>

    <div class="order-card__body">
      <div class="pet-panel">
        <div class="avatar" :style="avatarStyle(order.pet_avatar_wsh)">
          <span v-if="!order.pet_avatar_wsh">{{ firstLetter(order.pet_name_wsh, '宠') }}</span>
        </div>
        <div>
          <strong>{{ order.pet_name_wsh || `宠物 #${order.pet_id_wsh}` }}</strong>
          <span>{{ compactPetInfo(order) }}</span>
        </div>
      </div>

      <dl class="order-facts">
        <div>
          <dt>商家</dt>
          <dd>{{ order.merchant_name_wsh || `#${order.merchant_id_wsh || '-'}` }}</dd>
        </div>
        <div>
          <dt>看护人</dt>
          <dd>{{ order.keeper_name_wsh || `#${order.keeper_id_wsh || '-'}` }}</dd>
        </div>
        <div>
          <dt>服务时间</dt>
          <dd>{{ formatDate(order.start_date_wsh) }} 至 {{ formatDate(order.end_date_wsh) }}</dd>
        </div>
        <div>
          <dt>送达时间</dt>
          <dd>{{ formatDateTime(order.delivery_time_wsh) }}</dd>
        </div>
        <div>
          <dt>接回时间</dt>
          <dd>{{ formatDateTime(order.pickup_time_wsh) }}</dd>
        </div>
        <div>
          <dt>服务地址</dt>
          <dd>{{ order.delivery_address_wsh || order.merchant_address_wsh || '-' }}</dd>
        </div>
        <div v-if="order.emergency_contact_name_wsh || order.emergency_contact_phone_wsh">
          <dt>紧急联系人</dt>
          <dd>{{ order.emergency_contact_name_wsh || '-' }} {{ order.emergency_contact_phone_wsh || '' }}</dd>
        </div>
      </dl>
    </div>

    <div class="money-row">
      <div>
        <span>实付金额</span>
        <strong>¥{{ money(order.final_amount_wsh || order.total_amount_wsh) }}</strong>
        <small v-if="orderBillingText">{{ orderBillingText }}</small>
      </div>
      <div v-if="order.discount_wsh && Number(order.discount_wsh) > 0" class="discount">
        已优惠 ¥{{ money(order.discount_wsh) }}
      </div>
    </div>

    <p v-if="order.remark_wsh" class="order-remark">{{ order.remark_wsh }}</p>

    <footer class="order-actions">
      <button class="btn btn-sm btn-outline" type="button" @click="$emit('viewDetail')">
        <el-icon><View /></el-icon>
        详情
      </button>
      <template v-if="order.status_wsh === 'pending' && authStore.isOwner">
        <button class="btn btn-sm btn-success" type="button" :disabled="processing" @click="$emit('pay', 'balance')">
          <el-icon><Wallet /></el-icon>
          余额支付
        </button>
        <button class="btn btn-sm btn-danger" type="button" :disabled="processing" @click="$emit('cancel')">
          <el-icon><Close /></el-icon>
          取消
        </button>
      </template>
      <button v-if="order.status_wsh === 'confirmed' && authStore.isOwner" class="btn btn-sm btn-primary" type="button" :disabled="processing" @click="$emit('deliver')">
        <el-icon><Check /></el-icon>
        已送达
      </button>
      <template v-if="order.status_wsh === 'completed' && authStore.isOwner">
        <button v-if="!order.has_feedback_wsh" class="btn btn-sm btn-primary" type="button" @click="$emit('review')">
          <el-icon><Star /></el-icon>
          评价
        </button>
        <button class="btn btn-sm btn-danger" type="button" @click="goComplaint">
          <el-icon><Warning /></el-icon>
          投诉
        </button>
        <button class="btn btn-sm btn-success" type="button" @click="$emit('tip')">
          <el-icon><Present /></el-icon>
          打赏
        </button>
      </template>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Check, Close, Present, Star, View, Wallet, Warning } from '@element-plus/icons-vue'
import { OrderStatus, getStatusLabel, getStatusBadge } from '@/constants/statusMaps'
import { formatPaymentTimeoutRemaining, getPaymentTimeoutRemaining } from '@/utils/orderPaymentTimeout'
import { billingText } from '@/domain/BookingUnit'

const props = defineProps({
  order: { type: Object, required: true },
  nowMs: { type: Number, default: () => Date.now() },
  processing: { type: Boolean, default: false },
})
defineEmits(['cancel', 'pay', 'deliver', 'review', 'tip', 'viewDetail'])

const authStore = useAuthStore()
const router = useRouter()
const orderBillingText = computed(() => billingText(props.order))

function goComplaint() {
  router.push({
    path: '/complaints',
    query: {
      orderId: props.order.id_wsh,
      orderNo: props.order.order_no_wsh || '',
    },
  })
}
const paymentRemaining = computed(() => getPaymentTimeoutRemaining(props.order, props.nowMs))
const showPaymentCountdown = computed(() => props.order.status_wsh === 'pending' && paymentRemaining.value != null)
const paymentCountdownText = computed(() => formatPaymentTimeoutRemaining(paymentRemaining.value))

function statusLabel(status) { return getStatusLabel(OrderStatus, status) }
function statusBadge(status) { return getStatusBadge(OrderStatus, status) }
function formatDate(value) { return value ? String(value).slice(0, 10) : '-' }
function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}
function money(value) { return Number(value || 0).toFixed(2) }
function compactPetInfo(order) {
  const parts = [order.pet_breed_wsh, order.pet_age_wsh ? `${order.pet_age_wsh}岁` : '', order.pet_weight_wsh ? `${order.pet_weight_wsh}kg` : '']
  return parts.filter(Boolean).join(' · ') || '宠物信息待完善'
}
function avatarStyle(url) {
  return url ? { backgroundImage: `url(${url})` } : {}
}
function firstLetter(value, fallback) {
  return value ? String(value).charAt(0) : fallback
}
</script>

<style scoped>
.order-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 18px;
  box-shadow: var(--shadow-sm);
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}
.order-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-border);
}
.order-card__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}
.order-title-group { min-width: 0; }
.order-title-group h3 {
  margin: 4px 0 0;
  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}
.order-no {
  color: var(--color-muted-foreground);
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  overflow-wrap: anywhere;
}
.order-card__body {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 1fr;
  gap: 18px;
  margin-top: 16px;
}
.payment-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(245, 158, 11, .35);
  border-radius: 8px;
  background: rgba(245, 158, 11, .08);
  color: var(--color-warning, #b45309);
  font-size: 13px;
}
.payment-countdown strong {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 16px;
}
.payment-countdown small { color: var(--color-muted-foreground); }
.pet-panel {
  display: flex;
  gap: 12px;
  min-width: 0;
  align-items: center;
}
.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-muted);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-weight: 700;
  flex-shrink: 0;
}
.pet-panel strong,
.pet-panel span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pet-panel span {
  color: var(--color-muted-foreground);
  font-size: 12px;
  margin-top: 2px;
}
.order-facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px 16px;
}
.order-facts div { min-width: 0; }
.order-facts dt {
  color: var(--color-muted-foreground);
  font-size: 12px;
  margin-bottom: 2px;
}
.order-facts dd {
  font-size: 13px;
  min-width: 0;
  overflow-wrap: anywhere;
}
.money-row {
  display: grid;
  gap: 4px;
  margin-top: 16px;
  padding: 12px 14px;
  background: var(--color-muted);
  border-radius: 8px;
}
.money-row span {
  color: var(--color-muted-foreground);
  font-size: 12px;
  margin-right: 8px;
}
.money-row strong {
  color: var(--color-destructive);
  font-size: 20px;
  margin-right: 8px;
}
.money-row small,
.discount { color: var(--color-muted-foreground); }
.discount { font-size: 13px; }
.order-remark {
  margin: 12px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
  white-space: pre-wrap;
}
.order-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; align-items: center; min-width: 0; }
.payment-method {
  height: 32px;
  min-width: 112px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0 8px;
  background: var(--color-card);
  color: var(--color-foreground);
}
@media (max-width: 760px) {
  .order-card__body { grid-template-columns: 1fr; }
  .order-card__header {
    align-items: stretch;
    flex-direction: column;
  }
  .order-actions .btn {
    flex: 1 1 148px;
  }
}
</style>
