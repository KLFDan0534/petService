<template>
  <div>
    <PageHero title="支付记录" subtitle="查看您的支付历史" />
    <div class="card" style="margin-bottom:24px;padding:16px">
      <h3 style="margin-bottom:12px">创建支付</h3>
      <div style="display:flex;gap:12px;align-items:flex-end">
        <div class="form-group" style="flex:1">
          <label>订单号</label>
          <input v-model="orderNo" class="form-control" placeholder="输入订单号">
        </div>
        <div class="form-group" style="flex:1">
          <label>金额</label>
          <input v-model="amount" type="number" class="form-control" placeholder="输入金额">
        </div>
        <button class="btn btn-primary" @click="createPayment">创建支付</button>
      </div>
    </div>
    <div class="card" style="margin-bottom:24px;padding:16px">
      <h3 style="margin-bottom:12px">查询订单支付</h3>
      <div style="display:flex;gap:12px;align-items:flex-end">
        <div class="form-group" style="flex:1">
          <label>订单号</label>
          <input v-model="lookupOrderNo" class="form-control" placeholder="输入订单号查询支付">
        </div>
        <button class="btn btn-outline" @click="lookupPayment">查询</button>
      </div>
      <div v-if="orderPayment" class="card" style="margin-top:12px;padding:12px">
        <div style="display:flex;justify-content:space-between">
          <div><strong>¥{{ orderPayment.amount_wsh }}</strong> · {{ payMethodMap[orderPayment.method_wsh] || orderPayment.method_wsh }}</div>
          <span :class="['badge', orderPayment.status_wsh === 'pending' ? 'badge-warning' : 'badge-success']">
            {{ orderPayment.status_wsh === 'pending' ? '待支付' : '已支付' }}
          </span>
        </div>
        <div style="font-size:13px;color:var(--color-muted-foreground);margin-top:4px">支付号: {{ orderPayment.pay_no_wsh }} · {{ new Date(orderPayment.created_at_wsh).toLocaleString() }}</div>
      </div>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <EmptyState v-else-if="payments.length === 0" title="暂无支付记录" description="去下单吧" icon="💳">
      <router-link to="/orders" class="btn btn-primary">查看订单</router-link>
    </EmptyState>
    <div v-else class="card" style="margin-bottom:12px" v-for="p in payments" :key="p.id_wsh">
      <div style="display:flex;justify-content:space-between">
        <div><strong>¥{{ p.amount_wsh }}</strong> · {{ payMethodMap[p.method_wsh] || p.method_wsh }}</div>
        <span :class="['badge', p.status_wsh === 'pending' ? 'badge-warning' : 'badge-success']">
          {{ p.status_wsh === 'pending' ? '待支付' : '已支付' }}
        </span>
      </div>
      <div style="font-size:13px;color:var(--color-muted-foreground);margin-top:4px">{{ new Date(p.created_at_wsh).toLocaleString() }}</div>
      <div v-if="p.status_wsh === 'pending'" style="margin-top:8px">
        <button class="btn btn-sm btn-primary" @click="payPayment(p)">去支付</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const payments = ref([])
const loading = ref(true)
const orderNo = ref('')
const amount = ref('')
const lookupOrderNo = ref('')
const orderPayment = ref(null)
const payMethodMap = { wechat: '微信支付', alipay: '支付宝', bank: '银行转账', card: '银行卡' }

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/payments'); if (r.code === 200) payments.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function createPayment() {
  if (!orderNo.value || !amount.value) return appStore.addToast('请填写完整信息', 'error')
  try {
    const r = await authStore.apiPost('/api/payments/create', { order_no_wsh: orderNo.value, amount_wsh: amount.value })
    if (r.code === 200) { appStore.addToast('创建成功', 'success'); payments.value.unshift(r.data); orderNo.value = ''; amount.value = '' }
  } catch (e) { appStore.addToast('创建失败', 'error') }
}

async function payPayment(p) {
  try {
    const r = await authStore.apiPost('/api/payments/pay', { pay_no_wsh: p.pay_no_wsh })
    if (r.code === 200) { appStore.addToast('支付成功', 'success'); p.status_wsh = 'paid' }
  } catch (e) { appStore.addToast('支付失败', 'error') }
}

async function lookupPayment() {
  if (!lookupOrderNo.value.trim()) return appStore.addToast('请输入订单号', 'error')
  try {
    const r = await authStore.apiGet(`/api/payments/order/${lookupOrderNo.value}`)
    if (r.code === 200) orderPayment.value = r.data
    else { orderPayment.value = null; appStore.addToast('未找到支付记录', 'info') }
  } catch (e) { orderPayment.value = null; appStore.addToast('查询失败', 'error') }
}
</script>
