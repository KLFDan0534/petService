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
            <span class="ms-eyebrow-line" />
            <span>会员</span>
          </div>
          <h1 class="ms-title">会员中心</h1>
          <p class="ms-sub">开通会员后全部照护服务按折扣价结算。折扣在下单结算时自动生效，到期不自动续费。</p>
        </div>
        <button type="button" class="ms-ghost-btn" @click="scrollTo('ms-orders')">
          会员订单<span v-if="pendingOrders.length" class="ms-ghost-count tabular">· {{ pendingOrders.length }}</span>
        </button>
      </header>

      <!-- ═══ Facts ═══ -->
      <dl class="ms-facts" aria-label="会员概览">
        <div v-for="fact in facts" :key="fact.key" class="ms-fact" data-reveal :style="{ transitionDelay: `${fact.delay}ms` }">
          <dd><RollingNumber :value="fact.value" /></dd>
          <dt>{{ fact.label }}</dt>
        </div>
      </dl>

      <!-- ═══ 01 · 当前权益 ═══ -->
      <section class="ms-section" aria-label="当前权益">
        <header class="ms-sec-head" data-reveal>
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">01</span>
              <span class="ms-line" aria-hidden="true" />
              <span>当前方案</span>
            </p>
            <h2 class="ms-sec-title">当前权益</h2>
            <p class="ms-sec-desc">会员到期后不会自动续费，需要时再手动开通。</p>
          </div>
        </header>

        <div v-if="loading" class="ms-duo">
          <div class="ms-skeleton ms-skeleton-card" />
          <div class="ms-skeleton ms-skeleton-card" />
        </div>

        <div v-else class="ms-duo">
          <MemberIdentityCard
            class="ms-identity"
            :membership="membership"
            :plan="currentPlan"
            :preview="previewPlan"
            :sweep="sweep"
            data-reveal
            @browse="scrollTo('ms-plans')"
            @open-preview="previewPlan && openPlan(previewPlan)"
            @clear-preview="selectedId = currentPlan?.id_wsh ?? null"
          />

          <!-- 有效期面板：把后端返回的起止时间换算成「还剩多少天」，缺失就不画进度 -->
          <div class="ms-validity" data-reveal :style="{ transitionDelay: '80ms' }">
            <div>
              <p class="ms-validity-kicker">Validity</p>
              <h3 class="ms-validity-title">
                {{ active ? `${currentPlan?.name_wsh || membership.plan_name_wsh || '会员'}有效期` : '暂无生效中的会员' }}
              </h3>

              <template v-if="active">
                <p class="ms-validity-days">
                  <RollingNumber :value="String(validity.remainingDays ?? '—')" class="ms-validity-num" />
                  <span class="ms-validity-unit">天剩余</span>
                </p>
                <div v-if="usedPercent !== null" class="ms-progress-wrap">
                  <div
                    class="ms-progress"
                    role="progressbar"
                    :aria-valuenow="usedPercent"
                    aria-valuemin="0"
                    aria-valuemax="100"
                    aria-label="本期会员已使用比例"
                  >
                    <span class="ms-progress-fill" :style="{ width: `${usedPercent}%` }" />
                  </div>
                  <p class="ms-progress-note tabular">
                    已使用 {{ validity.usedDays ?? 0 }} / {{ validity.totalDays ?? 0 }} 天（{{ usedPercent }}%）
                  </p>
                </div>
                <p v-else class="ms-validity-note">起止时间不完整，暂不显示进度。</p>
              </template>
              <p v-else class="ms-validity-note">
                开通后这里会显示本期剩余天数、起止日期与折扣力度。到期后不自动扣费。
              </p>
            </div>

            <dl class="ms-validity-dates">
              <div>
                <dt>开通日期</dt>
                <dd class="tabular">{{ active ? validity.startedAt || '—' : '—' }}</dd>
              </div>
              <div>
                <dt>到期日期</dt>
                <dd class="tabular">{{ active ? validity.expiresAt || '—' : '—' }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </section>

      <!-- ═══ 02 · 选择套餐 ═══ -->
      <section id="ms-plans" class="ms-section" aria-label="选择套餐">
        <header class="ms-sec-head" data-reveal>
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">02</span>
              <span class="ms-line" aria-hidden="true" />
              <span>方案</span>
            </p>
            <h2 class="ms-sec-title">选择套餐</h2>
            <p class="ms-sec-desc">折扣适用于全部照护服务，节假日不涨价。点一张卡可以先预览这一档的身份卡。</p>
          </div>
          <span class="ms-sec-tag">{{ loading ? '--' : plans.length }} 个可选套餐</span>
        </header>

        <div v-if="loading" class="ms-plans" :style="planGridStyle">
          <div v-for="i in 3" :key="i" class="ms-skeleton ms-skeleton-plan" />
        </div>

        <div v-else-if="!plans.length" class="ms-empty">
          <p class="ms-empty-title">暂无可开通的套餐</p>
          <p class="ms-empty-desc">会员套餐正在调整，稍后再来看看。</p>
          <router-link to="/services" class="ms-cta ms-cta-primary">浏览照护服务</router-link>
        </div>

        <template v-else>
          <div class="ms-plans" :style="planGridStyle" role="radiogroup" aria-label="会员套餐">
            <MemberPlanCard
              v-for="(plan, index) in orderedPlans"
              :key="plan.id_wsh"
              :plan="plan"
              :featured="plan.id_wsh === featuredId"
              :current="currentPlan"
              :selected="selectedId === plan.id_wsh"
              :has-pending="pendingPlanIds.includes(plan.id_wsh)"
              :busy="submittingPlanId === plan.id_wsh"
              :style="{ transitionDelay: `${index * 60}ms` }"
              data-reveal
              @select="selectedId = plan.id_wsh"
              @open="openPlan(plan)"
            />
          </div>

          <!-- 横向对比：同一组字段，省得来回对照三张卡 -->
          <div class="ms-compare-wrap" data-reveal>
            <table class="ms-compare">
              <caption class="ms-compare-cap">套餐对比</caption>
              <thead>
                <tr>
                  <th scope="col">项目</th>
                  <th
                    v-for="plan in orderedPlans"
                    :key="plan.id_wsh"
                    scope="col"
                    :class="{ 'is-selected': selectedId === plan.id_wsh }"
                  >
                    <button type="button" class="ms-compare-pick" @click="selectedId = plan.id_wsh">
                      {{ plan.name_wsh }}
                    </button>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">价格</th>
                  <td v-for="plan in orderedPlans" :key="plan.id_wsh" class="tabular" :class="{ 'is-selected': selectedId === plan.id_wsh }">
                    {{ money(plan.price_wsh) }}
                  </td>
                </tr>
                <tr>
                  <th scope="row">有效期</th>
                  <td v-for="plan in orderedPlans" :key="plan.id_wsh" class="tabular" :class="{ 'is-selected': selectedId === plan.id_wsh }">
                    {{ plan.duration_days_wsh ?? '—' }} 天
                  </td>
                </tr>
                <tr>
                  <th scope="row">日均</th>
                  <td v-for="plan in orderedPlans" :key="plan.id_wsh" class="tabular" :class="{ 'is-selected': selectedId === plan.id_wsh }">
                    {{ perDayText(plan) }}
                  </td>
                </tr>
                <tr>
                  <th scope="row">服务折扣</th>
                  <td v-for="plan in orderedPlans" :key="plan.id_wsh" :class="{ 'is-selected': selectedId === plan.id_wsh }">
                    {{ discountLabel(plan.discount_rate_wsh) }}
                  </td>
                </tr>
                <tr>
                  <th scope="row">附加权益</th>
                  <td v-for="plan in orderedPlans" :key="plan.id_wsh" :class="{ 'is-selected': selectedId === plan.id_wsh }">
                    {{ extrasText(plan) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </section>

      <!-- ═══ 03 · 会员订单 ═══ -->
      <section id="ms-orders" class="ms-section" aria-label="会员订单">
        <header class="ms-sec-head" data-reveal>
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">03</span>
              <span class="ms-line" aria-hidden="true" />
              <span>订单</span>
            </p>
            <h2 class="ms-sec-title">会员订单</h2>
            <p class="ms-sec-desc">未完成支付的订单会保留在这里，可以继续支付或取消。</p>
          </div>
        </header>

        <div class="ms-order-grid">
          <div class="ms-pending" data-reveal>
            <h3 class="ms-pending-title">待支付</h3>
            <template v-if="pendingOrders.length">
              <div v-for="order in pendingOrders" :key="order.order_no_wsh" class="ms-pending-item">
                <p class="ms-pending-name">{{ order.plan_name_wsh || order.plan_code_wsh || '会员套餐' }} 尚未完成支付</p>
                <p class="ms-pending-amount tabular"><span class="ms-yen">¥</span>{{ amountText(order.amount_wsh) }}</p>
                <p class="ms-pending-meta tabular">{{ order.order_no_wsh }} · {{ fmtDateTime(order.created_at_wsh) }}</p>
                <div class="ms-pending-actions">
                  <button type="button" class="ms-cta ms-cta-primary" :disabled="payingOrderNo === order.order_no_wsh" @click="payOrder(order)">
                    {{ payingOrderNo === order.order_no_wsh ? '支付中…' : '余额支付' }}
                  </button>
                  <button type="button" class="ms-cta ms-cta-danger" :disabled="cancellingOrderNo === order.order_no_wsh" @click="cancelOrder(order)">
                    {{ cancellingOrderNo === order.order_no_wsh ? '取消中…' : '取消订单' }}
                  </button>
                </div>
              </div>
            </template>
            <p v-else class="ms-pending-empty">没有待支付的会员订单。支付完成后会员立即生效。</p>
          </div>

          <div class="ms-history" data-reveal :style="{ transitionDelay: '80ms' }">
            <h3 class="ms-pending-title">历史订单</h3>
            <ul v-if="historyOrders.length" class="ms-history-list">
              <li v-for="order in historyOrders" :key="order.order_no_wsh">
                <span class="ms-history-name">{{ order.plan_name_wsh || order.plan_code_wsh || '会员套餐' }}</span>
                <span class="ms-history-meta tabular">{{ fmtDateTime(order.paid_at_wsh || order.created_at_wsh) }}</span>
                <span class="ms-history-amount tabular">{{ money(order.amount_wsh) }}</span>
                <span class="ms-history-status" :class="`is-${order.status_wsh}`">{{ orderStatusText(order.status_wsh) }}</span>
              </li>
            </ul>
            <p v-else class="ms-pending-empty">还没有会员订单。</p>
          </div>
        </div>
      </section>

      <!-- ═══ 04 · 权益流水 ═══ -->
      <section class="ms-section" aria-label="权益流水">
        <header class="ms-sec-head" data-reveal>
          <div class="ms-head-copy">
            <p class="ms-eyebrow ms-sec-eyebrow">
              <span class="ms-idx">04</span>
              <span class="ms-line" aria-hidden="true" />
              <span>流水</span>
            </p>
            <h2 class="ms-sec-title">权益使用记录</h2>
            <p class="ms-sec-desc">每一次抵扣都会留一条记录，金额与订单可逐笔核对。</p>
          </div>
          <span class="ms-sec-tag tabular">累计省下 {{ money(savedTotal) }}</span>
        </header>

        <ul v-if="usages.length" class="ms-usages" data-reveal>
          <li v-for="usage in usages" :key="usage.id_wsh">
            <span class="ms-usage-dot" aria-hidden="true" />
            <span class="ms-usage-text">{{ usageText(usage) }}</span>
            <span class="ms-usage-at tabular">{{ fmtDate(usage.used_at_wsh || usage.created_at_wsh) }}</span>
            <span class="ms-usage-saved tabular">省 {{ money(usage.amount_wsh) }}</span>
            <span class="ms-usage-status" :class="`is-${usage.usage_status_wsh}`">{{ usageStatusText(usage.usage_status_wsh) }}</span>
          </li>
        </ul>
        <p v-else class="ms-empty-inline">
          {{ active ? '本期还没有产生抵扣记录。' : '开通会员后，每一次折扣抵扣都会记录在这里。' }}
        </p>
      </section>
    </div>

    <!-- ═══ 开通弹层：确认 → 处理中 → 成功 ═══ -->
    <AppDialog
      :visible="dialogOpen"
      :title="dialogStage === 'done' ? '开通成功' : `${isRenewing ? '续费' : '开通'}${dialogPlan?.name_wsh || '会员'}`"
      :description="dialogStage === 'done' ? '会员已生效，折扣会在下单结算时自动生效。' : '确认后从钱包余额扣款，支付成功立即生效。'"
      size="md"
      @close="closeDialog"
    >
      <div v-if="dialogPlan" class="ms-dialog">
        <div class="ms-dialog-head" :style="dialogTierStyle">
          <TierEmblem :tier="dialogTier" :size="40" :heartbeat="dialogStage === 'done'" ring />
          <div class="ms-dialog-head-copy">
            <p class="ms-dialog-kicker">{{ dialogTier.en }} · {{ dialogTier.lv }}</p>
            <p class="ms-dialog-name">{{ dialogPlan.name_wsh }}</p>
            <p class="ms-dialog-tagline">{{ dialogTier.tagline }}</p>
          </div>
        </div>

        <dl class="ms-dialog-facts">
          <div>
            <dt>应付金额</dt>
            <dd class="tabular">{{ money(dialogPlan.price_wsh) }}</dd>
          </div>
          <div>
            <dt>有效期</dt>
            <dd class="tabular">{{ dialogPlan.duration_days_wsh ?? '—' }} 天</dd>
          </div>
          <div>
            <dt>服务折扣</dt>
            <dd>{{ discountLabel(dialogPlan.discount_rate_wsh) }}</dd>
          </div>
          <div>
            <dt>生效时间</dt>
            <dd>{{ isRenewing ? '支付后顺延' : '支付后立即' }}</dd>
          </div>
        </dl>

        <p v-if="dialogError" class="ms-dialog-error">{{ dialogError }}</p>
        <p v-else class="ms-dialog-note">
          {{ isRenewing ? '同一套餐续费，有效期在当前到期日上顺延。' : '到期不会自动续费，需要时再手动开通。' }}
        </p>

        <div v-if="dialogStage === 'paying'" class="ms-dialog-bar" aria-hidden="true">
          <span class="ms-dialog-bar-fill" />
        </div>
      </div>

      <template #footer>
        <template v-if="dialogStage === 'done'">
          <button type="button" class="ms-cta ms-cta-primary" @click="closeDialog">知道了</button>
        </template>
        <template v-else>
          <button type="button" class="ms-cta ms-cta-outline" :disabled="dialogStage === 'paying'" @click="closeDialog">再想想</button>
          <button type="button" class="ms-cta ms-cta-primary" :disabled="dialogStage === 'paying'" @click="confirmPlan">
            {{ dialogStage === 'paying' ? '处理中…' : `余额支付 ${money(dialogPlan?.price_wsh)}` }}
          </button>
        </template>
      </template>
    </AppDialog>

    <!-- 取消确认：取消后需要重新下单，误点代价比多一次点击大 -->
    <AppDialog
      :visible="Boolean(cancelTarget)"
      title="取消会员订单"
      description="取消后需要重新下单，已支付订单不受影响。"
      size="sm"
      @close="cancelTarget = null"
    >
      <p v-if="cancelTarget" class="ms-dialog-note">
        {{ cancelTarget.order_no_wsh }} · {{ money(cancelTarget.amount_wsh) }}
      </p>
      <template #footer>
        <button type="button" class="ms-cta ms-cta-outline" @click="cancelTarget = null">再想想</button>
        <button type="button" class="ms-cta ms-cta-danger" :disabled="Boolean(cancellingOrderNo)" @click="confirmCancel">
          {{ cancellingOrderNo ? '取消中…' : '确认取消' }}
        </button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import AppDialog from '@/components/common/AppDialog.vue'
import MemberIdentityCard from '@/components/membership/MemberIdentityCard.vue'
import MemberPlanCard from '@/components/membership/MemberPlanCard.vue'
import RollingNumber from '@/components/membership/RollingNumber.vue'
import TierEmblem from '@/components/membership/TierEmblem.vue'
import {
  discountLabel,
  fmtDate,
  fmtDateTime,
  getValidity,
  isActiveMembership,
  money,
  perDay,
  planExtras,
  tierOf,
  tierVars,
} from '@/domain/MembershipTier'
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

const loading = ref(true)
const membership = ref(null)
const plans = ref([])
const orders = ref([])
const usages = ref([])
const selectedId = ref(null)
const sweep = ref(0)

const submittingPlanId = ref(null)
const payingOrderNo = ref('')
const cancellingOrderNo = ref('')

const dialogOpen = ref(false)
const dialogPlan = ref(null)
const dialogStage = ref('confirm')
const dialogError = ref('')
const cancelTarget = ref(null)

/* ── 派生状态 ── */

const active = computed(() => isActiveMembership(membership.value))
const validity = computed(() => getValidity(membership.value))
const currentPlan = computed(() => {
  if (!active.value) return null
  return plans.value.find(plan => plan.id_wsh === membership.value?.plan_id_wsh) || null
})
/** 点了一张未开通的套餐卡 → 身份卡预览这一档 */
const previewPlan = computed(() => {
  if (selectedId.value == null) return null
  if (selectedId.value === currentPlan.value?.id_wsh) return null
  return plans.value.find(plan => plan.id_wsh === selectedId.value) || null
})

const pendingOrders = computed(() => orders.value.filter(order => order.status_wsh === 'pending'))
const pendingPlanIds = computed(() => pendingOrders.value.map(order => order.plan_id_wsh))
const historyOrders = computed(() => orders.value.filter(order => order.status_wsh !== 'pending'))
const savedTotal = computed(() => usages.value
  .filter(usage => usage.usage_status_wsh !== 'released')
  .reduce((sum, usage) => sum + Number(usage.amount_wsh || 0), 0))

/** 主推档：已开通的一档；未开通时为挚爱（level 3）。桌面端占约 46%，另两档各约 27% */
const featuredId = computed(() => currentPlan.value?.id_wsh
  ?? plans.value.find(plan => Number(plan.level_wsh) === 3)?.id_wsh
  ?? null)

/** 主推档在窄屏排第一，桌面端按等级顺序 */
const orderedPlans = computed(() => {
  const list = [...plans.value].sort((a, b) => Number(a.level_wsh || 0) - Number(b.level_wsh || 0))
  return list
})

const planGridStyle = computed(() => {
  if (!plans.value.length) return {}
  const cols = plans.value.map(plan => (plan.id_wsh === featuredId.value ? 'minmax(0,46fr)' : 'minmax(0,27fr)'))
  return { '--plan-cols': cols.join(' ') }
})

const usedPercent = computed(() => {
  if (!active.value || validity.value.usedFraction == null) return null
  return Math.round(validity.value.usedFraction * 100)
})

const facts = computed(() => {
  if (loading.value) {
    return [
      { key: 'tier', label: '当前身份', value: '--', delay: 0 },
      { key: 'discount', label: '服务折扣', value: '--', delay: 60 },
      { key: 'days', label: '剩余天数', value: '--', delay: 120 },
      { key: 'saved', label: '累计省下', value: '--', delay: 180 },
    ]
  }
  return [
    { key: 'tier', label: '当前身份', value: active.value ? (currentPlan.value?.name_wsh || membership.value?.plan_name_wsh || '会员') : '未开通', delay: 0 },
    { key: 'discount', label: '服务折扣', value: active.value ? discountLabel(membership.value?.discount_rate_wsh) : '—', delay: 60 },
    { key: 'days', label: '剩余天数', value: String(active.value ? (validity.value.remainingDays ?? 0) : 0), delay: 120 },
    { key: 'saved', label: '累计省下', value: money(savedTotal.value), delay: 180 },
  ]
})

const dialogTier = computed(() => tierOf(dialogPlan.value?.level_wsh))
const dialogTierStyle = computed(() => tierVars(dialogTier.value))
const isRenewing = computed(() => Boolean(currentPlan.value)
  && currentPlan.value.id_wsh === dialogPlan.value?.id_wsh
  && active.value)

/* ── 数据加载 ── */

onMounted(async () => {
  await loadAll()
  observeReveal()
})

let revealObserver = null

onBeforeUnmount(() => {
  if (revealObserver) revealObserver.disconnect()
})

/** 分区进入视口时播一次：上移 12px + 淡入，60ms 错开；滚回来不重播 */
function observeReveal() {
  const nodes = document.querySelectorAll('.ms-page [data-reveal]:not(.is-in)')
  if (!nodes.length) return
  if (typeof IntersectionObserver === 'undefined') {
    nodes.forEach(node => node.classList.add('is-in'))
    return
  }
  if (!revealObserver) {
    revealObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-in')
          revealObserver.unobserve(entry.target)
        }
      })
    }, { threshold: 0.12 })
  }
  nodes.forEach(node => revealObserver.observe(node))
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
    if (membershipRes.code === 200) membership.value = membershipRes.data || null
    else appStore.addToast(membershipRes.message || '会员状态加载失败', 'error')
    if (plansRes.code === 200) plans.value = Array.isArray(plansRes.data) ? plansRes.data : []
    else appStore.addToast(plansRes.message || '会员套餐加载失败', 'error')
    if (ordersRes.code === 200) orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : []
    if (usagesRes.code === 200) usages.value = Array.isArray(usagesRes.data) ? usagesRes.data : []
    selectedId.value = currentPlan.value?.id_wsh ?? null
  } catch (e) {
    appStore.addToast(e?.message || '会员数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 支付成功后只重取状态类数据，套餐列表不变，避免整页闪一次骨架 */
async function reloadState() {
  const [membershipRes, ordersRes, usagesRes] = await Promise.all([
    getMyMembership(),
    getMyMembershipOrders(),
    getMembershipUsages(),
  ])
  if (membershipRes.code === 200) membership.value = membershipRes.data || null
  if (ordersRes.code === 200) orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : []
  if (usagesRes.code === 200) usages.value = Array.isArray(usagesRes.data) ? usagesRes.data : []
}

async function loadOrders() {
  const res = await getMyMembershipOrders()
  if (res.code === 200) orders.value = Array.isArray(res.data) ? res.data : []
}

/* ── 交互 ── */

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openPlan(plan) {
  if (!plan) return
  // 不切 selectedId：开通弹层打开时身份卡要保持当前档材质，切预览会让卡面跟着变浅、像消失
  dialogPlan.value = plan
  dialogStage.value = 'confirm'
  dialogError.value = ''
  dialogOpen.value = true
}

function closeDialog() {
  if (dialogStage.value === 'paying') return
  dialogOpen.value = false
  dialogPlan.value = null
  dialogStage.value = 'confirm'
  dialogError.value = ''
}

/**
 * 一次点击走完全程：没有待支付订单就下一笔，然后直接付。
 * 有同套餐的待支付订单时复用那一笔，不重复下单（后端 request_id_wsh 之外的第二道防重）。
 */
async function confirmPlan() {
  const plan = dialogPlan.value
  if (!plan || dialogStage.value === 'paying') return
  dialogStage.value = 'paying'
  dialogError.value = ''
  submittingPlanId.value = plan.id_wsh
  try {
    let order = pendingOrders.value.find(item => item.plan_id_wsh === plan.id_wsh) || null
    if (!order) {
      const created = await createMembershipOrder({
        plan_id_wsh: plan.id_wsh,
        pay_method_wsh: 'balance',
        request_id_wsh: createRequestId(plan.id_wsh),
      })
      if (created.code !== 200 || !created.data?.order_no_wsh) {
        throw new Error(created.message || '会员订单创建失败')
      }
      order = created.data
      await loadOrders()
    }
    const paid = await payMembershipOrder(order.order_no_wsh)
    if (paid.code !== 200) throw new Error(paid.message || '会员订单支付失败')
    await reloadState()
    selectedId.value = currentPlan.value?.id_wsh ?? plan.id_wsh
    sweep.value += 1
    dialogStage.value = 'done'
    appStore.addToast('会员已开通', 'success')
  } catch (e) {
    dialogStage.value = 'confirm'
    dialogError.value = e?.response?.data?.message || e?.message || '开通失败，请稍后重试'
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
      await reloadState()
      selectedId.value = currentPlan.value?.id_wsh ?? selectedId.value
      sweep.value += 1
    } else {
      appStore.addToast(res.message || '会员订单支付失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || e?.message || '会员订单支付失败', 'error')
  } finally {
    payingOrderNo.value = ''
  }
}

function cancelOrder(order) {
  if (cancellingOrderNo.value) return
  cancelTarget.value = order
}

/** 取消要走一次确认：取消后需要重新下单，误点代价比多一次点击大 */
async function confirmCancel() {
  const order = cancelTarget.value
  if (!order || cancellingOrderNo.value) return
  cancellingOrderNo.value = order.order_no_wsh
  try {
    const res = await cancelMembershipOrder(order.order_no_wsh)
    if (res.code === 200) {
      appStore.addToast('会员订单已取消', 'success')
      cancelTarget.value = null
      await loadOrders()
    } else {
      appStore.addToast(res.message || '会员订单取消失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || e?.message || '会员订单取消失败', 'error')
  } finally {
    cancellingOrderNo.value = ''
  }
}

/* ── 展示口径 ── */

function createRequestId(planId) {
  const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `member-${planId}-${uuid}`
}

function amountText(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function perDayText(plan) {
  const day = perDay(plan)
  return day == null ? '—' : `¥${day.toFixed(2)} / 天`
}

function extrasText(plan) {
  const extras = planExtras(plan)
  if (!extras.length) return '—'
  return extras.length === 1 ? extras[0] : `${extras.length} 项`
}

const USAGE_LABELS = {
  discount: '下单折扣',
  order_discount: '下单折扣',
  coupon: '会员优惠券',
  free_cancel: '免费取消',
  priority: '优先接单',
}

/** 流水标题：类型文案优先（code 是业务流水号，不是给人看的标签） */
function usageText(usage) {
  const label = USAGE_LABELS[usage.benefit_type_wsh]
    || USAGE_LABELS[usage.benefit_code_wsh]
    || usage.benefit_code_wsh
    || '权益抵扣'
  const business = usage.business_type_wsh === 'order' && usage.business_id_wsh
    ? ` · 订单 ${usage.business_id_wsh}`
    : ''
  return `${label}${business}`
}

function usageStatusText(status) {
  return { locked: '已锁定', used: '已抵扣', released: '已释放' }[status] || status || '—'
}

function orderStatusText(status) {
  return {
    pending: '待支付',
    paid: '已支付',
    cancelled: '已取消',
    expired: '已过期',
    refunded: '已退款',
  }[status] || status || '—'
}
</script>

<style scoped>
.ms-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.ms-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* Breadcrumb */
.ms-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.ms-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ms-crumb-link:hover { color: var(--ref-ink); }
.ms-crumb-sep { color: var(--ref-line); }
.ms-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow */
.ms-eyebrow { display: flex; align-items: center; gap: 10px; font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }
.ms-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ms-idx { font-variant-numeric: tabular-nums; }
.ms-line { width: 24px; height: 1px; background: var(--ref-line); }

/* Hero */
.ms-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 20px 24px; padding: 40px 0 28px; }
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
.ms-sub { margin: 14px 0 0; max-width: 620px; font-size: 14px; line-height: 1.75; color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent); }
.ms-ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 16px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 150ms ease, color 150ms ease, transform 150ms ease;
}
.ms-ghost-btn:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); transform: translateY(-1px); }
.ms-ghost-count { color: var(--ref-brand); font-weight: 600; }

/* Facts */
.ms-facts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin: 8px 0 0; }
.ms-fact {
  padding: 20px 24px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.ms-fact:hover { border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent); transform: translateY(-2px); box-shadow: var(--shadow-lift); }
