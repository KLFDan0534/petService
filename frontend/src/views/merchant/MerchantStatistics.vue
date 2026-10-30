<template>
  <div>
    <h2 style="margin-bottom:16px">数据统计</h2>
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-value">{{ stats.total_orders_wsh || 0 }}</div><div class="stat-label">总订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.total_revenue_wsh || 0 }}</div><div class="stat-label">总收入</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.avgRating || 0 }}</div><div class="stat-label">平均评分</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMerchantStatistics } from '@/api/statistics'

// 三个数字都从这一个对象里取，拉不到就一直是空对象，模板里显示 0
const stats = ref({})

onMounted(async () => {
  try {
    const r = await getMerchantStatistics()
    if (r.code === 200) stats.value = r.data
  } catch (e) {}
})
</script>
