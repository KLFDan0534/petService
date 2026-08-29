<template>
  <div>
    <div class="coupon-actions">
      <button class="btn btn-outline btn-sm" type="button" :disabled="loading" @click="loadAll">刷新</button>
    </div>

    <section class="coupon-section">
      <div class="section-title">
        <h3>可领取优惠券</h3>
        <span>{{ activeTemplates.length }} 张活动券</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="activeTemplates.length === 0" class="empty-panel">暂无可领取优惠券</div>
      <div v-else class="coupon-grid">
        <article v-for="item in activeTemplates" :key="item.id_wsh" class="coupon-card">
          <div>
            <div class="coupon-name">{{ item.name_wsh }}</div>
            <div class="coupon-value">{{ discountText(item) }}</div>
            <div class="coupon-meta">满 ¥{{ money(item.threshold_amount_wsh) }} 可用</div>
            <div class="coupon-meta">{{ scopeText(item) }} · {{ dateText(item.valid_to_wsh) }} 失效</div>
          </div>
          <button class="btn btn-primary btn-sm" type="button" :disabled="submittingId === item.id_wsh" @click="claim(item)">
            {{ submittingId === item.id_wsh ? '领取中...' : '领取' }}
          </button>
        </article>
      </div>
    </section>

    <section class="coupon-section">
      <div class="section-title">
        <h3>我的券包</h3>
        <span>{{ myCoupons.length }} 张</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="myCoupons.length === 0" class="empty-panel">暂无优惠券</div>
      <div v-else class="coupon-grid">
        <article v-for="item in myCoupons" :key="item.id_wsh" class="coupon-card" :class="{ muted: item.status_wsh !== 'available' }">
          <div>
            <div class="coupon-name">{{ item.name_wsh || `优惠券 #${item.id_wsh}` }}</div>
            <div class="coupon-value">{{ discountText(item) }}</div>
            <div class="coupon-meta">满 ¥{{ money(item.threshold_amount_wsh) }} 可用</div>
            <div class="coupon-meta">{{ scopeText(item) }} · {{ dateText(item.expire_at_wsh) }} 失效</div>
          </div>
          <span :class="['coupon-status', statusClass(item.status_wsh)]">{{ statusText(item.status_wsh) }}</span>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { claimCoupon, getActiveCouponTemplates, getMyCoupons } from '@/api/coupon'

const appStore = useAppStore()
const loading = ref(false)
const submittingId = ref(null)
const activeTemplates = ref([])
const myCoupons = ref([])

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [templatesRes, couponsRes] = await Promise.all([getActiveCouponTemplates(), getMyCoupons()])
    if (templatesRes.code === 200) activeTemplates.value = Array.isArray(templatesRes.data) ? templatesRes.data : []
    if (couponsRes.code === 200) myCoupons.value = Array.isArray(couponsRes.data) ? couponsRes.data : []
  } catch (e) {
    appStore.addToast('优惠券加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function claim(item) {
  if (submittingId.value) return
  submittingId.value = item.id_wsh
  try {
    const res = await claimCoupon(item.id_wsh)
    if (res.code === 200) {
      appStore.addToast('领取成功', 'success')
      await loadAll()
    }
  } catch (e) {
    appStore.addToast('领取失败', 'error')
  } finally {
    submittingId.value = null
  }
}

function discountText(item) {
  if (item.type_wsh === 'percent') {
    const rate = Number(item.discount_rate_wsh || 0)
    const max = Number(item.max_discount_amount_wsh || 0)
    return `${(rate * 10).toFixed(1)}折${max > 0 ? `，最高减 ¥${money(max)}` : ''}`
  }
  const amount = item.discount_amount_template_wsh ?? item.discount_amount_wsh
  return `立减 ¥${money(amount)}`
}

function scopeText(item) {
  return item.scope_type_wsh === 'merchant' ? `商家 ${item.merchant_id_wsh}` : '全平台'
}

function statusText(status) {
  return {
    available: '可用',
    locked: '已锁定',
    used: '已使用',
  }[status] || status || '-'
}

function statusClass(status) {
  return {
    available: 'is-on',
    locked: 'is-wait',
    used: 'is-off',
  }[status] || 'is-off'
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.coupon-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.coupon-section {
  margin-bottom: 28px;
}
.section-title {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-title h3 {
  margin: 0;
}
.section-title span,
.coupon-meta {
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.coupon-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}
.coupon-card {
  align-items: flex-start;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  min-height: 136px;
  padding: 16px;
}
.coupon-card.muted {
  opacity: 0.72;
}
.coupon-name {
  font-weight: 700;
  margin-bottom: 8px;
}
.coupon-value {
  color: var(--color-primary);
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 8px;
}
.coupon-status {
  border-radius: 999px;
  flex-shrink: 0;
  font-size: 12px;
  min-width: 56px;
  padding: 4px 8px;
  text-align: center;
}
.coupon-status.is-on {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}
.coupon-status.is-wait {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}
.coupon-status.is-off {
  background: rgba(100, 116, 139, 0.14);
  color: var(--color-muted-foreground);
}
.empty-panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-muted-foreground);
  padding: 24px;
  text-align: center;
}
</style>
