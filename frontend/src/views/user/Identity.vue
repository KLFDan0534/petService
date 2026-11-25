<template>
  <div class="id-page">
    <div class="id-shell">
      <!-- ═══ 面包屑 ═══ -->
      <nav class="id-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="id-crumb-link">首页</router-link>
        <span class="id-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="id-crumb-link">个人中心</router-link>
        <span class="id-crumb-sep" aria-hidden="true">›</span>
        <span class="id-crumb-here">身份与服务</span>
      </nav>

      <!-- ═══ 标题 ═══ -->
      <header class="id-head">
        <h1 class="id-title">身份与服务</h1>
        <p class="id-lede">商家、照护师、商家客服三类身份都在这里申请与进入，审核状态由平台或所属商家维护。</p>
      </header>

      <!-- ═══ 加载 ═══ -->
      <div v-if="loading" class="id-skel-stack" aria-hidden="true">
        <div class="id-skel"></div>
        <div class="id-skel"></div>
        <div class="id-skel"></div>
      </div>

      <!-- ═══ 身份列表 ═══ -->
      <section v-else class="id-list" aria-label="身份列表">
        <article v-for="entry in entries" :key="entry.key" class="id-card">
          <span class="id-icon">
            <AppIcon aria-hidden="true"><component :is="entry.icon" /></AppIcon>
          </span>

          <div class="id-copy">
            <div class="id-copy-top">
              <h2 class="id-card-title">{{ entry.title }}</h2>
              <span v-if="entry.badge" :class="['id-badge', `id-badge-${entry.badge.tone}`]">{{ entry.badge.label }}</span>
            </div>
            <p class="id-card-desc">{{ entry.desc }}</p>
          </div>

          <router-link v-if="entry.to" :to="entry.to" class="id-cta">
            {{ entry.cta }}<span class="id-cta-arrow" aria-hidden="true">→</span>
          </router-link>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Headset, Shop, UserFilled } from '@element-plus/icons-vue'
import { getMyMerchant } from '@/api/merchant'
import { getMyKeeperApplication } from '@/api/keeper'
import { getMyCustomerServiceApplications } from '@/api/merchantCustomerService'

const loading = ref(true)
const merchantStatus = ref(null)
const keeperStatus = ref(null)
const customerServiceApplications = ref([])

function merchantEntry(status) {
  if (status === 0) {
    return { title: '商家审核中', desc: '平台正在核验你的入驻资料，审核通过后即可发布服务。', to: '/merchants', cta: '查看进度', badge: { label: '待审核', tone: 'action' } }
  }
  if (status === 1) {
    return { title: '商家中心', desc: '管理门店信息、照护服务与门店订单。', to: '/merchant/dashboard', cta: '进入商家中心' }
  }
  if (status === 2) {
    return { title: '入驻申请被拒绝', desc: '可以补充资质资料后重新提交入驻申请。', to: '/merchants', cta: '重新提交', badge: { label: '已拒绝', tone: 'closed' } }
  }
  return { title: '申请成为商家', desc: '入驻平台，发布宠物寄养与照护服务。', to: '/merchants', cta: '去申请' }
}

function keeperEntry(status) {
  if (status === 0) {
    return { title: '照护师审核中', desc: '所属商户正在审核你的照护师申请。', to: '/keeper-apply', cta: '查看进度', badge: { label: '待审核', tone: 'action' } }
  }
  if (status === 1) {
    return { title: '照护师工作台', desc: '管理排班、接单与服务记录。', to: '/keeper-workflow', cta: '进入工作台', badge: { label: '在线', tone: 'active' } }
  }
  if (status === 3) {
    return { title: '照护师工作台', desc: '当前为离线状态，进入工作台可切换在线状态。', to: '/keeper-workflow', cta: '进入工作台', badge: { label: '离线', tone: 'closed' } }
  }
  if (status === 4) {
    return { title: '照护师工作台', desc: '当前为忙碌状态，进入工作台可切换接单状态。', to: '/keeper-workflow', cta: '进入工作台', badge: { label: '忙碌', tone: 'action' } }
  }
  if (status === 2) {
    return { title: '照护师申请被拒绝', desc: '可以补充资料后重新提交申请。', to: '/keeper-apply', cta: '重新提交', badge: { label: '已拒绝', tone: 'closed' } }
  }
  return { title: '申请成为照护师', desc: '加入商户，为宠物提供寄养与上门照护。', to: '/keeper-apply', cta: '去申请' }
}

function csEntry(application) {
  if (!application) {
    return { title: '申请成为商家客服', desc: '选择要服务的商家，由商家负责人审核。', to: '/customer-service/apply', cta: '去申请' }
  }
  const map = {
    pending: { label: '待审核', tone: 'action' }, approved: { label: '已通过', tone: 'active' },
    rejected: { label: '已拒绝', tone: 'closed' }, resigned: { label: '已退出', tone: 'closed' },
    terminated: { label: '已终止', tone: 'closed' },
  }
  const merchantName = application.merchant_name_wsh || `商家 #${application.merchant_id_wsh ?? '-'}`
  const status = application.status_wsh
  return {
    title: status === 'approved' ? '商家客服已开通' : '客服申请进度',
    desc: `服务商家：${merchantName}`,
    to: '/customer-service/apply',
    cta: '查看申请',
    badge: map[status],
  }
}

