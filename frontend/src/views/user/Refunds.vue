<template>
  <div class="rf-page">
    <div class="rf-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="rf-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="rf-crumb-link">首页</router-link>
        <span class="rf-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/orders" class="rf-crumb-link">我的订单</router-link>
        <span class="rf-crumb-sep" aria-hidden="true">›</span>
        <span class="rf-crumb-here">退款申请</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="rf-head">
        <div class="rf-head-copy">
          <div class="rf-eyebrow" aria-hidden="true">
            <span class="rf-eyebrow-line"></span>
            <span>Refunds</span>
          </div>
          <h1 class="rf-title">退款申请</h1>
          <p class="rf-sub">日期变更、服务未完成或订单重复支付，都可以在这里发起退款。审核通过后金额退回账户余额。</p>
        </div>
        <div class="rf-actions">
          <router-link to="/orders" class="cta cta-outline">我的订单</router-link>
          <button type="button" class="cta cta-primary" @click="openForm">申请退款</button>
        </div>
      </header>

      <!-- ═══ Facts ═══ -->
      <dl class="rf-facts" aria-label="退款概览">
        <div class="rf-fact">
          <dd>{{ loading ? '--' : refunds.length }}</dd>
          <dt>申请总数</dt>
        </div>
        <div class="rf-fact">
          <dd>{{ loading ? '--' : refunds.filter(r => r.status_wsh === 'pending').length }}</dd>
          <dt>待处理</dt>
        </div>
        <div class="rf-fact">
          <dd>{{ loading ? '--' : refunds.filter(r => r.status_wsh === 'completed').length }}</dd>
          <dt>已到账</dt>
        </div>
      </dl>

      <!-- ═══ 01 · 申请进度 ═══ -->
      <section class="rf-section" aria-label="申请进度">
        <header class="rf-sec-head">
          <div class="rf-head-copy">
            <p class="rf-eyebrow rf-sec-eyebrow">
              <span class="rf-idx">01</span>
              <span class="rf-line" aria-hidden="true"></span>
              <span>Progress</span>
            </p>
            <h2 class="rf-sec-title">申请进度</h2>
            <p class="rf-sec-desc">审核结果会同步到订单与站内通知。</p>
          </div>
        </header>

        <!-- loading -->
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="rf-skeleton" />
        </template>

        <!-- empty -->
        <div v-else-if="!refunds.length" class="rf-empty">
          <p class="rf-empty-title">暂无退款申请</p>
          <p class="rf-empty-desc">如果订单需要取消或改期，先在订单详情确认可退金额，再提交申请。</p>
          <div class="rf-empty-actions">
            <button type="button" class="cta cta-primary" @click="openForm">申请退款</button>
            <router-link to="/orders" class="cta cta-outline">查看我的订单</router-link>
          </div>
        </div>

        <!-- list -->
        <div v-else class="rf-list">
          <article v-for="r in refunds" :key="r.id_wsh" class="rf-card">
            <div class="rf-card-head">
              <div class="rf-main">
                <p class="rf-card-eyebrow">
                  退款单 <span class="rf-num">#{{ r.id_wsh }}</span>
                  <template v-if="r.order_id_wsh">
                    <span class="rf-sep" aria-hidden="true">·</span>
                    订单 <span class="rf-num">#{{ r.order_id_wsh }}</span>
                  </template>
                </p>
                <p class="rf-amount">
                  <span class="rf-yen">¥</span>
                  <span class="rf-num">{{ Number(r.amount_wsh || 0).toFixed(2) }}</span>
                </p>
              </div>
              <div class="rf-side">
                <span
                  :class="[
                    'badge',
                    r.status_wsh === 'pending' ? 'badge-action'
                      : r.status_wsh === 'approved' ? 'badge-active'
                      : r.status_wsh === 'rejected' ? 'badge-danger'
                      : 'badge-done'
                  ]"
                >
                  {{ statusMap[r.status_wsh] || r.status_wsh }}
                </span>
                <p class="rf-date">{{ r.created_at_wsh ? new Date(r.created_at_wsh).toLocaleDateString('zh-CN') : '-' }}</p>
              </div>
            </div>

            <div v-if="r.reason_wsh" class="rf-reason">
              <p class="rf-reason-label">退款原因</p>
              <p class="rf-reason-text">{{ r.reason_wsh }}</p>
            </div>

            <div class="rf-track" :class="{ 'is-failed': r.status_wsh === 'rejected' }">
              <div
                class="rf-step"
                :class="{
                  'is-done': ['approved', 'completed'].includes(r.status_wsh),
                  'is-active': r.status_wsh === 'pending'
                }"
              >
                <span class="rf-dot">1</span>
                <span class="rf-step-label">提交申请</span>
              </div>
              <div
                class="rf-step"
                :class="{
                  'is-done': r.status_wsh === 'completed',
                  'is-active': r.status_wsh === 'approved'
                }"
              >
                <span class="rf-dot">2</span>
                <span class="rf-step-label">平台审核</span>
              </div>
              <div class="rf-step" :class="{ 'is-active': r.status_wsh === 'completed' }">
                <span class="rf-dot">3</span>
                <span class="rf-step-label">退款到账</span>
              </div>
            </div>
            <p v-if="r.status_wsh === 'rejected'" class="rf-fail-note">该申请未通过审核，如有疑问可联系平台客服。</p>
          </article>
        </div>
      </section>
    </div>

    <!-- ═══ 申请退款 dialog ═══ -->
    <div v-if="showForm" class="dlg-overlay" @click.self="showForm = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="申请退款">
        <div class="dlg-head">
          <h3 class="dlg-title">申请退款</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="showForm = false">✕</button>
        </div>
        <form @submit.prevent="submitRefund">
          <div class="dlg-body">
            <div class="dlg-field">
              <label class="dlg-label" for="rf-order">订单号</label>
              <input id="rf-order" v-model="form.order_id_wsh" type="number" class="dlg-input" placeholder="请输入订单号" required>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="rf-reason">退款原因</label>
              <textarea id="rf-reason" v-model="form.reason_wsh" rows="4" class="dlg-input dlg-textarea" placeholder="请说明退款原因" required></textarea>
            </div>
            <p class="dlg-note">提交后进入平台审核，通过后金额将退回账户余额。</p>
          </div>
          <div class="dlg-foot">
            <button type="button" class="dlg-cancel" @click="showForm = false">取消</button>
            <button type="submit" class="cta cta-primary">提交申请</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyRefunds, createRefund } from '@/api/refund'
