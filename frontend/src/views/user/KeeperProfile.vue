<template>
  <div>
    <PageHero :title="keeper?.name_wsh || '看护者资料'" subtitle="查看当前看护者的基础信息、资质和服务表现" />

    <LoadingSpinner v-if="loading" text="加载看护者资料..." />

    <EmptyState
      v-else-if="!keeper"
      title="暂无看护者资料"
      description="当前账号还没有绑定看护者信息"
    >
      <button class="btn btn-primary" @click="goBack">返回工作台</button>
    </EmptyState>

    <div v-else class="detail-container">
      <div class="detail-main">
        <section class="card">
          <div class="keeper-hero">
            <img v-if="keeper.avatar_wsh" :src="keeper.avatar_wsh" class="keeper-avatar">
            <div v-else class="keeper-avatar-placeholder">{{ keeper.name_wsh?.charAt(0) || '护' }}</div>
            <div class="keeper-hero-info">
              <h1>{{ keeper.name_wsh }}</h1>
              <div class="keeper-meta">
                <span :class="['badge', onlineBadge]">{{ onlineLabel }}</span>
                <span class="rating-star">&#9733; {{ keeper.rating_wsh ?? '-' }}</span>
                <span class="text-muted">评价 {{ stats.ratingCount || 0 }} 条</span>
              </div>
            </div>
            <button class="btn btn-primary" @click="goBack">返回工作台</button>
          </div>

          <dl class="info-grid">
            <div>
              <dt>所属商家</dt>
              <dd>
                <button v-if="keeper.merchant_name_wsh" class="link-btn" @click="goMerchantDetail">
                  {{ keeper.merchant_name_wsh }}
                </button>
                <span v-else>-</span>
              </dd>
            </div>
            <div>
              <dt>从业经验</dt>
              <dd>{{ keeper.experience_years_wsh || 0 }} 年</dd>
            </div>
            <div>
              <dt>每日价格</dt>
              <dd class="price">&yen;{{ money(keeper.price_per_day_wsh) }} /天</dd>
            </div>
            <div>
              <dt>当前宠物</dt>
              <dd>{{ keeper.current_pets_wsh || 0 }} / {{ keeper.max_pets_wsh || 0 }}</dd>
            </div>
            <div>
              <dt>完成率</dt>
              <dd>{{ keeper.completion_rate_wsh ?? '-' }}%</dd>
            </div>
            <div>
              <dt>投诉率</dt>
              <dd>{{ keeper.complaint_rate_wsh ?? '-' }}%</dd>
            </div>
          </dl>

          <div v-if="keeper.bio_wsh" class="desc-block">
            <h4>个人简介</h4>
            <p>{{ keeper.bio_wsh }}</p>
          </div>

          <div v-if="qualifications.length > 0" class="desc-block">
            <h4>资质证书</h4>
            <div class="qual-grid">
              <div v-for="q in qualifications" :key="q.id_wsh" class="qual-item">
                <div v-if="qualificationUrls(q.file_url_wsh).length" class="qual-images">
                  <img
                    v-for="url in qualificationUrls(q.file_url_wsh)"
                    :key="url"
                    :src="url"
                    :alt="q.title_wsh"
                  >
                </div>
                <span>{{ q.title_wsh }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="card">
          <h3 class="section-title">评价概览</h3>
          <div class="rating-summary">
            <div class="rating-score">
              <span class="score-big">{{ stats.avgScore || '-' }}</span>
              <span class="text-muted">/ 5</span>
            </div>
          </div>
          <p class="text-muted">累计评价 {{ stats.ratingCount || 0 }} 条</p>
        </section>
      </div>

      <aside class="detail-sidebar">
        <div class="card">
          <h4>快捷操作</h4>
          <button class="btn btn-outline btn-block" @click="goPublicDetail">查看公开主页</button>
          <button class="btn btn-outline btn-block" @click="goBack">返回工作台</button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHero from '@/components/common/PageHero.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getMy } from '@/services/keeperService'
import * as KeeperDomain from '@/domain/KeeperDomain'
import { KeeperOnlineStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'
import { parseCommaSeparatedUrls } from '@/utils/fileUrls'

const router = useRouter()
const loading = ref(true)
const profile = ref(null)

const keeper = computed(() => profile.value?.keeper ?? null)
const qualifications = computed(() => profile.value?.qualifications ?? [])
const stats = computed(() => profile.value?.stats ?? {})

const onlineLabel = computed(() => {
  const k = keeper.value
  return k ? getStatusLabel(KeeperOnlineStatus, k.status_wsh) : '-'
})

const onlineBadge = computed(() => {
  const k = keeper.value
  return k ? getStatusBadge(KeeperOnlineStatus, k.status_wsh) : 'badge-info'
})

function money(v) {
  return Number(v || 0).toFixed(2)
}

function qualificationUrls(value) {
  return parseCommaSeparatedUrls(value)
}

function goBack() {
  router.push('/keeper-workflow')
}

function goPublicDetail() {
  if (keeper.value?.id_wsh) router.push(`/keepers/${keeper.value.id_wsh}`)
}

function goMerchantDetail() {
  if (keeper.value?.merchant_id_wsh) router.push(`/merchants/${keeper.value.merchant_id_wsh}`)
}

onMounted(async () => {
  try {
    const mine = await getMy()
    const current = mine?.keeper ?? mine
    if (current?.id_wsh) {
      profile.value = await KeeperDomain.getFullProfile(current.id_wsh)
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-container {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 0;
}
.detail-main { display: flex; flex-direction: column; gap: 20px; }
.card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 8px; padding: 20px; }
.keeper-hero { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.keeper-avatar { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.keeper-avatar-placeholder { width: 72px; height: 72px; border-radius: 50%; background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 700; flex-shrink: 0; }
.keeper-hero-info { flex: 1; min-width: 0; }
.keeper-hero-info h1 { margin: 0; font-size: 22px; }
.keeper-meta { display: flex; align-items: center; gap: 10px; margin-top: 6px; flex-wrap: wrap; }
.rating-star { color: #f59e0b; font-weight: 600; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.info-grid dt { color: var(--color-muted-foreground); font-size: 12px; }
.info-grid dd { margin: 2px 0 0; }
.price { color: var(--color-destructive); font-weight: 700; }
.desc-block { margin-top: 16px; }
.desc-block h4, .section-title { margin: 0 0 8px; font-size: 16px; }
.desc-block p { color: var(--color-muted-foreground); line-height: 1.6; }
.qual-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.qual-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.qual-images { display: grid; grid-template-columns: repeat(auto-fit, minmax(72px, 1fr)); gap: 6px; width: 100%; max-width: 180px; }
.qual-images img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 6px; }
.qual-item span { font-size: 12px; color: var(--color-muted-foreground); }
.text-muted { color: var(--color-muted-foreground); font-size: 13px; }
.rating-summary { margin-bottom: 12px; }
.score-big { font-size: 36px; font-weight: 700; }
.detail-sidebar { display: flex; flex-direction: column; gap: 16px; }
.btn-block { width: 100%; }
.link-btn { background: none; border: none; color: var(--color-primary); cursor: pointer; padding: 0; font-size: inherit; text-decoration: underline; }
@media (max-width: 760px) {
  .detail-container { grid-template-columns: 1fr; }
}
</style>
