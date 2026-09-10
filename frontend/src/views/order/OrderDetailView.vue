<template>
  <div class="od-detail">
    <div class="od-shell">
      <!-- Loading -->
      <template v-if="loading">
        <div class="od-skel-bar" />
        <div class="od-skel-block" />
        <div class="od-skel-block tall" />
      </template>

      <!-- Not found -->
      <div v-else-if="!order" class="od-state">
        <div class="od-state-icon">?</div>
        <h3>订单未找到</h3>
        <p>{{ error || '这笔订单可能已被删除，或者你没有查看权限。' }}</p>
        <button class="od-btn od-btn-primary" type="button" @click="init">重新加载</button>
      </div>

      <!-- Detail -->
      <template v-else>
        <nav class="od-crumb" aria-label="面包屑">
          <router-link to="/dashboard" class="od-crumb-link">首页</router-link>
          <span class="od-crumb-sep" aria-hidden="true">›</span>
          <router-link to="/orders" class="od-crumb-link">订单中心</router-link>
          <span class="od-crumb-sep" aria-hidden="true">›</span>
          <span class="od-crumb-here">订单详情</span>
        </nav>

        <!-- PageHeader -->
        <header class="od-head">
          <div class="od-head-main">
            <p class="od-eyebrow" aria-hidden="true">
              <span class="od-eyebrow-word">订单详情</span>
              <span class="od-eyebrow-line" />
              <span>订单详情</span>
            </p>
            <h1 class="od-title">{{ order.service_name_wsh || '寄养订单' }}</h1>
            <p v-if="order.merchant_name_wsh" class="od-lede">由 {{ order.merchant_name_wsh }} 承接</p>
          </div>
          <div class="od-head-actions">
            <button class="od-btn od-btn-quiet" type="button" @click="goList">← 返回列表</button>
            <span class="od-badge" :class="statusToneClass(meta.tone)">{{ meta.label }}</span>
          </div>
        </header>

        <dl class="od-facts">
          <div class="od-fact">
            <dt>实付金额</dt>
            <dd class="od-tabular od-fact-amount">
              <span class="od-fact-yen">¥</span>{{ formatMoney(finalAmount) }}
            </dd>
          </div>
          <div class="od-fact"><dt>宠物</dt><dd>{{ order.pet_name_wsh || '—' }}</dd></div>
          <div class="od-fact"><dt>寄养师</dt><dd>{{ order.keeper_name_wsh || '待分配' }}</dd></div>
          <div class="od-fact"><dt>服务周期</dt><dd class="od-tabular">{{ [order.start_date_wsh, order.end_date_wsh].filter(Boolean).join(' → ') || '—' }}</dd></div>
        </dl>

        <div class="od-orderno">
          <span class="od-orderno-value">订单号 {{ order.order_no_wsh || '—' }}</span>
          <button v-if="order.order_no_wsh" type="button" class="od-copy" @click="copyOrderNo">
            <span class="od-copy-glyph" aria-hidden="true">{{ copied ? '✓' : '⧉' }}</span>
            {{ copied ? '已复制' : '复制' }}
          </button>
        </div>

        <!-- 01 订单进度 -->
        <section class="od-section">
          <div class="od-sec-head"><h2 class="od-sec-title">订单进度 <span class="od-sec-en">进度</span></h2></div>
          <div class="od-panel">
            <ol v-if="phaseIndex >= 0" class="od-progress" aria-label="服务进度">
              <li v-for="(phase, index) in PHASES" :key="phase.key" class="od-phase">
                <div class="od-phase-head">
                  <span class="od-dot" :class="{ active: index === phaseIndex, done: index < phaseIndex }" aria-hidden="true" />
                  <span v-if="index < PHASES.length - 1" class="od-track" aria-hidden="true">
                    <span class="od-track-fill" :class="{ filled: index < phaseIndex }" />
                  </span>
                </div>
                <p class="od-phase-label" :class="{ active: index === phaseIndex, on: index <= phaseIndex }">{{ phase.label }}</p>
                <p class="od-phase-stamp">{{ phaseStamp(phase) }}</p>
              </li>
            </ol>
            <div v-else class="od-cancelled">
              <span class="od-cancelled-icon" aria-hidden="true">⛔</span>
              <span>该订单已{{ meta.label === '退款中' ? '进入退款流程' : '关闭' }}，服务流程未继续。</span>
            </div>

            <dl class="od-facts od-facts-inner">
              <div class="od-fact"><dt>下单时间</dt><dd class="od-tabular">{{ dateTime(order.created_at_wsh) }}</dd></div>
              <div class="od-fact">
                <dt>计划送达 / 实际</dt>
                <dd class="od-tabular">
                  {{ dateTime(order.delivery_time_wsh) || '—' }}
                  <span class="od-sep">/</span>
                  <span :class="order.delivered_at_wsh ? 'od-ink' : 'od-muted'">{{ dateTime(order.delivered_at_wsh) || '待送达' }}</span>
                </dd>
              </div>
              <div class="od-fact">
                <dt>计划接回 / 实际</dt>
                <dd class="od-tabular">
                  {{ dateTime(order.pickup_time_wsh) || '—' }}
                  <span class="od-sep">/</span>
                  <span :class="order.received_at_wsh ? 'od-ink' : 'od-muted'">{{ dateTime(order.received_at_wsh) || '待接回' }}</span>
                </dd>
              </div>
              <div class="od-fact">
                <dt>计费</dt>
                <dd>{{ order.quantity_wsh ? `${order.quantity_wsh} ${unitLabel(order.billing_unit_wsh)}` : order.days_wsh ? `${order.days_wsh} 天` : '—' }}</dd>
              </div>
            </dl>
          </div>
        </section>

        <!-- 02 服务与宠物 -->
        <section class="od-section">
          <div class="od-sec-head"><h2 class="od-sec-title">服务与宠物 <span class="od-sec-en">Service &amp; Pet</span></h2></div>
          <div class="od-two-col">
            <article class="od-card">
              <MediaWithFallback :src="cover" :alt="order.service_name_wsh || '服务图片'" :placeholder="''" fallback-label="暂无图片" class="od-cover" />
              <div class="od-card-body">
                <h3 class="od-card-title">{{ order.service_name_wsh || '寄养服务' }}</h3>
                <p v-if="order.remark_wsh" class="od-remark">备注：{{ order.remark_wsh }}</p>
                <dl class="od-facts od-facts-inner">
                  <div class="od-fact">
                    <dt>门店</dt>
                    <dd>
                      <router-link v-if="order.merchant_id_wsh" :to="{ name: 'MerchantDetail', params: { id: order.merchant_id_wsh } }" class="od-link">{{ order.merchant_name_wsh || '查看门店' }}</router-link>
                      <template v-else>{{ order.merchant_name_wsh || '—' }}</template>
                    </dd>
                  </div>
                  <div class="od-fact">
                    <dt>寄养师</dt>
                    <dd>
                      <router-link v-if="order.keeper_id_wsh" :to="{ name: 'KeeperDetail', params: { id: order.keeper_id_wsh } }" class="od-link">{{ order.keeper_name_wsh || '查看寄养师' }}</router-link>
                      <template v-else>{{ order.keeper_name_wsh || '待分配' }}</template>
                    </dd>
                  </div>
                  <div class="od-fact">
                    <dt>地址</dt>
                    <dd><span class="od-inline"><span class="od-pin" aria-hidden="true">◉</span>{{ order.delivery_address_wsh || order.merchant_address_wsh || '—' }}</span></dd>
                  </div>
                </dl>
              </div>
            </article>

            <div class="od-card">
              <div class="od-pet-head">
                <img v-if="order.pet_avatar_wsh" :src="order.pet_avatar_wsh" :alt="order.pet_name_wsh || '宠物'" class="od-pet-avatar" loading="lazy" decoding="async">
                <span v-else class="od-pet-avatar od-pet-avatar-fallback">{{ petInitial }}</span>
                <div class="od-pet-meta">
                  <p class="od-pet-name">{{ order.pet_name_wsh || '未命名宠物' }}</p>
                  <p class="od-pet-sub">{{ petSummary || '暂无档案信息' }}</p>
                </div>
              </div>
              <dl class="od-facts od-pet-facts">
                <div class="od-fact">
                  <dt>紧急联系人</dt>
                  <dd>
                    <span v-if="order.emergency_contact_name_wsh" class="od-inline">
                      <span class="od-phone-glyph" aria-hidden="true">☏</span>
                      <span>{{ order.emergency_contact_name_wsh }}{{ order.emergency_contact_phone_wsh ? ` · ${order.emergency_contact_phone_wsh}` : '' }}</span>
                    </span>
                    <template v-else>未填写</template>
                  </dd>
                </div>
                <div class="od-fact"><dt>服务反馈</dt><dd>{{ order.has_feedback_wsh ? '已评价' : '未评价' }}</dd></div>
              </dl>
              <figure v-if="order.start_photo_wsh" class="od-postimg">
                <img :src="order.start_photo_wsh" alt="入住照片" class="od-postimg-img" loading="lazy" decoding="async">
                <figcaption class="od-postimg-cap">寄养师上传的入住照片</figcaption>
              </figure>
            </div>
          </div>
        </section>

        <!-- 03 费用明细 -->
        <section class="od-section">
          <div class="od-sec-head"><h2 class="od-sec-title">费用明细 <span class="od-sec-en">费用</span></h2></div>
          <div class="od-card od-card-pad">
            <dl class="od-money">
              <div class="od-money-row">
                <dt>服务金额</dt>
                <dd class="od-tabular">¥{{ formatMoney(order.total_amount_wsh) }}</dd>
              </div>
              <div v-if="discount > 0" class="od-money-row">
                <dt>优惠减免</dt>
                <dd class="od-tabular od-discount">−¥{{ formatMoney(discount) }}</dd>
              </div>
              <div class="od-money-row od-money-total">
                <dt>实付金额</dt>
                <dd class="od-tabular od-total-amount"><span class="od-total-yen">¥</span>{{ formatMoney(finalAmount) }}</dd>
              </div>
            </dl>
          </div>
        </section>

        <!-- 04 可执行操作 -->
        <section class="od-section">
          <div class="od-sec-head"><h2 class="od-sec-title">可执行操作 <span class="od-sec-en">下一步</span></h2></div>
          <div class="od-card od-card-cream">
            <div class="od-actions">
              <template v-if="order.status_wsh === 'pending'">
                <button class="od-btn od-btn-primary" type="button" :disabled="busy === 'pay'" @click="payOpen = true">
                  {{ busy === 'pay' ? '处理中…' : '立即支付' }}
                </button>
                <button class="od-btn od-btn-danger" type="button" :disabled="busy === 'cancel'" @click="cancelOpen = true">取消订单</button>
              </template>
              <template v-else-if="order.status_wsh === 'confirmed'">
                <button class="od-btn od-btn-primary" type="button" :disabled="busy === 'deliver'" @click="deliverOpen = true">
                  {{ busy === 'deliver' ? '确认中…' : '确认送达' }}
                </button>
              </template>
              <template v-else-if="order.status_wsh === 'paid'">
                <button class="od-btn od-btn-danger" type="button" :disabled="busy === 'cancel'" @click="cancelOpen = true">申请取消</button>
              </template>
              <template v-else-if="order.status_wsh === 'completed' && !order.has_feedback_wsh">
                <router-link to="/orders" class="od-btn od-btn-outline">去评价与打赏</router-link>
              </template>

              <router-link to="/chat" class="od-btn od-btn-outline">联系客服</router-link>
              <router-link to="/ai" class="od-btn od-btn-outline">查看照护报告</router-link>
              <button class="od-btn od-btn-quiet" type="button" @click="openComplaint">
                <span class="od-act-shield" aria-hidden="true">△</span>发起投诉
              </button>
            </div>
            <p class="od-actions-note">评价与打赏在订单中心内完成，避免同一操作出现两套入口。</p>
          </div>
        </section>
      </template>
    </div>

    <!-- 支付方式弹层 -->
    <div v-if="payOpen" class="od-modal" role="dialog" aria-modal="true" aria-label="选择支付方式">
      <div class="od-modal-mask" @click="payOpen = false" />
      <div class="od-modal-box">
        <h3 class="od-modal-title">选择支付方式</h3>
        <p class="od-modal-desc">本次需支付 <strong class="od-tabular">¥{{ formatMoney(finalAmount) }}</strong></p>
        <ul class="od-methods">
          <li v-for="method in PAY_METHODS" :key="method.value">
            <button type="button" class="od-method" :disabled="busy === 'pay'" @click="choosePay(method.value)">
              <span>
                <span class="od-method-label">{{ method.label }}</span>
                <span class="od-method-hint">{{ method.hint }}</span>
              </span>
              <span class="od-method-cta">选择</span>
            </button>
          </li>
        </ul>
        <button class="od-modal-close" type="button" aria-label="关闭" @click="payOpen = false">✕</button>
      </div>
    </div>

    <!-- 取消确认 -->
    <div v-if="cancelOpen" class="od-modal" role="dialog" aria-modal="true" aria-label="取消订单">
      <div class="od-modal-mask" @click="cancelOpen = false" />
      <div class="od-modal-box od-modal-box-sm">
        <h3 class="od-modal-title">取消这笔订单？</h3>
        <p class="od-modal-desc">{{ order ? '取消后订单不可恢复，如已支付将按平台退款规则原路退回。' : '' }}</p>
        <p v-if="order?.order_no_wsh" class="od-modal-detail"><span class="od-tabular">{{ order.order_no_wsh }}</span></p>
        <div class="od-modal-btns">
          <button class="od-btn" type="button" @click="cancelOpen = false">再想想</button>
          <button class="od-btn od-btn-danger" type="button" :disabled="busy === 'cancel'" @click="doCancel">{{ busy === 'cancel' ? '取消中…' : '确认取消' }}</button>
        </div>
        <button class="od-modal-close" type="button" aria-label="关闭" @click="cancelOpen = false">✕</button>
      </div>
    </div>

    <!-- 确认送达 -->
    <div v-if="deliverOpen" class="od-modal" role="dialog" aria-modal="true" aria-label="确认送达">
      <div class="od-modal-mask" @click="deliverOpen = false" />
      <div class="od-modal-box od-modal-box-sm">
        <h3 class="od-modal-title">确认宠物已送达？</h3>
        <p class="od-modal-desc">确认后订单进入照护阶段，寄养师将开始记录服务动态。</p>
        <p v-if="order?.delivery_address_wsh" class="od-modal-detail">送达地址：{{ order.delivery_address_wsh }}</p>
        <div class="od-modal-btns">
          <button class="od-btn" type="button" @click="deliverOpen = false">再想想</button>
          <button class="od-btn od-btn-primary" type="button" :disabled="busy === 'deliver'" @click="doDeliver">{{ busy === 'deliver' ? '确认中…' : '确认送达' }}</button>
        </div>
        <button class="od-modal-close" type="button" aria-label="关闭" @click="deliverOpen = false">✕</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useOrderDetailStore } from '@/stores/orderDetail'
