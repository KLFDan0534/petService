<template>
  <div class="kpr-page">
    <div class="kpr-shell">

      <!-- ═══ Breadcrumb ═══ -->
      <nav class="kpr-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="kpr-crumb-link">首页</router-link>
        <span class="kpr-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/keepers" class="kpr-crumb-link">照护师</router-link>
        <span class="kpr-crumb-sep" aria-hidden="true">›</span>
        <span class="kpr-crumb-here">我的主页</span>
      </nav>

      <!-- ═══ Loading ═══ -->
      <template v-if="loading">
        <div class="kpr-skel kpr-skel-hero"></div>
        <div class="kpr-skel kpr-skel-block"></div>
      </template>

      <!-- ═══ 无档案 ═══ -->
      <div v-else-if="!keeper" class="kpr-empty">
        <p class="kpr-empty-title">还没有寄养师档案</p>
        <p class="kpr-empty-desc">这个账号尚未通过寄养师认证，提交申请并通过审核后就能生成公开主页。</p>
        <div class="kpr-empty-actions">
          <router-link to="/keeper-apply" class="cta cta-outline">去申请成为寄养师</router-link>
          <button type="button" class="cta cta-primary" @click="reload">重新加载</button>
        </div>
      </div>

      <template v-else>
        <!-- ═══ Header ═══ -->
        <header class="kpr-head">
          <div class="kpr-head-copy">
            <p class="kpr-eyebrow">
              <span class="kpr-idx">Keeper Profile</span>
              <span class="kpr-line" aria-hidden="true"></span>
              <span>我的寄养师主页</span>
            </p>
            <h1 class="kpr-title">{{ keeper.name_wsh || '我的寄养师主页' }}</h1>
            <p class="kpr-desc">
              {{ keeper.merchant_name_wsh
                ? `隶属门店 ${keeper.merchant_name_wsh}，以下内容与用户看到的公开主页一致。`
                : '以下内容与用户在平台上看到的公开主页一致。' }}
            </p>
          </div>
          <div class="kpr-head-actions">
            <router-link v-if="keeper.id_wsh" :to="`/keepers/${keeper.id_wsh}`" class="cta cta-outline">查看公开主页</router-link>
            <router-link to="/keeper-workflow" class="cta cta-ghost">返回工作台</router-link>
          </div>
        </header>

        <!-- ═══ 01 身份与状态 ═══ -->
        <section class="kpr-section" aria-labelledby="kpr-sec-id">
          <header class="kpr-section-head">
            <p class="kpr-eyebrow">
              <span class="kpr-idx">01</span>
              <span class="kpr-line" aria-hidden="true"></span>
              <span>Identity</span>
            </p>
            <h2 id="kpr-sec-id" class="kpr-section-title">身份与状态</h2>
          </header>

          <div class="kpr-identity">
            <div class="kpr-identity-top">
              <div class="kpr-avatar">
                <MediaWithFallback
                  :src="keeper.avatar_wsh"
                  :alt="keeper.name_wsh || '寄养师头像'"
                  :placeholder="keeper.name_wsh"
                  round
                />
              </div>
              <div class="kpr-identity-info">
                <div class="kpr-identity-title">
                  <h3 class="kpr-identity-name">{{ keeper.name_wsh || '未命名' }}</h3>
                  <span :class="['badge', warmBadge(onlineBadge)]">{{ onlineLabel }}</span>
                  <span :class="['badge', warmBadge(reviewBadge)]">{{ reviewLabel }}</span>
                </div>
                <div class="kpr-identity-meta">
                  <span v-if="Number(keeper.rating_wsh) > 0" class="kpr-rating">
                    <span class="kpr-rating-stars" aria-hidden="true">{{ renderStars(keeper.rating_wsh) }}</span>
                    <b class="tabular">{{ Number(keeper.rating_wsh).toFixed(1) }}</b>
                  </span>
                  <router-link v-if="keeper.merchant_id_wsh" :to="`/merchants/${keeper.merchant_id_wsh}`" class="kpr-merchant-link">
                    {{ keeper.merchant_name_wsh || '所属门店' }}
                  </router-link>
                  <span v-if="keeper.created_at_wsh" class="kpr-meta-muted">入驻于 {{ formatDate(keeper.created_at_wsh) }}</span>
                </div>
              </div>
            </div>

            <dl class="kpr-stats">
              <div class="kpr-stat">
                <dt>从业年限</dt>
                <dd>{{ keeper.experience_years_wsh || '—' }}<span class="kpr-stat-hint">年</span></dd>
              </div>
              <div class="kpr-stat">
                <dt>累计订单</dt>
                <dd>{{ keeper.order_count_wsh ?? '—' }}<span class="kpr-stat-hint">单</span></dd>
              </div>
              <div class="kpr-stat">
                <dt>完成率</dt>
                <dd>{{ percentLabel(keeper.completion_rate_wsh) }}<span class="kpr-stat-hint">按已结束订单计</span></dd>
              </div>
              <div class="kpr-stat">
                <dt>投诉率</dt>
                <dd>{{ percentLabel(keeper.complaint_rate_wsh) }}<span class="kpr-stat-hint">越低越好</span></dd>
              </div>
            </dl>
          </div>
        </section>

        <!-- ═══ 02 服务能力 ═══ -->
        <section class="kpr-section" aria-labelledby="kpr-sec-cap">
          <header class="kpr-section-head">
            <p class="kpr-eyebrow">
              <span class="kpr-idx">02</span>
              <span class="kpr-line" aria-hidden="true"></span>
              <span>Capacity</span>
            </p>
            <h2 id="kpr-sec-cap" class="kpr-section-title">服务能力</h2>
            <p class="kpr-section-desc">当前接待情况与报价。</p>
          </header>

          <div class="kpr-capacity">
            <div class="kpr-card kpr-load-card">
              <div class="kpr-load-head">
                <p class="kpr-load-label">在护宠物</p>
                <p class="tabular kpr-load-count">{{ load }} / {{ capacity || '—' }}</p>
              </div>
              <div class="kpr-load-track">
                <div class="kpr-load-bar" :style="{ width: loadPercent + '%' }"></div>
              </div>
              <p class="kpr-load-caption">
                {{ capacity > 0
                  ? (load >= capacity ? '已达接待上限，新订单将不再自动分配。' : `还可以接待 ${capacity - load} 只宠物。`)
                  : '尚未设置最大接待数量。' }}
              </p>
            </div>

            <div class="kpr-card kpr-price-card">
              <p class="kpr-price-label">每日报价</p>
              <p class="kpr-price">
                <span class="kpr-price-symbol">¥</span>{{ priceValue }}
              </p>
              <p class="kpr-price-caption">报价由所属门店在服务配置中维护，如需调整请联系门店管理员。</p>
            </div>
          </div>
        </section>

        <!-- ═══ 03 个人简介 ═══ -->
        <section class="kpr-section" aria-labelledby="kpr-sec-about">
          <header class="kpr-section-head">
            <p class="kpr-eyebrow">
              <span class="kpr-idx">03</span>
              <span class="kpr-line" aria-hidden="true"></span>
              <span>About</span>
            </p>
            <h2 id="kpr-sec-about" class="kpr-section-title">个人简介</h2>
          </header>
          <div class="kpr-card">
            <p v-if="keeper.bio_wsh" class="kpr-bio">{{ keeper.bio_wsh }}</p>
            <p v-else class="kpr-bio-empty">还没有填写简介。一段真实的照护经历，比任何标签都更有说服力。</p>
          </div>
        </section>

        <!-- ═══ 04 资质核验 ═══ -->
        <section class="kpr-section" aria-labelledby="kpr-sec-qual">
          <header class="kpr-section-head">
            <p class="kpr-eyebrow">
              <span class="kpr-idx">04</span>
              <span class="kpr-line" aria-hidden="true"></span>
              <span>Credentials</span>
            </p>
            <h2 id="kpr-sec-qual" class="kpr-section-title">资质核验</h2>
            <p class="kpr-section-desc">每一份证明都标注了平台的核验状态。</p>
          </header>
          <ul v-if="qualifications.length" class="kpr-qual-list">
            <li v-for="q in qualifications" :key="q.id_wsh" class="kpr-qual-item">
              <span class="kpr-qual-icon" aria-hidden="true">✓</span>
              <span class="kpr-qual-body">
                <span class="kpr-qual-title">{{ q.title_wsh }}</span>
                <span v-if="qualificationUrls(q.file_url_wsh).length" class="kpr-qual-thumbs">
                  <img v-for="url in qualificationUrls(q.file_url_wsh)" :key="url" :src="url" :alt="q.title_wsh">
                </span>
              </span>
              <span :class="['badge', warmBadge(qualStatus(q.status_wsh))]">{{ qualLabel(q.status_wsh) }}</span>
            </li>
          </ul>
          <div v-else class="kpr-empty kpr-empty-inline">
            <p class="kpr-empty-title">暂未上传资质证明</p>
            <p class="kpr-empty-desc">该账号尚未上传资质证明。</p>
          </div>
        </section>

        <!-- ═══ 05 用户评价 ═══ -->
        <section class="kpr-section" aria-labelledby="kpr-sec-ratings">
          <header class="kpr-section-head">
            <p class="kpr-eyebrow">
              <span class="kpr-idx">05</span>
              <span class="kpr-line" aria-hidden="true"></span>
              <span>In Their Words</span>
            </p>
            <h2 id="kpr-sec-ratings" class="kpr-section-title">用户评价</h2>
            <p v-if="ratingCount > 0" class="kpr-section-desc">{{ ratingCount }} 条评价，平均 {{ avgScore }} 分。</p>
          </header>

          <div v-if="!ratings.length" class="kpr-empty kpr-empty-inline">
            <p class="kpr-empty-title">还没有收到评价</p>
            <p class="kpr-empty-desc">完成第一次寄养服务后，用户的评价会展示在这里。</p>
          </div>
          <ul v-else class="kpr-rating-list">
            <li v-for="r in ratings" :key="r.id_wsh">
              <article class="kpr-rating-card">
                <div class="kpr-rating-head">
                  <span class="kpr-rating-stars" aria-hidden="true">{{ renderStars(r.score_wsh) }}</span>
                  <span class="kpr-rating-date">{{ formatDate(r.created_at_wsh) }}</span>
                </div>
                <p class="kpr-rating-content">{{ r.content_wsh || '用户没有留下文字评价。' }}</p>
              </article>
            </li>
          </ul>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMy } from '@/services/keeperService'
