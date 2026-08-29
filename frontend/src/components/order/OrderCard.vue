<template>
  <article class="oc-card">
    <button type="button" class="oc-main" :aria-label="`查看订单 ${serviceName} 详情`" @click="$emit('viewDetail', order)">
      <div class="oc-row">
        <div class="oc-cover-wrap">
          <MediaWithFallback :src="cover" :alt="serviceName" class="oc-cover" :placeholder="''" fallback-label="无图" />
          <span class="oc-pet-avatar">
            <img v-if="order.pet_avatar_wsh" :src="order.pet_avatar_wsh" :alt="petName" loading="lazy" decoding="async">
            <span v-else class="oc-pet-letter">{{ petName.charAt(0) }}</span>
          </span>
        </div>

        <div class="oc-info">
          <div class="oc-info-top">
            <p class="oc-order-no">{{ order.order_no_wsh || `#${order.id_wsh}` }}</p>
            <div class="oc-top-tags">
              <span v-if="isCompleted && !order.has_feedback_wsh" class="oc-tag-feedback">待反馈</span>
              <span class="oc-badge" :class="statusToneClass(meta.tone)">{{ meta.label }}</span>
            </div>
          </div>

          <h3 class="oc-service">{{ serviceName }}</h3>

          <div class="oc-meta">
            <span class="oc-meta-item">
              {{ petName }}
              <span v-if="petSummaryText" class="oc-muted"> · {{ petSummaryText }}</span>
            </span>
            <span class="oc-meta-item oc-tabular">
              {{ formatDayRange(order.start_date_wsh, order.end_date_wsh) }}
              <span v-if="nights > 0" class="oc-muted">· {{ nights }} 晚</span>
            </span>
          </div>

          <div class="oc-meta oc-muted">
            <span class="oc-meta-item oc-truncate">{{ merchantName }}</span>
            <span v-if="order.keeper_name_wsh" class="oc-meta-item oc-truncate">照护师 {{ order.keeper_name_wsh }}</span>
          </div>

          <div class="oc-amount-mobile">
            <p class="oc-amount-label">实付金额</p>
            <p class="oc-amount-row"><span class="oc-yen">¥</span><span class="oc-amount">{{ formatMoney(amount) }}</span></p>
          </div>
        </div>

        <div class="oc-amount-col">
          <p class="oc-amount-label">实付金额</p>
          <p class="oc-amount-row"><span class="oc-yen">¥</span><span class="oc-amount">{{ formatMoney(amount) }}</span></p>
          <p class="oc-amount-sub">
            <span v-if="billing">{{ billing }}</span>
            <span v-if="discount > 0" class="oc-discount">已优惠 ¥{{ formatMoney(discount) }}</span>
          </p>
          <span class="oc-view-detail">查看详情 <span class="oc-arrow">↗</span></span>
        </div>
      </div>
    </button>

    <div v-if="showPaymentCountdown" class="oc-countdown">
      <span class="oc-countdown-icon">🕐</span>
      <p v-if="paymentRemaining <= 0" class="oc-countdown-text">支付已超时，订单即将自动取消。</p>
      <template v-else>
        <p class="oc-countdown-text">
          请在 <span class="oc-countdown-num">{{ paymentCountdownText }}</span> 内完成支付
        </p>
        <span class="oc-countdown-note">超时后订单将自动取消，档期释放给其他家长</span>
      </template>
    </div>

    <div class="oc-progress-wrap">
      <ol v-if="phaseIndex >= 0" class="oc-progress" aria-label="服务进度">
        <li v-for="(phase, index) in PHASES" :key="phase.key" class="oc-phase">
          <div class="oc-phase-head">
            <span class="oc-dot" :class="{ active: index === phaseIndex, done: index < phaseIndex }" aria-hidden="true" />
            <span v-if="index < PHASES.length - 1" class="oc-track" aria-hidden="true">
              <span class="oc-track-fill" :class="{ filled: index < phaseIndex }" />
            </span>
          </div>
          <p class="oc-phase-label" :class="{ active: index === phaseIndex, on: index <= phaseIndex }">{{ phase.label }}</p>
          <p class="oc-phase-stamp">{{ phaseStamp(phase) }}</p>
        </li>
      </ol>
      <div v-else class="oc-cancelled">
        <span class="oc-cancelled-icon">⛔</span>
        <span>该订单已{{ meta.label === '退款中' ? '进入退款流程' : '关闭' }}，服务流程未继续。</span>
      </div>
    </div>

    <footer class="oc-actions">
      <template v-if="order.status_wsh === 'pending'">
        <button class="oc-btn oc-btn-danger" type="button" :disabled="processing" @click="$emit('cancel', order)">取消订单</button>
        <button class="oc-btn oc-btn-primary" type="button" :disabled="processing || processingAny" @click="$emit('pay', 'balance')">余额支付</button>
      </template>

      <template v-else-if="order.status_wsh === 'paid'">
        <span class="oc-note oc-note-left">门店确认中，通常 30 分钟内回复</span>
        <button class="oc-btn" type="button" @click="$emit('viewDetail', order)">订单详情</button>
      </template>

      <template v-else-if="order.status_wsh === 'confirmed'">
        <button class="oc-btn" type="button" @click="$emit('viewDetail', order)">订单详情</button>
        <button class="oc-btn oc-btn-primary" type="button" :disabled="processing" @click="$emit('deliver', order)">确认已送达</button>
      </template>

      <template v-else-if="isActive">
        <a v-if="contactPhone" class="oc-btn oc-btn-quiet" :href="`tel:${contactPhone}`">联系门店</a>
        <button class="oc-btn oc-btn-primary" type="button" @click="$emit('viewDetail', order)">查看服务动态</button>
      </template>

      <template v-else-if="isCompleted">
        <button class="oc-btn oc-btn-quiet" type="button" @click="rebook">再次预约</button>
        <button class="oc-btn" type="button" :disabled="processing" @click="$emit('tip', order)">打赏照护师</button>
        <button v-if="!order.has_feedback_wsh" class="oc-btn oc-btn-primary" type="button" :disabled="processing" @click="$emit('review', order)">评价服务</button>
        <button v-else class="oc-btn" type="button" @click="$emit('viewDetail', order)">订单详情</button>
      </template>

      <template v-else-if="['cancelled', 'refunded', 'refunding'].includes(order.status_wsh)">
        <button class="oc-btn oc-btn-quiet" type="button" @click="$emit('viewDetail', order)">订单详情</button>
        <button class="oc-btn" type="button" @click="rebook">再次预约</button>
      </template>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import { formatPaymentTimeoutRemaining, getPaymentTimeoutRemaining } from '@/utils/orderPaymentTimeout'