.ms-fact dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  line-height: 1.15;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.ms-fact dt { margin-top: 10px; font-size: 12.5px; color: var(--ref-muted); }

/* Section */
.ms-section { margin-top: 56px; scroll-margin-top: 24px; }
.ms-sec-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 16px 24px; }
.ms-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ms-sec-title { margin: 10px 0 0; font-family: var(--ref-font-display); font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink); }
.ms-sec-desc { margin: 8px 0 0; max-width: 620px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.ms-sec-tag {
  flex: 0 0 auto;
  padding: 6px 12px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-pill);
  background: var(--ref-surface);
  font-size: 12px;
  color: var(--ref-muted);
}

/* 01 · 身份卡 + 有效期 */
.ms-duo {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  gap: 20px;
  margin-top: 22px;
  align-items: stretch;
}
.ms-identity { grid-column: 1; }

.ms-validity {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 24px;
  padding: 22px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
}
.ms-validity-kicker {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ms-validity-title { margin: 8px 0 0; font-size: 17px; font-weight: 600; line-height: 1.35; color: var(--ref-ink); }
.ms-validity-days { display: flex; align-items: baseline; gap: 8px; margin: 22px 0 0; }
.ms-validity-num { font-family: var(--ref-font-display); font-size: 40px; line-height: 1; color: var(--ref-ink); }
.ms-validity-unit { font-size: 13px; color: var(--ref-ink-soft); }
.ms-progress-wrap { margin-top: 18px; }
.ms-progress {
  height: 6px;
  width: 100%;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--ref-sand);
}
.ms-progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--ref-brand);
  transform-origin: left;
  animation: ms-grow 600ms cubic-bezier(0.23, 1, 0.32, 1) both;
}
.ms-progress-note { margin: 8px 0 0; font-size: 12px; color: var(--ref-muted); }
.ms-validity-note { margin: 12px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }
.ms-validity-dates {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
  padding-top: 18px;
  border-top: 1px solid var(--ref-line);
}
.ms-validity-dates dt { font-size: 12px; color: var(--ref-muted); }
.ms-validity-dates dd {
  margin: 4px 0 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 13px;
  color: var(--ref-ink);
}

