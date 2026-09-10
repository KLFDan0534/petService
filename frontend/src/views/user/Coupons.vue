<template>
  <div class="cp-page">
    <div class="cp-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="cp-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="cp-crumb-link">首页</router-link>
        <span class="cp-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="cp-crumb-link">个人中心</router-link>
        <span class="cp-crumb-sep" aria-hidden="true">›</span>
        <span class="cp-crumb-here">我的优惠券</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="cp-head">
        <div class="cp-head-copy">
          <div class="cp-eyebrow" aria-hidden="true">
            <span class="cp-eyebrow-line"></span>
            <span>优惠券</span>
          </div>
          <h1 class="cp-title">优惠券</h1>
          <p class="cp-sub">活动券先领后用，下单时系统会自动匹配当前订单可用的最优券。</p>
        </div>
        <div class="cp-actions">
          <button type="button" class="cta cta-outline" :disabled="loading" @click="loadAll">刷新</button>
        </div>
      </header>

      <!-- ═══ Facts ═══ -->
      <dl class="cp-facts" aria-label="优惠券概览">
        <div class="cp-fact">
          <dd>{{ loading ? '--' : activeTemplates.length }}</dd>
          <dt>可领取</dt>
        </div>
        <div class="cp-fact">
          <dd>{{ loading ? '--' : myCoupons.length }}</dd>
          <dt>我的券包</dt>
        </div>
        <div class="cp-fact">
          <dd>{{ loading ? '--' : myCoupons.filter(c => c.status_wsh === 'available').length }}</dd>
          <dt>当前可用</dt>
        </div>
      </dl>

      <!-- ═══ 01 · 可领取优惠券 ═══ -->
      <section class="cp-section" aria-label="可领取优惠券">
        <header class="cp-sec-head">
          <div class="cp-head-copy">
            <p class="cp-eyebrow cp-sec-eyebrow">
              <span class="cp-idx">01</span>
              <span class="cp-line" aria-hidden="true"></span>
              <span>可用</span>
            </p>
            <h2 class="cp-sec-title">可领取优惠券</h2>
            <p class="cp-sec-desc">平台与门店的当期活动券，领取后进入券包。</p>
          </div>
        </header>

        <template v-if="loading">
          <div class="cp-grid"><div v-for="i in 3" :key="i" class="cp-skeleton" /></div>
        </template>

        <div v-else-if="!activeTemplates.length" class="cp-empty">
          <p class="cp-empty-title">暂无可领取优惠券</p>
          <p class="cp-empty-desc">新的活动券会在这里出现，也可以先看看已有券包里能用的券。</p>
          <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
        </div>

        <div v-else class="cp-grid">
          <article v-for="item in activeTemplates" :key="item.id_wsh" class="cp-card">
            <div class="cp-face">
              <p class="cp-value">{{ discountText(item) }}</p>
              <p class="cp-name">{{ item.name_wsh }}</p>
            </div>
            <div class="cp-body">
              <p class="cp-meta">满 <b>¥ {{ money(item.threshold_amount_wsh) }}</b> 可用</p>
              <p class="cp-meta">{{ scopeText(item) }}</p>
              <p class="cp-meta">有效期至 {{ dateText(item.valid_to_wsh) }}</p>
              <button
                type="button"
                class="cta cta-primary cp-claim"
                :disabled="submittingId === item.id_wsh"
                @click="claim(item)"
              >
                {{ submittingId === item.id_wsh ? '领取中...' : '立即领取' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <!-- ═══ 02 · 我的券包 ═══ -->
      <section class="cp-section" aria-label="我的券包">
        <header class="cp-sec-head">
          <div class="cp-head-copy">
            <p class="cp-eyebrow cp-sec-eyebrow">
              <span class="cp-idx">02</span>
              <span class="cp-line" aria-hidden="true"></span>
              <span>我的钱包</span>
            </p>
            <h2 class="cp-sec-title">我的券包</h2>
            <p class="cp-sec-desc">已锁定的券正被进行中的订单占用，订单结束后会释放。</p>
          </div>
        </header>

        <template v-if="loading">
          <div class="cp-grid"><div v-for="i in 2" :key="i" class="cp-skeleton" /></div>
        </template>

        <div v-else-if="!myCoupons.length" class="cp-empty">
          <p class="cp-empty-title">券包还是空的</p>
          <p class="cp-empty-desc">领取上方的活动券后，可用券会显示在这里，下单时自动参与抵扣。</p>
        </div>

        <div v-else class="cp-grid">
          <article v-for="item in myCoupons" :key="item.id_wsh" class="cp-card" :class="{ 'is-muted': item.status_wsh !== 'available' }">
            <div class="cp-face">
              <p class="cp-value">{{ discountText(item) }}</p>
              <p class="cp-name">{{ item.name_wsh || `优惠券 #${item.id_wsh}` }}</p>
            </div>
            <div class="cp-body">
              <p class="cp-meta">满 <b>¥ {{ money(item.threshold_amount_wsh) }}</b> 可用</p>
              <p class="cp-meta">{{ scopeText(item) }}</p>
              <p class="cp-meta">有效期至 {{ dateText(item.expire_at_wsh) }}</p>
              <span class="cp-status" :class="statusClass(item.status_wsh)">{{ statusText(item.status_wsh) }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { claimCoupon, getActiveCouponTemplates, getMyCoupons } from '@/api/coupon'
import { formatMoney } from '@/utils/format'

const appStore = useAppStore()
const loading = ref(false)
const submittingId = ref(null)
const activeTemplates = ref([])
const myCoupons = ref([])

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [templatesRes, couponsRes] = await Promise.all([getActiveCouponTemplates(), getMyCoupons()])
    if (templatesRes.code === 200) activeTemplates.value = Array.isArray(templatesRes.data) ? templatesRes.data : []
    if (couponsRes.code === 200) myCoupons.value = Array.isArray(couponsRes.data) ? couponsRes.data : []
  } catch (e) {
    appStore.addToast('优惠券加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function claim(item) {
  if (submittingId.value) return
  submittingId.value = item.id_wsh
  try {
    const res = await claimCoupon(item.id_wsh)
    if (res.code === 200) {
      appStore.addToast('领取成功', 'success')
      await loadAll()
    }
  } catch (e) {
    appStore.addToast('领取失败', 'error')
  } finally {
    submittingId.value = null
  }
}

function discountText(item) {
  if (item.type_wsh === 'percent') {
    const rate = Number(item.discount_rate_wsh || 0)
    const max = Number(item.max_discount_amount_wsh || 0)
    return `${(rate * 10).toFixed(1)}折${max > 0 ? `，最高减 ¥${money(max)}` : ''}`
  }
  const amount = item.discount_amount_template_wsh ?? item.discount_amount_wsh
  return `立减 ¥${money(amount)}`
}

function scopeText(item) {
  return item.scope_type_wsh === 'merchant' ? `商家 ${item.merchant_id_wsh}` : '全平台'
}

function statusText(status) {
  return {
    available: '可用',
    locked: '已锁定',
    used: '已使用',
  }[status] || status || '-'
}

function statusClass(status) {
  return {
    available: 'is-on',
    locked: 'is-wait',
    used: 'is-off',
  }[status] || 'is-off'
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function money(value) {
  return formatMoney(value)
}
</script>

<style scoped>
.cp-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.cp-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.cp-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.cp-crumb-link { color: var(--ref-muted); text-decoration: none; }
.cp-crumb-link:hover { color: var(--ref-ink); }
.cp-crumb-sep { color: var(--ref-line); }
.cp-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.cp-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.cp-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.cp-idx { font-variant-numeric: tabular-nums; }
.cp-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.cp-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.cp-head-copy { min-width: 0; }
.cp-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.cp-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.cp-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-control);
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

/* ═══ Facts ═══ */
.cp-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 8px 0 0;
}
.cp-fact {
  padding: 20px 24px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.cp-fact:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lift);
}
.cp-fact dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.cp-fact dt {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ref-muted);
}

