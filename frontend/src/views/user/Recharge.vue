<template>
  <div class="rc-page">
    <div class="rc-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="rc-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="rc-crumb-link">首页</router-link>
        <span class="rc-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/wallet" class="rc-crumb-link">我的钱包</router-link>
        <span class="rc-crumb-sep" aria-hidden="true">›</span>
        <span class="rc-crumb-here">余额充值</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="rc-head">
        <div class="rc-head-copy">
          <div class="rc-eyebrow" aria-hidden="true">
            <span class="rc-eyebrow-line"></span>
            <span>Recharge</span>
          </div>
          <h1 class="rc-title">余额充值</h1>
          <p class="rc-sub">充值即时到账，可用于寄养下单、上门服务与打赏照护师。余额不会过期。</p>
        </div>
        <div class="rc-actions">
          <router-link to="/wallet" class="cta cta-outline">返回钱包</router-link>
        </div>
      </header>

      <div class="rc-grid">
        <!-- ═══ 充值表单 ═══ -->
        <section class="rc-form-card" aria-label="充值金额">
          <h2 class="rc-card-title">充值金额</h2>
          <p class="rc-card-desc">选择一个常用金额，或直接输入需要充值的数额。</p>

          <div class="rc-quick" role="group" aria-label="常用金额">
            <button
              type="button"
              class="rc-chip"
              :class="{ active: amount === '100' }"
              @click="amount = '100'"
            >¥100</button>
            <button
              type="button"
              class="rc-chip"
              :class="{ active: amount === '300' }"
              @click="amount = '300'"
            >¥300</button>
            <button
              type="button"
              class="rc-chip"
              :class="{ active: amount === '500' }"
              @click="amount = '500'"
            >¥500</button>
            <button
              type="button"
              class="rc-chip"
              :class="{ active: amount === '1000' }"
              @click="amount = '1000'"
            >¥1000</button>
          </div>

          <form class="rc-form" @submit.prevent="submitRecharge">
            <div class="rc-field">
              <label class="rc-label" for="rc-amount">自定义金额</label>
              <div class="rc-input-wrap">
                <span class="rc-input-prefix" aria-hidden="true">¥</span>
                <input
                  id="rc-amount"
                  v-model="amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  class="rc-input"
                  placeholder="0.00"
                >
              </div>
              <p class="rc-field-hint">单笔最低 0.01 元，到账后可立即使用。</p>
            </div>

            <div class="rc-field">
              <span class="rc-label">支付方式</span>
              <div class="rc-methods" role="group" aria-label="支付方式">
                <button
                  type="button"
                  class="rc-method"
                  :class="{ active: method === 'wechat' }"
                  @click="method = 'wechat'"
                >微信支付</button>
                <button
                  type="button"
                  class="rc-method"
                  :class="{ active: method === 'alipay' }"
                  @click="method = 'alipay'"
                >支付宝</button>
                <button
                  type="button"
                  class="rc-method"
                  :class="{ active: method === 'bank' }"
                  @click="method = 'bank'"
                >银行卡</button>
              </div>
            </div>

            <div class="rc-foot">
              <p class="rc-note">充值金额不能为 0 或负数，充值成功后即时增加余额并生成充值记录。</p>
              <button class="cta cta-primary" type="submit" :disabled="submitting">
                {{ submitting ? '充值中…' : '提交充值' }}
              </button>
            </div>
          </form>
        </section>

        <!-- ═══ 当前余额 ═══ -->
        <aside class="rc-balance" aria-label="当前余额">
          <p class="rc-balance-label">Current Balance</p>
          <p class="rc-balance-amount">
            <span class="rc-balance-yen" aria-hidden="true">¥</span>
            <span>{{ money(wallet.balance_wsh) }}</span>
          </p>
          <p class="rc-balance-sub">冻结金额 ¥ {{ money(wallet.frozen_amount_wsh) }}</p>

          <dl class="rc-info">
            <div>
              <dt>到账时间</dt>
              <dd>充值成功后余额立即更新，无需等待。</dd>
            </div>
            <div>
              <dt>发票与对账</dt>
              <dd>每笔充值都会记入交易记录，可在钱包中随时核对。</dd>
            </div>
            <div>
              <dt>退款</dt>
              <dd>订单取消后的退款会退回账户余额，可继续用于下单。</dd>
            </div>
          </dl>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
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
.rc-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.rc-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.rc-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.rc-crumb-link { color: var(--ref-muted); text-decoration: none; }
.rc-crumb-link:hover { color: var(--ref-ink); }
.rc-crumb-sep { color: var(--ref-line); }
.rc-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.rc-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rc-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.rc-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.rc-head-copy { min-width: 0; }
.rc-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.rc-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.rc-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: 11px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Grid ═══ */
.rc-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);
  gap: 24px;
  margin-top: 8px;
  align-items: start;
}

/* ═══ Form card ═══ */
.rc-form-card {
  border: 1px solid var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
  padding: 28px 30px;
}
.rc-card-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 400;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.rc-card-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-ink-soft); }

.rc-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}
.rc-chip {
  height: 42px;
  padding: 0 18px;
  border-radius: 11px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.rc-chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: var(--ref-sand); }
.rc-chip.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }

.rc-form { margin-top: 26px; display: grid; gap: 20px; }
.rc-field { display: grid; gap: 8px; }
.rc-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.rc-input-wrap { position: relative; }
.rc-input-prefix {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ref-muted);
  font-size: 15px;
  font-family: var(--ref-font-display);
}
.rc-input {
  width: 100%;
  height: 46px;
  padding: 0 40px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.rc-input::placeholder { color: var(--ref-muted); }
.rc-input:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); }
.rc-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.rc-field-hint { margin: 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }

.rc-methods { display: flex; flex-wrap: wrap; gap: 10px; }
.rc-method {
  height: 42px;
  padding: 0 18px;
  border-radius: 11px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.rc-method:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: var(--ref-sand); }
.rc-method.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }

.rc-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
}
.rc-note {
  margin: 0;
  max-width: 300px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}

/* ═══ Balance aside ═══ */
.rc-balance {
  border: 1px solid var(--ref-line);
  border-radius: 20px;
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
  padding: 28px 30px;
}
.rc-balance-label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rc-balance-amount {
  margin: 16px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 3.6vw, 44px);
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--ref-brand);
}
.rc-balance-yen {
  font-size: 15px;
  margin-right: 4px;
  color: color-mix(in srgb, var(--ref-brand) 70%, transparent);
}
.rc-balance-sub {
  margin: 10px 0 0;
  font-size: 12.5px;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}
.rc-info {
  margin: 26px 0 0;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
  display: grid;
  gap: 16px;
}
.rc-info div { display: grid; gap: 4px; }
.rc-info dt { font-size: 13px; font-weight: 500; color: var(--ref-ink); }
.rc-info dd { margin: 0; font-size: 12px; line-height: 1.7; color: var(--ref-muted); }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .rc-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .rc-shell { padding: 0 16px; }
  .rc-head { padding: 30px 0 22px; }
  .rc-sub { font-size: 13.5px; }
  .rc-form-card, .rc-balance { padding: 22px 20px; }
  .rc-foot { align-items: stretch; }
  .rc-foot .cta { width: 100%; }
  .rc-note { max-width: none; }
}
</style>
