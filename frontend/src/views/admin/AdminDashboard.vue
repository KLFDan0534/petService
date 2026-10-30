<template>
  <div class="admin-dashboard">
    <div class="stat-grid dashboard-stats">
      <!-- 卡片是循环出来的，accent 只是让几张重点卡片描边亮一点 -->
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
          <!-- 圆环画多少由 CSS 变量 --done 决定，把 0~100 的数字直接塞进去就行 -->
          <div class="donut" :style="{ '--done': completedRate + '%' }">
            <strong>{{ completedRate }}%</strong>
            <span>完成率</span>
          </div>
          <div class="order-metrics">
            <div><span class="legend-dot done"></span>已完成 {{ stats.completed_orders_wsh || 0 }}</div>
            <div><span class="legend-dot pending"></span>待处理 {{ stats.pending_orders_wsh || 0 }}</div>
            <div><span class="legend-dot total"></span>总订单 {{ stats.total_orders_wsh || 0 }}</div>
          </div>
        </div>
      </div>

      <div class="dashboard-panel revenue-panel">
        <div class="panel-heading">
          <h3>收入概览</h3>
          <span>有效订单汇总</span>
        </div>
        <div class="revenue-total">¥{{ formatNumber(stats.total_revenue_wsh || 0) }}</div>
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
import { formatNumber } from '@/utils/format'

// 统计接口返回的那一堆数字，下面所有卡片和图表都从这儿取
const stats = ref({})

// 顶部八张卡片，直接照着 stats 里的数拼出来
const summaryCards = computed(() => [
  { label: '总用户', value: stats.value.total_users_wsh || 0, accent: true },
  { label: '商家', value: stats.value.total_merchants_wsh || 0 },
  { label: '寄养员', value: stats.value.total_keepers_wsh || 0 },
  { label: '宠物', value: stats.value.total_pets_wsh || 0 },
  { label: '订单', value: stats.value.total_orders_wsh || 0, accent: true },
  { label: '已完成', value: stats.value.completed_orders_wsh || 0 },
  { label: '总收入', value: `¥${formatNumber(stats.value.total_revenue_wsh || 0)}`, accent: true },
  { label: '待处理', value: stats.value.pending_orders_wsh || 0 },
])

// 横条长度按四项里最大的那个算百分比，最大的正好占满；
// 最小也留 4%，不然数字是 0 的时候条会彻底消失，看着像没加载出来
const scaleBars = computed(() => {
  const rows = [
    { label: '用户', value: Number(stats.value.total_users_wsh || 0) },
    { label: '商家', value: Number(stats.value.total_merchants_wsh || 0) },
    { label: '寄养员', value: Number(stats.value.total_keepers_wsh || 0) },
    { label: '宠物', value: Number(stats.value.total_pets_wsh || 0) },
  ]
  // 拿最大的那项当分母，其他项按比例缩，条才不会被撑出格子
  const max = Math.max(...rows.map(item => item.value), 1)
  return rows.map(item => ({ ...item, percent: Math.max(4, Math.round((item.value / max) * 100)) }))
})

// 完成率 = 已完成 ÷ 总订单；总数为 0 时先挡一下，不然会算出 NaN
const completedRate = computed(() => {
  const total = Number(stats.value.total_orders_wsh || 0)
  if (!total) return 0
  return Math.round((Number(stats.value.completed_orders_wsh || 0) / total) * 100)
})

// 这条进度条拿单均收入当百分比用，纯粹为了视觉上有长短，不是真的占比
const revenueMeter = computed(() => {
  const revenue = Number(stats.value.total_revenue_wsh || 0)
  const orders = Math.max(Number(stats.value.total_orders_wsh || 0), 1)
  const avg = revenue / orders
  return Math.max(8, Math.min(100, Math.round(avg)))
})

// 单均收入，没有订单就显示 0.00
const avgOrderRevenue = computed(() => {
  const total = Number(stats.value.total_orders_wsh || 0)
  if (!total) return '0.00'
  return formatNumber(Number(stats.value.total_revenue_wsh || 0) / total)
})

onMounted(loadStats)

// 进来拉一次统计；接口挂了就什么都不做，卡片上继续显示 0
async function loadStats() {
  try {
    const response = await getAdminStatistics()
    if (response.code === 200) stats.value = response.data || {}
  } catch (e) {}
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
  border-radius: var(--radius-pill);
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