/* ═══ Section ═══ */
.cp-section { margin-top: 56px; }
.cp-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.cp-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.cp-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.cp-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Grid ═══ */
.cp-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

/* ═══ Skeleton ═══ */
.cp-skeleton {
  height: 200px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: cp-shimmer 1.3s linear infinite;
}

/* ═══ Empty ═══ */
.cp-empty {
  margin-top: 20px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  text-align: center;
}
.cp-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.cp-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.cp-empty .cta { margin-top: 22px; }

/* ═══ Coupon card ═══ */
.cp-card {
  position: relative;
  display: flex;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s;
}
.cp-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-lift);
}
.cp-card.is-muted { opacity: 0.62; }

/* left face */
.cp-face {
  position: relative;
  flex-shrink: 0;
  width: 42%;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  border-right: 1px dashed color-mix(in srgb, var(--ref-ink) 22%, transparent);
}
.cp-face::before,
.cp-face::after {
  content: '';
  position: absolute;
  right: -8px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--ref-canvas);
  border: 1px solid var(--ref-line);
}
.cp-face::before { top: -9px; }
.cp-face::after { bottom: -9px; }
.cp-value {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: clamp(20px, 2vw, 24px);
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.02em;
  color: var(--ref-brand);
  overflow-wrap: anywhere;
}
.cp-name {
  margin: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--ref-ink-soft);
  overflow-wrap: anywhere;
}

/* right body */
.cp-body {
  flex: 1;
  min-width: 0;
  padding: 18px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cp-meta {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--ref-muted);
  overflow-wrap: anywhere;
}
.cp-meta b { color: var(--ref-ink-soft); font-weight: 500; font-variant-numeric: tabular-nums; }
.cp-claim { width: 100%; margin-top: auto; height: 38px; }
.cp-status {
  align-self: flex-start;
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  border: 1px solid transparent;
}
.cp-status.is-on {
  background: color-mix(in srgb, var(--color-success) 12%, transparent);
  color: var(--color-success);
  border-color: color-mix(in srgb, var(--color-success) 30%, transparent);
}
.cp-status.is-wait {
  background: color-mix(in srgb, var(--color-warning) 14%, transparent);
  color: var(--color-warning);
  border-color: color-mix(in srgb, var(--color-warning) 32%, transparent);
}
.cp-status.is-off {
  background: color-mix(in srgb, var(--ref-ink) 5%, transparent);
  color: var(--ref-muted);
  border-color: var(--ref-line);
}

/* ═══ Animations ═══ */
@keyframes cp-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .cp-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .cp-facts { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
  .cp-fact { padding: 16px 16px; }
}
@media (max-width: 520px) {
  .cp-shell { padding: 0 16px; }
  .cp-head { padding: 30px 0 22px; }
  .cp-sub { font-size: 13.5px; }
  .cp-facts { grid-template-columns: 1fr; gap: 10px; }
  .cp-fact dd { font-size: 24px; }
  .cp-section { margin-top: 44px; }
  .cp-grid { grid-template-columns: 1fr; }
  .cp-face { width: 46%; }
}
@media (prefers-reduced-motion: reduce) {
  .cp-skeleton { animation: none; }
}
</style>