import { confirmDelivered } from '@/api/order'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import { unitLabel } from '@/domain/BookingUnit'
import { PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS, isPaymentTimeoutExpired } from '@/utils/orderPaymentTimeout'
import { formatMoney } from '@/utils/format'

const PAY_METHODS = [
  { value: 'balance', label: '余额支付', hint: '从钱包余额直接扣款' },
  { value: 'wechat', label: '微信支付', hint: '跳转微信完成付款' },
  { value: 'alipay', label: '支付宝', hint: '跳转支付宝完成付款' },
]

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const store = useOrderDetailStore()

const orderId = ref(Number(route.params.id))
const payOpen = ref(false)
const cancelOpen = ref(false)
const deliverOpen = ref(false)
const copied = ref(false)
const busy = ref('')
let timeoutTimer = null
let nextRefreshAt = 0

const order = computed(() => store.order)

/* ── 状态 ╱ 进度 ── */
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

const meta = computed(() => STATUS_META[order.value?.status_wsh] || { label: order.value?.status_wsh || '未知状态', tone: 'closed' })
const phaseIndex = computed(() => PHASE_INDEX[order.value?.status_wsh] ?? -1)

function statusToneClass(tone) {
  return { action: 'od-tone-action', queued: 'od-tone-queued', active: 'od-tone-active', done: 'od-tone-done', closed: 'od-tone-closed' }[tone] || 'od-tone-closed'
}

