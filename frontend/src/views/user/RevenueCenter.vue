<template>
  <div class="rv-page">
    <div class="rv-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="rv-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="rv-crumb-link">首页</router-link>
        <span class="rv-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="rv-crumb-link">个人中心</router-link>
        <span class="rv-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/keeper-workflow" class="rv-crumb-link">照护师工作台</router-link>
        <span class="rv-crumb-sep" aria-hidden="true">›</span>
        <span class="rv-crumb-here">收益中心</span>
      </nav>

      <!-- ═══ Page header ═══ -->
      <header class="rv-head">
        <div class="rv-head-copy">
          <div class="rv-eyebrow" aria-hidden="true">
            <span class="rv-eyebrow-line"></span>
            <span>收益</span>
          </div>
          <h1 class="rv-title">收益中心</h1>
          <p class="rv-sub">收益按已完成的订单结算，在途订单单独列出。提现与明细在钱包中处理。</p>
        </div>
        <div class="rv-actions">
          <router-link to="/wallet" class="cta cta-primary">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" /><path d="M3 5v14a2 2 0 0 0 2 2h16v-5" /><path d="M18 12a2 2 0 0 0 0 4h4v-4Z" />
            </svg>
            去钱包提现
          </router-link>
          <router-link to="/keeper-workflow" class="cta cta-outline">返回工作台</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 收益概览 ═══ -->
      <section class="rv-section" aria-label="收益概览">
        <header class="rv-sec-head">
          <div class="rv-head-copy">
            <p class="rv-eyebrow">
              <span class="rv-idx">01</span>
              <span class="rv-line" aria-hidden="true"></span>
              <span>概览</span>
            </p>
            <h2 class="rv-sec-title">收益概览</h2>
            <p class="rv-sec-desc">累计收益为平台已结算金额，与钱包余额可能存在结算周期差。</p>
          </div>
        </header>

        <div v-if="loading" class="rv-skeleton rv-over-skeleton"></div>
        <div v-else-if="error" class="rv-empty">
          <p class="rv-empty-title">收益数据加载失败</p>
          <p class="rv-empty-desc">{{ error }}</p>
          <button type="button" class="cta cta-outline" @click="load">重试</button>
        </div>

        <div v-else class="rv-metrics">
          <div class="rv-metric rv-metric-strong">
            <p class="rv-m-label">累计收益</p>
            <p class="rv-m-value">
              <span class="rv-yen">¥</span>
              <span class="tabular">{{ formatMoney(stats.total_income_wsh) }}</span>
            </p>
            <p class="rv-m-note">本月 ¥{{ formatMoney(stats.month_income_wsh) }} · 打赏 ¥{{ formatMoney(stats.tip_income_wsh) }}</p>
          </div>

          <div class="rv-metric">
            <p class="rv-m-label">在途订单金额</p>
            <p class="rv-m-value">
              <span class="rv-yen">¥</span>
              <span class="tabular">{{ formatMoney(inFlightAmount) }}</span>
            </p>
            <p class="rv-m-note">{{ inFlight.length }} 笔进行中，完成后结算</p>
          </div>

          <div class="rv-metric">
            <p class="rv-m-label">完成率</p>
            <p class="rv-m-value tabular">{{ percentLabel(completionRate) }}</p>
            <p class="rv-m-note">已完成 {{ stats.completed_orders_wsh ?? 0 }} / 共 {{ stats.total_orders_wsh ?? 0 }} 单</p>
          </div>
        </div>
      </section>

      <!-- ═══ 02 · 在途订单 ═══ -->
      <section class="rv-section" aria-label="在途订单">
        <header class="rv-sec-head">
          <div class="rv-head-copy">
            <p class="rv-eyebrow">
              <span class="rv-idx">02</span>
              <span class="rv-line" aria-hidden="true"></span>
              <span>进行中</span>
            </p>
            <h2 class="rv-sec-title">在途订单</h2>
            <p class="rv-sec-desc">这些金额尚未结算，服务完成后才会入账。</p>
          </div>
        </header>

        <div v-if="loading" class="rv-skeleton" style="height: 150px"></div>
        <div v-else-if="!inFlight.length" class="rv-empty">
          <p class="rv-empty-title">没有在途订单</p>
          <p class="rv-empty-desc">接单后，进行中的订单金额会显示在这里。</p>
          <router-link to="/keeper-workflow" class="cta cta-outline">去工作台接单</router-link>
        </div>
        <ul v-else class="rv-list">
          <li v-for="(task, index) in inFlight" :key="task.id_wsh ?? index" class="rv-row">
            <div class="rv-main">
              <p class="rv-title">{{ task.pet_name_wsh || '宠物' }} · {{ task.service_name_wsh || '照护服务' }}</p>
              <p class="rv-meta tabular">{{ task.order_no_wsh }} · {{ formatDay(task.start_date_wsh) }} 起</p>
            </div>
            <p class="rv-amount">¥{{ formatMoney(task.amount_wsh) }}</p>
          </li>
        </ul>
      </section>

      <!-- ═══ 03 · 已结算订单 ═══ -->
      <section class="rv-section" aria-label="已结算订单">
        <header class="rv-sec-head">
          <div class="rv-head-copy">
            <p class="rv-eyebrow">
              <span class="rv-idx">03</span>
              <span class="rv-line" aria-hidden="true"></span>
              <span>已结算</span>
            </p>
            <h2 class="rv-sec-title">已结算订单</h2>
            <p class="rv-sec-desc">按服务结束时间排序，可与钱包流水逐笔核对。</p>
          </div>
        </header>

        <div v-if="loading" class="rv-skeleton" style="height: 150px"></div>
        <div v-else-if="!settled.length" class="rv-empty">
          <p class="rv-empty-title">还没有已结算订单</p>
          <p class="rv-empty-desc">完成第一笔服务后，结算记录会出现在这里。</p>
          <router-link to="/keeper-workflow" class="cta cta-outline">去工作台接单</router-link>
        </div>
        <ul v-else class="rv-list">
          <li v-for="(task, index) in settled" :key="task.id_wsh ?? index" class="rv-row">
            <div class="rv-main">
              <p class="rv-title">{{ task.pet_name_wsh || '宠物' }} · {{ task.service_name_wsh || '照护服务' }}</p>
              <p class="rv-meta tabular">{{ task.order_no_wsh }} · 完成于 {{ formatDay(task.end_date_wsh) }}</p>
            </div>
            <p class="rv-settled-amount">+¥{{ formatMoney(task.amount_wsh) }}</p>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getUserStatistics } from '@/api/statistics'