import { RefundStatus, getStatusLabel } from '@/constants/statusMaps'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const refunds = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = reactive({ order_id_wsh: '', reason_wsh: '' })
const statusMap = Object.fromEntries(Object.entries(RefundStatus).map(([k, v]) => [k, v.label]))
function statusLabel(s) { return getStatusLabel(RefundStatus, s) }

function openForm() { form.order_id_wsh = ''; form.reason_wsh = ''; showForm.value = true }

onMounted(async () => {
  try { const r = await getMyRefunds(); if (r.code === 200) refunds.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function submitRefund() {
  try {
    const r = await createRefund(form)
    if (r.code === 200) {
      appStore.addToast('退款申请已提交', 'success')
      showForm.value = false
      refunds.value.unshift(r.data)
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}
</script>

<style scoped>
.rf-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.rf-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.rf-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.rf-crumb-link { color: var(--ref-muted); text-decoration: none; }
.rf-crumb-link:hover { color: var(--ref-ink); }
.rf-crumb-sep { color: var(--ref-line); }
.rf-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.rf-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rf-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.rf-idx { font-variant-numeric: tabular-nums; }
.rf-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.rf-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.rf-head-copy { min-width: 0; }
.rf-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.rf-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.rf-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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

/* ═══ Badges ═══ */
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
.badge-action { background: var(--ref-brand); color: #fff; }
.badge-active { background: color-mix(in srgb, var(--color-success) 12%, transparent); color: var(--color-success); border-color: color-mix(in srgb, var(--color-success) 30%, transparent); }
.badge-done { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); }
.badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-danger { background: color-mix(in srgb, var(--color-danger) 10%, transparent); color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 28%, transparent); }

/* ═══ Facts ═══ */
.rf-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 8px 0 0;
}
.rf-fact {
  padding: 20px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.rf-fact:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.rf-fact dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.rf-fact dt {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ref-muted);
}

/* ═══ Section ═══ */
.rf-section { margin-top: 56px; }
.rf-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.rf-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.rf-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.rf-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Skeleton ═══ */
.rf-skeleton {
  margin-top: 20px;
  height: 172px;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: rf-shimmer 1.3s linear infinite;
}

/* ═══ Empty ═══ */
.rf-empty {
  margin-top: 20px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  text-align: center;
}
.rf-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.rf-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.rf-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

/* ═══ Refund cards ═══ */
.rf-list { margin-top: 20px; display: grid; gap: 14px; }
.rf-card {
  padding: 20px;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s;
}
.rf-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 26px 56px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.rf-card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px 20px;
}
.rf-main { min-width: 0; }
.rf-card-eyebrow {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}
.rf-num { font-variant-numeric: tabular-nums; }
.rf-sep { margin: 0 4px; color: var(--ref-line); }
.rf-amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.4vw, 40px);
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.rf-yen { font-size: 15px; color: color-mix(in srgb, var(--ref-brand) 72%, transparent); }
.rf-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.rf-date { margin: 0; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }

.rf-reason {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--ref-line);
}
.rf-reason-label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rf-reason-text {
  margin: 6px 0 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
  overflow-wrap: anywhere;
}

