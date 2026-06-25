<template>
  <div>
    <h2>商户控制台</h2>
    <p class="subtitle">欢迎回来，{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</p>
    <div class="stat-grid" style="margin-top:24px">
      <div class="stat-card"><div class="stat-value">{{ stats.pets || 0 }}</div><div class="stat-label">我的宠物</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.activeOrders || 0 }}</div><div class="stat-label">进行中订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.completedOrders || 0 }}</div><div class="stat-label">已完成订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalRevenue || 0 }}</div><div class="stat-label">总收入</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const authStore = useAuthStore()
const stats = ref({})

onMounted(async () => {
  try {
    const r = await request.get('/statistics/merchant')
    if (r.data.code === 200) stats.value = r.data.data
  } catch (e) {}
})
</script>