/* ── 展示字段 ── */
const finalAmount = computed(() => Number(order.value?.final_amount_wsh || order.value?.total_amount_wsh || order.value?.settlement_amount_wsh || 0))
const discount = computed(() => Number(order.value?.discount_wsh ?? 0))
const cover = computed(() => parseImages(order.value?.service_images_wsh)[0] || order.value?.start_photo_wsh || '')
const petInitial = computed(() => (order.value?.pet_name_wsh || '宠').charAt(0))
const petSummary = computed(() => [
  order.value?.pet_breed_wsh,
  order.value?.pet_age_wsh ? `${order.value.pet_age_wsh} 岁` : '',
  order.value?.pet_weight_wsh ? `${order.value.pet_weight_wsh} kg` : '',
].filter(Boolean).join(' · '))

function phaseStamp(phase) {
  const stamp = order.value?.[phase.timeField]
  return stamp ? dateTime(stamp) : '—'
}

/* ── 生命周期 ── */
onMounted(() => {
  init()
  timeoutTimer = window.setInterval(() => {
    const now = Date.now()
    if (order.value?.status_wsh === 'pending' && now >= nextRefreshAt && isPaymentTimeoutExpired(order.value, now)) {
      nextRefreshAt = now + PAYMENT_TIMEOUT_REFRESH_INTERVAL_MS
      void store.fetchOrderDetail(orderId.value)
    }
  }, 1000)
})
onUnmounted(() => {
  if (timeoutTimer) window.clearInterval(timeoutTimer)
  store.$reset()
})
watch(() => route.params.id, (nextId) => {
  if (nextId) {
    orderId.value = Number(nextId)
    init()
  }
})

