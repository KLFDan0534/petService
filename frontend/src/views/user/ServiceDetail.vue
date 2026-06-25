<template>
  <div>
    <div v-if="loading" class="loading"><LoadingSpinner /></div>
    <template v-else-if="service">
      <div class="detail-header">
        <div v-if="service.images_wsh" class="detail-img" :style="{ backgroundImage: `url(${firstImage})` }"></div>
        <div class="detail-info">
          <h1>{{ service.name_wsh }}</h1>
          <span class="badge badge-info">{{ typeLabels[service.type_wsh] || service.type_wsh || '服务' }}</span>
          <p class="detail-price">¥{{ service.price_wsh }} <span>/ {{ service.unit_wsh }}</span></p>
          <p class="detail-desc">{{ service.description_wsh }}</p>
          <button class="btn btn-primary" @click="createOrder">立即预约</button>
        </div>
      </div>

      <h2 class="section-title">用户评价 ({{ ratings.length }})</h2>

      <div v-if="authStore.isLoggedIn" class="review-form">
        <h3>发表评价</h3>
        <div class="star-rating">
          <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= formScore }" @click="formScore = i">★</span>
        </div>
        <textarea v-model="formContent" rows="3" placeholder="分享您的使用体验..." class="form-control"></textarea>
        <button class="btn btn-primary btn-sm" :disabled="submitting" @click="submitRating">{{ submitting ? '提交中...' : '提交评价' }}</button>
      </div>
      <p v-else class="login-hint"><router-link to="/login">登录</router-link>后发表评价</p>

      <div v-if="ratings.length === 0" class="empty-state">
        <p>暂无评价</p>
      </div>
      <div v-else v-for="r in ratings" :key="r.id_wsh" class="review-card">
        <div class="review-header">
          <span class="review-user">用户 #{{ r.user_id_wsh }}</span>
          <span class="review-stars">★{{ r.score_wsh }}</span>
          <span class="review-date">{{ formatDate(r.created_at_wsh) }}</span>
        </div>
        <p class="review-content">{{ r.content_wsh }}</p>
        <div v-if="r.reply_wsh" class="review-reply">
          <strong>商家回复：</strong>{{ r.reply_wsh }}
        </div>
      </div>
    </template>
    <EmptyState v-else title="服务不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const service = ref(null)
const ratings = ref([])
const loading = ref(true)
const formScore = ref(5)
const formContent = ref('')
const submitting = ref(false)

const typeLabels = {
  boarding: '寄养',
  grooming: '美容',
  training: '训练',
  walk: '遛弯',
  medical: '医疗',
}

const firstImage = computed(() => {
  if (!service.value?.images_wsh) return ''
  return service.value.images_wsh.split(',')[0].trim()
})

function formatDate(d) {
  if (!d) return ''
  try { return new Date(d).toLocaleDateString() } catch { return d }
}

async function loadDetail() {
  loading.value = true
  try {
    const [svcRes, ratingRes] = await Promise.all([
      request.get(`/services/${route.params.id}`),
      request.get('/ratings', { params: { targetId: route.params.id, targetType: 'service' } }),
    ])
    if (svcRes.data.code === 200) service.value = svcRes.data.data
    if (ratingRes.data.code === 200) ratings.value = ratingRes.data.data || []
  } catch (e) {
    appStore.addToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function createOrder() {
  if (!service.value) return
  router.push({ path: '/orders', query: { create: 'true', serviceId: service.value.id_wsh, serviceName: service.value.name_wsh, price: service.value.price_wsh } })
}

async function submitRating() {
  if (!formContent.value.trim()) return appStore.addToast('请输入评价内容', 'error')
  submitting.value = true
  try {
    const r = await authStore.apiPost('/api/ratings', {
      target_id_wsh: Number(route.params.id),
      target_type_wsh: 'service',
      score_wsh: formScore.value,
      content_wsh: formContent.value,
    })
    if (r.code === 200) {
      appStore.addToast('评价成功', 'success')
      ratings.value.unshift(r.data)
      formContent.value = ''
      formScore.value = 5
    }
  } catch (e) {
    appStore.addToast('提交失败', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-header {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
}
.detail-img {
  width: 360px;
  min-height: 240px;
  background-size: cover;
  background-position: center;
  border-radius: 12px;
  flex-shrink: 0;
}
.detail-info { flex: 1; }
.detail-info h1 { margin: 0 0 8px; font-size: 24px; }
.detail-price { font-size: 22px; font-weight: 700; color: var(--color-primary); margin: 12px 0; }
.detail-price span { font-size: 14px; font-weight: 400; color: var(--color-muted-foreground); }
.detail-desc { font-size: 14px; line-height: 1.6; color: var(--color-muted-foreground); margin: 8px 0 16px; }
.section-title { margin: 32px 0 16px; font-size: 18px; }
.review-form { background: var(--color-muted); padding: 16px; border-radius: 12px; margin-bottom: 24px; }
.review-form h3 { margin: 0 0 12px; font-size: 15px; }
.star-rating { margin-bottom: 8px; }
.star { font-size: 24px; cursor: pointer; color: #ddd; margin-right: 4px; }
.star.filled { color: gold; }
.review-form textarea { width: 100%; margin-bottom: 8px; resize: vertical; }
.login-hint { text-align: center; padding: 16px; color: var(--color-muted-foreground); }
.review-card { padding: 16px 0; border-bottom: 1px solid var(--color-border); }
.review-header { display: flex; gap: 12px; align-items: center; margin-bottom: 8px; font-size: 13px; }
.review-user { font-weight: 600; }
.review-stars { color: gold; }
.review-date { color: var(--color-muted-foreground); margin-left: auto; }
.review-content { font-size: 14px; line-height: 1.5; }
.review-reply { margin-top: 8px; padding: 8px 12px; background: var(--color-muted); border-radius: 8px; font-size: 13px; }
.empty-state { text-align: center; padding: 32px; color: var(--color-muted-foreground); }
@media (max-width: 640px) {
  .detail-header { flex-direction: column; }
  .detail-img { width: 100%; height: 200px; }
}
</style>