import * as KeeperDomain from '@/domain/KeeperDomain'
import { KeeperOnlineStatus, KeeperReviewStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { parseCommaSeparatedUrls } from '@/utils/fileUrls'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const router = useRouter()
const loading = ref(true)
const profile = ref(null)

const keeper = computed(() => profile.value?.keeper ?? null)
const qualifications = computed(() => profile.value?.qualifications ?? [])
const ratings = computed(() => profile.value?.ratings || profile.value?.stats?.ratings || [])
const stats = computed(() => profile.value?.stats ?? {})
const avgScore = computed(() => stats.value?.avgScore ?? 0)
const ratingCount = computed(() => stats.value?.ratingCount ?? ratings.value.length ?? 0)

const onlineLabel = computed(() => keeper.value ? getStatusLabel(KeeperOnlineStatus, keeper.value.status_wsh) : '-')
const onlineBadge = computed(() => keeper.value ? getStatusBadge(KeeperOnlineStatus, keeper.value.status_wsh) : 'badge-info')

// 后端的 status_wsh 双语义：0=待审核,1=已通过,2=已拒绝；3/4=离线/忙碌均隐含已通过
const reviewLevel = computed(() => {
  const s = keeper.value?.status_wsh
  return s === 0 ? 0 : (s === 2 ? 2 : 1)
})
const reviewBadge = computed(() => keeper.value ? getStatusBadge(KeeperReviewStatus, reviewLevel.value) : 'badge-info')
const reviewLabel = computed(() => keeper.value ? getStatusLabel(KeeperReviewStatus, reviewLevel.value) : '-')

const load = computed(() => Number(keeper.value?.current_pets_wsh ?? 0))
const capacity = computed(() => Number(keeper.value?.max_pets_wsh ?? 0))
const loadPercent = computed(() => (capacity.value > 0 ? Math.min(Math.round((load.value / capacity.value) * 100), 100) : 0))

const priceValue = computed(() => (keeper.value?.price_per_day_wsh ? Number(keeper.value.price_per_day_wsh).toFixed(2) : '—'))

/** 全局 badge-* 色调 → 暖色编辑风色调 */
const WARM_TONE = {
  'badge-success': 'badge-active',
  'badge-warning': 'badge-action',
  'badge-secondary': 'badge-closed',
  'badge-info': 'badge-queued',
  'badge-primary': 'badge-done',
  'badge-danger': 'badge-danger',
  'badge-error': 'badge-danger',
}
function warmBadge(globalClass) {
  return WARM_TONE[globalClass] || 'badge-queued'
}

function qualStatus(s) {
  if (s === 'approved') return 'badge-success'
  if (s === 'pending') return 'badge-warning'
  return 'badge-danger'
}
function qualLabel(s) {
  if (s === 'approved') return '已认证'
  if (s === 'pending') return '审核中'
  return '未通过'
}
function percentLabel(v) {
  return v == null ? '—' : `${Number(v)}%`
}
function renderStars(score) {
  const n = Number(score) || 0
  return '\u2605'.repeat(Math.round(n)) + '\u2606'.repeat(5 - Math.round(n))
}
function formatDate(v) {
  return v ? String(v).slice(0, 10) : '-'
}
function qualificationUrls(value) {
  return parseCommaSeparatedUrls(value)
}

function goBack() {
  router.push('/keeper-workflow')
}

async function reload() {
  loading.value = true
  try {
    const mine = await getMy()
    const current = mine?.keeper ?? mine
    if (current?.id_wsh) {
      profile.value = await KeeperDomain.getFullProfile(current.id_wsh)
    } else {
      profile.value = null
    }
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode is handled globally via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.kpr-page {
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.kpr-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.kpr-crumb {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.kpr-crumb-link { color: var(--ref-muted); text-decoration: none; transition: color 0.15s; }
.kpr-crumb-link:hover { color: var(--ref-ink); }
.kpr-crumb-sep { color: var(--ref-line); }
.kpr-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex; align-items: center; gap: 6px;
  border-radius: 6px; border: 1px solid transparent;
  padding: 4px 10px; font-size: 11px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.badge-action { background: var(--ref-ink); color: var(--ref-cream); }
.badge-active { background: color-mix(in srgb, var(--ref-brand) 14%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand) 28%, transparent); }
.badge-queued { background: var(--ref-surface); color: var(--ref-ink); border-color: color-mix(in srgb, var(--ref-ink) 22%, transparent); }
.badge-done { background: color-mix(in srgb, var(--ref-ink) 6%, transparent); color: var(--ref-ink-soft); }
.badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-danger { background: color-mix(in srgb, var(--ref-brand-deep) 10%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand-deep) 24%, transparent); }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-ghost { background: transparent; color: var(--ref-ink-soft); }
.cta-ghost:hover { color: var(--ref-brand); background: transparent; }

/* ═══ Header ═══ */
.kpr-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 24px; padding: 20px 0 8px;
}
.kpr-head-copy { min-width: 0; flex: 1 1 320px; }
.kpr-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.kpr-idx { font-variant-numeric: tabular-nums; }
.kpr-line { width: 24px; height: 1px; background: var(--ref-line); }
.kpr-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 42px); font-weight: 500; line-height: 1.1;
  letter-spacing: -0.01em; color: var(--ref-ink); text-wrap: balance;
}
.kpr-desc { margin: 10px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }
.kpr-head-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ Section ═══ */
.kpr-section { margin-top: 44px; }
.kpr-section-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 4px 24px; }
.kpr-section-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.kpr-section-desc { margin: 8px 0 0; max-width: 640px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }

/* ═══ Cards ═══ */
.kpr-card {
  margin-top: 20px; padding: 24px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
}

/* ═══ 01 身份与状态 ═══ */
.kpr-identity {
  margin-top: 20px; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: var(--r-panel); background: var(--ref-surface);
}
.kpr-identity-top {
  display: flex; flex-direction: column; gap: 20px;
  padding: 28px 28px;
}
.kpr-avatar {
  width: 96px; height: 96px; flex-shrink: 0; overflow: hidden;
  border-radius: 50%; border: 1px solid var(--ref-line); background: var(--ref-sand);
}
.kpr-avatar :deep(.media-image) { width: 100%; height: 100%; object-fit: cover; }
.kpr-avatar :deep(.media-placeholder) { width: 100%; height: 100%; border-radius: 50%; background: var(--ref-sand); }
.kpr-identity-info { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 10px; }
.kpr-identity-title { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.kpr-identity-name {
  margin: 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 500; line-height: 1.2; letter-spacing: -0.01em; color: var(--ref-ink);
}
.kpr-identity-meta {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px 18px;
  font-size: 13px; color: var(--ref-ink-soft);
}
.kpr-rating { display: inline-flex; align-items: center; gap: 8px; }
.kpr-rating-stars { font-size: 13px; color: var(--ref-brand); letter-spacing: 2px; }
.kpr-rating b { font-variant-numeric: tabular-nums; color: var(--ref-ink); font-weight: 500; }
.kpr-merchant-link {
  color: var(--ref-ink-soft); text-decoration: underline; text-underline-offset: 4px;
  text-decoration-color: var(--ref-line); transition: color 0.15s, text-decoration-color 0.15s;
}
.kpr-merchant-link:hover { color: var(--ref-brand); text-decoration-color: var(--ref-brand); }
.kpr-meta-muted { color: var(--ref-muted); }
.kpr-stats {
  display: grid; grid-template-columns: 1fr 1fr; margin: 0;
  border-top: 1px solid var(--ref-line);
}
.kpr-stat { padding: 20px 28px; border-right: 1px solid var(--ref-line); border-bottom: 1px solid var(--ref-line); }
.kpr-stat:nth-child(2n) { border-right: 0; }
.kpr-stat:nth-last-child(-n+2) { border-bottom: 0; }
.kpr-stat dt { font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.kpr-stat dd {
  margin: 8px 0 0; font-family: var(--ref-font-display);
  font-size: 26px; font-weight: 400; line-height: 1.1; color: var(--ref-ink); font-variant-numeric: tabular-nums;
  display: flex; align-items: baseline; gap: 8px;
}
.kpr-stat-hint { font-size: 10.5px; font-weight: 400; color: var(--ref-muted); font-variant-numeric: normal; }

/* ═══ 02 服务能力 ═══ */
.kpr-capacity {
  display: grid; grid-template-columns: 1fr; gap: 16px; margin-top: 20px;
}
.kpr-load-head { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; }
.kpr-load-label { margin: 0; font-size: 13px; color: var(--ref-ink-soft); }
.kpr-load-count { margin: 0; font-size: 13px; color: var(--ref-muted); }
.kpr-load-track {
  margin-top: 14px; height: 8px; width: 100%; overflow: hidden;
  border-radius: 999px; background: var(--ref-sand);
}
.kpr-load-bar { height: 100%; border-radius: 999px; background: var(--ref-brand); transition: width 0.3s ease; }
.kpr-load-caption { margin: 12px 0 0; font-size: 11px; line-height: 1.7; color: var(--ref-muted); }
.kpr-price-card { background: color-mix(in srgb, var(--ref-cream) 50%, var(--ref-surface)); }
.kpr-price-label { margin: 0; font-size: 10.5px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--ref-muted); }
.kpr-price {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 34px; font-weight: 400; line-height: 1; letter-spacing: -0.02em; color: var(--ref-ink); font-variant-numeric: tabular-nums;
}
.kpr-price-symbol { font-size: 17px; color: var(--ref-muted); }
.kpr-price-caption { margin: 12px 0 0; font-size: 11px; line-height: 1.7; color: var(--ref-muted); }

/* ═══ 03 简介 ═══ */
.kpr-bio {
  margin: 0; max-width: 760px; white-space: pre-line;
  font-size: 14px; line-height: 1.85; color: var(--ref-ink-soft);
}
.kpr-bio-empty { margin: 0; font-size: 13px; color: var(--ref-muted); }

/* ═══ 04 资质 ═══ */
.kpr-qual-list {
  margin: 20px 0 0; padding: 0; list-style: none;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface); overflow: hidden;
}
.kpr-qual-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-top: 1px solid var(--ref-line);
}
.kpr-qual-item:first-child { border-top: 0; }
.kpr-qual-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--ref-sand); color: var(--ref-brand); font-size: 13px; flex-shrink: 0;
}
.kpr-qual-body { min-width: 0; flex: 1; display: grid; gap: 6px; }
.kpr-qual-title { font-size: 13.5px; font-weight: 500; color: var(--ref-ink); }
.kpr-qual-thumbs { display: flex; flex-wrap: wrap; gap: 6px; }
.kpr-qual-thumbs img { width: 44px; height: 44px; border-radius: 6px; object-fit: cover; border: 1px solid var(--ref-line); }