/* 02 · 套餐 */
/* 主推档更宽：列宽由 --plan-cols 注入（46fr / 27fr / 27fr），窄屏退回等分 */
.ms-plans {
  display: grid;
  grid-template-columns: var(--plan-cols, repeat(3, minmax(0, 1fr)));
  gap: 18px;
  margin-top: 22px;
  align-items: stretch;
}
.ms-compare-wrap {
  margin-top: 18px;
  overflow-x: auto;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
}
.ms-compare { width: 100%; border-collapse: collapse; font-size: 13px; }
.ms-compare-cap { padding: 14px 18px 0; text-align: left; font-size: 12px; color: var(--ref-muted); }
.ms-compare th,
.ms-compare td { padding: 11px 18px; text-align: left; white-space: nowrap; border-bottom: 1px solid var(--ref-line); }
.ms-compare thead th { font-weight: 600; color: var(--ref-ink); }
.ms-compare tbody th { font-weight: 400; color: var(--ref-muted); }
.ms-compare tbody tr:last-child th,
.ms-compare tbody tr:last-child td { border-bottom: 0; }
.ms-compare .is-selected { background: color-mix(in srgb, var(--ref-brand) 6%, transparent); }
.ms-compare-pick {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}
.ms-compare-pick:hover { color: var(--ref-brand); }

