<template>
  <div>
    <div class="membership-actions">
      <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="loading" @click="loadAll">
        <el-icon><Refresh /></el-icon>
        <span>刷新</span>
      </button>
    </div>

    <section class="membership-section">
      <div class="section-title">
        <h3>当前会员</h3>
        <span>{{ currentMembership.active_wsh ? '权益生效中' : '未开通' }}</span>
      </div>
      <div class="current-membership">
        <div>
          <div class="current-title">{{ currentMembership.plan_name_wsh || currentMembership.plan_code_wsh || '普通用户' }}</div>
          <div class="current-meta">
            <span>状态：{{ membershipStatusText(currentMembership.status_wsh) }}</span>
            <span>等级：Lv{{ currentMembership.level_wsh || 0 }}</span>
            <span>折扣：{{ discountText(currentMembership.discount_rate_wsh) }}</span>
          </div>
        </div>
        <div class="current-period">
          <strong>{{ currentMembership.remaining_days_wsh || 0 }}</strong>
          <span>剩余天数</span>
          <small>{{ periodText(currentMembership, 'started_at_wsh', 'expires_at_wsh') }}</small>
        </div>
      </div>
    </section>

    <section class="membership-section">
      <div class="section-title">
        <h3>会员套餐</h3>
        <span>{{ plans.length }} 个可选套餐</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="plans.length === 0" class="empty-panel">暂无可购买会员套餐</div>
      <div v-else class="plan-grid">
        <article v-for="plan in plans" :key="plan.id_wsh" class="plan-card">
          <div class="plan-head">
            <div>
              <div class="plan-name">{{ plan.name_wsh }}</div>
              <div class="plan-code">{{ plan.code_wsh }}</div>
            </div>
            <span class="level-badge">Lv{{ plan.level_wsh || 1 }}</span>
          </div>
          <div class="plan-price">¥{{ money(plan.price_wsh) }}</div>
          <div class="plan-meta">
            <span>{{ plan.duration_days_wsh }} 天</span>
            <span>{{ discountText(plan.discount_rate_wsh) }}</span>
          </div>
          <button
            class="btn btn-primary btn-sm icon-btn"
            type="button"
            :disabled="submittingPlanId === plan.id_wsh"
            @click="createOrder(plan)"
          >
            <el-icon><ShoppingCart /></el-icon>
            <span>{{ submittingPlanId === plan.id_wsh ? '生成中...' : '生成订单' }}</span>
          </button>
        </article>
      </div>
    </section>

    <section class="membership-section">
      <div class="section-title">
        <h3>会员订单</h3>
        <span>{{ orders.length }} 条记录</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="orders.length === 0" class="empty-panel">暂无会员订单</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
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
              <td>{{ order.plan_name_wsh || order.plan_code_wsh || '-' }}</td>
              <td>¥{{ money(order.amount_wsh) }}</td>
              <td>{{ payMethodText(order.pay_method_wsh) }}</td>
              <td>
                <span :class="['order-status', statusClass(order.status_wsh)]">
                  {{ statusText(order.status_wsh) }}
                </span>
              </td>
              <td>{{ periodText(order, 'membership_start_at_wsh', 'membership_end_at_wsh') }}</td>
              <td>{{ dateText(order.created_at_wsh) }}</td>
              <td>
                <div v-if="order.status_wsh === 'pending'" class="row-actions">
                  <button
                    class="btn btn-primary btn-sm icon-btn"
                    type="button"
                    :disabled="payingOrderNo === order.order_no_wsh || cancellingOrderNo === order.order_no_wsh"
                    @click="payOrder(order)"
                  >
                    <el-icon><CreditCard /></el-icon>
                    <span>{{ payingOrderNo === order.order_no_wsh ? '支付中...' : '支付' }}</span>
                  </button>
                  <button
                    class="btn btn-danger btn-sm icon-btn"
                    type="button"
                    :disabled="payingOrderNo === order.order_no_wsh || cancellingOrderNo === order.order_no_wsh"
                    @click="cancelOrder(order)"
                  >
                    <el-icon><Close /></el-icon>
                    <span>{{ cancellingOrderNo === order.order_no_wsh ? '取消中...' : '取消' }}</span>
                  </button>
                </div>
                <span v-else class="muted">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="membership-section">
      <div class="section-title">
        <h3>权益流水</h3>
        <span>{{ usages.length }} 条记录</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="usages.length === 0" class="empty-panel">暂无权益使用记录</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>业务</th>
              <th>权益</th>
              <th>金额</th>
              <th>状态</th>
              <th>使用时间</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="usage in usages" :key="usage.id_wsh || usage.request_id_wsh">
              <td>{{ usage.business_type_wsh }} #{{ usage.business_id_wsh }}</td>
              <td>{{ usage.benefit_code_wsh || usage.benefit_type_wsh || '-' }}</td>
              <td>¥{{ money(usage.amount_wsh) }}</td>
              <td>
                <span :class="['order-status', usageStatusClass(usage.usage_status_wsh)]">
                  {{ usageStatusText(usage.usage_status_wsh) }}
                </span>
              </td>
              <td>{{ dateText(usage.used_at_wsh) }}</td>
              <td>{{ dateText(usage.created_at_wsh) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Close, CreditCard, Refresh, ShoppingCart } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import {
  cancelMembershipOrder,
  createMembershipOrder,
  getActiveMemberPlans,
  getMembershipUsages,
  getMyMembership,
  getMyMembershipOrders,
  payMembershipOrder,
} from '@/api/membership'

const appStore = useAppStore()
const loading = ref(false)
const plans = ref([])
const orders = ref([])
const usages = ref([])
const currentMembership = ref({})
const submittingPlanId = ref(null)
const cancellingOrderNo = ref('')
const payingOrderNo = ref('')

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [membershipRes, plansRes, ordersRes, usagesRes] = await Promise.all([
      getMyMembership(),
      getActiveMemberPlans(),
      getMyMembershipOrders(),
      getMembershipUsages(),
    ])
    if (membershipRes.code === 200) currentMembership.value = membershipRes.data || {}
    else appStore.addToast(membershipRes.message || '会员状态加载失败', 'error')
    if (plansRes.code === 200) plans.value = Array.isArray(plansRes.data) ? plansRes.data : []
    else appStore.addToast(plansRes.message || '会员套餐加载失败', 'error')
    if (ordersRes.code === 200) orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : []
    else appStore.addToast(ordersRes.message || '会员订单加载失败', 'error')
    if (usagesRes.code === 200) usages.value = Array.isArray(usagesRes.data) ? usagesRes.data : []
    else appStore.addToast(usagesRes.message || '权益流水加载失败', 'error')
  } catch (e) {
    appStore.addToast(e?.message || '会员数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadMembershipState() {
  const [membershipRes, ordersRes, usagesRes] = await Promise.all([
    getMyMembership(),
    getMyMembershipOrders(),
    getMembershipUsages(),
  ])
  if (membershipRes.code === 200) currentMembership.value = membershipRes.data || {}
  if (ordersRes.code === 200) orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : []
  if (usagesRes.code === 200) usages.value = Array.isArray(usagesRes.data) ? usagesRes.data : []
}

async function loadOrders() {
  const res = await getMyMembershipOrders()
  if (res.code === 200) orders.value = Array.isArray(res.data) ? res.data : []
}

async function createOrder(plan) {
  if (submittingPlanId.value) return
  submittingPlanId.value = plan.id_wsh
  try {
    const res = await createMembershipOrder({
      plan_id_wsh: plan.id_wsh,
      pay_method_wsh: 'balance',
      request_id_wsh: createRequestId(plan.id_wsh),
    })
    if (res.code === 200) {
      appStore.addToast('会员订单已创建', 'success')
      await loadOrders()
    } else {
      appStore.addToast(res.message || '会员订单创建失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单创建失败', 'error')
  } finally {
    submittingPlanId.value = null
  }
}

async function payOrder(order) {
  if (payingOrderNo.value) return
  payingOrderNo.value = order.order_no_wsh
  try {
    const res = await payMembershipOrder(order.order_no_wsh)
    if (res.code === 200) {
      appStore.addToast('会员已开通', 'success')
      await loadMembershipState()
    } else {
      appStore.addToast(res.message || '会员订单支付失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单支付失败', 'error')
  } finally {
    payingOrderNo.value = ''
  }
}

async function cancelOrder(order) {
  if (cancellingOrderNo.value) return
  if (!confirm(`确定取消会员订单 ${order.order_no_wsh} 吗？`)) return
  cancellingOrderNo.value = order.order_no_wsh
  try {
    const res = await cancelMembershipOrder(order.order_no_wsh)
    if (res.code === 200) {
      appStore.addToast('会员订单已取消', 'success')
      await loadOrders()
    } else {
      appStore.addToast(res.message || '会员订单取消失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单取消失败', 'error')
  } finally {
    cancellingOrderNo.value = ''
  }
}

function createRequestId(planId) {
  const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `member-${planId}-${uuid}`
}

function discountText(value) {
  const rate = Number(value || 1)
  return rate >= 1 ? '无折扣' : `${(rate * 10).toFixed(1)} 折`
}

function membershipStatusText(status) {
  return {
    active: '生效中',
    inactive: '未开通',
    expired: '已过期',
    cancelled: '已取消',
  }[status] || status || '未开通'
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

function usageStatusText(status) {
  return {
    locked: '已锁定',
    used: '已使用',
    released: '已释放',
  }[status] || status || '-'
}

function usageStatusClass(status) {
  return {
    locked: 'is-wait',
    used: 'is-on',
    released: 'is-off',
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

function periodText(entity, startKey, endKey) {
  const start = dateText(entity?.[startKey])
  const end = dateText(entity?.[endKey])
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
.membership-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.membership-section {
  margin-bottom: 28px;
}

.section-title {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title h3 {
  margin: 0;
}

.section-title span,
.plan-code,
.plan-meta,
.current-meta,
.current-period small,
.muted {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.current-membership {
  align-items: center;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  padding: 18px;
}

.current-membership > div {
  min-width: 0;
}

.current-title {
  font-size: 20px;
  font-weight: 900;
  margin-bottom: 8px;
  overflow-wrap: anywhere;
}

.current-meta,
.row-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.current-period {
  display: grid;
  gap: 4px;
  min-width: 128px;
  text-align: right;
}

.current-period strong {
  color: var(--color-primary);
  font-size: 28px;
}

.current-period span {
  font-size: 13px;
}

.plan-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.plan-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  display: grid;
  gap: 14px;
  min-height: 210px;
  padding: 16px;
}

.plan-head,
.plan-meta,
.icon-btn,
.order-status {
  align-items: center;
  display: flex;
}

.plan-head {
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
}

.plan-head > div {
  min-width: 0;
}

.plan-name {
  font-weight: 800;
  margin-bottom: 4px;
  overflow-wrap: anywhere;
}

.level-badge,
.order-status {
  border-radius: 999px;
  font-size: 12px;
  justify-content: center;
  min-width: 58px;
  padding: 4px 8px;
}

.level-badge {
  background: rgba(37, 99, 235, 0.12);
  color: var(--color-accent);
  flex: 0 0 auto;
}

.plan-price {
  color: var(--color-primary);
  font-size: 28px;
  font-weight: 900;
}

.plan-meta {
  gap: 10px;
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

.empty-panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-muted-foreground);
  padding: 24px;
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
  .membership-actions,
  .current-membership {
    align-items: stretch;
    flex-direction: column;
  }

  .membership-actions .btn,
  .row-actions .btn {
    width: 100%;
  }

  .current-period {
    text-align: left;
  }
}
</style>