async function init() {
  store.$reset()
  await store.fetchOrderDetail(orderId.value)
}

/* ── 交互 ── */
function goList() { router.push('/orders') }

async function copyOrderNo() {
  if (!order.value?.order_no_wsh) return
  try {
    await navigator.clipboard.writeText(order.value.order_no_wsh)
    copied.value = true
  } catch { copied.value = false }
  window.setTimeout(() => { copied.value = false }, 1600)
}

async function choosePay(method) {
  payOpen.value = false
  if (busy.value) return
  busy.value = 'pay'
  try {
    const result = await store.pay(orderId.value, method)
    appStore.addToast(result.message || (result.success ? '支付成功' : '支付失败'), result.success ? 'success' : 'error')
    await store.fetchOrderDetail(orderId.value)
  } catch (error) {
    appStore.addToast(error?.message || '支付失败', 'error')
    await store.fetchOrderDetail(orderId.value)
  } finally {
    busy.value = ''
  }
}

async function doCancel() {
  if (busy.value) return
  busy.value = 'cancel'
  try {
    const result = await store.cancel(orderId.value)
    appStore.addToast(result.message || (result.success ? '订单已取消' : '取消失败'), result.success ? 'success' : 'error')
    cancelOpen.value = false
    await store.fetchOrderDetail(orderId.value)
  } catch (error) {
    appStore.addToast(error?.message || '取消失败', 'error')
  } finally {
    busy.value = ''
  }
}

