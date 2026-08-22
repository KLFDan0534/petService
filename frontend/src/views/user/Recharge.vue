<template>
  <div>
    <PageHero title="充值" subtitle="充值成功后余额即时到账，可在钱包与交易记录中查看" />

    <section class="recharge-shell">
      <div class="balance-panel">
        <span>当前余额</span>
        <strong>¥{{ money(wallet.balance_wsh) }}</strong>
        <small>冻结金额 ¥{{ money(wallet.frozen_amount_wsh) }}</small>
      </div>

      <form class="recharge-form" @submit.prevent="submitRecharge">
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
        <button class="btn btn-primary" type="submit" :disabled="submitting">{{ submitting ? '充值中...' : '提交充值' }}</button>
      </form>

      <p class="mock-note">
        充值金额不能为 0 或负数，充值成功后即时增加余额并生成充值记录。
      </p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageHero from '@/components/common/PageHero.vue'
import { getMyWallet, rechargeWallet } from '@/api/wallet'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const wallet = ref({})
const amount = ref('')
const method = ref('wechat')
const submitting = ref(false)

onMounted(async () => {
  await loadWallet()
})

async function loadWallet() {
  try {
    const res = await getMyWallet()
    if (res.code === 200) wallet.value = res.data || {}
  } catch (e) {}
}

async function submitRecharge() {
  if (!amount.value || Number(amount.value) <= 0) {
    appStore.addToast('请输入大于 0 的充值金额', 'warning')
    return
  }
  const reqId = `recharge-fe-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  submitting.value = true
  try {
    // 支付方式目前仅为展示，充值统一直接入账钱包余额
    const res = await rechargeWallet({ amount_wsh: Number(Number(amount.value).toFixed(2)), request_id_wsh: reqId })
    if (res.code === 200) {
      if (res.data) wallet.value = res.data
      amount.value = ''
      appStore.addToast('充值成功，余额已到账', 'success')
    } else {
      appStore.addToast(res.msg || '充值失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '充值失败', 'error')
  } finally {
    submitting.value = false
  }
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