const entries = computed(() => [
  { key: 'merchant', ...merchantEntry(merchantStatus.value === null ? null : Number(merchantStatus.value)), icon: Shop },
  { key: 'keeper', ...keeperEntry(keeperStatus.value === null ? null : Number(keeperStatus.value)), icon: UserFilled },
  { key: 'cs', ...csEntry(customerServiceApplications.value[0] || null), icon: Headset },
])

async function loadMerchantStatus() {
  try {
    const r = await getMyMerchant()
    if (r.code === 200 && r.data) merchantStatus.value = r.data.status_wsh
  } catch (e) {}
}

async function loadKeeperStatus() {
  try {
    const r = await getMyKeeperApplication()
    keeperStatus.value = r.code === 200 && r.data ? r.data.status_wsh : null
  } catch (e) { keeperStatus.value = null }
}

async function loadCustomerServiceApplications() {
  try {
    const r = await getMyCustomerServiceApplications()
    if (r.code === 200) customerServiceApplications.value = r.data || []
  } catch (e) { customerServiceApplications.value = [] }
}

onMounted(async () => {
  await Promise.all([
    loadMerchantStatus(),
    loadKeeperStatus(),
    loadCustomerServiceApplications(),
  ].map(t => t.catch(() => {})))
  loading.value = false
})
</script>

<style scoped>
.id-page {
  --r-btn: 10px;
  --r-card: 14px;
  width: 100%;
  min-height: 60vh;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.id-shell { max-width: 860px; margin: 0 auto; padding: 0 24px; }

.id-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.id-crumb-link { color: var(--ref-muted); text-decoration: none; }
.id-crumb-link:hover { color: var(--ref-ink); }
.id-crumb-sep { color: var(--ref-line); }
.id-crumb-here { color: var(--ref-ink-soft); }

.id-head { margin: 18px 0 24px; }
.id-title {
  margin: 0; font-family: var(--ref-font-display);
  font-size: 30px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.id-lede { margin: 10px 0 0; max-width: 620px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }

.id-list { display: flex; flex-direction: column; gap: 12px; }
.id-card {
  display: flex; align-items: center; gap: 16px;
  padding: 18px 20px; border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface);
}
.id-icon {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; flex-shrink: 0; border-radius: var(--r-btn);
  background: var(--ref-sand); color: var(--ref-brand); font-size: 17px;
}
.id-copy { min-width: 0; flex: 1; }
.id-copy-top { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.id-card-title { margin: 0; font-size: 14px; font-weight: 500; letter-spacing: -0.01em; color: var(--ref-ink); }
.id-card-desc { margin: 5px 0 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }

.id-badge {
  display: inline-flex; align-items: center;
  padding: 3px 8px; border-radius: 6px; border: 1px solid transparent;
  font-size: 10px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.id-badge-action { background: var(--ref-brand); color: #fff; }
.id-badge-active {
  background: color-mix(in srgb, var(--ref-moss, #3f5347) 12%, transparent);
  color: var(--ref-moss, #3f5347);
  border-color: color-mix(in srgb, var(--ref-moss, #3f5347) 25%, transparent);
}
.id-badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }

.id-cta {
  display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0;
  height: 34px; padding: 0 14px; border-radius: var(--r-btn);
  border: 1px solid color-mix(in srgb, var(--ref-ink) 20%, transparent);
  background: var(--ref-surface); color: var(--ref-ink);
  font-size: 12.5px; font-weight: 500; text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.id-cta:hover { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-canvas); }
.id-cta-arrow { transition: transform 0.15s; }
.id-cta:hover .id-cta-arrow { transform: translateX(3px); }

.id-skel-stack { display: flex; flex-direction: column; gap: 12px; }
.id-skel {
  height: 84px; border-radius: var(--r-card); border: 1px solid var(--ref-line);
  background: linear-gradient(100deg, var(--ref-sand) 30%, var(--ref-cream) 50%, var(--ref-sand) 70%);
  background-size: 200% 100%;
  animation: id-shimmer 1.6s linear infinite;
}
@keyframes id-shimmer { to { background-position: -200% 0; } }

@media (max-width: 640px) {
  .id-shell { padding: 0 16px; }
  .id-title { font-size: 24px; }
  .id-card { flex-wrap: wrap; gap: 12px; }
  .id-copy { flex: 1 1 100%; order: 2; }
  .id-cta { order: 3; width: 100%; justify-content: center; }
}
@media (prefers-reduced-motion: reduce) {
  .id-skel { animation: none; }
}
</style>
