<template>
  <div class="ms-page">
    <div class="ms-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ms-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ms-crumb-link">首页</router-link>
        <span class="ms-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="ms-crumb-link">个人中心</router-link>
        <span class="ms-crumb-sep" aria-hidden="true">›</span>
        <span class="ms-crumb-here">会员中心</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ms-head">
        <div class="ms-head-copy">
          <div class="ms-eyebrow" aria-hidden="true">
            <span class="ms-eyebrow-line"></span>
            <span>Membership</span>
          </div>
          <h1 class="ms-title">会员中心</h1>
          <p class="ms-sub">会员折扣直接作用在下单金额上，权益的每一次使用都记录在案，可逐笔核对。</p>
        </div>
        <div class="ms-actions">
          <button type="button" class="cta cta-outline" @click="scrollTo('ms-orders')">会员订单</button>
        </div>
      </header>

      <!-- ═══ Facts ═══ -->
      <dl class="ms-facts" aria-label="会员概览">
        <div class="ms-fact">
          <dd>{{ loading ? '--' : membershipStatusText(currentMembership.status_wsh) }}</dd>
          <dt>当前身份</dt>
        </div>
        <div class="ms-fact">
          <dd>{{ loading ? '--' : discountText(currentMembership.discount_rate_wsh) }}</dd>
          <dt>服务折扣</dt>
        </div>
        <div class="ms-fact">
          <dd class="tabular">{{ loading ? '--' : (active ? (currentMembership.remaining_days_wsh || 0) : 0) }}</dd>
          <dt>剩余天数</dt>
        </div>
        <div class="ms-fact">
          <dd class="tabular">{{ loading ? '--' : `¥${money(savedTotal)}` }}</dd>
          <dt>累计省下</dt>
        </div>
      </dl>

      <!-- ═══ 01 · 当前权益 ═══ -->
      <section class="ms-section" aria-label="当前权益">
        <header class="ms-sec-head">
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">01</span>
              <span class="ms-line" aria-hidden="true"></span>
              <span>Your plan</span>
            </p>
            <h2 class="ms-sec-title">当前权益</h2>
            <p class="ms-sec-desc">会员到期后不会自动续费，需要时再手动开通。</p>
          </div>
        </header>

        <template v-if="loading">
          <div class="ms-benefit-grid">
            <div v-for="i in 2" :key="i" class="ms-skeleton" />
          </div>
        </template>

        <template v-else>
          <div class="ms-benefit-grid">
            <!-- 当前会员卡片 -->
            <div
              class="ms-benefit"
              :class="active ? 'is-dark' : 'is-light'"
            >
              <div class="ms-benefit-top">
                <div class="ms-benefit-copy">
                  <p class="ms-benefit-kicker" :class="active ? 'on-dark' : ''">
                    {{ active ? 'Active' : 'Not a member' }}
                  </p>
                  <h3 class="ms-benefit-name" :class="active ? 'on-dark' : ''">
                    {{ active ? (currentMembership.plan_name_wsh || currentMembership.plan_code_wsh || '会员') : '你还不是会员' }}
                  </h3>
                </div>
                <span class="ms-benefit-badge" :class="active ? 'on-dark' : ''" aria-hidden="true">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                </span>
              </div>

              <template v-if="active">
                <dl class="ms-benefit-facts">
                  <div>
                    <dt>折扣</dt>
                    <dd>{{ discountText(currentMembership.discount_rate_wsh) }}</dd>
                  </div>
                  <div>
                    <dt>剩余天数</dt>
                    <dd class="tabular">{{ currentMembership.remaining_days_wsh || 0 }} 天</dd>
                  </div>
                  <div class="ms-span-2">
                    <dt>到期日期</dt>
                    <dd class="tabular">{{ dateText(currentMembership.expires_at_wsh) }}</dd>
                  </div>
                </dl>
                <p class="ms-benefit-note">
                  开通于 {{ dateText(currentMembership.started_at_wsh) }}，折扣会在下单结算时自动生效，无需手动选择。
                </p>
              </template>

              <template v-else>
                <p class="ms-benefit-promo">非会员按原价结算。如果一年里会用上三次以上寄养或洗护，季卡通常就已经回本。</p>
                <div class="ms-benefit-cta">
                  <button type="button" class="cta cta-primary" @click="scrollTo('ms-plans')">查看推荐套餐</button>
                </div>
              </template>
            </div>

            <!-- 待支付会员订单 -->
            <div class="ms-pending">
              <h4 class="ms-pending-title">待支付会员订单</h4>
              <template v-if="pendingOrder">
                <p class="ms-pending-desc">{{ pendingOrder.plan_name_wsh || pendingOrder.plan_code_wsh || '会员套餐' }} 尚未完成支付，支付后立即生效。</p>
                <p class="ms-pending-amount tabular"><span class="ms-yen">¥</span>{{ money(pendingOrder.amount_wsh) }}</p>
                <p class="ms-pending-meta tabular">{{ pendingOrder.order_no_wsh }} · {{ dateTimeText(pendingOrder.created_at_wsh) }}</p>
                <div class="ms-pending-actions">
                  <button
                    type="button"
                    class="cta cta-primary"
                    :disabled="payingOrderNo === pendingOrder.order_no_wsh"
                    @click="payOrder(pendingOrder)"
                  >
                    {{ payingOrderNo === pendingOrder.order_no_wsh ? '支付中...' : '余额支付' }}
                  </button>
                  <button
                    type="button"
                    class="cta cta-danger"
                    :disabled="cancellingOrderNo === pendingOrder.order_no_wsh"
                    @click="cancelOrder(pendingOrder)"
                  >
                    {{ cancellingOrderNo === pendingOrder.order_no_wsh ? '取消中...' : '取消订单' }}
                  </button>
                </div>
              </template>
              <template v-else>
                <p class="ms-pending-desc muted">没有待支付的会员订单。</p>
                <div class="ms-pending-cta">
                  <button type="button" class="text-link" @click="scrollTo('ms-orders')">
                    查看历史会员订单
                    <span class="tl-arrow" aria-hidden="true">→</span>
                  </button>
                </div>
              </template>
            </div>
          </div>
        </template>
      </section>

      <!-- ═══ 02 · 选择套餐 ═══ -->
      <section id="ms-plans" class="ms-section" aria-label="选择套餐">
        <header class="ms-sec-head">
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">02</span>
              <span class="ms-line" aria-hidden="true"></span>
              <span>Plans</span>
            </p>
            <h2 class="ms-sec-title">选择套餐</h2>
            <p class="ms-sec-desc">折扣适用于全部照护服务，节假日不涨价。</p>
          </div>
          <span class="ms-sec-tag">{{ loading ? '--' : plans.length }} 个可选套餐</span>
        </header>

        <template v-if="loading">
          <div class="ms-plans-grid"><div v-for="i in 3" :key="i" class="ms-skeleton ms-skeleton-plan" /></div>
        </template>

        <div v-else-if="!plans.length" class="ms-empty">
          <p class="ms-empty-title">暂无可开通的套餐</p>
          <p class="ms-empty-desc">会员套餐正在调整，稍后再来看看。</p>
          <div class="ms-empty-actions">
            <router-link to="/services" class="cta cta-primary">浏览照护服务</router-link>
          </div>
        </div>

        <div v-else class="ms-plans-grid">
          <article
            v-for="(plan, index) in plans"
            :key="plan.id_wsh"
            class="ms-plan"
            :class="{
              'is-featured': index === featuredIndex,
              'is-current': active && plan.code_wsh === currentMembership.plan_code_wsh
            }"
          >
            <div class="ms-plan-head">
              <div class="ms-plan-title">
                <h3 class="ms-plan-name">{{ plan.name_wsh }}</h3>
                <p class="ms-plan-code">{{ plan.code_wsh }}</p>
              </div>
              <span class="ms-level-badge">Lv{{ plan.level_wsh || 1 }}</span>
            </div>
            <p class="ms-plan-price tabular"><span class="ms-yen">¥</span>{{ money(plan.price_wsh) }}</p>
            <div class="ms-plan-meta">
              <span class="ms-plan-days tabular">{{ plan.duration_days_wsh }} 天</span>
              <span class="ms-plan-dot" aria-hidden="true"></span>
              <span>{{ discountText(plan.discount_rate_wsh) }}</span>
            </div>
            <button
              type="button"
              class="cta ms-plan-cta"
              :class="index === featuredIndex ? 'cta-primary' : 'cta-outline'"
              :disabled="submittingPlanId === plan.id_wsh"
              @click="createOrder(plan)"
            >
              {{ submittingPlanId === plan.id_wsh ? '生成中...' : '开通套餐' }}
            </button>
            <p v-if="active && plan.code_wsh === currentMembership.plan_code_wsh" class="ms-plan-current">
              当前生效中
            </p>
          </article>
        </div>
      </section>

      <!-- ═══ 03 · 会员订单 ═══ -->
      <section id="ms-orders" class="ms-section" aria-label="会员订单">
        <header class="ms-sec-head">
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">03</span>
              <span class="ms-line" aria-hidden="true"></span>
              <span>Orders</span>
            </p>
            <h2 class="ms-sec-title">会员订单</h2>
            <p class="ms-sec-desc">开通与续费的每一笔订单都记录在这里，可随时查看或取消待支付订单。</p>
          </div>
          <span class="ms-sec-tag">{{ loading ? '--' : orders.length }} 条记录</span>
        </header>

        <template v-if="loading">
          <div v-for="i in 2" :key="i" class="ms-skeleton" />
        </template>

        <div v-else-if="!orders.length" class="ms-empty">
          <p class="ms-empty-title">暂无会员订单</p>
          <p class="ms-empty-desc">开通第一个会员套餐后，订单会出现在这里。</p>
          <div class="ms-empty-actions">
            <button type="button" class="cta cta-primary" @click="scrollTo('ms-plans')">去选套餐</button>
          </div>
        </div>

        <div v-else class="ms-list">
          <article v-for="order in orders" :key="order.id_wsh || order.order_no_wsh" class="ms-row">
            <div class="ms-row-main">
              <div class="ms-row-line">
                <h4 class="ms-row-title">{{ order.plan_name_wsh || order.plan_code_wsh || '会员套餐' }}</h4>
                <span :class="['badge', statusClass(order.status_wsh)]">{{ statusText(order.status_wsh) }}</span>
              </div>
              <p class="ms-row-meta tabular">
                <span class="ms-num">#{{ order.order_no_wsh }}</span>
                <span class="ms-sep" aria-hidden="true">·</span>
                <span>{{ payMethodText(order.pay_method_wsh) }}</span>
                <span class="ms-sep" aria-hidden="true">·</span>
                <span>{{ periodText(order, 'membership_start_at_wsh', 'membership_end_at_wsh') }}</span>
                <span class="ms-sep" aria-hidden="true">·</span>
                <span>{{ dateTimeText(order.created_at_wsh) }}</span>
              </p>
            </div>
            <div class="ms-row-side">
              <p class="ms-row-amount tabular"><span class="ms-yen">¥</span>{{ money(order.amount_wsh) }}</p>
              <div v-if="order.status_wsh === 'pending'" class="ms-row-actions">
                <button
                  type="button"
                  class="cta cta-primary cta-sm"
                  :disabled="payingOrderNo === order.order_no_wsh || cancellingOrderNo === order.order_no_wsh"
                  @click="payOrder(order)"
                >
                  {{ payingOrderNo === order.order_no_wsh ? '支付中' : '支付' }}
                </button>
                <button
                  type="button"
                  class="cta cta-danger cta-sm"
                  :disabled="payingOrderNo === order.order_no_wsh || cancellingOrderNo === order.order_no_wsh"
                  @click="cancelOrder(order)"
                >
                  {{ cancellingOrderNo === order.order_no_wsh ? '取消中' : '取消' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- ═══ 04 · 权益使用记录 ═══ -->
      <section class="ms-section" aria-label="权益使用记录">
        <header class="ms-sec-head">
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">04</span>
              <span class="ms-line" aria-hidden="true"></span>
              <span>Benefits used</span>
            </p>
            <h2 class="ms-sec-title">权益使用记录</h2>
            <p class="ms-sec-desc">每一次折扣与赠送权益的抵扣金额都在这里，可与订单逐笔核对。</p>
          </div>
          <span class="ms-sec-tag">{{ loading ? '--' : usages.length }} 条记录</span>
        </header>

        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="ms-skeleton ms-skeleton-row" />
        </template>

        <div v-else-if="!usages.length" class="ms-empty">
          <p class="ms-empty-title">还没有权益使用记录</p>
          <p class="ms-empty-desc">下单时如果用到了会员折扣或赠送权益，记录会出现在这里。</p>
          <div class="ms-empty-actions">
            <router-link to="/services" class="cta cta-primary">浏览照护服务</router-link>
          </div>
        </div>

        <div v-else class="ms-list">
          <article v-for="usage in usages" :key="usage.id_wsh || usage.request_id_wsh" class="ms-row ms-usage">
            <div class="ms-row-main">
              <div class="ms-row-line">
                <h4 class="ms-row-title">{{ usage.benefit_type_wsh || usage.benefit_code_wsh || '会员权益' }}</h4>
                <span :class="['badge', usageStatusClass(usage.usage_status_wsh)]">{{ usageStatusText(usage.usage_status_wsh) }}</span>
              </div>
              <p class="ms-row-meta tabular">
                <span>{{ dateTimeText(usage.used_at_wsh || usage.created_at_wsh) }}</span>
                <template v-if="usage.business_id_wsh">
                  <span class="ms-sep" aria-hidden="true">·</span>
                  <span>{{ usage.business_type_wsh || '订单' }} #{{ usage.business_id_wsh }}</span>
                </template>
              </p>
            </div>
            <div class="ms-row-side">
              <p class="ms-row-amount is-save tabular">-¥{{ money(usage.amount_wsh) }}</p>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import {
  cancelMembershipOrder,
  createMembershipOrder,
  getActiveMemberPlans,
  getMembershipUsages,
  getMyMembership,
  getMyMembershipOrders,
  payMembershipOrder,
} from '@/api/membership'

const appStore = useAppStore()
const loading = ref(false)
const plans = ref([])
const orders = ref([])
const usages = ref([])
const currentMembership = ref({})
const submittingPlanId = ref(null)
const cancellingOrderNo = ref('')
const payingOrderNo = ref('')

const active = computed(() => Boolean(currentMembership.value.active_wsh))
const pendingOrder = computed(() => orders.value.find(o => o.status_wsh === 'pending') || null)
const featuredIndex = computed(() => plans.value.length > 2 ? 1 : plans.value.length - 1)
const savedTotal = computed(() =>
  usages.value.reduce((sum, u) => sum + Number(u.amount_wsh || 0), 0)
)

onMounted(loadAll)

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadAll() {
  loading.value = true
  try {
    const [membershipRes, plansRes, ordersRes, usagesRes] = await Promise.all([
      getMyMembership(),
      getActiveMemberPlans(),
      getMyMembershipOrders(),
      getMembershipUsages(),
    ])
    if (membershipRes.code === 200) currentMembership.value = membershipRes.data || {}
    else appStore.addToast(membershipRes.message || '会员状态加载失败', 'error')
    if (plansRes.code === 200) plans.value = Array.isArray(plansRes.data) ? plansRes.data : []
    else appStore.addToast(plansRes.message || '会员套餐加载失败', 'error')
    if (ordersRes.code === 200) orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : []
    else appStore.addToast(ordersRes.message || '会员订单加载失败', 'error')
    if (usagesRes.code === 200) usages.value = Array.isArray(usagesRes.data) ? usagesRes.data : []
    else appStore.addToast(usagesRes.message || '权益流水加载失败', 'error')
  } catch (e) {
    appStore.addToast(e?.message || '会员数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadMembershipState() {
  const [membershipRes, ordersRes, usagesRes] = await Promise.all([
    getMyMembership(),
    getMyMembershipOrders(),
    getMembershipUsages(),
  ])
  if (membershipRes.code === 200) currentMembership.value = membershipRes.data || {}
  if (ordersRes.code === 200) orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : []
  if (usagesRes.code === 200) usages.value = Array.isArray(usagesRes.data) ? usagesRes.data : []
}

async function loadOrders() {
  const res = await getMyMembershipOrders()
  if (res.code === 200) orders.value = Array.isArray(res.data) ? res.data : []
}

async function createOrder(plan) {
  if (submittingPlanId.value) return
  submittingPlanId.value = plan.id_wsh
  try {
    const res = await createMembershipOrder({
      plan_id_wsh: plan.id_wsh,
      pay_method_wsh: 'balance',
      request_id_wsh: createRequestId(plan.id_wsh),
    })
    if (res.code === 200) {
      appStore.addToast('会员订单已创建', 'success')
      await loadOrders()
    } else {
      appStore.addToast(res.message || '会员订单创建失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单创建失败', 'error')
  } finally {
    submittingPlanId.value = null
  }
}

async function payOrder(order) {
  if (payingOrderNo.value) return
  payingOrderNo.value = order.order_no_wsh
  try {
    const res = await payMembershipOrder(order.order_no_wsh)
    if (res.code === 200) {
      appStore.addToast('会员已开通', 'success')
      await loadMembershipState()
    } else {
      appStore.addToast(res.message || '会员订单支付失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单支付失败', 'error')
  } finally {
    payingOrderNo.value = ''
  }
}

async function cancelOrder(order) {
  if (cancellingOrderNo.value) return
  if (!confirm(`确定取消会员订单 ${order.order_no_wsh} 吗？`)) return
  cancellingOrderNo.value = order.order_no_wsh
  try {
    const res = await cancelMembershipOrder(order.order_no_wsh)
    if (res.code === 200) {
      appStore.addToast('会员订单已取消', 'success')
      await loadOrders()
    } else {
      appStore.addToast(res.message || '会员订单取消失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员订单取消失败', 'error')
  } finally {
    cancellingOrderNo.value = ''
  }
}

function createRequestId(planId) {
  const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `member-${planId}-${uuid}`
}

function discountText(value) {
  const rate = Number(value || 1)
  return rate >= 1 ? '无折扣' : `${(rate * 10).toFixed(1)} 折`
}

function membershipStatusText(status) {
  return {
    active: '生效中',
    inactive: '未开通',
    expired: '已过期',
    cancelled: '已取消',
  }[status] || status || '未开通'
}

function statusText(status) {
  return {
    pending: '待支付',
    paid: '已支付',
    cancelled: '已取消',
    refunded: '已退款',
  }[status] || status || '-'
}

function statusClass(status) {
  return {
    pending: 'badge-action',
    paid: 'badge-active',
    cancelled: 'badge-done',
    refunded: 'badge-done',
  }[status] || 'badge-done'
}

function usageStatusText(status) {
  return {
    locked: '已锁定',
    used: '已使用',
    released: '已释放',
  }[status] || status || '-'
}

function usageStatusClass(status) {
  return {
    locked: 'badge-action',
    used: 'badge-active',
    released: 'badge-done',
  }[status] || 'badge-done'
}

function payMethodText(value) {
  return {
    mock: '模拟支付（已停用）',
    balance: '余额支付',
    wechat: '微信支付',
    alipay: '支付宝',
  }[value] || value || '-'
}

function periodText(entity, startKey, endKey) {
  const start = dateText(entity?.[startKey])
  const end = dateText(entity?.[endKey])
  return start === '-' && end === '-' ? '-' : `${start} 至 ${end}`
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

function dateTimeText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.ms-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ms-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.ms-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ms-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ms-crumb-link:hover { color: var(--ref-ink); }
.ms-crumb-sep { color: var(--ref-line); }
.ms-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.ms-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ms-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ms-idx { font-variant-numeric: tabular-nums; }
.ms-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ms-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.ms-head-copy { min-width: 0; }
.ms-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ms-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ms-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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
.cta-danger { background: color-mix(in srgb, var(--color-danger) 10%, transparent); color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 28%, transparent); }
.cta-danger:hover:not(:disabled) { background: color-mix(in srgb, var(--color-danger) 16%, transparent); }
.cta-sm { height: 34px; padding: 0 12px; font-size: 12px; border-radius: 9px; }

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

/* ═══ Facts ═══ */
.ms-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 8px 0 0;
}
.ms-fact {
  padding: 20px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.ms-fact:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.ms-fact dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  line-height: 1.1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.ms-fact dt {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ref-muted);
}

/* ═══ Section ═══ */
.ms-section { margin-top: 56px; scroll-margin-top: 24px; }
.ms-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.ms-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ms-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ms-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}
.ms-sec-tag {
  flex: 0 0 auto;
  padding: 6px 12px;
  border: 1px solid var(--ref-line);
  border-radius: 999px;
  background: var(--ref-surface);
  font-size: 12px;
  color: var(--ref-muted);
}

/* ═══ Skeleton ═══ */
.ms-skeleton {
  height: 172px;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: ms-shimmer 1.3s linear infinite;
}
.ms-skeleton-row { height: 84px; }

/* ═══ 01 · 当前权益 ═══ */
.ms-benefit-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  margin-top: 20px;
  align-items: stretch;
}
.ms-benefit {
  border-radius: 18px;
  padding: 26px 28px;
  display: flex;
  flex-direction: column;
}
.ms-benefit.is-dark {
  background: var(--ref-ink);
  color: var(--ref-cream);
}
.ms-benefit.is-light {
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
}
.ms-benefit-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.ms-benefit-copy { min-width: 0; }
.ms-benefit-kicker {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ms-benefit-kicker.on-dark { color: color-mix(in srgb, var(--ref-cream) 45%, transparent); }
.ms-benefit-name {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(22px, 3vw, 30px);
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ms-benefit-name.on-dark { color: var(--ref-cream); }
.ms-benefit-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  border-radius: 50%;
}
.ms-benefit-badge:not(.on-dark) { background: var(--ref-sand); color: var(--ref-brand); }
.ms-benefit-badge.on-dark { background: color-mix(in srgb, var(--ref-cream) 10%, transparent); color: var(--ref-cream); }

.ms-benefit-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin: 26px 0 0;
}
.ms-benefit-facts dt {
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ms-benefit-facts dd {
  margin: 6px 0 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.is-dark .ms-benefit-facts dt { color: color-mix(in srgb, var(--ref-cream) 40%, transparent); }
.is-dark .ms-benefit-facts dd { color: var(--ref-cream); }
.ms-span-2 { grid-column: span 2; }
.is-dark .ms-span-2 dd { font-size: 13.5px; font-family: var(--font-body); opacity: 0.9; }

.ms-benefit-note {
  margin: 24px 0 0;
  padding-top: 18px;
  border-top: 1px solid color-mix(in srgb, var(--ref-cream) 14%, transparent);
  font-size: 12.5px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-cream) 55%, transparent);
}
.ms-benefit-promo {
  margin: 18px 0 0;
  max-width: 460px;
  font-size: 13.5px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.ms-benefit-cta { margin-top: 26px; }

.ms-pending {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 26px 28px;
  display: flex;
  flex-direction: column;
}
.ms-pending-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 19px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ms-pending-desc {
  margin: 14px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.ms-pending-desc.muted { color: var(--ref-muted); }
.ms-pending-amount {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ms-yen { font-size: 15px; color: color-mix(in srgb, var(--ref-brand) 72%, transparent); }
.ms-pending-meta {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
}
.ms-pending-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
  padding-top: 22px;
}
.ms-pending-cta { margin-top: auto; padding-top: 22px; }

/* ═══ Text link ═══ */
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-brand);
  cursor: pointer;
  text-decoration: none;
}
.text-link:hover { color: var(--ref-brand-deep); }
.tl-arrow { transition: transform 0.15s; }
.text-link:hover .tl-arrow { transform: translateX(3px); }

/* ═══ 02 · 选择套餐 ═══ */
.ms-plans-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 20px;
}
.ms-plan {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px 22px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s;
}
.ms-plan:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent);
  transform: translateY(-3px);
  box-shadow: 0 26px 56px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.ms-plan.is-featured {
  border-color: var(--ref-brand);
  background: var(--ref-ink);
  color: var(--ref-cream);
  box-shadow: 0 26px 56px -40px color-mix(in srgb, var(--ref-brand) 60%, transparent);
}
.ms-plan.is-current::before {
  content: '当前生效';
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 4px 8px;
  border-radius: 6px;
  background: color-mix(in srgb, var(--ref-brand) 14%, transparent);
  color: var(--ref-brand);
  font-size: 10px;
  font-weight: 500;
}
.ms-plan-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}
.ms-plan-title { min-width: 0; }
.ms-plan-name {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 18px;
  font-weight: 500;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.is-featured .ms-plan-name { color: var(--ref-cream); }
.ms-plan-code {
  margin: 5px 0 0;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ms-level-badge {
  flex: 0 0 auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ref-brand) 12%, transparent);
  color: var(--ref-brand);
  font-size: 11px;
  font-weight: 600;
}
.ms-plan-price {
  margin: 20px 0 0;
  font-family: var(--ref-font-display);
  font-size: 32px;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.is-featured .ms-plan-price { color: var(--ref-cream); }
.ms-plan-price .ms-yen { font-size: 16px; }
.ms-plan-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.is-featured .ms-plan-meta { color: color-mix(in srgb, var(--ref-cream) 55%, transparent); }
.ms-plan-dot { width: 3px; height: 3px; border-radius: 50%; background: var(--ref-line); }
.ms-plan-cta { margin-top: 24px; width: 100%; }
.is-featured .ms-plan-cta { background: var(--ref-brand); color: #fff; }
.is-featured .ms-plan-cta:hover:not(:disabled) { background: var(--ref-brand-deep); }
.ms-plan-current {
  margin: 12px 0 0;
  font-size: 12px;
  text-align: center;
  color: var(--ref-muted);
}

/* ═══ Empty ═══ */
.ms-empty {
  margin-top: 20px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  text-align: center;
}
.ms-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ms-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.ms-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

/* ═══ 03/04 · 列表 ═══ */
.ms-list {
  margin-top: 20px;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  overflow: hidden;
}
.ms-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 20px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--ref-line);
}
.ms-row:last-child { border-bottom: none; }
.ms-row-main { min-width: 0; }
.ms-row-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.ms-row-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 15.5px;
  font-weight: 500;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ms-row-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 8px 0 0;
  font-size: 11.5px;
  color: var(--ref-muted);
}
.ms-num { font-variant-numeric: tabular-nums; }
.ms-sep { color: var(--ref-line); }
.ms-row-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}
.ms-row-amount {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.ms-row-amount.is-save { color: var(--color-success); }
.ms-row-actions { display: flex; flex-wrap: wrap; gap: 8px; }

/* ═══ Animations ═══ */
@keyframes ms-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 980px) {
  .ms-facts { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ms-benefit-grid { grid-template-columns: 1fr; }
  .ms-plans-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .ms-shell { padding: 0 16px; }
  .ms-head { padding: 30px 0 22px; }
  .ms-sub { font-size: 13.5px; }
  .ms-facts { grid-template-columns: 1fr; gap: 10px; }
  .ms-fact { padding: 16px 20px; }
  .ms-fact dd { font-size: 22px; }
  .ms-section { margin-top: 44px; }
  .ms-plans-grid { grid-template-columns: 1fr; }
  .ms-benefit { padding: 22px 20px; }
  .ms-pending { padding: 22px 20px; }
  .ms-row { padding: 16px; }
  .ms-row-side { align-items: flex-start; flex-direction: row; justify-content: space-between; width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .ms-skeleton { animation: none; }
  .ms-benefit, .ms-plan, .ms-fact, .ms-row { transition: none; }
}
</style>
