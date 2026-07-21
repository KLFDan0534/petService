<template>
  <div>
    <PageHero title="充值" subtitle="这是预留的充值入口，当前不会产生任何资金操作" />

    <section class="recharge-shell">
      <div class="balance-panel">
        <span>当前余额</span>
        <strong>¥{{ money(wallet.balance_wsh) }}</strong>
        <small>冻结金额 ¥{{ money(wallet.frozen_amount_wsh) }}</small>
      </div>

      <form class="recharge-form" @submit.prevent="submitMockRecharge">
        <div class="form-group">
          <label>充值金额</label>
          <input v-model="amount" type="number" min="0.01" step="0.01" class="form-control" placeholder="输入金额">
        </div>
        <div class="form-group">
          <label>支付方式</label>
          <select v-model="method" class="form-control">
            <option value="wechat">微信支付</option>
            <option value="alipay">支付宝</option>
            <option value="bank">银行卡</option>
          </select>
        </div>
        <button class="btn btn-primary" type="submit">提交充值</button>
      </form>

      <p class="mock-note">
        当前页面只做展示和流程占位，不会调用后端，也不会改变余额。正式充值以后可以在这里接入支付网关或管理员审核流程。
      </p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import { getMyWallet } from '@/api/wallet'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const wallet = ref({})
const amount = ref('')
const method = ref('wechat')

onMounted(async () => {
  try {
    const res = await getMyWallet()
    if (res.code === 200) wallet.value = res.data || {}
  } catch (e) {}
})

function submitMockRecharge() {
  if (!amount.value || Number(amount.value) <= 0) {
    appStore.addToast('请输入充值金额', 'warning')
    return
  }
  appStore.addToast('充值入口已预留，当前不会实际入账', 'info')
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.recharge-shell {
  display: grid;
  gap: 18px;
  max-width: 720px;
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
.mock-note {
  color: var(--color-muted-foreground);
}
.balance-panel strong {
  font-size: 32px;
  color: var(--color-primary);
}
.recharge-form {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: 8px;
  padding: 20px;
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(160px, 220px) auto;
  gap: 12px;
  align-items: end;
}
.mock-note {
  margin: 0;
  line-height: 1.6;
}
@media (max-width: 720px) {
  .recharge-form {
    grid-template-columns: 1fr;
  }
}
</style>