async function doDeliver() {
  if (busy.value) return
  busy.value = 'deliver'
  let location
  try {
    location = await getCurrentAddress()
  } catch (error) {
    appStore.addToast(error?.message || '请允许定位后再确认送达', 'warning')
    busy.value = ''
    return
  }
  try {
    const res = await confirmDelivered({ order_no_wsh: order.value?.order_no_wsh, delivered_address_wsh: location.address_wsh })
    if (res.code === 200) appStore.addToast('已确认送达', 'success')
    else appStore.addToast(res.message || '确认送达失败', 'error')
    deliverOpen.value = false
    await store.fetchOrderDetail(orderId.value)
  } catch (error) {
    appStore.addToast(error?.message || '确认送达失败', 'error')
  } finally {
    busy.value = ''
  }
}

function openComplaint() {
  const o = order.value
  if (!o) return
  const targetType = o.keeper_id_wsh ? 'keeper' : 'merchant'
  router.push({
    name: 'Complaints',
    query: { orderId: o.id_wsh, orderNo: o.order_no_wsh || '', targetType, targetId: o.keeper_id_wsh || o.merchant_id_wsh || '' },
  })
}

/* ── 工具 ── */
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
  } catch { /* fall through */ }
  return trimmed.split(',').map(s => s.trim()).filter(Boolean)
}
function dateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.od-detail { width: 100%; padding: 6px 0 72px; }
.od-shell { max-width: 1080px; margin: 0 auto; padding: 0 24px; }
.od-tabular { font-variant-numeric: tabular-nums; }

/* Breadcrumb */
.od-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.od-crumb-link { color: var(--ref-muted); }
.od-crumb-link:hover { color: var(--ref-ink); }
.od-crumb-sep { color: var(--ref-line); }
.od-crumb-here { color: var(--ref-ink-soft); }