import { getMyKeeperInfo } from '@/api/keeper'
import { getMyKeeperOrders } from '@/api/order'

const loading = ref(true)
const error = ref('')
const stats = ref({})
const keeper = ref(null)
const orders = ref([])

const IN_FLIGHT_STATUSES = ['paid', 'confirmed', 'delivered', 'received', 'in_progress']

const inFlight = computed(() => orders.value.filter((o) => IN_FLIGHT_STATUSES.includes(o.status_wsh)))
const settled = computed(() => orders.value.filter((o) => o.status_wsh === 'completed').slice().sort((a, b) => String(b.end_date_wsh || '').localeCompare(String(a.end_date_wsh || ''))))
const completionRate = computed(() => keeper.value?.completion_rate_wsh)

const inFlightAmount = computed(() => orders.value
  .filter((o) => IN_FLIGHT_STATUSES.includes(o.status_wsh))
  .reduce((sum, o) => sum + (Number(o.amount_wsh) || 0), 0))

function formatMoney(value) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function percentLabel(value) {
  return value == null ? '—' : `${Number(value)}%`
}

function formatDay(value) {
  if (!value) return '—'
  const d = new Date(String(value).slice(0, 10))
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 10)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${mm}.${dd}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [statsRes, keeperRes, ordersRes] = await Promise.all([
      getUserStatistics(),
      getMyKeeperInfo().catch(() => null),
      getMyKeeperOrders(),
    ])
    if (statsRes.code === 200) stats.value = statsRes.data || {}
    if (keeperRes && keeperRes.code === 200) keeper.value = keeperRes.data || null
    if (ordersRes.code === 200) orders.value = (ordersRes.data || []).filter((o) => !!o.id_wsh || !!o.order_no_wsh)
  } catch (e) {
    error.value = e?.message || '收益数据加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rv-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.rv-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* Breadcrumb */
.rv-crumb { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.rv-crumb-link { color: var(--ref-muted); text-decoration: none; }
.rv-crumb-link:hover { color: var(--ref-ink); }
.rv-crumb-sep { color: var(--ref-line); }
.rv-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow / head */
.rv-eyebrow { display: flex; align-items: center; gap: 10px; font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }
.rv-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.rv-idx { font-variant-numeric: tabular-nums; }
.rv-line { width: 24px; height: 1px; background: var(--ref-line); }

/* Header */
.rv-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 20px 32px; padding: 40px 0 8px; }
.rv-head-copy { min-width: 0; }
.rv-title { margin: 18px 0 0; font-family: var(--ref-font-display); font-size: clamp(34px, 4.4vw, 52px); line-height: 1.12; letter-spacing: -0.01em; font-weight: 500; color: var(--ref-ink); text-wrap: balance; }
.rv-sub { margin: 14px 0 0; max-width: 620px; font-size: 14px; line-height: 1.75; color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent); }
.rv-actions { display: flex; flex-wrap: wrap; gap: 10px; flex-shrink: 0; padding-top: 4px; }

