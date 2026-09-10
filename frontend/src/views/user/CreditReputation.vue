<template>
  <div class="cr-page">
    <div class="cr-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="cr-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="cr-crumb-link">首页</router-link>
        <span class="cr-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="cr-crumb-link">个人中心</router-link>
        <span class="cr-crumb-sep" aria-hidden="true">›</span>
        <span class="cr-crumb-here">信誉中心</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="cr-head">
        <div class="cr-head-copy">
          <div class="cr-eyebrow" aria-hidden="true">
            <span class="cr-eyebrow-line"></span>
            <span>信用中心</span>
          </div>
          <h1 class="cr-title">信誉中心</h1>
          <p class="cr-sub">查看寄养师或门店的履约记录，以及你自己提交过的投诉与处理结果。</p>
        </div>
      </header>

      <!-- ═══ 01 · 查询 ═══ -->
      <section class="cr-section" aria-label="查询对象">
        <header class="cr-sec-head">
          <div class="cr-head-copy">
            <p class="cr-eyebrow cr-sec-eyebrow">
              <span class="cr-idx">01</span>
              <span class="cr-line" aria-hidden="true"></span>
              <span>查询</span>
            </p>
            <h2 class="cr-sec-title">查询对象</h2>
            <p class="cr-sec-desc">选择类型并输入对象编号，即可查看其公开的履约与评价记录。</p>
          </div>
        </header>

        <form class="cr-panel" @submit.prevent="query">
          <fieldset>
            <legend class="cr-label cr-label-field">对象类型</legend>
            <div class="cr-chips" role="tablist" aria-label="对象类型">
              <button
                type="button"
                role="tab"
                :aria-selected="targetType === 'keeper'"
                class="cr-chip"
                :class="{ active: targetType === 'keeper' }"
                @click="targetType = 'keeper'"
              >寄养师</button>
              <button
                type="button"
                role="tab"
                :aria-selected="targetType === 'merchant'"
                class="cr-chip"
                :class="{ active: targetType === 'merchant' }"
                @click="targetType = 'merchant'"
              >门店</button>
            </div>
          </fieldset>

          <div class="cr-query">
            <div class="cr-field">
              <label class="cr-label" for="cr-target-id">对象编号</label>
              <input
                id="cr-target-id"
                v-model.trim="targetId"
                class="cr-input"
                inputmode="numeric"
                placeholder="例如 12"
                :class="{ 'has-error': error }"
                @input="error = ''"
              >
              <p v-if="error" class="cr-error">{{ error }}</p>
              <p v-else class="cr-hint">编号可以在寄养师或门店主页的地址栏中找到。</p>
            </div>
            <button type="submit" class="cta cta-primary" :disabled="querying">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
              </svg>
              {{ querying ? '查询中…' : '查询' }}
            </button>
          </div>

          <p class="cr-note">
            平台的信誉统计接口按服务提供方计算（寄养师 / 门店），目前没有面向普通用户的个人信用评分接口，因此这里不展示个人分数。
          </p>
        </form>
      </section>

      <!-- ═══ 02 · 记录 ═══ -->
      <section v-if="queried" class="cr-section" aria-label="履约记录">
        <header class="cr-sec-head">
          <div class="cr-head-copy">
            <p class="cr-eyebrow cr-sec-eyebrow">
              <span class="cr-idx">02</span>
              <span class="cr-line" aria-hidden="true"></span>
              <span>记录</span>
            </p>
            <h2 class="cr-sec-title">履约记录</h2>
          </div>
        </header>

        <div class="cr-records">
          <div class="cr-score-block">
            <p class="cr-muted-label">综合评分</p>
            <div class="cr-score-row">
              <p class="cr-score">{{ stats?.avg_rating_wsh ? Number(stats.avg_rating_wsh).toFixed(1) : '—' }}</p>
              <span v-if="stats?.avg_rating_wsh" class="cr-stars" aria-hidden="true">
                <span v-for="n in 5" :key="n" :class="{ on: n <= Math.round(Number(stats.avg_rating_wsh)) }" class="cr-star">★</span>
              </span>
            </div>
            <p class="cr-score-note">基于 {{ stats?.total_ratings_wsh ?? 0 }} 条真实订单评价计算。</p>
          </div>
          <dl class="cr-figures">
            <div class="cr-figure">
              <dt>完成订单</dt>
              <dd class="tabular">{{ stats?.total_completed_wsh ?? '—' }}</dd>
              <p>已结束的服务</p>
            </div>
            <div class="cr-figure">
              <dt>完成率</dt>
              <dd class="tabular">{{ percentText(stats?.completion_rate_wsh) }}</dd>
              <p>越高越可靠</p>
            </div>
            <div class="cr-figure">
              <dt>投诉率</dt>
              <dd class="tabular">{{ percentText(stats?.complaint_rate_wsh) }}</dd>
              <p>越低越好</p>
            </div>
            <div class="cr-figure">
              <dt>累计打赏</dt>
              <dd class="tabular">{{ stats?.total_tips_wsh !== undefined ? `¥${money(stats.total_tips_wsh)}` : '—' }}</dd>
              <p>用户自愿给出</p>
            </div>
          </dl>
        </div>
      </section>

      <!-- ═══ 03 · 评价 ═══ -->
      <section v-if="queried" class="cr-section" aria-label="评价记录">
        <header class="cr-sec-head">
          <div class="cr-head-copy">
            <p class="cr-eyebrow cr-sec-eyebrow">
              <span class="cr-idx">03</span>
              <span class="cr-line" aria-hidden="true"></span>
              <span>评价</span>
            </p>
            <h2 class="cr-sec-title">评价记录</h2>
            <p class="cr-sec-desc">{{ ratings.length }} 条公开评价。</p>
          </div>
        </header>

        <div v-if="!ratings.length" class="cr-empty quiet">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </svg>
          <h3>暂无公开评价</h3>
          <p>这个对象还没有收到评价，或评价尚未公开。</p>
        </div>

        <ul v-else class="cr-list">
          <li v-for="r in ratings" :key="r.id_wsh" class="cr-review">
            <div class="cr-review-top">
              <span class="cr-stars" aria-label="评分">
                <span v-for="n in 5" :key="n" :class="{ on: n <= Math.round(Number(r.score_wsh ?? 0)) }" class="cr-star">★</span>
              </span>
              <span class="cr-review-date">{{ dateText(r.created_at_wsh) }}</span>
            </div>
            <p class="cr-review-content">{{ r.content_wsh || '用户没有留下文字评价。' }}</p>
          </li>
        </ul>
      </section>

      <!-- ═══ 我的投诉 ═══ -->
      <section class="cr-section" aria-label="我的投诉记录">
        <header class="cr-sec-head">
          <div class="cr-head-copy">
            <p class="cr-eyebrow cr-sec-eyebrow">
              <span class="cr-idx">{{ queried ? '04' : '02' }}</span>
              <span class="cr-line" aria-hidden="true"></span>
              <span>我的投诉</span>
            </p>
            <h2 class="cr-sec-title">我的投诉记录</h2>
            <p class="cr-sec-desc">你提交过的投诉及平台处理结果。</p>
          </div>
        </header>

        <div v-if="!complaintsLoading && !complaints.length" class="cr-empty">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </svg>
          <h3>没有投诉记录</h3>
          <p>你还没有提交过投诉。如果服务出现问题，可以在订单详情中发起。</p>
          <router-link to="/orders" class="cta cta-outline">查看订单</router-link>
        </div>

        <ul v-else-if="complaints.length" class="cr-list">
          <li v-for="c in complaints" :key="c.id_wsh" class="cr-complaint">
            <div class="cr-complaint-main">
              <router-link :to="`/complaints/${c.id_wsh}`" class="cr-complaint-title">{{ c.title_wsh || `投诉 #${c.id_wsh}` }}</router-link>
              <p v-if="c.content_wsh" class="cr-complaint-desc">{{ c.content_wsh }}</p>
              <p v-if="c.result_wsh" class="cr-complaint-result">处理结果：{{ c.result_wsh }}</p>
            </div>
            <div class="cr-complaint-side">
              <span :class="['badge', complaintBadge(c.status_wsh)]">{{ complaintLabel(c.status_wsh) }}</span>
              <span class="cr-complaint-date">{{ dateText(c.created_at_wsh) }}</span>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRatings } from '@/api/rating'