/* ═══ 05 评价 ═══ */
.kpr-rating-list {
  margin: 20px 0 0; padding: 0; list-style: none;
  display: grid; grid-template-columns: 1fr; gap: 16px;
}
.kpr-rating-card {
  height: 100%; padding: 20px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-surface);
}
.kpr-rating-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.kpr-rating-stars { font-size: 13px; color: var(--ref-brand); letter-spacing: 2px; }
.kpr-rating-date { font-size: 11px; color: var(--ref-muted); }
.kpr-rating-content { margin: 12px 0 0; font-size: 13.5px; line-height: 1.7; color: var(--ref-ink-soft); }

/* ═══ Empty ═══ */
.kpr-empty {
  margin-top: 20px; padding: 72px 24px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.kpr-empty-inline { margin-top: 20px; padding: 48px 24px; }
.kpr-empty-title { margin: 0; font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--ref-ink); }
.kpr-empty-desc { margin: 8px 0 0; font-size: 13px; color: var(--ref-muted); }
.kpr-empty-actions { margin-top: 22px; display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; }

/* ═══ Loading skeleton ═══ */
.kpr-skel {
  border-radius: var(--r-card);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: kpr-shimmer 1.3s linear infinite;
}
.kpr-skel-hero { height: 180px; margin-top: 24px; }
.kpr-skel-block { height: 220px; margin-top: 24px; }

/* ═══ Animations ═══ */
@keyframes kpr-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (min-width: 720px) {
  .kpr-identity-top { flex-direction: row; align-items: center; }
  .kpr-stats { grid-template-columns: repeat(4, 1fr); }
  .kpr-stat { border-right: 1px solid var(--ref-line); border-bottom: 0; }
  .kpr-stat:last-child { border-right: 0; }
  .kpr-capacity { grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr); }
  .kpr-rating-list { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 600px) {
  .kpr-shell { padding: 0 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .kpr-skel { animation: none; }
  .kpr-load-bar { transition: none; }
  .cta:hover { transform: none; }
}
</style>