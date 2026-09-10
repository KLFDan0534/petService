<template>
  <div class="admin-statistics-page">
    <section class="summary-strip">
      <div>
        <h2>数据统计</h2>
        <p>平台核心规模、订单处理、收入概览和管理覆盖检查。</p>
      </div>
      <button class="btn btn-outline btn-sm" type="button" :disabled="loading" @click="loadStats">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </section>

    <div class="stat-grid dashboard-stats">
      <div v-for="item in summaryCards" :key="item.label" :class="['stat-card', item.accent ? 'accent' : '']">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <section class="chart-grid">
      <div class="chart-panel">
        <div class="panel-heading">
          <h3>业务规模</h3>
          <span>用户 / 商家 / 寄养员 / 宠物</span>
        </div>
        <div class="bar-chart">
          <div v-for="item in scaleBars" :key="item.label" class="bar-row">
            <span class="bar-label">{{ item.label }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: item.percent + '%' }"></div>
            </div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="chart-panel">
        <div class="panel-heading">
          <h3>订单处理</h3>
          <span>完成率与待处理占比</span>
        </div>
        <div class="donut-wrap">
          <div class="donut" :style="{ '--done': completedRate + '%' }">
            <strong>{{ completedRate }}%</strong>
            <span>完成率</span>
          </div>
          <div class="order-breakdown">
            <div>
              <span class="legend-dot done"></span>
              已完成 {{ stats.completed_orders_wsh || 0 }}
            </div>
            <div>
              <span class="legend-dot pending"></span>
              待处理 {{ stats.pending_orders_wsh || 0 }}
            </div>
            <div>
              <span class="legend-dot total"></span>
              总订单 {{ stats.total_orders_wsh || 0 }}
            </div>
          </div>
        </div>
      </div>

      <div class="chart-panel wide">
        <div class="panel-heading">
          <h3>管理覆盖检查</h3>
          <span>按当前管理端页面与业务模块盘点</span>
        </div>
        <div class="coverage-grid">
          <div>
            <h4>已覆盖管理</h4>
            <div class="coverage-list">
              <span v-for="item in coveredModules" :key="item" class="coverage-pill covered">{{ item }}</span>
            </div>
          </div>
          <div>
            <h4>建议补齐</h4>
            <div class="coverage-list">
              <span v-for="item in missingModules" :key="item" class="coverage-pill missing">{{ item }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-panel">
        <div class="panel-heading">
          <h3>收入概览</h3>
          <span>按有效订单金额汇总</span>
        </div>
        <div class="revenue-card">
          <strong>¥{{ formatNumber(stats.total_revenue_wsh || 0) }}</strong>
          <span>平台总收入</span>
          <div class="revenue-meter">
            <div :style="{ width: revenueMeter + '%' }"></div>
          </div>
          <small>单均收入 ¥{{ avgOrderRevenue }}</small>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminStatistics } from '@/api/statistics'
import { useAppStore } from '@/stores/app'
import { formatNumber } from '@/utils/format'

const appStore = useAppStore()
const loading = ref(false)
const stats = ref({})

const coveredModules = [
  '用户管理', '角色管理', '实名审核', '商家审核', '寄养员管理', '资质审核',
  '宠物查看', '宠物品种管理', '服务分类管理', '订单管理', '退款审核',
  '优惠券管理', '提现审核', '钱包管理', '交易记录', '工单管理',
  '投诉处理', '内容审核', '公告管理', '广告管理', '知识库管理',
  '操作日志', '回收站',
]

const missingModules = [
  '支付记录管理', '打赏记录管理', '文件资源管理', '通知推送管理',
  '收藏数据管理', '地址数据管理', '看护出勤管理', '请假审批管理',
  '服务项目审核', '客服人员审核',
]

const summaryCards = computed(() => [
  { label: '总用户', value: stats.value.total_users_wsh || 0, accent: true },
  { label: '商家', value: stats.value.total_merchants_wsh || 0 },
  { label: '寄养员', value: stats.value.total_keepers_wsh || 0 },
  { label: '宠物', value: stats.value.total_pets_wsh || 0 },
  { label: '订单', value: stats.value.total_orders_wsh || 0, accent: true },
  { label: '已完成', value: stats.value.completed_orders_wsh || 0 },
  { label: '待处理', value: stats.value.pending_orders_wsh || 0 },
  { label: '总收入', value: `¥${formatNumber(stats.value.total_revenue_wsh || 0)}`, accent: true },
])

const scaleBars = computed(() => {
  const rows = [
    { label: '用户', value: Number(stats.value.total_users_wsh || 0) },
    { label: '商家', value: Number(stats.value.total_merchants_wsh || 0) },
    { label: '寄养员', value: Number(stats.value.total_keepers_wsh || 0) },
    { label: '宠物', value: Number(stats.value.total_pets_wsh || 0) },
  ]
  const max = Math.max(...rows.map(item => item.value), 1)
  return rows.map(item => ({ ...item, percent: Math.max(4, Math.round((item.value / max) * 100)) }))
})

const completedRate = computed(() => {
  const total = Number(stats.value.total_orders_wsh || 0)
  if (!total) return 0
  return Math.round((Number(stats.value.completed_orders_wsh || 0) / total) * 100)
})

const revenueMeter = computed(() => {
  const revenue = Number(stats.value.total_revenue_wsh || 0)
  const orders = Math.max(Number(stats.value.total_orders_wsh || 0), 1)
  const avg = revenue / orders
  return Math.max(8, Math.min(100, Math.round(avg)))
})

const avgOrderRevenue = computed(() => {
  const total = Number(stats.value.total_orders_wsh || 0)
  if (!total) return '0.00'
  return formatNumber(Number(stats.value.total_revenue_wsh || 0) / total)
})

onMounted(loadStats)

async function loadStats() {
  loading.value = true
  try {
    const response = await getAdminStatistics()
    if (response.code === 200) {
      stats.value = response.data || {}
    }
  } catch (e) {
    appStore.addToast('统计数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-statistics-page {
  display: grid;
  gap: 20px;
}

.summary-strip,
.chart-panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.summary-strip {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  padding: 18px 20px;
}

.summary-strip h2,
.panel-heading h3,
.coverage-grid h4 {
  margin: 0;
}

.summary-strip p,
.panel-heading span {
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin: 4px 0 0;
}

.dashboard-stats {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.stat-card.accent {
  border-color: color-mix(in srgb, var(--color-primary) 42%, var(--color-border));
}

.chart-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.chart-panel {
  padding: 18px;
}

.chart-panel.wide {
  grid-column: 1 / -1;
}

.panel-heading {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.bar-chart {
  display: grid;
  gap: 14px;
}

.bar-row {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: 54px minmax(0, 1fr) 56px;
}

.bar-label {
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

.donut-wrap {
  align-items: center;
  display: grid;
  gap: 20px;
  grid-template-columns: 160px minmax(0, 1fr);
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
  font-size: 30px;
}

.donut span,
.revenue-card span,
.revenue-card small {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.order-breakdown {
  display: grid;
  gap: 10px;
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

.coverage-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: 1fr 1fr;
}

.coverage-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.coverage-pill {
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 700;
  padding: 5px 9px;
}

.coverage-pill.covered {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.coverage-pill.missing {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.revenue-card {
  display: grid;
  gap: 10px;
}

.revenue-card strong {
  color: var(--color-primary);
  font-size: 34px;
}

@media (max-width: 900px) {
  .chart-grid,
  .coverage-grid {
    grid-template-columns: 1fr;
  }

  .donut-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