/* Skeleton */
.od-skel-bar { width: 120px; height: 16px; border-radius: var(--radius-inline); background: var(--ref-sand); }
.od-skel-block { margin-top: 24px; height: 160px; border-radius: var(--radius-card); border: 1px solid var(--ref-line); }
.od-skel-block.tall { height: 260px; }

/* State */
.od-state { padding: 64px 24px; border-radius: var(--radius-card); border: 1px solid var(--ref-line); background: var(--ref-surface); text-align: center; }
.od-state-icon { display: flex; align-items: center; justify-content: center; width: 56px; height: 56px; margin: 0 auto; border-radius: 50%; background: var(--ref-sand); color: var(--ref-brand); font-size: 26px; }
.od-state h3 { margin: 20px 0 0; font-family: var(--ref-font-display); font-size: 22px; font-weight: 500; color: var(--ref-ink); }
.od-state p { max-width: 400px; margin: 10px auto 0; font-size: 13.5px; line-height: 1.7; color: var(--ref-muted); }

/* Eyebrow */
.od-eyebrow { display: flex; align-items: center; gap: 10px; }
.od-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.od-eyebrow > span { font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }
.od-eyebrow-word { letter-spacing: 0.28em; text-transform: none; }

/* Header */
.od-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 14px 24px; padding: 24px 0 2px; }
.od-head-main { min-width: 0; }
.od-title { margin: 8px 0 0; font-family: var(--ref-font-display); font-size: clamp(32px, 4.4vw, 48px); line-height: 1.12; letter-spacing: -0.01em; font-weight: 500; color: var(--ref-ink); text-wrap: balance; }
.od-lede { margin: 10px 0 0; font-size: 14px; color: color-mix(in srgb, var(--ref-muted) 90%, transparent); }
.od-head-actions { display: flex; align-items: center; gap: 10px; }

/* Status badge */
.od-badge { display: inline-flex; align-items: center; border-radius: var(--radius-pill); border: 1px solid transparent; padding: 6px 12px; font-size: 12px; font-weight: 600; line-height: 1; white-space: nowrap; }
.od-tone-action { background: var(--ref-brand); color: #fff; border-color: transparent; }
.od-tone-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent); }
.od-tone-active { background: color-mix(in srgb, #3f5347 12%, transparent); color: #3f5347; border-color: color-mix(in srgb, #3f5347 25%, transparent); }
.od-tone-done { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); border-color: transparent; }
.od-tone-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }

/* Facts */
.od-facts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px 20px; margin-top: 18px; padding: 4px 0; }
.od-fact { min-width: 0; }
.od-fact dt { font-size: 11px; text-transform: uppercase; letter-spacing: 0.14em; color: var(--ref-muted); }
.od-fact dd { margin: 7px 0 0; font-size: 13.5px; line-height: 1.5; color: var(--ref-ink-soft); word-break: break-word; }
.od-fact-amount { font-family: var(--ref-font-display); font-size: 21px; letter-spacing: -0.01em; color: var(--ref-ink); }
.od-fact-yen { font-size: 0.6em; color: var(--ref-muted); }
.od-ink { color: var(--ref-ink); }
.od-muted { color: var(--ref-muted); }
.od-sep { margin: 0 6px; color: var(--ref-muted); }

/* Order no */
.od-orderno { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.od-orderno-value { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 12px; font-variant-numeric: tabular-nums; color: var(--ref-muted); }
.od-copy { display: inline-flex; align-items: center; gap: 5px; border: 1px solid var(--ref-line); background: var(--ref-surface); color: var(--ref-ink-soft); border-radius: var(--radius-pill); padding: 3px 10px; font-size: 12px; cursor: pointer; transition: border-color 150ms ease, background 150ms ease, color 150ms ease; }
.od-copy:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent); color: var(--ref-ink); }
.od-copy-glyph { font-size: 11px; }

/* Section */
.od-section { margin-top: 26px; }
.od-sec-head { margin-bottom: 14px; }
.od-sec-title { display: flex; align-items: baseline; gap: 10px; margin: 0; font-family: var(--ref-font-display); font-size: clamp(18px, 2.4vw, 21px); font-weight: 500; line-height: 1.2; letter-spacing: -0.01em; color: var(--ref-ink); }
.od-sec-en { font-family: var(--ref-font-sans); font-size: 9.5px; font-weight: 500; letter-spacing: 0.24em; text-transform: uppercase; color: var(--ref-muted); }

