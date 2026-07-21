<template>
  <div>
    <PageHero title="收益中心" subtitle="查看收入、打赏记录和收益统计" />
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-value">¥{{ stats.totalRevenue || 0 }}</div><div class="stat-label">总收入</div></div>
      <div class="stat-card"><div class="stat-value">¥{{ stats.monthRevenue || 0 }}</div><div class="stat-label">本月收入</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalTips || 0 }}</div><div class="stat-label">打赏次数</div></div>
      <div class="stat-card"><div class="stat-value">¥{{ stats.tipsAmount || 0 }}</div><div class="stat-label">打赏总金额</div></div>
    </div>

    <h2 style="margin:24px 0 12px">钱包余额</h2>
    <div class="card" style="padding:24px">
      <div style="font-size:32px;font-weight:700;color:var(--color-primary)">¥{{ wallet.balance_wsh || 0 }}</div>
      <div style="font-size:14px;color:var(--color-muted-foreground);margin-top:4px">可用余额</div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:12px">
        <router-link to="/recharge" class="btn btn-outline btn-sm">充值</router-link>
        <button class="btn btn-primary btn-sm" @click="showWithdrawForm = true">申请提现</button>
      </div>
    </div>

    <div v-if="showWithdrawForm" class="modal-overlay" @mousedown.self="showWithdrawForm = false">
      <div class="modal">
        <h2>申请提现</h2>
        <form @submit.prevent="submitWithdraw">
          <div class="form-group">
            <label>提现金额 (¥)</label>
            <input v-model="withdrawAmount" type="number" step="0.01" required placeholder="输入金额" max="wallet.balance_wsh">
          </div>
          <div class="form-group">
            <label>收款账户</label>
            <input v-model="withdrawAccount" required placeholder="支付宝/银行卡号">
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showWithdrawForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="!withdrawAmount || !withdrawAccount">提交</button>
          </div>
        </form>
      </div>
    </div>

    <h2 style="margin:24px 0 12px">打赏记录</h2>
    <div v-if="tipsLoading" class="loading">加载中...</div>
    <div v-else-if="tips.length === 0" class="empty-state"><h3>暂无打赏记录</h3></div>
    <div v-else class="card" style="margin-bottom:12px" v-for="t in tips" :key="t.id_wsh">
      <div style="display:flex;justify-content:space-between">
        <span>打赏 <strong>¥{{ t.amount_wsh }}</strong></span>
        <span style="font-size:12px;color:var(--color-muted-foreground)">{{ new Date(t.created_at_wsh).toLocaleString() }}</span>
      </div>
      <div v-if="t.message_wsh" style="margin-top:4px;font-size:14px">{{ t.message_wsh }}</div>
    </div>

    <h2 style="margin:24px 0 12px">交易记录</h2>
    <div v-if="txLoading" class="loading">加载中...</div>
    <div v-else-if="transactions.length === 0" class="empty-state"><h3>暂无交易记录</h3></div>
    <div v-else>
      <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'金额',key:'amount_wsh'},{label:'类型',key:'type_wsh'},{label:'状态',key:'tx_status_label_wsh'},{label:'描述',key:'description_wsh'},{label:'时间',key:'created_at_wsh'}]" :data="transactions" />
    </div>

    <h2 style="margin:24px 0 12px">提现记录</h2>
    <div v-if="wdLoading" class="loading">加载中...</div>
    <div v-else-if="withdrawals.length === 0" class="empty-state"><h3>暂无提现记录</h3></div>
    <div v-else>
      <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'金额',key:'amount_wsh'},{label:'状态',key:'wd_status_label_wsh'},{label:'账户',key:'account_wsh'},{label:'时间',key:'created_at_wsh'}]" :data="withdrawals" />
    </div>

    <h2 style="margin:24px 0 12px">收益统计</h2>
    <div v-if="statsLoading" class="loading">加载中...</div>
    <div v-else class="stat-grid" style="grid-template-columns:repeat(3,1fr)">
      <div class="stat-card"><div class="stat-value">{{ stats.totalOrders || 0 }}</div><div class="stat-label">总订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.completedOrders || 0 }}</div><div class="stat-label">已完成</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.pendingOrders || 0 }}</div><div class="stat-label">待处理</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyWallet, getMyTips, getMyTransactions, getMyWithdrawals, createWithdrawal } from '@/api/wallet'
import { getUserStatistics } from '@/api/statistics'
import { TransactionStatus, WithdrawalStatus, enrichWithStatus } from '@/constants/statusMaps'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import DataTable from '@/components/common/DataTable.vue'

const appStore = useAppStore()
const tips = ref([])
const wallet = ref({})
const stats = ref({})
const transactions = ref([])
const withdrawals = ref([])

function enrichTx(t) { return enrichWithStatus(t, 'status_wsh', TransactionStatus, 'tx_status_label_wsh') }
function enrichWd(w) { return enrichWithStatus(w, 'status_wsh', WithdrawalStatus, 'wd_status_label_wsh') }
const tipsLoading = ref(true)
const statsLoading = ref(true)
const txLoading = ref(true)
const wdLoading = ref(true)
const showWithdrawForm = ref(false)
const withdrawAmount = ref('')
const withdrawAccount = ref('')

onMounted(async () => {
  try {
    const r = await getMyWallet()
    if (r.code === 200) wallet.value = r.data
  } catch (e) {}
  try {
    const r = await getMyTips()
    if (r.code === 200) tips.value = r.data
  } catch (e) {}
  finally { tipsLoading.value = false }
  try {
    const r = await getMyTransactions()
    if (r.code === 200) transactions.value = (Array.isArray(r.data) ? r.data : []).map(enrichTx)
  } catch (e) {}
  finally { txLoading.value = false }
  try {
    const r = await getMyWithdrawals()
    if (r.code === 200) withdrawals.value = (Array.isArray(r.data) ? r.data : []).map(enrichWd)
  } catch (e) {}
  finally { wdLoading.value = false }
  try {
    const r = await getUserStatistics()
    if (r.code === 200) stats.value = r.data
  } catch (e) {}
  finally { statsLoading.value = false }
})

async function submitWithdraw() {
  if (!withdrawAmount.value || !withdrawAccount.value) return
  try {
    const r = await createWithdrawal({ amount_wsh: withdrawAmount.value, account_name_wsh: withdrawAccount.value, bank_name_wsh: '', bank_card_wsh: '' })
    if (r.code === 200) {
      appStore.addToast('提现申请已提交', 'success')
      showWithdrawForm.value = false
      withdrawAmount.value = ''
      withdrawAccount.value = ''
      withdrawals.value.unshift(r.data)
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}
</script>
