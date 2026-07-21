<template>
  <div>
    <PageHero :title="keeper?.name_wsh || '看护者详情'" subtitle="看护者信息与服务评价" />

    <LoadingSpinner v-if="loading" text="加载看护者信息..." />

    <div v-else-if="keeper" class="detail-container">
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
                  <button
                    class="btn btn-sm"
                    :class="isFavorited ? 'btn-primary' : 'btn-outline'"
                    :disabled="favoriteLoading || favoriteToggling"
                    @click="handleToggleFavorite"
                  >
                    <el-icon><Star /></el-icon>
                    <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
                  </button>
              </div>
            </div>
            <button class="btn btn-primary" @click="goToChat">联系</button>
          </div>

          <dl class="info-grid">
            <div>
              <dt>所属商家</dt>
              <dd>
                <button v-if="keeper.merchant_name_wsh" class="link-btn" @click="goToMerchant(keeper.merchant_id_wsh)">
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
              <dt>服务完成率</dt>
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
          <h3 class="section-title">用户评价 ({{ ratings.length }})</h3>
          <div v-if="ratings.length > 0" class="rating-summary">
            <div class="rating-score">
              <span class="score-big">{{ avgScore }}</span>
              <span class="text-muted">/ 5</span>
            </div>
          </div>
          <div v-if="ratings.length > 0" class="rating-list">
            <div v-for="r in ratings" :key="r.id_wsh" class="rating-item">
              <div class="rating-header">
                <strong>用户 #{{ r.user_id_wsh }}</strong>
                <span class="stars">{{ renderStars(r.score_wsh) }}</span>
                <span class="text-muted">{{ formatDate(r.created_at_wsh) }}</span>
              </div>
              <p v-if="r.content_wsh">{{ r.content_wsh }}</p>
              <div v-if="r.reply_wsh" class="rating-reply">
                <span class="text-muted">回复：</span>{{ r.reply_wsh }}
              </div>
            </div>
          </div>
          <EmptyState v-else title="暂无评价" description="还没有用户评价" />
        </section>
      </div>

      <aside class="detail-sidebar">
        <div class="card">
          <h4>快捷操作</h4>
          <button class="btn btn-primary btn-block" @click="goToChat">联系看护者</button>
          <button v-if="keeper.merchant_id_wsh" class="btn btn-outline btn-block" @click="goToMerchant(keeper.merchant_id_wsh)">
            查看商家
          </button>
          <button class="btn btn-outline btn-block" @click="goBack">返回列表</button>
        </div>
      </aside>
    </div>

    <EmptyState v-else title="看护者不存在" description="找不到该看护者信息">
      <button class="btn btn-primary" @click="goBack">返回列表</button>
    </EmptyState>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useKeeperDetail } from '@/composables/useKeeperDetail'
import { useFavoriteState } from '@/composables/useFavoriteState'
import PageHero from '@/components/common/PageHero.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { Star } from '@element-plus/icons-vue'
import { FAVORITE_TARGET_TYPES } from '@/constants/favorite'
import { parseCommaSeparatedUrls } from '@/utils/fileUrls'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const {
  loading, keeper, ratings, qualifications,
  avgScore, onlineLabel, onlineBadge,
  load,
} = useKeeperDetail(route.params.id)

const favoriteTargetId = computed(() => Number(route.params.id))
const {
  isFavorited,
  loading: favoriteLoading,
  toggling: favoriteToggling,
  toggle: toggleFavoriteState,
} = useFavoriteState(favoriteTargetId, FAVORITE_TARGET_TYPES.KEEPER)

function renderStars(score) {
  const n = Number(score) || 0
  return '\u2605'.repeat(Math.round(n)) + '\u2606'.repeat(5 - Math.round(n))
}

function money(v) { return Number(v || 0).toFixed(2) }
function formatDate(v) { return v ? String(v).slice(0, 10) : '-' }
function qualificationUrls(value) {
  return parseCommaSeparatedUrls(value)
}

async function handleToggleFavorite() {
  if (!authStore.isLoggedIn) {
    appStore.showLoginPrompt = true
    return
  }
  try {
    await toggleFavoriteState()
    appStore.addToast(isFavorited.value ? '已收藏' : '已取消收藏', 'success')
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  }
}

function goToChat() {
  if (!authStore.isLoggedIn) {
    appStore.showLoginPrompt = true
    return
  }
  router.push({
    path: '/orders',
    query: {
      create: 'true',
      merchantId: keeper.value.merchant_id_wsh,
      keeperId: keeper.value.id_wsh,
    },
  })
}

function goToMerchant(merchantId) {
  router.push(`/merchants/${merchantId}`)
}

function goBack() {
  router.push('/keepers')
}

onMounted(load)
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
.keeper-meta { display: flex; align-items: center; gap: 10px; margin-top: 6px; }
.rating-star { color: #f59e0b; font-weight: 600; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.info-grid dt { color: var(--color-muted-foreground); font-size: 12px; }
.info-grid dd { margin: 2px 0 0; }
.price { color: var(--color-destructive); font-weight: 700; }
.desc-block { margin-top: 16px; }
.desc-block h4 { margin: 0 0 8px; font-size: 14px; }
.desc-block p { color: var(--color-muted-foreground); line-height: 1.6; }
.qual-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.qual-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.qual-images { display: grid; grid-template-columns: repeat(auto-fit, minmax(72px, 1fr)); gap: 6px; width: 100%; max-width: 180px; }
.qual-images img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 6px; }
.qual-item span { font-size: 12px; color: var(--color-muted-foreground); }
.section-title { margin: 0 0 14px; font-size: 16px; }
.text-muted { color: var(--color-muted-foreground); font-size: 13px; }
.rating-summary { margin-bottom: 12px; }
.score-big { font-size: 36px; font-weight: 700; }
.rating-list { display: flex; flex-direction: column; gap: 14px; }
.rating-item { border-top: 1px solid var(--color-border); padding-top: 14px; }
.rating-header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.stars { color: #f59e0b; }
.rating-reply { margin-top: 6px; padding: 8px; background: var(--color-muted); border-radius: 6px; font-size: 13px; }
.detail-sidebar { display: flex; flex-direction: column; gap: 16px; }
.btn-block { width: 100%; }
.link-btn { background: none; border: none; color: var(--color-primary); cursor: pointer; padding: 0; font-size: inherit; text-decoration: underline; }
@media (max-width: 760px) { .detail-container { grid-template-columns: 1fr; } }
</style>