/* 03 · 订单 */
.ms-order-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; margin-top: 22px; }
.ms-pending,
.ms-history { padding: 22px; border: 1px solid var(--ref-line); border-radius: var(--radius-card); background: var(--ref-surface); }
.ms-pending-title { margin: 0; font-family: var(--ref-font-display); font-size: 17px; font-weight: 500; color: var(--ref-ink); }
.ms-pending-item { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--ref-line); }
.ms-pending-item:first-of-type { border-top: 0; padding-top: 0; }
.ms-pending-name { margin: 0; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }
.ms-pending-amount { margin: 12px 0 0; font-family: var(--ref-font-display); font-size: 28px; line-height: 1; letter-spacing: -0.02em; color: var(--ref-ink); }
.ms-yen { font-size: 14px; color: color-mix(in srgb, var(--ref-brand) 72%, transparent); }
.ms-pending-meta { margin: 8px 0 0; font-size: 11px; color: var(--ref-muted); }
.ms-pending-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
.ms-pending-empty { margin: 12px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }

.ms-history-list { margin: 12px 0 0; padding: 0; list-style: none; display: grid; gap: 2px; }
.ms-history-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid var(--ref-line);
  font-size: 12.5px;
}
.ms-history-list li:first-child { border-top: 0; }
.ms-history-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ref-ink); }
.ms-history-meta { color: var(--ref-muted); font-size: 11.5px; }
.ms-history-amount { color: var(--ref-ink-soft); }
.ms-history-status { padding: 2px 8px; border-radius: var(--radius-pill); font-size: 11px; background: var(--ref-sand); color: var(--ref-ink-soft); }
.ms-history-status.is-paid { background: color-mix(in srgb, #3f5347 12%, transparent); color: #3f5347; }
.ms-history-status.is-pending { background: color-mix(in srgb, var(--ref-brand) 12%, transparent); color: var(--ref-brand); }

/* 04 · 流水 */
.ms-usages { margin: 18px 0 0; padding: 0; list-style: none; display: grid; gap: 2px; }
.ms-usages li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  font-size: 13px;
}
.ms-usages li + li { margin-top: 8px; }
.ms-usage-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--ref-brand); }
.ms-usage-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ref-ink); }
.ms-usage-at { font-size: 11.5px; color: var(--ref-muted); }
.ms-usage-saved { font-weight: 600; color: var(--ref-brand); }
.ms-usage-status { padding: 2px 8px; border-radius: var(--radius-pill); font-size: 11px; background: var(--ref-sand); color: var(--ref-ink-soft); }
.ms-usage-status.is-used { background: color-mix(in srgb, #3f5347 12%, transparent); color: #3f5347; }
.ms-usage-status.is-released { background: transparent; color: var(--ref-muted); border: 1px solid var(--ref-line); }
.ms-empty-inline { margin: 16px 0 0; font-size: 13px; color: var(--ref-muted); }

/* 按钮 */
.ms-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  padding: 0 16px;
  border-radius: var(--radius-control);
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease, transform 150ms ease;
}
.ms-cta:hover:not(:disabled) { transform: translateY(-1px); }
.ms-cta:disabled { opacity: 0.55; cursor: not-allowed; }
.ms-cta-primary { background: var(--ref-brand); border-color: var(--ref-brand); color: #fff; }
.ms-cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); border-color: var(--ref-brand-deep); }
.ms-cta-outline { background: var(--ref-surface); border-color: var(--ref-line); color: var(--ref-ink-soft); }
.ms-cta-outline:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); }
.ms-cta-danger { background: transparent; border-color: color-mix(in srgb, var(--color-danger) 30%, transparent); color: var(--color-danger); }
.ms-cta-danger:hover:not(:disabled) { background: color-mix(in srgb, var(--color-danger) 8%, transparent); }

