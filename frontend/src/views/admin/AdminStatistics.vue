<template>
  <div>
    <div class="stat-grid" style="margin-bottom:32px">
      <div class="stat-card"><div class="stat-value">{{ stats.totalOrders || 0 }}</div><div class="stat-label">总订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalRevenue || 0 }}</div><div class="stat-label">总收入(¥)</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.activeUsers || 0 }}</div><div class="stat-label">活跃用户</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.completionRate || 0 }}%</div><div class="stat-label">完成率</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const stats = ref({})

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/statistics/admin'); if (r.code === 200) stats.value = r.data }
  catch (e) {}
})
</script>
