<template>
  <div>
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-value">{{ stats.totalUsers || 0 }}</div><div class="stat-label">总用户</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalMerchants || 0 }}</div><div class="stat-label">商家</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalKeepers || 0 }}</div><div class="stat-label">寄养员</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalPets || 0 }}</div><div class="stat-label">宠物</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalOrders || 0 }}</div><div class="stat-label">订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.completedOrders || 0 }}</div><div class="stat-label">已完成</div></div>
      <div class="stat-card"><div class="stat-value">¥{{ stats.totalRevenue || 0 }}</div><div class="stat-label">总收入</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.pendingOrders || 0 }}</div><div class="stat-label">待处理</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const stats = ref({})

onMounted(async () => {
  try {
    const r = await request.get('/statistics/admin')
    if (r.data.code === 200) stats.value = r.data.data
  } catch (e) {}
})
</script>
