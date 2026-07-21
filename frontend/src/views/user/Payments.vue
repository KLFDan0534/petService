<template>
  <div>
    <PageHero title="支付记录" subtitle="查看订单支付和支付历史" />

    <div class="card payment-tool">
      <h3>创建订单支付</h3>
      <div class="payment-form">
        <div class="form-group">
          <label>订单号</label>
          <input v-model="orderNo" class="form-control" placeholder="输入订单号">
        </div>
        <div class="form-group">
          <label>支付方式</label>
          <select v-model="paymentMethod" class="form-control">
            <option value="balance">余额支付</option>
          </select>
        </div>
        <button class="btn btn-primary" type="button" @click="createPayment">创建支付</button>
      </div>
    </div>

    <div class="card payment-tool">
      <h3>查询订单支付</h3>
      <div class="payment-form">
        <div class="form-group">
          <label>订单号</label>
          <input v-model="lookupOrderNo" class="form-control" placeholder="输入订单号查询支付">
        </div>
        <button class="btn btn-outline" type="button" @click="lookupPayment">查询</button>
      </div>
      <div v-if="orderPayment" class="payment-result">
        <div>
          <strong>¥{{ money(orderPayment.amount_wsh) }}</strong>
          <span>{{ payMethodMap[orderPayment.method_wsh] || orderPayment.method_wsh }}</span>
        </div>
        <span :class="['badge', paymentStatusBadge(orderPayment.status_wsh)]">
          {{ paymentStatusText(orderPayment.status_wsh) }}
        </span>
        <small>支付号: {{ orderPayment.pay_no_wsh }}</small>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <EmptyState v-else-if="payments.length === 0" title="暂无支付记录" description="去下单吧" icon="💳">
      <router-link to="/orders" class="btn btn-primary">查看订单</router-link>
    </EmptyState>
    <div v-else class="card payment-row" v-for="p in payments" :key="p.id_wsh">
      <div>
        <strong>¥{{ money(p.amount_wsh) }}</strong>
        <span>{{ payMethodMap[p.method_wsh] || p.method_wsh }}</span>
      </div>
      <span :class="['badge', paymentStatusBadge(p.status_wsh)]">
        {{ paymentStatusText(p.status_wsh) }}
      </span>
      <small>{{ formatTime(p.created_at_wsh) }}</small>
      <button v-if="canExecutePayment(p)" class="btn btn-sm btn-primary" type="button" @click="payPayment(p)">
        去支付
      </button>
      <small v-else-if="p.status_wsh === 'pending'" class="payment-note">{{ pendingPaymentHint(p) }}</small>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPayments, createPayment as apiCreatePayment, executePayment, getPaymentByOrder } from '@/api/payment'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const appStore = useAppStore()
const payments = ref([])
const loading = ref(true)
const orderNo = ref('')
const paymentMethod = ref('balance')
const lookupOrderNo = ref('')
const orderPayment = ref(null)

const payMethodMap = {
  mock: '模拟支付（已停用）',
  balance: '余额支付',
  wechat: '微信支付',
  alipay: '支付宝',
}

onMounted(loadPayments)

async function loadPayments() {
  loading.value = true
  try {
    const r = await getPayments()
    if (r.code === 200) payments.value = Array.isArray(r.data) ? r.data : []
  } finally {
    loading.value = false
  }
}

async function createPayment() {
  if (!orderNo.value.trim()) return appStore.addToast('请输入订单号', 'error')
  try {
    const r = await apiCreatePayment({ order_no_wsh: orderNo.value.trim(), method_wsh: paymentMethod.value })
    if (r.code === 200) {
      appStore.addToast('支付记录已创建', 'success')
      payments.value = [r.data, ...payments.value.filter(item => item.id_wsh !== r.data.id_wsh)]
      orderNo.value = ''
    }
  } catch (e) {
    appStore.addToast(e?.message || '创建失败', 'error')
  }
}

async function payPayment(p) {
  try {
    const r = await executePayment({ pay_no_wsh: p.pay_no_wsh })
    if (r.code === 200) {
      appStore.addToast('支付成功', 'success')
      await loadPayments()
    }
  } catch (e) {
    appStore.addToast(e?.message || '支付失败', 'error')
  }
}

async function lookupPayment() {
  if (!lookupOrderNo.value.trim()) return appStore.addToast('请输入订单号', 'error')
  try {
    const r = await getPaymentByOrder(lookupOrderNo.value.trim())
    if (r.code === 200) orderPayment.value = r.data
    else {
      orderPayment.value = null
      appStore.addToast('未找到支付记录', 'info')
    }
  } catch (e) {
    orderPayment.value = null
    appStore.addToast(e?.message || '查询失败', 'error')
  }
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function paymentStatusText(status) {
  return {
    pending: '待支付',
    success: '已支付',
    failed: '支付失败',
  }[status] || status || '-'
}

function paymentStatusBadge(status) {
  return {
    pending: 'badge-warning',
    success: 'badge-success',
    failed: 'badge-danger',
  }[status] || 'badge-secondary'
}

function canExecutePayment(payment) {
  return payment?.status_wsh === 'pending' && payment?.method_wsh === 'balance'
}

function pendingPaymentHint(payment) {
  if (payment?.method_wsh === 'mock') return '模拟支付已停用'
  return '等待支付平台确认'
}
</script>

<style scoped>
.payment-tool {
  margin-bottom: 24px;
  padding: 16px;
}
.payment-tool h3 {
  margin-bottom: 12px;
  font-size: 18px;
}
.payment-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 220px) max-content;
  gap: 12px;
  align-items: end;
}
.payment-result,
.payment-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content max-content max-content;
  gap: 12px;
  align-items: center;
  min-width: 0;
}
.payment-result {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
}
.payment-row {
  margin-bottom: 12px;
  padding: 14px 16px;
}
.payment-result strong,
.payment-row strong {
  margin-right: 8px;
}
.payment-result > div,
.payment-row > div {
  min-width: 0;
  overflow-wrap: anywhere;
}
.payment-result small,
.payment-row small {
  color: var(--color-muted-foreground);
  min-width: 0;
  overflow-wrap: anywhere;
}
.payment-note {
  justify-self: end;
}
@media (max-width: 720px) {
  .payment-form,
  .payment-result,
  .payment-row {
    grid-template-columns: 1fr;
  }
  .payment-row .btn,
  .payment-note {
    justify-self: stretch;
  }
}
</style>
