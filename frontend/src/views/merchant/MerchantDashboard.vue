<template>
  <div class="merchant-dashboard">
    <div class="merchant-title">
      <h2>商户控制台</h2>
      <p class="subtitle">欢迎回来，{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</p>
    </div>

    <div class="stat-grid merchant-stats">
      <div class="stat-card"><div class="stat-value">{{ stats.pets_wsh || 0 }}</div><div class="stat-label">我的宠物</div></div>
      <div class="stat-card accent"><div class="stat-value">{{ stats.active_orders_wsh || 0 }}</div><div class="stat-label">进行中订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.completed_orders_wsh || 0 }}</div><div class="stat-label">已完成订单</div></div>
      <div class="stat-card revenue"><div class="stat-value">¥{{ stats.total_revenue_wsh || 0 }}</div><div class="stat-label">总收入</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

// 顶部问候用登录账号的昵称，没昵称就退回用户名
const authStore = useAuthStore()
// 四张卡片的数字都放这儿，接口还没回来时是空对象，模板里靠 || 0 顶着
const stats = ref({})

// 进页面拉一次统计，拉不到就算了，卡片显示 0 也比甩个报错强
onMounted(async () => {
  try {
    const r = await request.get('/statistics/merchant')
    if (r.data.code === 200) stats.value = r.data.data
  } catch (e) {}
})
</script>

<style scoped>
.merchant-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.merchant-title {
  padding-bottom: 4px;
}

.merchant-title h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.3;
  color: var(--color-foreground);
}

.subtitle {
  margin: 6px 0 0;
  color: var(--color-muted-foreground);
}

.merchant-stats {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.merchant-stats .stat-card {
  min-height: 112px;
}

.merchant-stats .stat-value {
  font-size: 30px;
  font-variant-numeric: tabular-nums;
}

.stat-card.accent,
.stat-card.revenue {
  border-color: color-mix(in srgb, var(--color-primary) 42%, var(--color-border));
}
</style>
