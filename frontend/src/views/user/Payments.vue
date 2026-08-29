<template>
  <div class="pm-page">
    <div class="pm-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="pm-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="pm-crumb-link">首页</router-link>
        <span class="pm-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/wallet" class="pm-crumb-link">我的钱包</router-link>
        <span class="pm-crumb-sep" aria-hidden="true">›</span>
        <span class="pm-crumb-here">支付中心</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="pm-head">
        <div class="pm-head-copy">
          <div class="pm-eyebrow" aria-hidden="true">
            <span class="pm-eyebrow-line"></span>
            <span>Payments</span>
          </div>
          <h1 class="pm-title">支付中心</h1>
          <p class="pm-sub">为已下单但未支付的订单创建支付、查询某个订单的支付状态，或用余额直接完成付款。</p>
        </div>
        <div class="pm-actions">
          <router-link to="/orders" class="cta cta-outline">我的订单</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 创建与查询 ═══ -->
      <section class="pm-section" aria-label="创建与查询">
        <header class="pm-sec-head">
          <div class="pm-head-copy">
            <p class="pm-eyebrow">
              <span class="pm-idx">01</span>
              <span class="pm-line" aria-hidden="true"></span>
              <span>New Payment</span>
            </p>
            <h2 class="pm-sec-title">创建与查询</h2>
            <p class="pm-sec-desc">订单号可在订单详情中复制。</p>
          </div>
        </header>

        <div class="pm-duo">
          <!-- 创建支付 -->
          <div class="pm-card">
            <h3 class="pm-card-title">为订单创建支付</h3>
            <div class="pm-field">
              <label class="pm-label" for="pm-order">订单号</label>
              <input id="pm-order" v-model="orderNo" class="pm-input" placeholder="如 QY202608270045">
            </div>
            <div class="pm-field">
              <span class="pm-label">支付方式</span>
              <div class="pm-chips" role="group" aria-label="支付方式">
                <span class="pm-chip active">余额支付</span>
              </div>
            </div>
            <div class="pm-card-foot">
              <button class="cta cta-primary" type="button" @click="createPayment">创建支付</button>
            </div>
          </div>

          <!-- 查询支付 -->
          <div class="pm-card pm-card-cream">
            <h3 class="pm-card-title">查询订单支付状态</h3>
            <div class="pm-field">
              <label class="pm-label" for="pm-lookup">订单号</label>
              <input id="pm-lookup" v-model="lookupOrderNo" class="pm-input" placeholder="输入订单号后查询">
              <p class="pm-field-hint">查询结果会显示该订单最新的一条支付记录。</p>
            </div>
            <div class="pm-card-foot">
              <button v-if="orderPayment" type="button" class="pm-quiet" @click="orderPayment = null">清除结果</button>
              <button class="cta cta-outline" type="button" @click="lookupPayment">查询</button>
            </div>

            <div v-if="orderPayment" class="pm-result">
              <div class="pm-row pm-row-highlight">
                <div class="pm-row-main">
                  <p class="pm-row-title">
                    订单 <span class="tabular">{{ orderPayment.order_no_wsh || '—' }}</span>
                  </p>
                  <p class="pm-row-meta">
                    <span>{{ payMethodMap[orderPayment.method_wsh] || orderPayment.method_wsh }}</span>
                    <span v-if="orderPayment.pay_no_wsh">
                      <span class="pm-sep" aria-hidden="true">·</span>
                      <span class="tabular">{{ orderPayment.pay_no_wsh }}</span>
                    </span>
                    <span v-if="orderPayment.created_at_wsh">
                      <span class="pm-sep" aria-hidden="true">·</span>
                      <span class="tabular">{{ formatTime(orderPayment.created_at_wsh) }}</span>
                    </span>
                  </p>
                </div>
                <p class="pm-row-amount">¥ {{ money(orderPayment.amount_wsh) }}</p>
                <span :class="['badge', paymentStatusBadge(orderPayment.status_wsh)]">
                  {{ paymentStatusText(orderPayment.status_wsh) }}
                </span>
                <button v-if="canExecutePayment(orderPayment)" class="cta cta-primary" type="button" @click="payPayment(orderPayment)">去支付</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ═══ 02 · 支付记录 ═══ -->
      <section class="pm-section" aria-label="支付记录">
        <header class="pm-sec-head">
          <div class="pm-head-copy">
            <p class="pm-eyebrow">
              <span class="pm-idx">02</span>
              <span class="pm-line" aria-hidden="true"></span>
              <span>History</span>
            </p>
            <h2 class="pm-sec-title">支付记录</h2>
            <p class="pm-sec-desc">待支付的余额订单可以直接在这里完成付款。</p>
          </div>
        </header>

        <div v-if="loading" class="pm-skeleton" aria-label="加载中"></div>

        <div v-else-if="!payments.length" class="pm-empty">
          <p class="pm-empty-title">暂无支付记录</p>
          <p class="pm-empty-desc">下单并支付后，每笔支付都会留在这里，方便你核对金额与方式。</p>
          <div class="pm-empty-actions">
            <router-link to="/services" class="cta cta-primary">去下单</router-link>
            <router-link to="/orders" class="cta cta-outline">我的订单</router-link>
          </div>
        </div>

        <div v-else class="pm-list">
          <div v-for="p in payments" :key="p.id_wsh" class="pm-row">
            <div class="pm-row-main">
              <p class="pm-row-title">
                <template v-if="p.order_no_wsh">订单 <span class="tabular">{{ p.order_no_wsh }}</span></template>
                <template v-else>{{ payMethodMap[p.method_wsh] || p.method_wsh }}</template>
              </p>
              <p class="pm-row-meta">
                <span>{{ payMethodMap[p.method_wsh] || p.method_wsh }}</span>
                <span v-if="p.pay_no_wsh">
                  <span class="pm-sep" aria-hidden="true">·</span>
                  <span class="tabular">{{ p.pay_no_wsh }}</span>
                </span>
                <span v-if="p.created_at_wsh">
                  <span class="pm-sep" aria-hidden="true">·</span>
                  <span class="tabular">{{ formatTime(p.created_at_wsh) }}</span>
                </span>
              </p>
            </div>
            <p class="pm-row-amount">¥ {{ money(p.amount_wsh) }}</p>
            <span :class="['badge', paymentStatusBadge(p.status_wsh)]">
              {{ paymentStatusText(p.status_wsh) }}
            </span>
            <button v-if="canExecutePayment(p)" class="cta cta-primary" type="button" @click="payPayment(p)">去支付</button>
            <span v-else-if="p.status_wsh === 'pending'" class="pm-hint">{{ pendingPaymentHint(p) }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPayments, createPayment as apiCreatePayment, executePayment, getPaymentByOrder } from '@/api/payment'