.od-panel { border: 1px solid var(--ref-line); border-radius: var(--radius-lg); background: var(--ref-surface); padding: 20px 22px; }

/* Progress rail */
.od-progress { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin: 0; padding: 0; list-style: none; }
.od-phase { min-width: 0; }
.od-phase-head { display: flex; align-items: center; gap: 6px; }
.od-dot { width: 8px; height: 8px; flex-shrink: 0; border-radius: 50%; background: var(--ref-line); }
.od-dot.active { background: var(--ref-brand); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 18%, transparent); }
.od-dot.done { background: var(--ref-brand); }
.od-track { position: relative; height: 1px; flex: 1; overflow: hidden; background: var(--ref-line); }
.od-track-fill { position: absolute; inset: 0; transform-origin: left; background: var(--ref-brand); transform: scaleX(0); }
.od-track-fill.filled { transform: scaleX(1); }
.od-phase-label { margin: 8px 0 0; font-size: 12.5px; line-height: 1; color: var(--ref-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.od-phase-label.active { font-weight: 600; color: var(--ref-ink); }
.od-phase-label.on { color: var(--ref-ink-soft); }
.od-phase-stamp { margin: 5px 0 0; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.od-cancelled { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--ref-muted); }
.od-cancelled-icon { font-size: 14px; }
.od-facts-inner { grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px 20px; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--ref-line); }

/* Two-col cards */
.od-two-col { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr); gap: 16px; align-items: start; }
.od-card { overflow: hidden; border: 1px solid var(--ref-line); border-radius: var(--radius-lg); background: var(--ref-surface); }
.od-card-pad { padding: 20px 22px; }
.od-card-cream { background: color-mix(in srgb, var(--ref-cream) 50%, var(--ref-surface)); padding: 20px 22px; }
.od-cover { display: block; width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
.od-card-body { padding: 16px 18px; }
.od-card-title { font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; line-height: 1.3; letter-spacing: -0.01em; color: var(--ref-ink); }
.od-remark { margin: 12px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.od-card .od-facts-inner { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--ref-line); grid-template-columns: 1fr; }
.od-link { color: var(--ref-ink); text-decoration: underline; text-decoration-color: var(--ref-line); text-underline-offset: 4px; }
.od-link:hover { text-decoration-color: var(--ref-brand); color: var(--ref-brand); }
.od-inline { display: inline-flex; align-items: flex-start; gap: 6px; }
.od-pin { font-size: 9px; line-height: 18px; color: var(--ref-muted); }
.od-phone-glyph { color: var(--ref-muted); }

/* Pet card */
.od-pet-facts { grid-template-columns: 1fr; gap: 14px; margin: 16px 0 0; padding: 16px 18px 2px; border-top: 1px solid var(--ref-line); }
.od-pet-head { display: flex; align-items: center; gap: 14px; border-bottom: none; padding: 16px 18px; }
.od-pet-avatar { width: 58px; height: 58px; flex-shrink: 0; border-radius: var(--radius-pill); object-fit: cover; overflow: hidden; }
.od-pet-avatar-fallback { display: flex; align-items: center; justify-content: center; background: var(--ref-sand); color: var(--ref-brand); font-family: var(--ref-font-display); font-size: 20px; }
.od-pet-meta { min-width: 0; }
.od-pet-name { font-family: var(--ref-font-display); font-size: 18px; font-weight: 500; line-height: 1.2; letter-spacing: -0.01em; color: var(--ref-ink); }
.od-pet-sub { margin: 5px 0 0; font-size: 12px; color: var(--ref-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.od-postimg { margin: 16px 18px; }
.od-postimg-img { display: block; width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 14px; }
.od-postimg-cap { margin-top: 8px; font-size: 12px; color: var(--ref-muted); }

/* Money */
.od-money { margin: 0; }
.od-money-row { display: flex; align-items: baseline; justify-content: space-between; gap: 20px; padding: 9px 0; }
.od-money-row dt { font-size: 13.5px; color: var(--ref-ink-soft); }
.od-money-row dd { font-size: 14px; color: var(--ref-ink); }
.od-discount { color: var(--ref-brand-deep); }
.od-money-total { margin-top: 8px; padding-top: 16px; border-top: 1px solid var(--ref-line); }
.od-money-total dt { font-weight: 500; color: var(--ref-ink); }
.od-total-amount { font-family: var(--ref-font-display); font-size: 24px; line-height: 1; letter-spacing: -0.02em; color: var(--ref-ink); }
.od-total-yen { font-size: 0.6em; color: var(--ref-muted); }

/* Actions */
.od-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.od-actions-note { margin: 16px 0 0; font-size: 12px; line-height: 1.7; color: var(--ref-muted); }
.od-act-shield { font-size: 11px; }
.od-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 7px; height: 40px; padding: 0 18px; border-radius: 10px;
  border: 1px solid var(--ref-line); background: var(--ref-surface); color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; text-decoration: none; cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease, transform 150ms ease;
}
.od-btn:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: var(--ref-sand); color: var(--ref-ink); transform: translateY(-1px); }
.od-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.od-btn-primary { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.od-btn-primary:hover:not(:disabled) { background: var(--ref-brand-deep); border-color: var(--ref-brand-deep); color: #fff; }
.od-btn-outline { border-color: color-mix(in srgb, var(--ref-ink) 22%, transparent); background: transparent; color: var(--ref-ink-soft); }
.od-btn-outline:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 45%, transparent); background: var(--ref-sand); color: var(--ref-ink); }
.od-btn-danger { background: transparent; border-color: color-mix(in srgb, #a03422 35%, transparent); color: #a03422; }
.od-btn-danger:hover:not(:disabled) { background: color-mix(in srgb, #a03422 9%, transparent); border-color: color-mix(in srgb, #a03422 45%, transparent); color: #a03422; }
.od-btn-quiet { background: transparent; border-color: transparent; }
.od-btn-quiet:hover:not(:disabled) { background: var(--ref-sand); border-color: transparent; color: var(--ref-ink); }

/* Modal */
.od-modal { position: fixed; inset: 0; z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 20px; }
.od-modal-mask { position: absolute; inset: 0; background: color-mix(in srgb, #120d08 38%, transparent); backdrop-filter: blur(2px); }
.od-modal-box { position: relative; width: 100%; max-width: 440px; border: 1px solid var(--ref-line); border-radius: var(--radius-card); background: var(--ref-surface); padding: 26px; box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 55%, transparent); }
.od-modal-box-sm { max-width: 400px; }
.od-modal-title { margin: 0; font-family: var(--ref-font-display); font-size: 22px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); }
.od-modal-desc { margin: 8px 0 0; font-size: 13.5px; line-height: 1.6; color: var(--ref-muted); }
.od-modal-desc strong { color: var(--ref-ink); }
.od-modal-detail { margin: 14px 0 0; font-size: 13px; line-height: 1.6; color: var(--ref-ink-soft); }
.od-modal-close { position: absolute; top: 16px; right: 16px; width: 28px; height: 28px; border-radius: 8px; border: 0; background: transparent; color: var(--ref-muted); font-size: 14px; cursor: pointer; }
.od-modal-close:hover { color: var(--ref-ink); background: var(--ref-sand); }
.od-methods { list-style: none; margin: 20px 0 0; padding: 0; display: grid; gap: 10px; }
.od-method { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; text-align: left; border: 1px solid var(--ref-line); border-radius: 14px; background: var(--ref-surface); padding: 15px 16px; cursor: pointer; transition: border-color 150ms ease, background-color 150ms ease; }
.od-method:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-brand) 50%, transparent); background: color-mix(in srgb, var(--ref-brand) 4%, var(--ref-surface)); }
.od-method:disabled { opacity: 0.5; cursor: not-allowed; }
.od-method-label { display: block; font-size: 14px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); }
.od-method-hint { display: block; margin-top: 3px; font-size: 12px; color: var(--ref-muted); }
.od-method-cta { font-size: 12px; color: var(--ref-brand-deep); flex-shrink: 0; }
.od-modal-btns { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; margin-top: 24px; }

@media (max-width: 860px) {
  .od-facts, .od-facts-inner { grid-template-columns: repeat(2, minmax(0, 1fr)); row-gap: 20px; }
  .od-two-col { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .od-shell { padding: 0 16px; }
  .od-head { padding-top: 26px; }
  .od-panel, .od-card-cream { padding: 20px; }
}
</style>