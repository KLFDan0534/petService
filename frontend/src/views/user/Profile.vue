<template>
  <div class="pr-page">
    <div class="pr-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="pr-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="pr-crumb-link">首页</router-link>
        <span class="pr-crumb-sep" aria-hidden="true">›</span>
        <span class="pr-crumb-here">个人中心</span>
      </nav>

      <!-- ═══ Loading / error ═══ -->
      <template v-if="loading">
        <div class="ph-skeleton"></div>
        <div class="ps-skeleton-stack">
          <div class="ps-skeleton"></div>
          <div class="ps-skeleton"></div>
        </div>
      </template>

      <template v-else-if="loadError">
        <div class="pr-error">
          <p class="pr-error-title">个人中心加载失败</p>
          <p class="pr-error-desc">{{ loadError }}</p>
          <button type="button" class="cta cta-primary" @click="reloadAll">重新加载</button>
        </div>
      </template>

      <template v-else>
        <!-- ═══════════════════════════════════════════
             两栏骨架：左栏（身份 / 余额）
                       右栏（我的服务 / 会员权益）
             说明：顶部导航已不再挂「订单」「收藏」，
             这两个入口下沉到右栏 01 的两张主卡，权重最高。
             ═══════════════════════════════════════════ -->
        <div class="pr-layout">

          <!-- ───────────── 左栏：我是谁 + 我的钱 ───────────── -->
          <div class="pr-col-left">

            <!-- ① 个人身份卡（紧凑） -->
            <section class="ph-card ph-card-sm" aria-label="个人资料">
              <div class="ph-top-sm">
                <div class="ph-identity-sm">
                  <button
                    type="button"
                    class="ph-avatar-btn ph-avatar-sm"
                    :disabled="avatarUploading"
                    aria-label="更换头像"
                    @click="triggerAvatarInput"
                  >
                    <img v-if="avatarUrl" :src="avatarUrl" :alt="`${displayName} 的头像`" class="ph-avatar-img">
                    <span v-else class="ph-avatar-initial">{{ avatarInitial }}</span>
                    <span class="ph-avatar-overlay">
                      <AppIcon aria-hidden="true"><Camera /></AppIcon>
                    </span>
                    <span v-if="avatarUploading" class="ph-avatar-busy">
                      <span class="ph-spinner" aria-hidden="true"></span>
                    </span>
                  </button>
                  <input
                    ref="avatarInputRef"
                    type="file"
                    accept="image/*"
                    class="ph-avatar-file"
                    tabindex="-1"
                    @change="onAvatarFile"
                  >

                  <div class="ph-id-copy">
                    <h1 class="ph-name ph-name-sm">{{ displayName }}</h1>
                    <div class="ph-tags ph-tags-sm">
                      <MemberPill
                        v-if="memberTag"
                        :name="memberTag.name"
                        :level="memberTag.level"
                        :discount-rate="memberTag.discountRate"
                      />
                      <span v-for="role in roleTags" :key="role" class="tag-pill tag-pill-sm">{{ roleLabel(role) }}</span>
                    </div>
                    <p class="ph-meta ph-meta-sm">
                      <span v-if="profile.username_wsh">@{{ profile.username_wsh }}</span>
                      <span v-if="profile.username_wsh && joinedLabel" class="ph-meta-sep" aria-hidden="true">·</span>
                      <span v-if="joinedLabel">{{ joinedLabel }}</span>
                    </p>
                  </div>
                </div>

                <div class="ph-actions ph-actions-sm">
                  <button type="button" class="cta cta-dark cta-sm" @click="openEditDialog">编辑资料</button>
                  <router-link to="/profile/account" class="ph-link-btn ph-link-btn-sm">账户与安全</router-link>
                </div>
              </div>

              <!-- 服务状态条（状态一行 + 入口一行） -->
              <div v-if="activeStay" class="ph-stay ph-stay-sm">
                <p class="ph-stay-text ph-stay-text-sm">
                  <span class="ph-stay-dot" aria-hidden="true"></span>
                  <span class="ph-stay-trunc">
                    <span class="ph-stay-name">{{ activeStay.pet_name_wsh || '你的宠物' }}</span>
                    正在 {{ activeStay.merchant_name_wsh || '门店' }}
                    <span class="ph-stay-sep" aria-hidden="true">·</span>
                    {{ statusLabel(activeStay.status_wsh) }}
                  </span>
                </p>
                <router-link to="/orders" class="ph-stay-link ph-stay-link-sm">查看服务动态 →</router-link>
              </div>
            </section>

            <!-- ② 余额卡（紧凑高亮） -->
            <section class="wp-balance wp-balance-sm" aria-label="账户余额">
              <div>
                <p class="wp-b-label">可用余额</p>
                <p class="wp-b-amount wp-b-amount-sm">
                  <span class="wp-b-yen">¥</span>
                  <span>{{ formatMoney(walletAvailable) }}</span>
                </p>
                <div class="wp-b-sub wp-b-sub-sm">
                  <div>
                    <p class="wp-b-sub-label">账户总额</p>
                    <p class="wp-b-sub-val">¥ {{ formatMoney(wallet.balance_wsh) }}</p>
                  </div>
                  <div>
                    <p class="wp-b-sub-label">冻结金额</p>
                    <p class="wp-b-sub-val">¥ {{ formatMoney(wallet.frozen_amount_wsh) }}</p>
                  </div>
                </div>
              </div>
              <div>
                <div class="wp-b-actions">
                  <button type="button" class="wp-btn wp-btn-light wp-btn-sm" @click="openRecharge">充值</button>
                  <button type="button" class="wp-btn wp-btn-ghost wp-btn-sm" @click="openWithdraw">提现</button>
                </div>
                <router-link to="/wallet" class="wp-b-wallet">
                  查看钱包 <span aria-hidden="true">→</span>
                </router-link>
              </div>
            </section>
          </div>

          <!-- ───────────── 右栏：我的服务 + 会员权益 ───────────── -->
          <div class="pr-col-right">

            <!-- 我的服务：宠物 / 订单为主卡，工单 / 投诉 / 收藏 / 身份为次要入口。
                 不设区块标题，主卡顶部与左栏身份卡顶边对齐 -->
            <section aria-label="我的服务">
              <!-- 主入口 -->
              <div class="svc-heroes">
                <router-link
                  v-for="card in heroCards"
                  :key="card.key"
                  :to="card.to"
                  class="svc-hero"
                >
                  <span class="svc-hero-head">
                    <span class="svc-hero-icon">
                      <AppIcon aria-hidden="true"><component :is="card.icon" /></AppIcon>
                    </span>
                    <span class="svc-hero-go" aria-hidden="true">→</span>
                  </span>
                  <span class="svc-hero-body">
                    <span class="svc-hero-title">{{ card.title }}</span>
                    <span class="svc-hero-desc">{{ card.desc }}</span>
                    <span class="svc-hero-foot">
                      <span class="svc-hero-foot-label">{{ card.footLabel }}</span>
                      <span class="svc-hero-foot-val">{{ card.footVal }}</span>
                    </span>
                  </span>
                </router-link>
              </div>

              <!-- 次要入口：一行四格 -->
              <nav class="svc-tiles" aria-label="更多入口">
                <router-link
                  v-for="entry in serviceEntries"
                  :key="entry.key"
                  :to="entry.to"
                  class="svc-tile"
                >
                  <span class="svc-tile-icon">
                    <AppIcon aria-hidden="true"><component :is="entry.icon" /></AppIcon>
                  </span>
                  <span class="svc-tile-copy">
                    <span class="svc-tile-title">{{ entry.label }}</span>
                    <span class="svc-tile-desc">{{ entry.desc }}</span>
                  </span>
                </router-link>
              </nav>
            </section>

            <!-- 会员权益 -->
            <section aria-label="权益与入口">
              <header class="ps-head">
                <div class="ps-head-copy">
                  <h2 class="ps-title">为你推荐</h2>
                </div>
              </header>

              <div class="rec-grid">
                <router-link
                  v-for="action in memberActions"
                  :key="action.key"
                  :to="action.to"
                  class="rec-card"
                >
                  <span class="rec-top">
                    <span class="rec-icon">
                      <AppIcon aria-hidden="true"><component :is="action.icon" /></AppIcon>
                    </span>
                    <span class="rec-arrow" aria-hidden="true">→</span>
                  </span>
                  <span class="rec-body">
                    <span class="rec-title">{{ action.label }}</span>
                    <span class="rec-desc">
                      {{ action.key === 'membership' && memberTag
                        ? `${memberTag.name} · 下单 ${Math.round((memberTag.discountRate ?? 1) * 1000) / 100} 折`
                        : action.desc }}
                    </span>
                  </span>
                </router-link>
              </div>
            </section>
          </div>
        </div>

      </template>
    </div>

    <!-- ═══════════════════════════════════════════
         DIALOGS
         ═══════════════════════════════════════════ -->

    <!-- 编辑资料 -->
    <div v-if="editDialogOpen" class="dlg-overlay" @click.self="editDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="编辑资料">
        <div class="dlg-head">
          <h3 class="dlg-title">编辑资料</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="editDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-avatar-row">
            <button type="button" class="dlg-avatar" :disabled="avatarUploading" @click="triggerAvatarInput">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像">
              <span v-else>{{ avatarInitial }}</span>
              <span class="dlg-avatar-overlay"><AppIcon aria-hidden="true"><Camera /></AppIcon></span>
            </button>
            <p class="dlg-avatar-hint">点击头像上传，支持 JPG / PNG，不超过 5MB</p>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-nickname">昵称</label>
            <input id="dlg-nickname" v-model="editDraft.nickname_wsh" class="dlg-input" placeholder="请输入昵称">
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-gender">性别</label>
            <select id="dlg-gender" v-model.number="editDraft.gender_wsh" class="dlg-input">
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="editDialogOpen = false">取消</button>
          <button type="button" class="cta cta-primary" :disabled="submitting" @click="submitProfileEdit">
            {{ submitting ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 充值 -->
    <div v-if="rechargeDialogOpen" class="dlg-overlay" @click.self="rechargeDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="充值">
        <div class="dlg-head">
          <h3 class="dlg-title">余额充值</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="rechargeDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-balance">
            <span>当前余额</span>
            <b>¥ {{ formatMoney(wallet.balance_wsh) }}</b>
            <small>冻结金额 ¥ {{ formatMoney(wallet.frozen_amount_wsh) }}</small>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-recharge-amount">充值金额 (¥)</label>
            <input id="dlg-recharge-amount" v-model="rechargeAmount" type="number" min="0.01" step="0.01" class="dlg-input" placeholder="输入金额">
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-recharge-method">支付方式</label>
            <select id="dlg-recharge-method" v-model="rechargeMethod" class="dlg-input">
              <option value="wechat">微信支付</option>
              <option value="alipay">支付宝</option>
              <option value="bank">银行卡</option>
            </select>
          </div>
          <p class="dlg-note">充值成功后即时增加余额并生成充值记录，金额不能为 0 或负数。</p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="rechargeDialogOpen = false">取消</button>
          <button type="button" class="cta cta-primary" :disabled="rechargeSubmitting" @click="submitRecharge">
            {{ rechargeSubmitting ? '充值中...' : '确认充值' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 提现 -->
    <div v-if="withdrawDialogOpen" class="dlg-overlay" @click.self="withdrawDialogOpen = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="提现">
        <div class="dlg-head">
          <h3 class="dlg-title">申请提现</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="withdrawDialogOpen = false">✕</button>
        </div>
        <div class="dlg-body">
          <div class="dlg-balance">
            <span>可用余额</span>
            <b>¥ {{ formatMoney(walletAvailable) }}</b>
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-withdraw-amount">提现金额 (¥)</label>
            <input id="dlg-withdraw-amount" v-model="withdrawAmount" type="number" step="0.01" class="dlg-input" placeholder="输入金额" :max="walletAvailable">
          </div>
          <div class="dlg-field">
            <label class="dlg-label" for="dlg-withdraw-account">收款账户</label>
            <input id="dlg-withdraw-account" v-model="withdrawAccount" class="dlg-input" placeholder="支付宝 / 银行卡号">
          </div>
          <p class="dlg-note">提交后需经审核与打款，审核期间对应余额将被冻结。</p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="withdrawDialogOpen = false">取消</button>
          <button
            type="button"
            class="cta cta-primary"
            :disabled="withdrawSubmitting || !withdrawAmount || !withdrawAccount"
            @click="submitWithdraw"
          >
            {{ withdrawSubmitting ? '提交中...' : '申请提现' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Camera,
  ChatDotRound,
  Collection,
  Medal,
  Message,
  Shop,
  Star,
  Ticket,
  Warning,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getCurrentUser, updateCurrentUser } from '@/api/auth'
import { uploadFileToDirectory } from '@/api/file'
import { getMyWallet, getMyTransactions, getMyWithdrawals, createWithdrawal, rechargeWallet, getMyTips } from '@/api/wallet'
import { getUserStatistics } from '@/api/statistics'
import { getOrders } from '@/api/order'
import { getMyMembership } from '@/api/membership'
import { getPage as getFavoritePage } from '@/services/favoriteService'
import { TransactionStatus, WithdrawalStatus, enrichWithStatus } from '@/constants/statusMaps'
import MemberPill from '@/components/membership/MemberPill.vue'
import { isActiveMembership, memberName } from '@/domain/MembershipTier'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

/* status vocabulary (mirrors Orders/orderMeta) */
const ORDER_STATUS_LABEL = {
  pending: '待付款', paid: '已支付', confirmed: '待接收', received: '已接收',
  in_progress: '服务中', completed: '已完成', cancelled: '已取消', refunding: '退款中', refunded: '已退款',
}
const ACTIVE_STATUSES = ['pending', 'paid', 'confirmed', 'received', 'in_progress']
const STAYING_STATUSES = ['received', 'in_progress']
const NON_BILLED = ['pending', 'cancelled', 'refunded']

const ROLE_LABEL = {
  OWNER: '宠物家长', USER: '宠物家长', MERCHANT: '商家', KEEPER: '照护师',
  CS: '客服', CUSTOMER_SERVICE: '客服', ADMIN: '管理员',
}
function roleLabel(role) { return ROLE_LABEL[String(role).toUpperCase()] || role }

function statusLabel(status) {
  return ORDER_STATUS_LABEL[String(status ?? '')] || status || '未知状态'
}

/* presentation helpers */
const TYPE_MAP = {
  recharge: '充值', admin_adjust: '管理员调整', order_pay: '订单支付',
  order_refund: '退款', withdrawal: '提现', membership: '会员', tip: '打赏',
  membership_payment: '会员支付', payment: '订单支付',
  coupon_subsidy: '优惠券补贴', reject_subsidy: '补贴退回',
}
function typeLabel(t) { return TYPE_MAP[t.type_wsh] || t.business_type_wsh || t.type_wsh }
function withdrawalBadge(status) {
  const map = {
    pending: { label: '待审核', tone: 'action' }, approved: { label: '已通过', tone: 'queued' },
    completed: { label: '已打款', tone: 'active' }, rejected: { label: '已驳回', tone: 'closed' },
    cancelled: { label: '已取消', tone: 'closed' },
  }
  return map[String(status ?? '')] || { label: status || '未知状态', tone: 'closed' }
}

function formatMoney(value) {
  const n = Number(value || 0)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '0.00'
}
function fmtTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}
function orderAmount(order) {
  return Number(order.final_amount_wsh || order.total_amount_wsh || 0) || 0
}
function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

/* state */
const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const avatarUploading = ref(false)
const avatarInputRef = ref(null)

const profile = reactive({
  username_wsh: '', nickname_wsh: '', phone_wsh: '', avatar_wsh: '', gender_wsh: 0,
  email_wsh: '', real_name_wsh: '', id_card_no_wsh: '', real_name_status_wsh: 0,
  payment_password_set_wsh: false, created_at_wsh: '',
})
const stats = ref({ pets_wsh: 0, active_orders_wsh: 0, completed_orders_wsh: 0, total_spent_wsh: 0 })
const wallet = ref({})
const transactions = ref([])
const withdrawals = ref([])
const tips = ref([])
const orders = ref([])
const membership = ref(null)
/* 收藏总数：只给「我的收藏」入口卡做计数，取不到就显示 0 */
const favoriteTotal = ref(0)

/* ledger */
const ledgerTab = ref('transactions')
const ledgerTabs = [
  { key: 'transactions', label: '交易记录' },
  { key: 'withdrawals', label: '提现记录' },
  { key: 'tips', label: '打赏记录' },
]

/* dialogs */
const editDialogOpen = ref(false)
/* 编辑弹窗的草稿：只有点“保存修改”并成功后才写回 profile，避免切换选项就改掉页面数据 */
const editDraft = reactive({ nickname_wsh: '', gender_wsh: 0 })
const rechargeDialogOpen = ref(false)
const withdrawDialogOpen = ref(false)
const deleteDialogOpen = ref(false)
const rechargeAmount = ref('')
const rechargeMethod = ref('wechat')
const rechargeSubmitting = ref(false)
const withdrawAmount = ref('')
const withdrawAccount = ref('')
const withdrawSubmitting = ref(false)

/* computed */
const avatarUrl = computed(() => profile.avatar_wsh || authStore.user?.avatar_wsh || '')
const displayName = computed(() => profile.nickname_wsh || authStore.user?.nickname_wsh || authStore.user?.username_wsh || '宠物家长')
const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const joinedLabel = computed(() => {
  const value = profile.created_at_wsh || authStore.user?.created_at_wsh
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月加入`
})
const roleTags = computed(() => {
  const roles = authStore.user?.roles_wsh?.length ? authStore.user.roles_wsh : ['OWNER']
  return roles
})

/**
 * 会员胶囊：只有会员生效时才渲染（没有会员不占位、不写「未开通」）。
 * 名字取后端套餐名，等级只认 level_wsh —— 颜色与徽记都按等级取，不按名字猜。
 */
const memberTag = computed(() => {
  if (!isActiveMembership(membership.value)) return null
  const name = memberName(membership.value)
  if (!name) return null
  return {
    name,
    level: membership.value?.level_wsh ?? 1,
    discountRate: membership.value?.discount_rate_wsh,
  }
})

const activeStay = computed(() =>
  orders.value.find(o => STAYING_STATUSES.includes(String(o.status_wsh))) ?? null
)

const summary = computed(() => {
  const list = orders.value
  const billed = list.filter(o => !NON_BILLED.includes(String(o.status_wsh)))
  return {
    pets_wsh: stats.value.pets_wsh ?? 0,
    active_orders_wsh: stats.value.active_orders_wsh ?? list.filter(o => ACTIVE_STATUSES.includes(String(o.status_wsh))).length,
    completed_orders_wsh: stats.value.completed_orders_wsh ?? list.filter(o => o.status_wsh === 'completed').length,
    total_spent_wsh: stats.value.total_spent_wsh ?? billed.reduce((total, o) => total + orderAmount(o), 0),
  }
})

const walletAvailable = computed(() =>
  Math.max(Number(wallet.value.balance_wsh || 0) - Number(wallet.value.frozen_amount_wsh || 0), 0)
)

const ledgerCounts = computed(() => ({
  transactions: transactions.value.length,
  withdrawals: withdrawals.value.length,
  tips: tips.value.length,
}))
const ledgerTabLabel = computed(() =>
  ledgerTabs.find(t => t.key === ledgerTab.value)?.label || ''
)

/* 我的服务：两张主卡（顺序即展示顺序：宠物在前、订单在后） */
const heroCards = computed(() => [
  {
    key: 'pets', to: '/pets', icon: Collection, title: '我的宠物',
    desc: '疫苗 · 健康 · 照护记录',
    footLabel: '在册宠物', footVal: `${summary.value.pets_wsh} 只`,
  },
  {
    key: 'orders', to: '/orders', icon: Ticket, title: '我的订单',
    desc: `${summary.value.active_orders_wsh} 张进行中 · 已完成 ${summary.value.completed_orders_wsh} 次`,
    footLabel: '累计照护投入', footVal: `¥ ${formatMoney(summary.value.total_spent_wsh)}`,
  },
])

/* 次要入口：工单 / 投诉 / 收藏 / 身份与服务 */
const serviceEntries = computed(() => [
  { key: 'tickets', label: '我的工单', desc: '申诉与沟通记录', to: '/tickets', icon: ChatDotRound },
  { key: 'complaints', label: '我的投诉', desc: '订单与门店问题', to: '/complaints', icon: Warning },
  { key: 'favorites', label: '我的收藏', desc: `已收藏 ${favoriteTotal.value} 项`, to: '/favorites', icon: Star },
  { key: 'identity', label: '身份与服务', desc: '商家 · 照护师 · 客服', to: '/profile/identity', icon: Shop },
])

/* 会员权益：只留会员中心与优惠券，顺序按重要度排，不再靠 filter+reverse 猜顺序 */
const memberActions = [
  { key: 'membership', label: '会员中心', desc: '套餐、权益与会员订单', to: '/membership', icon: Medal },
  { key: 'coupons', label: '我的优惠券', desc: '可领取与已拥有的券', to: '/coupons', icon: Ticket },
]

/* data loading */
async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    await Promise.all([
      getCurrentUser(),
      getUserStatistics(),
    ]).then(([ur, sr]) => {
      if (ur.code === 200) syncUser(ur.data)
      if (sr.code === 200) stats.value = sr.data
    })
  } catch (e) {
    loadError.value = '无法连接账户服务，请稍后重试。'
  } finally {
    loading.value = false
  }
  void loadCommunity()
}

async function loadCommunity() {
  const tasks = [
    loadWallet(),
    loadTransactions(),
    loadWithdrawals(),
    loadTips(),
    loadOrders(),
    loadMembership(),
    loadFavoriteTotal(),
  ]
  await Promise.all(tasks.map(t => t.catch(() => {})))
}

/** 会员状态只用来点亮名字旁边的胶囊，取不到就当没有会员，不影响页面其它部分 */
async function loadMembership() {
  try {
    const res = await getMyMembership()
    if (res?.code === 200) membership.value = res.data || null
  } catch (e) { membership.value = null }
}

async function loadOrders() {
  try {
    const res = await getOrders()
    if (res?.code === 200) orders.value = normalizeList(res.data)
  } catch (e) { orders.value = [] }
}

/** 收藏入口的计数，只取 total，不拉列表；失败静默为 0，不打断页面 */
async function loadFavoriteTotal() {
  try {
    const data = await getFavoritePage({ page: 1, size: 1 })
    favoriteTotal.value = Number(data?.total || 0)
  } catch (e) { favoriteTotal.value = 0 }
}

async function loadWallet() {
  try {
    const res = await getMyWallet()
    if (res.code === 200 && res.data) wallet.value = res.data
  } catch (e) {}
}
async function loadTransactions() {
  try {
    const res = await getMyTransactions()
    if (res.code === 200) transactions.value = normalizeList(res.data).map(t => enrichWithStatus(t, 'status_wsh', TransactionStatus))
  } catch (e) { appStore.addToast('交易记录加载失败', 'error') }
}
async function loadWithdrawals() {
  try {
    const res = await getMyWithdrawals()
    if (res.code === 200) withdrawals.value = normalizeList(res.data).map(w => enrichWithStatus(w, 'status_wsh', WithdrawalStatus))
  } catch (e) { appStore.addToast('提现记录加载失败', 'error') }
}
async function loadTips() {
  try {
    const res = await getMyTips()
    if (res.code === 200) tips.value = normalizeList(res.data)
  } catch (e) { appStore.addToast('打赏记录加载失败', 'error') }
}

async function reloadAll() {
  await loadDashboard()
}

/* actions */
function openEditDialog() {
  editDraft.nickname_wsh = profile.nickname_wsh || ''
  editDraft.gender_wsh = Number(profile.gender_wsh || 0)
  editDialogOpen.value = true
}

function triggerAvatarInput() { avatarInputRef.value?.click() }
function onAvatarFile(event) {
  const file = event.target.files?.[0]
  if (file) void uploadAvatar(file)
  event.target.value = ''
}
async function uploadAvatar(file) {
  if (!file.type.startsWith('image/')) { appStore.addToast('请选择图片文件', 'error'); return }
  if (file.size > 5 * 1024 * 1024) { appStore.addToast('头像需小于 5MB', 'error'); return }
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await uploadFileToDirectory('avatars', formData)
    if (r.code === 200 && r.data?.url_wsh) {
      profile.avatar_wsh = r.data.url_wsh
      const updateRes = await updateCurrentUser({ avatar_wsh: profile.avatar_wsh })
      if (updateRes.code === 200) syncUser(updateRes.data || { avatar_wsh: profile.avatar_wsh })
      appStore.addToast('头像上传成功', 'success')
    }
  } catch (error) { appStore.addToast('头像上传失败', 'error') } finally { avatarUploading.value = false }
}

async function submitProfileEdit() {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      nickname_wsh: editDraft.nickname_wsh,
      avatar_wsh: profile.avatar_wsh,
      gender_wsh: editDraft.gender_wsh,
    }
    const r = await updateCurrentUser(payload)
    if (r.code === 200) {
      appStore.addToast('保存成功', 'success')
      syncUser(r.data || { ...profile, ...payload })
      editDialogOpen.value = false
    }
  } catch (e) { appStore.addToast('保存失败', 'error') } finally { submitting.value = false }
}

function openRecharge() { rechargeDialogOpen.value = true }
function openWithdraw() { withdrawDialogOpen.value = true }

async function submitRecharge() {
  if (!rechargeAmount.value || Number(rechargeAmount.value) <= 0) {
    appStore.addToast('请输入大于 0 的充值金额', 'warning'); return
  }
  const reqId = `recharge-fe-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  rechargeSubmitting.value = true
  try {
    const res = await rechargeWallet({ amount_wsh: Number(Number(rechargeAmount.value).toFixed(2)), request_id_wsh: reqId })
    if (res.code === 200) {
      if (res.data) wallet.value = res.data
      rechargeAmount.value = ''
      appStore.addToast('充值成功，余额已到账', 'success')
      rechargeDialogOpen.value = false
      loadWallet()
      loadTransactions()
    } else { appStore.addToast(res.msg || '充值失败', 'error') }
  } catch (e) { appStore.addToast(e?.message || '充值失败', 'error') } finally { rechargeSubmitting.value = false }
}

async function submitWithdraw() {
  if (!withdrawAmount.value || !withdrawAccount.value) return
  if (Number(withdrawAmount.value) > walletAvailable.value) {
    appStore.addToast('提现金额超出可用余额', 'error'); return
  }
  withdrawSubmitting.value = true
  try {
    const res = await createWithdrawal({ amount_wsh: withdrawAmount.value, account_name_wsh: withdrawAccount.value, bank_name_wsh: '', bank_card_wsh: '' })
    if (res.code === 200) {
      appStore.addToast('提现申请已提交，审核期间金额将被冻结', 'success')
      withdrawAmount.value = ''
      withdrawAccount.value = ''
      withdrawDialogOpen.value = false
      await loadWallet()
      await loadWithdrawals()
    }
  } catch (e) { appStore.addToast('提交失败', 'error') } finally { withdrawSubmitting.value = false }
}

function syncUser(userData) {
  if (!userData) return
  Object.assign(profile, userData)
  if (authStore.user) {
    authStore.user = { ...authStore.user, ...userData }
    localStorage.setItem('user', JSON.stringify(authStore.user))
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   Intentional dark "paper" panels use a fixed #17130f slab.
   ═══════════════════════════════════════════════════════ */
.pr-page {
  --paper: #17130f;
  --cream-fixed: #f5efe7;
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  --r-frame: 26px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.pr-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* Breadcrumb */
.pr-crumb {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.pr-crumb-link { color: var(--ref-muted); text-decoration: none; }
.pr-crumb-link:hover { color: var(--ref-ink); }
.pr-crumb-sep { color: var(--ref-line); }
.pr-crumb-here { color: var(--ref-ink-soft); }

/* Badges */
.badge {
  display: inline-flex; align-items: center; gap: 6px;
  border-radius: var(--r-tag); border: 1px solid transparent;
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.badge-action { background: var(--ref-brand); color: #fff; }
.badge-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent); }
.badge-active { background: color-mix(in srgb, var(--ref-moss, #3f5347) 12%, transparent); color: var(--ref-moss, #3f5347); border-color: color-mix(in srgb, var(--ref-moss, #3f5347) 25%, transparent); }
.badge-done { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); }
.badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.tag-pill {
  display: inline-flex; align-items: center;
  border-radius: var(--r-tag); border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; color: var(--ref-ink-soft);
}

/* CTA */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-dark { background: var(--paper); color: var(--cream-fixed); }
.cta-dark:hover { background: #000; }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* PROFILE HERO */
.ph-card {
  position: relative; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: var(--r-frame);
  background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.08);
}
.ph-top {
  display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between;
  gap: 28px 40px; padding: 28px 28px 32px;
}
.ph-identity { display: flex; min-width: 0; gap: 24px; }
.ph-avatar-btn {
  position: relative; display: block; width: 104px; height: 104px; flex-shrink: 0;
  overflow: hidden; border-radius: var(--r-panel);
  border: 1px solid var(--ref-line); background: var(--ref-sand);
  cursor: pointer; padding: 0;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ph-avatar-btn:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent);
  box-shadow: 0 18px 40px -26px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.ph-avatar-btn:disabled { opacity: 0.7; cursor: wait; }
.ph-avatar-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.ph-avatar-btn:hover .ph-avatar-img { transform: scale(1.05); }
.ph-avatar-initial {
  display: flex; width: 100%; height: 100%; align-items: center; justify-content: center;
  font-family: var(--ref-font-display); font-size: 2rem; color: var(--ref-ink-soft);
}
.ph-avatar-overlay {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  background: color-mix(in srgb, var(--paper) 55%, transparent); color: var(--cream-fixed);
  font-size: 11px; opacity: 0; transition: opacity 0.15s;
}
.ph-avatar-btn:hover .ph-avatar-overlay, .ph-avatar-btn:focus-visible .ph-avatar-overlay { opacity: 1; }
.ph-avatar-overlay .el-icon { font-size: 15px; }
.ph-avatar-busy {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--paper) 60%, transparent);
}
.ph-spinner {
  width: 20px; height: 20px; border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.25); border-left-color: var(--cream-fixed);
  animation: pr-spin 0.8s linear infinite;
}
.ph-avatar-file { display: none; }
.ph-id-copy { min-width: 0; }
.ph-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted);
}
.ph-eyebrow-line { width: 24px; height: 1px; background: var(--ref-line); }
.ph-eyebrow-idx { letter-spacing: 0.28em; }
.ph-name {
  margin: 14px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.ph-meta {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
  margin: 10px 0 0; font-size: 13px; color: var(--ref-muted);
}
.ph-meta-sep { color: var(--ref-line); }
.ph-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 14px; }
.ph-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; flex-shrink: 0; padding-top: 4px; }
.ph-link-btn {
  display: inline-flex; align-items: center; height: 42px; padding: 0 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; text-decoration: none;
  transition: border-color 0.15s, color 0.15s;
}
.ph-link-btn:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); }

/* active stay */
.ph-stay {
  display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px;
  padding: 14px 28px; border-top: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 60%, transparent);
}
.ph-stay-dot { width: 6px; height: 6px; flex-shrink: 0; border-radius: 50%; background: var(--ref-brand); }
.ph-stay-text { margin: 0; min-width: 0; font-size: 13px; color: var(--ref-ink-soft); }
.ph-stay-name { font-weight: 500; color: var(--ref-ink); }
.ph-stay-sep { margin: 0 6px; color: var(--ref-line); }
.ph-stay-link { margin-left: auto; font-size: 13px; font-weight: 500; color: var(--ref-ink); text-decoration: none; transition: color 0.15s; }
.ph-stay-link:hover { color: var(--ref-brand); }

/* SECTION SHELL */
.ps-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 16px 24px;
}
.ps-head-copy { min-width: 0; }
.ps-title {
  margin: 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}

/* 01 我的服务：主入口（订单 / 收藏）—— 承接原顶部导航的两个入口 */
.svc-heroes { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 0; }
.svc-hero {
  display: flex; flex-direction: column; gap: 16px;
  padding: 20px; border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.svc-hero:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 22px 50px -38px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.svc-hero-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.svc-hero-icon {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; flex-shrink: 0; border-radius: var(--r-btn);
  background: var(--ref-sand); color: var(--ref-brand); font-size: 17px;
  transition: background 0.15s, color 0.15s;
}
.svc-hero:hover .svc-hero-icon { background: var(--ref-brand); color: #fff; }
.svc-hero-go { padding-top: 2px; font-size: 14px; color: var(--ref-muted); transition: transform 0.2s, color 0.2s; }
.svc-hero:hover .svc-hero-go { transform: translateX(3px); color: var(--ref-brand); }
.svc-hero-body { display: block; min-width: 0; }
.svc-hero-title { display: block; font-size: 15px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); transition: color 0.15s; }
.svc-hero:hover .svc-hero-title { color: var(--ref-brand); }
.svc-hero-desc { display: block; margin-top: 6px; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }
.svc-hero-foot {
  display: flex; align-items: baseline; justify-content: space-between; gap: 8px;
  margin-top: 14px; padding-top: 10px; border-top: 1px solid var(--ref-line);
}
.svc-hero-foot-label { font-size: 10.5px; letter-spacing: 0.04em; color: var(--ref-muted); }
.svc-hero-foot-val { font-size: 13px; font-weight: 500; color: var(--ref-ink); font-variant-numeric: tabular-nums; }

/* 01 次要入口：一行三格 */
.svc-tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-top: 12px; }
.svc-tile {
  display: flex; align-items: center; gap: 10px; min-width: 0;
  padding: 12px 14px; border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-decoration: none;
  transition: background 0.15s, border-color 0.15s;
}
.svc-tile:hover {
  background: color-mix(in srgb, var(--ref-cream) 55%, transparent);
  border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
}
.svc-tile-icon {
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; flex-shrink: 0; border-radius: 9px;
  background: var(--ref-sand); color: var(--ref-brand); font-size: 14px;
  transition: background 0.15s, color 0.15s;
}
.svc-tile:hover .svc-tile-icon { background: var(--ref-brand); color: #fff; }
.svc-tile-copy { display: block; min-width: 0; }
.svc-tile-title {
  display: block; font-size: 12.5px; font-weight: 500; color: var(--ref-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.svc-tile-desc {
  display: block; margin-top: 2px; font-size: 10.5px; color: var(--ref-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* WALLET */
.wp-grid { display: grid; grid-template-columns: minmax(0, 320px) 1fr; gap: 16px; margin-top: 20px; }
.wp-balance {
  display: flex; flex-direction: column; justify-content: space-between; gap: 32px;
  padding: 24px; border-radius: var(--r-panel); background: var(--paper); color: var(--cream-fixed);
}
.wp-b-label { margin: 0; font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: rgba(255, 255, 255, 0.45); }
.wp-b-amount {
  margin: 10px 0 0; display: flex; align-items: baseline; gap: 6px;
  font-family: var(--ref-font-display); font-size: 38px; font-weight: 400; line-height: 1; letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.wp-b-yen { font-size: 14px; color: rgba(255, 255, 255, 0.55); }
.wp-b-sub {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
  margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.wp-b-sub-label { margin: 0; font-size: 10px; color: rgba(255, 255, 255, 0.45); }
.wp-b-sub-val { margin: 6px 0 0; font-size: 14px; font-variant-numeric: tabular-nums; }
.wp-b-actions { display: flex; gap: 10px; }
.wp-btn {
  height: 40px; flex: 1; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; transition: background 0.15s, border-color 0.15s, transform 0.15s;
}
.wp-btn:hover { transform: translateY(-1px); }
.wp-btn-light { background: var(--cream-fixed); color: var(--paper); border: 1px solid transparent; }
.wp-btn-light:hover { background: #fff; }
.wp-btn-ghost { background: transparent; color: var(--cream-fixed); border: 1px solid rgba(255, 255, 255, 0.25); }
.wp-btn-ghost:hover { border-color: rgba(255, 255, 255, 0.45); background: rgba(255, 255, 255, 0.1); }
.wp-b-note { margin: 16px 0 0; font-size: 11px; line-height: 1.7; color: rgba(255, 255, 255, 0.45); }

.wp-ledger { overflow: hidden; border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface); }
.ledger-tabs { display: flex; gap: 4px; overflow-x: auto; padding: 0 8px; border-bottom: 1px solid var(--ref-line); scrollbar-width: none; }
.ledger-tabs::-webkit-scrollbar { display: none; }
.ledger-tab {
  position: relative; flex-shrink: 0; padding: 14px 14px 12px;
  background: none; border: none; cursor: pointer;
  font-size: 13.5px; letter-spacing: -0.01em; color: var(--ref-muted); transition: color 0.15s;
}
.ledger-tab:hover { color: var(--ref-ink-soft); }
.ledger-tab.active { color: var(--ref-ink); }
.ledger-count {
  margin-left: 6px; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums;
}
.ledger-tab.active .ledger-count { color: var(--ref-brand); }
.ledger-tab.active::after {
  content: ''; position: absolute; inset-inline: 12px; bottom: -1px; height: 2px;
  border-radius: 2px; background: var(--ref-brand);
}
.ledger-body { min-height: 220px; }
.ledger-list { margin: 0; padding: 0; list-style: none; }
.ledger-row {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 20px; border-bottom: 1px solid var(--ref-line);
}
.ledger-row:last-child { border-bottom: 0; }
.lr-main { min-width: 0; }
.lr-title { margin: 0; font-size: 13.5px; letter-spacing: -0.01em; color: var(--ref-ink); }
.lr-title b { font-variant-numeric: tabular-nums; }
.lr-meta {
  display: flex; align-items: center; gap: 6px; margin: 4px 0 0;
  font-size: 11px; color: var(--ref-muted);
}
.lr-trunc { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.lr-sep { color: var(--ref-line); }
.lr-side { flex-shrink: 0; text-align: right; }
.lr-side-row { display: flex; align-items: center; gap: 12px; }
.lr-amount { margin: 0; font-size: 13.5px; font-weight: 500; font-variant-numeric: tabular-nums; }
.lr-in { color: var(--ref-moss, #3f5347); }
.lr-out { color: var(--ref-ink); }
.lr-balance { margin: 4px 0 0; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.ledger-row-tip { display: block; }
.lr-tip-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.lr-tip-msg { margin: 6px 0 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }
.ledger-empty {
  display: flex; align-items: center; justify-content: center;
  min-height: 220px; font-size: 13px; color: var(--ref-muted);
}
.ledger-more { margin: 0; padding: 12px 20px; border-top: 1px solid var(--ref-line); font-size: 11px; color: var(--ref-muted); }
.ledger-more b { color: var(--ref-ink-soft); font-variant-numeric: tabular-nums; }

/* EMPTY / ERROR / SKELETON */
.pr-error {
  padding: 64px 24px; border: 1px dashed var(--ref-line); border-radius: var(--r-frame);
  background: var(--ref-surface); text-align: center;
}
.pr-error-title { margin: 0; font-family: var(--ref-font-display); font-size: 22px; font-weight: 500; color: var(--ref-ink); }
.pr-error-desc { margin: 10px 0 0; font-size: 13.5px; color: var(--ref-muted); }
.pr-error .cta { margin-top: 22px; }
.ph-skeleton {
  height: 320px; border-radius: var(--r-frame);
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: pr-shimmer 1.3s linear infinite;
}
.ps-skeleton-stack { display: grid; gap: 20px; margin-top: 32px; }
.ps-skeleton { height: 180px; border-radius: var(--r-card); border: 1px solid var(--ref-line); background: var(--ref-sand); animation: pr-pulse 1.4s ease-in-out infinite; }

/* DIALOG */
.dlg-overlay {
  position: fixed; inset: 0; z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  padding: 20px; background: rgba(10, 8, 6, 0.5); backdrop-filter: blur(2px);
  animation: pr-fade 0.15s ease;
}
.dlg-panel {
  width: 100%; max-width: 420px; max-height: min(90vh, 720px); overflow-y: auto;
  border-radius: var(--r-panel); background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: var(--shadow-pop);
  animation: pr-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 0;
}
.dlg-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.dlg-close {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 8px; background: transparent; border: none;
  color: var(--ref-muted); font-size: 13px; cursor: pointer; transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-avatar-row { display: flex; align-items: center; gap: 14px; }
.dlg-avatar {
  position: relative; width: 64px; height: 64px; flex-shrink: 0; overflow: hidden;
  border-radius: 14px; border: 1px solid var(--ref-line); background: var(--ref-sand);
  font-family: var(--ref-font-display); font-size: 20px; color: var(--ref-ink-soft);
  display: flex; align-items: center; justify-content: center; cursor: pointer;
}
.dlg-avatar img { width: 100%; height: 100%; object-fit: cover; }
.dlg-avatar-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--paper) 50%, transparent); color: var(--cream-fixed);
  font-size: 16px; opacity: 0; transition: opacity 0.15s;
}
.dlg-avatar:hover .dlg-avatar-overlay { opacity: 1; }
.dlg-avatar-hint { margin: 0; font-size: 12px; color: var(--ref-muted); line-height: 1.6; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-input {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-input::placeholder { color: var(--ref-muted); }
.dlg-input:focus { outline: none; border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent); }
.dlg-balance {
  display: grid; gap: 4px; padding: 16px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-sand);
}
.dlg-balance span, .dlg-balance small { font-size: 12px; color: var(--ref-muted); }
.dlg-balance b { font-family: var(--ref-font-display); font-size: 28px; font-weight: 400; color: var(--ref-brand); font-variant-numeric: tabular-nums; }
.dlg-note { margin: 0; font-size: 11.5px; line-height: 1.7; color: var(--ref-muted); }
.dlg-foot {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  background: var(--ref-surface); border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ANIMATIONS */
@keyframes pr-spin { to { transform: rotate(360deg); } }
@keyframes pr-shimmer { to { background-position: -200% 0; } }
@keyframes pr-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.55; } }
@keyframes pr-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes pr-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* RESPONSIVE */
@media (max-width: 1100px) {
  .wp-grid { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .ph-actions { width: 100%; }
}
@media (max-width: 560px) {
  .pr-shell { padding: 0 16px; }
  .ph-top { padding: 20px 20px 24px; }
  .ph-identity { flex-direction: column; gap: 16px; }
  .ph-avatar-btn { width: 88px; height: 88px; }
  .ph-stay { padding: 12px 20px; }
  .svc-heroes { grid-template-columns: 1fr; }
  .svc-tiles { grid-template-columns: 1fr; }
  .ledger-row { padding: 13px 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .ph-skeleton { animation: none; }
  .ps-skeleton { animation: none; }
  .ph-spinner { animation-duration: 1.4s; }
}
/* ═══════════════════════════════════════════════════════
   两栏布局（结构重排新增）—— 只含 grid / flex / 间距 / 尺寸，
   颜色、字体、圆角全部沿用上面的 --ref-* 与 --r-* 变量。
   ═══════════════════════════════════════════════════════ */
.pr-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  align-items: stretch;
  gap: 32px;
  margin-top: 16px;
}
.pr-col-left { display: flex; flex-direction: column; gap: 12px; min-width: 0; }
/* 左栏跟随右栏高度：余额卡吃掉剩余空间，两栏底边对齐 */
.pr-col-left > :last-child { flex: 1 1 auto; }
.pr-col-right { display: flex; flex-direction: column; gap: 28px; min-width: 0; }

/* ① 紧凑身份卡 */
.ph-card-sm { border-radius: var(--r-panel); }
.ph-top-sm { display: flex; flex-direction: column; gap: 12px; padding: 16px; }
.ph-identity-sm { display: flex; align-items: flex-start; gap: 12px; min-width: 0; }
.ph-avatar-sm { width: 48px; height: 48px; border-radius: var(--r-btn); }
.ph-avatar-sm .ph-avatar-initial { font-size: 1.1rem; }
.ph-avatar-sm .ph-avatar-overlay { font-size: 0; }
.ph-avatar-sm .ph-avatar-overlay .el-icon { font-size: 14px; }
.ph-avatar-sm .ph-spinner { width: 16px; height: 16px; }
.ph-name-sm { margin: 0; font-size: 20px; line-height: 1.2; }
.ph-tags-sm { margin-top: 6px; gap: 5px; }
.ph-meta-sm { margin-top: 6px; gap: 6px; font-size: 11.5px; }
.ph-actions-sm { width: auto; padding-top: 0; gap: 8px; }
.cta-sm { height: 34px; padding: 0 14px; font-size: 12.5px; }
.ph-link-btn-sm { height: 34px; padding: 0 14px; font-size: 12.5px; }
.tag-pill-sm { padding: 3px 8px; font-size: 10px; }
.badge-sm { padding: 3px 8px; font-size: 10px; }

/* 状态条：状态一行 + 入口一行 */
.ph-stay-sm {
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
  padding: 7px 16px;
}
.ph-stay-text-sm { display: flex; align-items: center; gap: 6px; font-size: 11.5px; line-height: 1.5; }
.ph-stay-trunc { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ph-stay-link-sm { margin-left: 0; align-self: flex-start; font-size: 11.5px; }

/* ② 紧凑余额卡 */
.wp-balance-sm { gap: 12px; padding: 16px; }
/* 左栏被拉到与右栏等高时，余额卡吃掉多余高度：
   底部操作区加一条分隔线，避免数字与按钮之间出现无意义的空洞 */
.wp-balance-sm > div:last-child { padding-top: 12px; border-top: 1px solid rgba(255, 255, 255, 0.12); }
.wp-b-amount-sm { margin-top: 6px; font-size: 24px; }
.wp-b-amount-sm .wp-b-yen { font-size: 12px; }
.wp-b-sub-sm { gap: 12px; margin-top: 12px; padding-top: 10px; }
.wp-b-sub-sm .wp-b-sub-label { font-size: 9px; }
.wp-b-sub-sm .wp-b-sub-val { margin-top: 3px; font-size: 12px; }
.wp-btn-sm { height: 34px; font-size: 12.5px; }
.wp-b-wallet {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 6px; padding: 4px 0; border-radius: var(--r-btn);
  font-size: 11.5px; font-weight: 500; color: rgba(255, 255, 255, 0.72);
  text-decoration: none; transition: color 0.15s;
}
.wp-b-wallet:hover { color: var(--cream-fixed); }

/* 02 为你推荐：两张横向大卡 */
.rec-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-top: 20px; }
.rec-card {
  display: flex; flex-direction: column; justify-content: space-between; gap: 18px;
  padding: 20px; border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.rec-card:hover {
  transform: translateY(-2px); border-color: color-mix(in srgb, var(--ref-ink) 15%, transparent);
  box-shadow: 0 22px 50px -38px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
.rec-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.rec-icon {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; flex-shrink: 0; border-radius: var(--r-btn);
  background: var(--ref-sand); color: var(--ref-brand); font-size: 17px;
}
.rec-arrow { padding-top: 2px; font-size: 14px; color: var(--ref-muted); transition: transform 0.2s, color 0.2s; }
.rec-card:hover .rec-arrow { transform: translateX(3px); color: var(--ref-brand); }
.rec-body { display: block; min-width: 0; }
.rec-title { display: block; font-size: 15px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); transition: color 0.15s; }
.rec-card:hover .rec-title { color: var(--ref-brand); }
.rec-desc { display: block; margin-top: 6px; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }

/* 窄屏：两栏退化为单栏，左栏在上 */
@media (max-width: 1023px) {
  .pr-layout { grid-template-columns: 1fr; gap: 24px; }
}
@media (max-width: 720px) {
  .rec-grid { grid-template-columns: 1fr; }
}

</style>