import { getReputationStatistics } from '@/api/statistics'
import { getMyComplaints } from '@/api/complaint'
import { ComplaintStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'

const stats = ref(null)
const ratings = ref([])
const complaints = ref([])
const ratingsLoading = ref(false)
const complaintsLoading = ref(true)
const querying = ref(false)
const queried = ref(false)
const targetType = ref('keeper')
const targetId = ref('')
const error = ref('')

async function query() {
  if (!targetId.value) {
    error.value = '请输入对象编号'
    return
  }
  error.value = ''
  querying.value = true
  ratingsLoading.value = true
  try {
    const [ratingRes, statRes] = await Promise.all([
      getRatings({ targetId: targetId.value, targetType: targetType.value }),
      getReputationStatistics({ targetId: targetId.value, targetType: targetType.value }),
    ])
    if (ratingRes.code === 200) ratings.value = ratingRes.data || []
    if (statRes.code === 200) stats.value = statRes.data
    queried.value = true
  } catch (e) {
    error.value = '查询失败，请稍后再试'
  } finally {
    querying.value = false
    ratingsLoading.value = false
  }
}

function percentText(value) {
  if (value === null || value === undefined) return '—'
  const num = Number(value)
  return Number.isNaN(num) ? '—' : `${Math.round(num)}%`
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function dateText(value) {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleDateString('zh-CN')
  } catch (e) {
    return String(value)
  }
}

function complaintBadge(status) {
  return getStatusBadge(ComplaintStatus, status)
}
function complaintLabel(status) {
  return getStatusLabel(ComplaintStatus, status)
}

onMounted(async () => {
  try {
    const r = await getMyComplaints()
    if (r.code === 200) complaints.value = r.data || []
  } catch (e) {}
  finally { complaintsLoading.value = false }
})
</script>

<style scoped>
.cr-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.cr-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.cr-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.cr-crumb-link { color: var(--ref-muted); text-decoration: none; }
.cr-crumb-link:hover { color: var(--ref-ink); }
.cr-crumb-sep { color: var(--ref-line); }
.cr-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.cr-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.cr-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.cr-idx { font-variant-numeric: tabular-nums; }
.cr-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.cr-head { padding: 40px 0 8px; }
.cr-head-copy { min-width: 0; }
.cr-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.cr-sub {
  margin: 14px 0 0;
  max-width: 660px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}

/* ═══ Section ═══ */
.cr-section { margin-top: 52px; }
.cr-sec-head { padding-bottom: 20px; }
.cr-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.cr-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.cr-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Panel ═══ */
.cr-panel {
  max-width: 640px;
  padding: 22px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
}
.cr-panel fieldset { margin: 0; padding: 0; border: 0; }
.cr-label { margin: 0; font-size: 13px; font-weight: 500; color: var(--ref-ink-soft); }
.cr-label-field { margin-bottom: 10px; }
.cr-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.cr-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.cr-chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: var(--ref-sand); }
.cr-chip.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }

