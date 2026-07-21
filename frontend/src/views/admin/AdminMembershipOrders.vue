<template>
  <div class="membership-order-page">
    <div class="membership-order-toolbar">
      <h2>会员订单管理</h2>
      <div class="toolbar-actions">
        <select v-model="statusFilter" class="filter-select" @change="loadOrders">
          <option value="">全部状态</option>
          <option value="pending">待支付</option>
          <option value="paid">已支付</option>
          <option value="cancelled">已取消</option>
          <option value="refunded">已退款</option>
        </select>
        <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="loading" @click="loadOrders">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户ID</th>
            <th>套餐</th>
            <th>金额</th>
            <th>支付方式</th>
            <th>状态</th>
            <th>会员有效期</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id_wsh || order.order_no_wsh">
            <td><code>{{ order.order_no_wsh }}</code></td>
            <td>{{ order.user_id_wsh }}</td>
            <td>
              <div class="plan-cell">
                <strong>{{ order.plan_name_wsh || order.plan_code_wsh || '-' }}</strong>
                <small>{{ order.plan_code_wsh || '-' }}</small>
              </div>
            </td>
            <td>¥{{ money(order.amount_wsh) }}</td>
            <td>{{ payMethodText(order.pay_method_wsh) }}</td>
            <td>
              <span :class="['order-status', statusClass(order.status_wsh)]">
                {{ statusText(order.status_wsh) }}
              </span>
            </td>
            <td>{{ periodText(order) }}</td>
            <td>{{ dateText(order.created_at_wsh) }}</td>
            <td>
              <button
                v-if="order.status_wsh === 'pending'"
                class="btn btn-primary btn-sm icon-btn"
                type="button"
                :disabled="confirmingOrderNo === order.order_no_wsh"
                @click="confirmPaid(order)"
              >
                <el-icon><CreditCard /></el-icon>
                <span>{{ confirmingOrderNo === order.order_no_wsh ? '确认中...' : '确认支付' }}</span>
              </button>
              <span v-else class="muted">-</span>
            </td>
          </tr>
          <tr v-if="orders.length === 0">
            <td colspan="9" class="empty-cell">暂无会员订单</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { CreditCard, Refresh } from '@element-plus/icons-vue'
import { confirmMembershipOrderPaid, getAdminMembershipOrders } from '@/api/membership'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const statusFilter = ref('')
const orders = ref([])
const confirmingOrderNo = ref('')

onMounted(loadOrders)

async function loadOrders() {
  loading.value = true
  try {
    const params = statusFilter.value ? { status_wsh: statusFilter.value } : {}
    const res = await getAdminMembershipOrders(params)
    if (res.code === 200) {
      orders.value = Array.isArray(res.data) ? res.data : []
    } else {
      appStore.addToast(res.message || '会员订单加载失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function confirmPaid(order) {
  if (confirmingOrderNo.value) return
  if (!confirm(`确认会员订单 ${order.order_no_wsh} 已支付吗？`)) return
  confirmingOrderNo.value = order.order_no_wsh
  try {
    const res = await confirmMembershipOrderPaid(order.order_no_wsh)
    if (res.code === 200) {
      appStore.addToast('会员订单已确认支付', 'success')
      await loadOrders()
    } else {
      appStore.addToast(res.message || '会员订单确认失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单确认失败', 'error')
  } finally {
    confirmingOrderNo.value = ''
  }
}

function statusText(status) {
  return {
    pending: '待支付',
    paid: '已支付',
    cancelled: '已取消',
    refunded: '已退款',
  }[status] || status || '-'
}

function statusClass(status) {
  return {
    pending: 'is-wait',
    paid: 'is-on',
    cancelled: 'is-off',
    refunded: 'is-off',
  }[status] || 'is-off'
}

function payMethodText(value) {
  return {
    mock: '模拟支付（已停用）',
    balance: '余额支付',
    wechat: '微信支付',
    alipay: '支付宝',
  }[value] || value || '-'
}

function periodText(order) {
  const start = dateText(order.membership_start_at_wsh)
  const end = dateText(order.membership_end_at_wsh)
  return start === '-' && end === '-' ? '-' : `${start} 至 ${end}`
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.membership-order-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.membership-order-toolbar,
.toolbar-actions {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  min-width: 0;
}

.membership-order-toolbar h2 {
  font-size: 22px;
  margin: 0;
  min-width: 0;
}

.toolbar-actions {
  flex-wrap: wrap;
  min-width: 0;
}

.membership-order-toolbar {
  flex-wrap: wrap;
}

.filter-select {
  max-width: 100%;
  min-height: 36px;
  padding: 8px 12px;
  width: 120px;
}

.plan-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.plan-cell strong,
.plan-cell small {
  overflow-wrap: anywhere;
}

.plan-cell small,
.empty-cell,
.muted {
  color: var(--color-muted-foreground);
}

.order-status {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-size: 12px;
  justify-content: center;
  min-width: 58px;
  padding: 4px 8px;
}

.order-status.is-on {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.order-status.is-wait {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.order-status.is-off {
  background: rgba(100, 116, 139, 0.14);
  color: var(--color-muted-foreground);
}

.empty-cell {
  padding: 32px;
  text-align: center;
}

code {
  background: var(--color-muted);
  border-radius: 6px;
  color: var(--color-foreground);
  display: inline-block;
  font-size: 12px;
  max-width: 100%;
  overflow-wrap: anywhere;
  padding: 3px 6px;
  white-space: normal;
}

@media (max-width: 720px) {
  .membership-order-toolbar,
  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-select,
  .toolbar-actions .btn {
    width: 100%;
  }
}
</style>