/* 空态 / 骨架 */
.ms-empty {
  margin-top: 22px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  text-align: center;
}
.ms-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.ms-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.ms-empty .ms-cta { margin-top: 20px; }
.ms-skeleton {
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: ms-shimmer 1.3s linear infinite;
}
.ms-skeleton-card { height: 248px; }
.ms-skeleton-plan { height: 380px; }

/* 弹层 */
.ms-dialog { display: flex; flex-direction: column; gap: 18px; }
.ms-dialog-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--t-card-line);
  border-radius: var(--radius-control);
  background: var(--t-card);
}
.ms-dialog-head-copy { min-width: 0; }
.ms-dialog-kicker {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--t-card-sub);
}
.ms-dialog-name { margin: 6px 0 0; font-family: var(--ref-font-display); font-size: 18px; font-weight: 600; color: var(--t-card-ink); }
.ms-dialog-tagline { margin: 4px 0 0; font-size: 12.5px; line-height: 1.6; color: var(--t-card-sub); }
.ms-dialog-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}
.ms-dialog-facts dt { font-size: 11.5px; color: var(--ref-muted); }
.ms-dialog-facts dd { margin: 4px 0 0; font-size: 14px; font-weight: 500; color: var(--ref-ink); }
.ms-dialog-note { margin: 0; font-size: 12.5px; line-height: 1.7; color: var(--ref-muted); }
.ms-dialog-error { margin: 0; font-size: 12.5px; line-height: 1.7; color: var(--color-danger); }
.ms-dialog-bar { height: 3px; overflow: hidden; border-radius: var(--radius-pill); background: var(--ref-sand); }
.ms-dialog-bar-fill {
  display: block;
  width: 40%;
  height: 100%;
  border-radius: inherit;
  background: var(--ref-brand);
  animation: ms-indeterminate 1.1s cubic-bezier(0.45, 0, 0.55, 1) infinite;
}

