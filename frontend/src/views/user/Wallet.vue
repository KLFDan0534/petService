<template>
  <div class="wl-page">
    <div class="wl-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="wl-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="wl-crumb-link">首页</router-link>
        <span class="wl-crumb-sep" aria-hidden="true">›</span>
        <span class="wl-crumb-here">我的钱包</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="wl-head">
        <div class="wl-head-copy">
          <div class="wl-eyebrow" aria-hidden="true">
            <span class="wl-eyebrow-line"></span>
            <span>钱包</span>
          </div>
          <h1 class="wl-title">我的钱包</h1>
          <p class="wl-sub">余额用于寄养下单、上门服务与打赏照护师。每一笔进出都留有记录，可随时核对。</p>
        </div>
        <div class="wl-actions">
          <button type="button" class="cta cta-outline" @click="loadWallet(); loadTxns()">刷新</button>
          <router-link to="/recharge" class="cta cta-primary">去充值</router-link>
        </div>
      </header>

      <!-- ═══ Balance panel ═══ -->
      <section class="wl-balance" aria-label="账户余额">
        <div class="wl-b-main">
          <div class="wl-b-copy">
            <p class="wl-b-label">可用余额 <span class="wl-b-en">当前可用</span></p>
            <p class="wl-b-amount">
              <span class="wl-b-yen">¥</span>
              <span class="wl-b-num">{{ money(available) }}</span>
            </p>
            <p class="wl-b-desc">可用余额可直接用于下单支付与打赏；冻结金额是进行中订单预留的款项，订单完成或取消后自动释放。</p>
          </div>
          <div class="wl-b-actions">
            <router-link to="/recharge" class="cta cta-primary">充值</router-link>
            <a href="/profile#wallet" class="cta cta-outline">提现与打赏记录</a>
          </div>
        </div>
        <dl class="wl-b-sub">
          <div class="wl-b-item">
            <dt>冻结金额</dt>
            <dd>¥ {{ money(wallet.frozen_amount_wsh) }}</dd>
          </div>
          <div class="wl-b-item">
            <dt>账户总额</dt>
            <dd>¥ {{ money(wallet.balance_wsh) }}</dd>
          </div>
        </dl>
      </section>

      <!-- ═══ 01 · 交易记录 ═══ -->
      <section class="wl-section" aria-label="交易记录">
        <header class="wl-sec-head">
          <div class="wl-head-copy">
            <p class="wl-eyebrow">
              <span class="wl-idx">01</span>
              <span class="wl-line" aria-hidden="true"></span>
              <span>交易记录</span>
            </p>
            <h2 class="wl-sec-title">交易记录</h2>
            <p class="wl-sec-desc">充值、订单支付、退款与提现都会记在这里。</p>
          </div>
          <div class="wl-sec-action">
            <router-link to="/payments" class="text-link">
              <span class="tl-text">查看支付记录</span>
              <span class="tl-arrow" aria-hidden="true">→</span>
            </router-link>
          </div>
        </header>

        <div v-if="!txns.length" class="wl-empty">
          <p class="wl-empty-title">暂无交易记录</p>
          <p class="wl-empty-desc">完成第一次充值或下单后，账户变动会按时间顺序出现在这里。</p>
          <div class="wl-empty-actions">
            <router-link to="/recharge" class="cta cta-primary">去充值</router-link>
            <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
          </div>
        </div>

        <div v-else class="wl-ledger">
          <ul class="wl-list">
            <li v-for="t in txns" :key="t.id_wsh" class="wl-row">
              <div class="lr-main">
                <p class="lr-title">{{ typeLabel(t) }}</p>
                <p class="lr-meta">
                  <span class="lr-trunc">{{ t.description_wsh || '—' }}</span>
                  <span class="lr-sep" aria-hidden="true">·</span>
                  <span>{{ fmtTime(t.created_at_wsh) }}</span>
                </p>
              </div>
              <div class="lr-side">
                <p :class="['lr-amount', t.direction_wsh === 'in' ? 'lr-in' : 'lr-out']">
                  {{ t.direction_wsh === 'in' ? '+' : '−' }}¥ {{ money(t.amount_wsh) }}
                </p>
                <p v-if="t.balance_after_wsh !== undefined" class="lr-balance">余额 ¥ {{ money(t.balance_after_wsh) }}</p>
              </div>
            </li>
          </ul>
        </div>

        <p class="wl-frozen-note">
          冻结资金：指暂不可用的金额，例如提现申请在审核与打款完成前会冻结对应余额，打款完成后从冻结金额中扣减。
        </p>
      </section>
    </div>
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
  membership: '会员',
  membership_payment: '会员支付',
  payment: '订单支付',
  coupon_subsidy: '优惠券补贴',
  reject_subsidy: '补贴退回',
  tip: '打赏'
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
.wl-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.wl-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.wl-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.wl-crumb-link { color: var(--ref-muted); text-decoration: none; }
.wl-crumb-link:hover { color: var(--ref-ink); }
.wl-crumb-sep { color: var(--ref-line); }
.wl-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.wl-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.wl-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.wl-idx { font-variant-numeric: tabular-nums; }
.wl-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.wl-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.wl-head-copy { min-width: 0; }
.wl-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.wl-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.wl-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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