import { useAppStore } from '@/stores/app'
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
.pm-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.pm-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.pm-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.pm-crumb-link { color: var(--ref-muted); text-decoration: none; }
.pm-crumb-link:hover { color: var(--ref-ink); }
.pm-crumb-sep { color: var(--ref-line); }
.pm-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.pm-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.pm-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.pm-idx { font-variant-numeric: tabular-nums; }
.pm-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.pm-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.pm-head-copy { min-width: 0; }
.pm-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.pm-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.pm-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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

/* ═══ Badges (tone = script 的 paymentStatusBadge 输出类名) ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-success { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); }
.badge-warning { background: var(--ref-brand); color: #fff; }
.badge-danger { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-secondary { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent); }

/* ═══ Section ═══ */
.pm-section { margin-top: 56px; }
.pm-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.pm-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.pm-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Create / Lookup cards ═══ */
.pm-duo {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 20px;
  align-items: stretch;
}
.pm-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px 26px;
}
.pm-card-cream { background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface)); }
.pm-card-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.pm-field { display: grid; gap: 8px; margin-top: 18px; }
.pm-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.pm-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.pm-input::placeholder { color: var(--ref-muted); }
.pm-input:hover { border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); }
.pm-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.pm-field-hint { margin: 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }
.pm-chips { display: flex; flex-wrap: wrap; gap: 10px; }
.pm-chip {
  display: inline-flex;
  align-items: center;
  height: 42px;
  padding: 0 18px;
  border-radius: 11px;
  border: 1px solid var(--ref-ink);
  background: var(--ref-ink);
  color: var(--ref-cream);
  font-size: 13.5px;
  font-weight: 500;
}
.pm-card-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--ref-line);
}
.pm-quiet {
  background: none;
  border: none;
  color: var(--ref-muted);
  font-size: 12.5px;
  cursor: pointer;
  transition: color 0.15s;
}
.pm-quiet:hover { color: var(--ref-ink); }

/* ═══ Lookup result ═══ */
.pm-result {
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--ref-ink) 18%, transparent);
  border-radius: 14px;
  background: var(--ref-surface);
}

/* ═══ Payment rows ═══ */
.pm-list {
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
}
.pm-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--ref-line);
}
.pm-row:last-child { border-bottom: 0; }
.pm-list .pm-row { transition: background 0.15s; }
.pm-list .pm-row:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.pm-row-highlight { background: color-mix(in srgb, var(--ref-sand) 50%, var(--ref-surface)); }
.pm-row-main { min-width: 0; flex: 1; }
.pm-row-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pm-row-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 5px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
  min-width: 0;
  overflow-wrap: anywhere;
}
.pm-sep { color: var(--ref-line); }
.pm-row-amount {
  margin: 0;
  flex-shrink: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
  color: var(--ref-ink);
}
.pm-hint { flex-shrink: 0; font-size: 11.5px; color: var(--ref-muted); }
.tabular { font-variant-numeric: tabular-nums; }

/* ═══ Skeleton / Empty ═══ */
.pm-skeleton {
  height: 120px;
  margin-top: 20px;
  border-radius: 16px;
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: pm-shimmer 1.3s linear infinite;
}
.pm-empty {
  margin-top: 20px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  text-align: center;
}
.pm-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.pm-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.pm-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

@keyframes pm-shimmer {
  to { background-position: -200% 0; }
}

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .pm-duo { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .pm-row { align-items: flex-start; }
  .pm-row-amount { order: -1; width: 100%; }
}
@media (max-width: 560px) {
  .pm-shell { padding: 0 16px; }
  .pm-head { padding: 30px 0 22px; }
  .pm-sub { font-size: 13.5px; }
  .pm-section { margin-top: 44px; }
  .pm-card { padding: 20px 18px; }
  .pm-card-foot { align-items: stretch; flex-direction: column-reverse; }
  .pm-card-foot .cta { width: 100%; }
  .pm-quiet { align-self: flex-end; }
  .pm-row { padding: 14px 16px; }
}

@media (prefers-reduced-motion: reduce) {
  .pm-skeleton { animation: none; }
}
</style>
