<template>
  <div>
    <section class="wallet-grid">
      <div class="balance-panel">
        <span>可用余额</span>
        <strong>¥{{ money(available) }}</strong>
        <small>账户总额 ¥{{ money(wallet.balance_wsh) }} · 冻结资金 ¥{{ money(wallet.frozen_amount_wsh) }}</small>
      </div>
      <div class="action-panel">
        <router-link to="/recharge" class="btn btn-primary">去充值</router-link>
      </div>
    </section>

    <section class="txn-card">
      <div class="txn-head">
        <h3>交易明细</h3>
        <button class="btn btn-outline btn-sm" type="button" @click="loadTxns">刷新</button>
      </div>
      <div v-if="txns.length === 0" class="empty">暂无交易记录</div>
      <table v-else class="txn-table">
        <thead>
          <tr>
            <th>类型</th>
            <th>说明</th>
            <th>变动金额</th>
            <th>余额</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in txns" :key="t.id_wsh">
            <td><span class="tag" :class="t.direction_wsh === 'in' ? 'tag-in' : 'tag-out'">{{ typeLabel(t) }}</span></td>
            <td>{{ t.description_wsh || '-' }}</td>
            <td :class="t.direction_wsh === 'in' ? 'amount-in' : 'amount-out'">
              {{ t.direction_wsh === 'in' ? '+' : '-' }}¥{{ money(t.amount_wsh) }}
            </td>
            <td>¥{{ money(t.balance_after_wsh) }}</td>
            <td class="muted">{{ fmtTime(t.created_at_wsh) }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <p class="frozen-note">
      冻结资金：指暂不可用的金额，例如提现申请在审核与打款完成前会冻结对应余额，打款完成后从冻结金额中扣减。
    </p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyWallet, getMyTransactions } from '@/api/wallet'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const wallet = ref({})
const txns = ref([])

const available = computed(() => Number(wallet.value.balance_wsh || 0) - Number(wallet.value.frozen_amount_wsh || 0))

const TYPE_MAP = {
  recharge: '充值',
  admin_adjust: '管理员调整',
  order_pay: '订单支付',
  order_refund: '退款',
  withdrawal: '提现',
  membership: '会员'
}

function typeLabel(t) {
  return TYPE_MAP[t.type_wsh] || t.business_type_wsh || t.type_wsh
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function fmtTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

async function loadWallet() {
  try {
    const res = await getMyWallet()
    if (res.code === 200) wallet.value = res.data || {}
  } catch (e) {
    appStore.addToast(e?.message || '钱包加载失败', 'error')
  }
}

async function loadTxns() {
  try {
    const res = await getMyTransactions()
    if (res.code === 200) txns.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    appStore.addToast(e?.message || '交易记录加载失败', 'error')
  }
}

onMounted(() => {
  loadWallet()
  loadTxns()
})
</script>

<style scoped>
.wallet-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 16px;
  align-items: stretch;
  margin-bottom: 16px;
}
.balance-panel {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: 8px;
  padding: 20px;
  display: grid;
  gap: 6px;
}
.balance-panel span,
.balance-panel small,
.muted,
.empty {
  color: var(--color-muted-foreground);
}
.balance-panel strong {
  font-size: 36px;
  color: var(--color-primary);
}
.action-panel {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  min-width: 120px;
}
.txn-card {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: 8px;
  padding: 16px;
}
.txn-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.txn-head h3 {
  margin: 0;
  font-size: 18px;
}
.txn-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.txn-table th,
.txn-table td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid var(--color-border);
}
.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.tag-in {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}
.tag-out {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}
.amount-in {
  color: #16a34a;
  font-weight: 600;
}
.amount-out {
  color: #dc2626;
  font-weight: 600;
}
.empty {
  padding: 30px;
  text-align: center;
}
.frozen-note {
  margin-top: 16px;
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.6;
}
@media (max-width: 720px) {
  .wallet-grid {
    grid-template-columns: 1fr;
  }
}
</style>