/* Section */
.rv-section { margin-top: 48px; }
.rv-sec-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 12px 24px; }
.rv-sec-title { margin: 10px 0 0; font-family: var(--ref-font-display); font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink); }
.rv-sec-desc { margin: 8px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: color-mix(in srgb, var(--ref-ink-soft) 80%, transparent); }

/* Metrics */
.rv-metrics { display: grid; grid-template-columns: 1.3fr 1fr 1fr; gap: 16px; margin-top: 24px; }
.rv-metric { padding: 22px; border: 1px solid var(--ref-line); border-radius: var(--radius-lg); background: var(--ref-surface); }
.rv-metric-strong { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }
.rv-metric-strong .rv-m-label { color: color-mix(in srgb, var(--ref-cream) 45%, transparent); }
.rv-metric-strong .rv-m-note { color: color-mix(in srgb, var(--ref-cream) 55%, transparent); }
.rv-m-label { margin: 0; font-size: 11px; letter-spacing: 0.18em; text-transform: uppercase; color: var(--ref-muted); }
.rv-m-value {
  margin: 16px 0 0; display: flex; align-items: baseline; gap: 4px;
  font-family: var(--ref-font-display); font-size: 30px; font-weight: 400;
  line-height: 1; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; color: var(--ref-ink);
}
.rv-metric-strong .rv-m-value { color: var(--ref-cream); }
.rv-yen { font-size: 15px; opacity: 0.6; }
.rv-m-note { margin: 14px 0 0; font-size: 12px; line-height: 1.7; color: var(--ref-muted); }

/* List */
.rv-list { margin: 24px 0 0; overflow: hidden; border: 1px solid var(--ref-line); border-radius: var(--radius-lg); background: var(--ref-surface); }
.rv-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px 20px; border-bottom: 1px solid var(--ref-line); }
.rv-row:last-child { border-bottom: 0; }
.rv-main { min-width: 0; }
.rv-title { margin: 0; font-size: 13.5px; font-weight: 500; color: var(--ref-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rv-meta { margin: 6px 0 0; font-size: 11.5px; color: var(--ref-muted); }
.rv-amount { margin: 0; flex-shrink: 0; font-size: 14px; font-weight: 500; font-variant-numeric: tabular-nums; color: var(--ref-ink-soft); }
.rv-settled-amount { margin: 0; flex-shrink: 0; font-size: 14px; font-weight: 500; font-variant-numeric: tabular-nums; color: var(--ref-moss, #3f5347); }

/* Empty */
.rv-empty { margin-top: 24px; padding: 48px 24px; border: 1px dashed var(--ref-line); border-radius: var(--radius-lg); background: var(--ref-surface); text-align: center; }
.rv-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--ref-ink); }
.rv-empty-desc { margin: 10px auto 0; max-width: 420px; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.rv-empty .cta { margin-top: 18px; }

/* Skeleton */
.rv-skeleton { margin-top: 24px; border-radius: var(--radius-lg); border: 1px solid var(--ref-line); background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%); background-size: 200% 100%; animation: rv-shimmer 1.3s linear infinite; }

/* CTA */
.cta { display: inline-flex; align-items: center; justify-content: center; gap: 8px; height: 42px; padding: 0 18px; border-radius: var(--radius-control); font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent; text-decoration: none; transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s; }
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* Animations */
@keyframes rv-shimmer { to { background-position: -200% 0; } }

/* Responsive */
@media (max-width: 900px) {
  .rv-metrics { grid-template-columns: 1fr; }
  .rv-metric-strong { grid-column: 1 / -1; }
}
@media (max-width: 600px) {
  .rv-shell { padding: 0 16px; }
  .rv-head { padding: 30px 0 8px; }
  .rv-sub { font-size: 13.5px; }
  .rv-section { margin-top: 40px; }
  .rv-actions { width: 100%; }
  .rv-actions .cta { flex: 1; }
  .rv-row { padding: 14px 16px; }
}
@media (prefers-reduced-motion: reduce) { .rv-skeleton { animation: none; } }
</style>