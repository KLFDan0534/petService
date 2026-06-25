<template>
  <div>
    <h2 style="margin-bottom:16px">数据统计</h2>
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-value">{{ stats.totalOrders || 0 }}</div><div class="stat-label">总订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalRevenue || 0 }}</div><div class="stat-label">总收入</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.avgRating || 0 }}</div><div class="stat-label">平均评分</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const stats = ref({})

onMounted(async () => {
  try {
    const r = await request.get('/statistics/merchant')
    if (r.data.code === 200) stats.value = r.data.data
  } catch (e) {}
})
</script>