import { billingText } from '@/domain/BookingUnit'

const props = defineProps({
  order: { type: Object, required: true },
  nowMs: { type: Number, default: () => Date.now() },
  processing: { type: Boolean, default: false },
})
defineEmits(['cancel', 'pay', 'deliver', 'review', 'tip', 'viewDetail'])

const router = useRouter()

/* ── 状态与语气 ── */
const STATUS_META = {
  pending: { label: '待付款', tone: 'action' },
  paid: { label: '已支付', tone: 'queued' },
  confirmed: { label: '待送达', tone: 'queued' },
  delivered: { label: '已送达', tone: 'active' },
  received: { label: '已接收', tone: 'active' },
  in_progress: { label: '服务中', tone: 'active' },
  completed: { label: '已完成', tone: 'done' },
  cancelled: { label: '已取消', tone: 'closed' },
  refunding: { label: '退款中', tone: 'action' },
  refunded: { label: '已退款', tone: 'closed' },
}
const PHASE_INDEX = { pending: 0, paid: 1, confirmed: 1, delivered: 2, received: 2, in_progress: 2, completed: 3, cancelled: -1, refunding: -1, refunded: -1 }
const PHASES = [
  { key: 'placed', label: '已下单', timeField: 'created_at_wsh' },
  { key: 'paid', label: '已支付', timeField: 'delivery_time_wsh' },
  { key: 'staying', label: '寄养中', timeField: 'delivered_at_wsh' },
  { key: 'done', label: '已完成', timeField: 'received_at_wsh' },
]

const status = computed(() => props.order.status_wsh || '')
const meta = computed(() => STATUS_META[status.value] || { label: status.value || '未知状态', tone: 'closed' })
const phaseIndex = computed(() => PHASE_INDEX[status.value] ?? -1)
const isActive = computed(() => ['delivered', 'received', 'in_progress'].includes(status.value))
const isCompleted = computed(() => status.value === 'completed')

function statusToneClass(tone) {
  return {
    action: 'oc-tone-action',
    queued: 'oc-tone-queued',
    active: 'oc-tone-active',
    done: 'oc-tone-done',
    closed: 'oc-tone-closed',
  }[tone] || 'oc-tone-closed'
}

/* ── 展示字段 ── */
const serviceName = computed(() => props.order.service_name_wsh || '宠物寄养服务')
const petName = computed(() => props.order.pet_name_wsh || `宠物 #${props.order.pet_id_wsh ?? '-'}`)
const merchantName = computed(() => props.order.merchant_name_wsh || `商家 #${props.order.merchant_id_wsh ?? '-'}`)
const amount = computed(() => Number(props.order.final_amount_wsh || props.order.total_amount_wsh || props.order.settlement_amount_wsh || 0))
const discount = computed(() => Number(props.order.discount_wsh ?? 0))
const billing = computed(() => billingText(props.order))
const nights = computed(() => nightsBetween(props.order.start_date_wsh, props.order.end_date_wsh))
const contactPhone = computed(() => props.order.merchant_phone_wsh || props.order.contact_phone_wsh || '')