/* ═══ Text link ═══ */
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
  text-decoration: none;
  transition: color 0.15s;
}
.text-link:hover { color: var(--ref-brand); }
.tl-text {
  border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent);
  padding-bottom: 2px;
}
.text-link:hover .tl-text { border-color: var(--ref-brand); }
.tl-arrow { transition: transform 0.15s; }
.text-link:hover .tl-arrow { transform: translateX(4px); }

/* ═══ Balance panel ═══ */
.wl-balance {
  margin-top: 8px;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: 22px;
  background: var(--ref-surface);
}
.wl-b-main {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px 32px;
  padding: 30px 32px;
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
}
.wl-b-copy { min-width: 0; }
.wl-b-label {
  margin: 0;
  font-size: 10.5px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.wl-b-en { margin-left: 8px; letter-spacing: 0.16em; opacity: 0.7; }
.wl-b-amount {
  margin: 14px 0 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-family: var(--ref-font-display);
  font-size: clamp(38px, 5vw, 54px);
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.wl-b-yen { font-size: 16px; color: color-mix(in srgb, var(--ref-brand) 75%, transparent); }
.wl-b-num { color: var(--ref-brand); }
.wl-b-desc {
  margin: 16px 0 0;
  max-width: 460px;
  font-size: 13px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 80%, transparent);
}
.wl-b-actions { display: flex; flex-wrap: wrap; gap: 10px; padding-bottom: 2px; }
.wl-b-sub {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin: 0;
  border-top: 1px solid var(--ref-line);
}
.wl-b-item { padding: 18px 32px; }
.wl-b-item + .wl-b-item { border-left: 1px solid var(--ref-line); }
.wl-b-item dt {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.wl-b-item dd {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 400;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: var(--ref-ink);
}

/* ═══ Section ═══ */
.wl-section { margin-top: 56px; }
.wl-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.wl-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.wl-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}
.wl-sec-action { padding-bottom: 4px; flex-shrink: 0; }

/* ═══ Ledger ═══ */
.wl-ledger {
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
}
.wl-list { margin: 0; padding: 0; list-style: none; }
.wl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 20px;
  border-bottom: 1px solid var(--ref-line);
  transition: background 0.15s;
}
.wl-row:last-child { border-bottom: 0; }
.wl-row:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.lr-main { min-width: 0; }
.lr-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.lr-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 5px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
}
.lr-trunc { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; }
.lr-sep { color: var(--ref-line); }
.lr-side { flex-shrink: 0; text-align: right; }
.lr-amount { margin: 0; font-size: 14px; font-weight: 500; font-variant-numeric: tabular-nums; }
.lr-in { color: var(--ref-brand); }
.lr-out { color: var(--ref-ink); }
.lr-balance { margin: 4px 0 0; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }

/* ═══ Empty ═══ */
.wl-empty {
  margin-top: 20px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  text-align: center;
}
.wl-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.wl-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.wl-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

/* ═══ Frozen note ═══ */
.wl-frozen-note {
  margin: 16px 0 0;
  padding: 14px 18px;
  border: 1px solid var(--ref-line);
  border-radius: 12px;
  background: color-mix(in srgb, var(--ref-sand) 45%, var(--ref-surface));
  font-size: 12px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .wl-b-main { flex-direction: column; align-items: stretch; }
  .wl-b-actions { padding-bottom: 0; }
}
@media (max-width: 560px) {
  .wl-shell { padding: 0 16px; }
  .wl-head { padding: 30px 0 22px; }
  .wl-sub { font-size: 13.5px; }
  .wl-b-main { padding: 24px 20px; }
  .wl-b-sub { grid-template-columns: 1fr; }
  .wl-b-item { padding: 16px 20px; }
  .wl-b-item + .wl-b-item { border-left: 0; border-top: 1px solid var(--ref-line); }
  .wl-section { margin-top: 44px; }
  .wl-row { padding: 13px 16px; }
}
</style>
