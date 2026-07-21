<template>
  <div class="admin-dashboard">
    <div class="stat-grid dashboard-stats">
      <div v-for="item in summaryCards" :key="item.label" :class="['stat-card', item.accent ? 'accent' : '']">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <section class="dashboard-grid">
      <div class="dashboard-panel">
        <div class="panel-heading">
          <h3>业务规模</h3>
          <span>核心资源占比</span>
        </div>
        <div class="bar-chart">
          <div v-for="item in scaleBars" :key="item.label" class="bar-row">
            <span>{{ item.label }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: item.percent + '%' }"></div>
            </div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="dashboard-panel">
        <div class="panel-heading">
          <h3>订单处理</h3>
          <span>完成率 / 待处理</span>
        </div>
        <div class="order-chart">
          <div class="donut" :style="{ '--done': completedRate + '%' }">
            <strong>{{ completedRate }}%</strong>
            <span>完成率</span>
          </div>
          <div class="order-metrics">
            <div><span class="legend-dot done"></span>已完成 {{ stats.completedOrders || 0 }}</div>
            <div><span class="legend-dot pending"></span>待处理 {{ stats.pendingOrders || 0 }}</div>
            <div><span class="legend-dot total"></span>总订单 {{ stats.totalOrders || 0 }}</div>
          </div>
        </div>
      </div>

      <div class="dashboard-panel revenue-panel">
        <div class="panel-heading">
          <h3>收入概览</h3>
          <span>有效订单汇总</span>
        </div>
        <div class="revenue-total">¥{{ formatNumber(stats.totalRevenue || 0) }}</div>
        <div class="revenue-meter">
          <div :style="{ width: revenueMeter + '%' }"></div>
        </div>
        <p>单均收入 ¥{{ avgOrderRevenue }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminStatistics } from '@/api/statistics'

const stats = ref({})

const summaryCards = computed(() => [
  { label: '总用户', value: stats.value.totalUsers || 0, accent: true },
  { label: '商家', value: stats.value.totalMerchants || 0 },
  { label: '寄养员', value: stats.value.totalKeepers || 0 },
  { label: '宠物', value: stats.value.totalPets || 0 },
  { label: '订单', value: stats.value.totalOrders || 0, accent: true },
  { label: '已完成', value: stats.value.completedOrders || 0 },
  { label: '总收入', value: `¥${formatNumber(stats.value.totalRevenue || 0)}`, accent: true },
  { label: '待处理', value: stats.value.pendingOrders || 0 },
])

const scaleBars = computed(() => {
  const rows = [
    { label: '用户', value: Number(stats.value.totalUsers || 0) },
    { label: '商家', value: Number(stats.value.totalMerchants || 0) },
    { label: '寄养员', value: Number(stats.value.totalKeepers || 0) },
    { label: '宠物', value: Number(stats.value.totalPets || 0) },
  ]
  const max = Math.max(...rows.map(item => item.value), 1)
  return rows.map(item => ({ ...item, percent: Math.max(4, Math.round((item.value / max) * 100)) }))
})

const completedRate = computed(() => {
  const total = Number(stats.value.totalOrders || 0)
  if (!total) return 0
  return Math.round((Number(stats.value.completedOrders || 0) / total) * 100)
})

const revenueMeter = computed(() => {
  const revenue = Number(stats.value.totalRevenue || 0)
  const orders = Math.max(Number(stats.value.totalOrders || 0), 1)
  const avg = revenue / orders
  return Math.max(8, Math.min(100, Math.round(avg)))
})

const avgOrderRevenue = computed(() => {
  const total = Number(stats.value.totalOrders || 0)
  if (!total) return '0.00'
  return formatNumber(Number(stats.value.totalRevenue || 0) / total)
})

onMounted(loadStats)

async function loadStats() {
  try {
    const response = await getAdminStatistics()
    if (response.code === 200) stats.value = response.data || {}
  } catch (e) {}
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}
</script>

<style scoped>
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-stats {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.dashboard-stats .stat-card {
  min-height: 112px;
}

.dashboard-stats .stat-value {
  font-size: 30px;
  font-variant-numeric: tabular-nums;
}

.stat-card.accent {
  border-color: color-mix(in srgb, var(--color-primary) 42%, var(--color-border));
}

.dashboard-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  padding: 18px;
}

.panel-heading {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-heading h3 {
  font-size: 16px;
  margin: 0;
}

.panel-heading span,
.revenue-panel p {
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin: 0;
}

.bar-chart,
.order-metrics {
  display: grid;
  gap: 12px;
}

.bar-row {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: 54px minmax(0, 1fr) 52px;
}

.bar-row span {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.bar-track,
.revenue-meter {
  background: var(--color-muted);
  border-radius: 999px;
  height: 10px;
  overflow: hidden;
}

.bar-fill,
.revenue-meter div {
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  border-radius: inherit;
  height: 100%;
}

.order-chart {
  align-items: center;
  display: grid;
  gap: 18px;
  grid-template-columns: 132px minmax(0, 1fr);
}

.donut {
  align-items: center;
  aspect-ratio: 1;
  background:
    radial-gradient(circle at center, var(--color-card) 0 58%, transparent 59%),
    conic-gradient(var(--color-primary) var(--done), var(--color-muted) 0);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.donut strong {
  color: var(--color-primary);
  font-size: 26px;
}

.donut span {
  color: var(--color-muted-foreground);
  font-size: 12px;
}

.legend-dot {
  border-radius: 50%;
  display: inline-block;
  height: 10px;
  margin-right: 8px;
  width: 10px;
}

.legend-dot.done { background: var(--color-primary); }
.legend-dot.pending { background: var(--color-warning); }
.legend-dot.total { background: var(--color-accent); }

.revenue-panel {
  display: grid;
  gap: 12px;
}

.revenue-total {
  color: var(--color-primary);
  font-size: 32px;
  font-weight: 800;
}

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