const petSummaryText = computed(() => {
  const parts = [
    props.order.pet_breed_wsh,
    props.order.pet_age_wsh ? `${props.order.pet_age_wsh} 岁` : '',
    props.order.pet_weight_wsh ? `${props.order.pet_weight_wsh} kg` : '',
  ]
  return parts.filter(Boolean).join(' · ') || '宠物信息待完善'
})

const cover = computed(() => {
  const images = parseImages(props.order?.service_images_wsh)
  return images[0] || props.order?.start_photo_wsh || ''
})

function parseImages(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean)
  if (typeof raw !== 'string') return []
  const trimmed = raw.trim()
  if (!trimmed) return []
  try {
    const parsed = JSON.parse(trimmed)
    if (Array.isArray(parsed)) return parsed.filter(Boolean)
    if (typeof parsed === 'string') return [parsed]
    return []
  } catch { /* fall through */ }
  return trimmed.split(',').map(s => s.trim()).filter(Boolean)
}

/* ── 支付倒计时 ── */
const paymentRemaining = computed(() => props.order.status_wsh === 'pending' ? getPaymentTimeoutRemaining(props.order, props.nowMs) : null)
const showPaymentCountdown = computed(() => paymentRemaining.value != null)
const paymentCountdownText = computed(() => formatPaymentTimeoutRemaining(paymentRemaining.value))

function rebook() {
  if (props.order.service_id_wsh) {
    router.push({ path: `/services/${props.order.service_id_wsh}`, query: { book: 1 } })
  } else {
    router.push('/services')
  }
}

/* ── 工具 ── */
function formatMoney(value) {
  const n = Number(value || 0)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '0.00'
}
function formatDayRange(start, end) {
  const compact = (value) => {
    const day = value ? String(value).slice(0, 10) : ''
    return day ? day.slice(5).replace('-', '.') : '-'
  }
  const s = compact(start)
  const e = compact(end)
  if (s === '-' && e === '-') return '-'
  return `${s} — ${e}`
}
function nightsBetween(start, end) {
  if (!start || !end) return 0
  const a = new Date(start).getTime()
  const b = new Date(end).getTime()
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0
  return Math.round((b - a) / 86400000)
}
function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
const processingAny = computed(() => props.processing)
function phaseStamp(phase) {
  const stamp = props.order?.[phase.timeField]
  return stamp ? formatDateTime(stamp) : '—'
}
</script>

<style scoped>
.oc-card {
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  overflow: hidden;
  transition: transform 200ms ease, box-shadow 200ms ease, border-color 200ms ease;
}
.oc-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 28px 60px -40px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}

.oc-main {
  display: block;
  width: 100%;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 0;
}

.oc-row {
  display: flex;
  gap: 20px;
  padding: 20px;
}

/* 封面 + 宠物头像角标 */
.oc-cover-wrap { position: relative; flex-shrink: 0; align-self: flex-start; }
.oc-cover {
  width: 92px;
  height: 92px;
  border-radius: 14px;
  overflow: hidden;
}
.oc-pet-avatar {
  position: absolute;
  right: -9px;
  bottom: -9px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--ref-surface);
  background: var(--ref-sand);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.oc-pet-avatar img { width: 100%; height: 100%; object-fit: cover; }
.oc-pet-letter { font-family: var(--ref-font-display); font-size: 13px; color: var(--ref-brand); }

/* 信息区 */
.oc-info { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.oc-info-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.oc-order-no {
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  color: color-mix(in srgb, var(--ref-muted) 82%, transparent);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.oc-top-tags { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.oc-tag-feedback {
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 25%, transparent);
  background: color-mix(in srgb, var(--ref-brand) 6%, transparent);
  color: var(--ref-brand-deep);
  font-size: 11px;
  line-height: 1;
  padding: 5px 9px;
  white-space: nowrap;
}

/* 状态徽章 */
.oc-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}
.oc-tone-action { background: var(--ref-brand); color: #fff; border-color: transparent; }
.oc-tone-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent); }
.oc-tone-active { background: color-mix(in srgb, #3f5347 12%, transparent); color: #3f5347; border-color: color-mix(in srgb, #3f5347 25%, transparent); }
.oc-tone-done { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); border-color: transparent; }
.oc-tone-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }

.oc-service {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 19px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.oc-card:hover .oc-service { color: var(--ref-brand); }

.oc-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 16px;
  margin-top: 9px;
  font-size: 12.5px;
  color: var(--ref-ink-soft);
}
.oc-meta.oc-muted { color: var(--ref-muted); }
.oc-meta-item { display: inline-flex; align-items: center; min-width: 0; }
.oc-muted { color: var(--ref-muted); }
.oc-tabular { font-variant-numeric: tabular-nums; }
.oc-truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 金额列 */
.oc-amount-col {
  display: flex;
  width: 180px;
  flex-shrink: 0;
  flex-direction: column;
  align-items: flex-start;
  border-left: 1px solid var(--ref-line);
  padding-left: 20px;
}
.oc-amount-label { font-size: 12px; color: var(--ref-muted); }
.oc-amount-row { display: flex; align-items: baseline; gap: 3px; margin-top: 6px; }
.oc-yen { font-size: 12px; color: var(--ref-muted); }
.oc-amount {
  font-family: var(--ref-font-display);
  font-size: 27px;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.oc-amount-sub {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 3px 10px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ref-muted);
}
.oc-discount { color: var(--ref-brand-deep); }
.oc-view-detail {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: auto;
  padding-top: 14px;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.oc-arrow { display: inline-block; }
.oc-card:hover .oc-arrow { transform: translate(2px, -2px); }

/* 移动端金额 */
.oc-amount-mobile { display: none; }

/* 倒计时 */
.oc-countdown {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 10px;
  border-top: 1px solid color-mix(in srgb, var(--ref-brand) 15%, transparent);
  background: color-mix(in srgb, var(--ref-brand) 5%, transparent);
  padding: 12px 20px;
}
.oc-countdown-icon { color: var(--ref-brand-deep); font-size: 14px; }
.oc-countdown-text { font-size: 12.5px; color: var(--ref-brand-deep); }
.oc-countdown-num { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 15px; font-weight: 600; margin: 0 3px; font-variant-numeric: tabular-nums; }
.oc-countdown-note { font-size: 12px; color: var(--ref-muted); }

/* 进度条 */
.oc-progress-wrap {
  border-top: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 45%, var(--ref-surface));
  padding: 16px 20px;
}
.oc-progress { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin: 0; padding: 0; list-style: none; }
.oc-phase { min-width: 0; }
.oc-phase-head { display: flex; align-items: center; gap: 6px; }
.oc-dot { width: 8px; height: 8px; flex-shrink: 0; border-radius: 50%; background: var(--ref-line); }
.oc-dot.active { background: var(--ref-brand); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 18%, transparent); }
.oc-dot.done { background: var(--ref-brand); }
.oc-track { position: relative; height: 1px; flex: 1; overflow: hidden; background: var(--ref-line); }
.oc-track-fill { position: absolute; inset: 0; transform-origin: left; background: var(--ref-brand); transform: scaleX(0); }
.oc-track-fill.filled { transform: scaleX(1); }
.oc-phase-label { margin: 8px 0 0; font-size: 12.5px; line-height: 1; color: var(--ref-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.oc-phase-label.active { font-weight: 600; color: var(--ref-ink); }
.oc-phase-label.on { color: var(--ref-ink-soft); }
.oc-phase-stamp { margin: 4px 0 0; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.oc-cancelled { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--ref-muted); }
.oc-cancelled-icon { font-size: 14px; }

/* 操作区 */
.oc-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--ref-line);
  padding: 14px 20px;
}
.oc-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease, transform 150ms ease, opacity 150ms ease;
}
.oc-btn:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); transform: translateY(-1px); }
.oc-btn:disabled { opacity: 0.45; cursor: not-allowed; transform: none; }
.oc-btn-primary { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.oc-btn-primary:hover { background: var(--ref-brand-deep); border-color: var(--ref-brand-deep); color: #fff; }
.oc-btn-danger { background: transparent; border-color: color-mix(in srgb, #a03422 35%, transparent); color: #a03422; }
.oc-btn-danger:hover { background: color-mix(in srgb, #a03422 9%, transparent); border-color: color-mix(in srgb, #a03422 45%, transparent); color: #a03422; }
.oc-btn-quiet { background: transparent; border-color: transparent; }
.oc-btn-quiet:hover { background: var(--ref-sand); border-color: transparent; color: var(--ref-ink); }
.oc-note { font-size: 12.5px; color: var(--ref-muted); }
.oc-note-left { margin-right: auto; }

@media (max-width: 900px) {
  .oc-amount-col { display: none; }
  .oc-amount-mobile { display: block; margin-top: 12px; }
  .oc-amount-mobile .oc-amount-row { margin-top: 3px; }
}
</style>