/* 分区进入视口 */
[data-reveal] {
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 420ms cubic-bezier(0.23, 1, 0.32, 1), transform 420ms cubic-bezier(0.23, 1, 0.32, 1);
}
[data-reveal].is-in { opacity: 1; transform: none; }

@keyframes ms-shimmer { to { background-position: -200% 0; } }
@keyframes ms-grow { from { transform: scaleX(0); } to { transform: scaleX(1); } }
@keyframes ms-indeterminate {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(260%); }
}

/* 响应式 */
@media (max-width: 1080px) {
  .ms-plans { grid-template-columns: repeat(2, minmax(0, 1fr)) !important; }
  /* 窄屏主推档提到第一个，别让人先划过两张卡才看到主推 */
  .ms-plans > .is-featured { order: -1; }
  .ms-duo { grid-template-columns: 1fr; }
  .ms-order-grid { grid-template-columns: 1fr; }
}
@media (max-width: 980px) {
  .ms-facts { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .ms-shell { padding: 0 16px; }
  .ms-head { padding: 30px 0 22px; }
  .ms-sub { font-size: 13.5px; }
  .ms-facts { grid-template-columns: 1fr; gap: 10px; }
  .ms-fact { padding: 16px 20px; }
  .ms-fact dd { font-size: 22px; }
  .ms-section { margin-top: 44px; }
  .ms-plans { grid-template-columns: 1fr !important; }
  .ms-validity,
  .ms-pending,
  .ms-history { padding: 18px; }
  .ms-usages li { grid-template-columns: auto minmax(0, 1fr) auto; row-gap: 4px; }
  .ms-usage-at { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  [data-reveal] { opacity: 1; transform: none; transition: none; }
  .ms-skeleton,
  .ms-progress-fill,
  .ms-dialog-bar-fill { animation: none; }
  .ms-fact,
  .ms-cta,
  .ms-ghost-btn { transition: none; }
}
</style>
