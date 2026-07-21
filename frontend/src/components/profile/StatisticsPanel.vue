<template>
  <div class="dashboard-stats">
    <div class="stat-item">
      <div class="stat-label">我的宠物</div>
      <div class="stat-number">{{ stats.pets ?? 0 }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-label">进行中订单</div>
      <div class="stat-number">{{ stats.activeOrders ?? 0 }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-label">已完成订单</div>
      <div class="stat-number">{{ stats.completedOrders ?? 0 }}</div>
    </div>
    <div class="stat-item highlight-item">
      <div class="stat-label">累计消费</div>
      <div class="stat-number">¥ {{ formatPrice(stats.totalSpent) }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  stats: {
    type: Object,
    default: () => ({ pets: 0, activeOrders: 0, completedOrders: 0, totalSpent: 0 }),
  },
})

const formatPrice = (val) => {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #dee0e3;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  margin: 24px 0;
  overflow: hidden;
}
.stat-item {
  background: #ffffff;
  padding: 16px 20px;
}
.highlight-item {
  background: #fafafa;
}
.stat-label {
  font-size: 12px;
  color: #646a73;
  margin-bottom: 8px;
}
.stat-number {
  font-size: 20px;
  font-weight: 700;
  color: #1f2329;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  overflow-wrap: anywhere;
}

@media (max-width: 760px) {
  .dashboard-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 420px) {
  .dashboard-stats {
    grid-template-columns: 1fr;
  }
}
</style>