/* ═══ Progress track ═══ */
.rf-track {
  display: flex;
  align-items: flex-start;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--ref-line);
}
.rf-step {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}
.rf-step::after {
  content: '';
  position: absolute;
  top: 12px;
  left: 50%;
  width: 100%;
  height: 1px;
  background: var(--ref-line);
  z-index: 0;
}
.rf-step:last-child::after { display: none; }
.rf-dot {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  color: var(--ref-muted);
  font-size: 11px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.rf-step-label {
  font-size: 11px;
  color: var(--ref-muted);
  white-space: nowrap;
}
.rf-step.is-done .rf-dot { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.rf-step.is-done::after { background: var(--ref-brand); }
.rf-step.is-done .rf-step-label { color: var(--ref-ink-soft); }
.rf-step.is-active .rf-dot {
  border-color: var(--ref-brand);
  color: var(--ref-brand);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.rf-step.is-active .rf-step-label { color: var(--ref-ink); }
.rf-track.is-failed .rf-dot {
  background: color-mix(in srgb, var(--color-danger) 8%, var(--ref-surface));
  border-color: color-mix(in srgb, var(--color-danger) 30%, transparent);
  color: var(--color-danger);
}
.rf-track.is-failed .rf-step-label { color: var(--ref-muted); }
.rf-fail-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: color-mix(in srgb, var(--color-danger) 80%, transparent);
}

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(10, 8, 6, 0.5);
  backdrop-filter: blur(2px);
  animation: rf-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 440px;
  max-height: min(90vh, 720px);
  overflow-y: auto;
  border-radius: 20px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  animation: rf-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.dlg-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.dlg-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--ref-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-input::placeholder { color: var(--ref-muted); }
.dlg-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.dlg-textarea { height: auto; min-height: 96px; padding: 12px 14px; resize: vertical; line-height: 1.6; }
.dlg-note { margin: 0; font-size: 11.5px; line-height: 1.7; color: var(--ref-muted); }
.dlg-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══ Animations ═══ */
@keyframes rf-shimmer { to { background-position: -200% 0; } }
@keyframes rf-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes rf-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .rf-facts { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 560px) {
  .rf-shell { padding: 0 16px; }
  .rf-head { padding: 30px 0 22px; }
  .rf-sub { font-size: 13.5px; }
  .rf-facts { grid-template-columns: 1fr; gap: 10px; }
  .rf-fact { padding: 16px 20px; }
  .rf-fact dd { font-size: 24px; }
  .rf-section { margin-top: 44px; }
  .rf-card { padding: 16px; }
  .rf-card-head { flex-direction: column; }
  .rf-side { align-items: flex-start; flex-direction: row; }
}
@media (prefers-reduced-motion: reduce) {
  .rf-skeleton { animation: none; }
}
</style>