.cr-query { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; margin-top: 20px; }
.cr-field { flex: 1; min-width: 200px; display: grid; gap: 8px; }
.cr-input {
  height: var(--control-height);
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.cr-input::placeholder { color: color-mix(in srgb, var(--ref-muted) 70%, transparent); }
.cr-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.cr-input.has-error { border-color: var(--color-danger, #ef4444); }
.cr-error { margin: 0; font-size: 12px; color: var(--color-danger, #ef4444); }
.cr-hint { margin: 0; font-size: 12px; color: var(--ref-muted); }
.cr-note {
  margin: 20px 0 0;
  padding: 12px 16px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--ref-cream) 50%, transparent);
  font-size: 12px;
  line-height: 1.7;
  color: var(--ref-muted);
}

/* ═══ Records ═══ */
.cr-records {
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
}
.cr-score-block { padding: 22px 24px; border-bottom: 1px solid var(--ref-line); }
.cr-muted-label { margin: 0; font-size: 10px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.cr-score-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 14px; margin-top: 10px; }
.cr-score {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 5vw, 46px);
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.cr-score-note { margin: 12px 0 0; font-size: 12.5px; color: var(--ref-muted); }
.cr-figures {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}
.cr-figure {
  padding: 18px 20px;
  border-right: 1px solid var(--ref-line);
  border-bottom: 1px solid var(--ref-line);
}
.cr-figure:nth-child(even) { border-right: 0; }
.cr-figure:nth-last-child(-n+2) { border-bottom: 0; }
.cr-figure dt { margin: 0; font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--ref-muted); }
.cr-figure dd {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.cr-figure p { margin: 8px 0 0; font-size: 11px; color: var(--ref-muted); }

/* ═══ Stars ═══ */
.cr-stars { display: inline-flex; gap: 2px; }
.cr-star { color: var(--ref-line); font-size: 16px; line-height: 1; }
.cr-star.on { color: #f6b100; }

/* ═══ List ═══ */
.cr-list {
  margin: 0;
  padding: 0;
  list-style: none;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
}
.cr-review { padding: 18px 20px; border-bottom: 1px solid var(--ref-line); }
.cr-review:last-child { border-bottom: 0; }
.cr-review-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.cr-review-date { font-size: 11px; color: var(--ref-muted); }
.cr-review-content { margin: 12px 0 0; font-size: 13.5px; line-height: 1.7; color: var(--ref-ink-soft); }

/* ═══ Complaint row ═══ */
.cr-complaint {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--ref-line);
}
.cr-complaint:last-child { border-bottom: 0; }
.cr-complaint-main { min-width: 0; flex: 1; }
.cr-complaint-title {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ref-ink);
  text-decoration: underline;
  text-decoration-color: var(--ref-line);
  text-underline-offset: 4px;
  transition: text-decoration-color 0.15s;
}
.cr-complaint-title:hover { text-decoration-color: var(--ref-brand); }
.cr-complaint-desc {
  margin: 6px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.cr-complaint-result { margin: 8px 0 0; font-size: 12px; line-height: 1.6; color: var(--ref-ink-soft); }
.cr-complaint-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.cr-complaint-date { font-size: 11px; color: var(--ref-muted); }

/* ═══ Empty ═══ */
.cr-empty {
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  text-align: center;
}
.cr-empty.quiet { border-style: solid; }
.cr-empty svg { color: var(--ref-brand); }
.cr-empty h3 { margin: 16px 0 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.cr-empty p { margin: 10px auto 0; max-width: 420px; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.cr-empty .cta { margin-top: 22px; }

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

/* ═══ Responsive ═══ */
@media (min-width: 760px) {
  .cr-figures { grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .cr-figure { border-right: 1px solid var(--ref-line); border-bottom: 0; }
  .cr-figure:last-child { border-right: 0; }
}
@media (max-width: 520px) {
  .cr-shell { padding: 0 16px; }
  .cr-head { padding: 30px 0 4px; }
  .cr-sub { font-size: 13.5px; }
  .cr-section { margin-top: 40px; }
  .cr-panel { padding: 18px; }
  .cr-score-block { padding: 18px; }
  .cr-figure { padding: 16px; }
}
</style>