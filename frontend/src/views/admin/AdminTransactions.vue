<template>
  <div>
    <section class="toolbar-card">
      <div>
        <h2>交易记录</h2>
        <p>查看全平台资金流水，可按类型筛选充值记录、余额调整记录。</p>
      </div>
      <button class="btn btn-outline btn-sm" type="button" @click="load">刷新</button>
    </section>

    <section class="filter-bar">
      <button
        v-for="opt in filters"
        :key="opt.value"
        class="btn btn-sm"
        :class="type === opt.value ? 'btn-primary' : 'btn-ghost'"
        type="button"
        @click="setType(opt.value)"
      >{{ opt.label }}</button>
    </section>

    <DataTable :columns="columns" :data="transactions">
      <template #default="{ row }">
        <span class="tag" :class="typeTagClass(row.type_wsh)">{{ typeLabel(row.type_wsh) }}</span>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTransactions } from '@/api/wallet'
import DataTable from '@/components/common/DataTable.vue'
import { TransactionStatus, enrichWithStatus } from '@/constants/statusMaps'

const transactions = ref([])
const type = ref('')
const filters = [
  { value: '', label: '全部' },
  { value: 'recharge', label: '充值记录' },
  { value: 'admin_adjust', label: '余额调整' },
  { value: 'payment', label: '订单支付' },
]

const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '类型', slot: true },
  { label: '金额', key: 'amount_wsh' },
  { label: '变动前', key: 'balance_before_wsh' },
  { label: '变动后', key: 'balance_after_wsh' },
  { label: '用户ID', key: 'user_id_wsh' },
  { label: '创建时间', key: 'created_at_wsh' },
]

const TYPE_LABELS = {
  recharge: '充值',
  admin_adjust: '余额调整',
  payment: '订单支付',
  coupon_subsidy: '优惠补贴',
  withdrawal: '提现',
}

function typeLabel(t) {
  return TYPE_LABELS[t] || t || '-'
}
function typeTagClass(t) {
  if (t === 'recharge') return 'tag-success'
  if (t === 'payment') return 'tag-info'
  if (t === 'admin_adjust') return 'tag-warning'
  return 'tag-neutral'
}

onMounted(load)

async function setType(t) {
  type.value = t
  await load()
}

async function load() {
  try {
    const r = await getTransactions({ type: type.value || undefined })
    if (r.code === 200) transactions.value = (Array.isArray(r.data) ? r.data : []).map(t => enrichWithStatus(t, 'status_wsh', TransactionStatus))
  } catch (e) {}
}
</script>

<style scoped>
.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
}
.toolbar-card h2 { margin: 0 0 4px; font-size: 20px; }
.toolbar-card p { margin: 0; color: var(--color-muted-foreground); font-size: 13px; }
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  line-height: 18px;
}
.tag-success { background: #e6f7ec; color: #16894a; }
.tag-info { background: #e6f0fa; color: #1a5fb4; }
.tag-warning { background: #fdf3e3; color: #b06a00; }
.tag-neutral { background: #f1f3f5; color: #5c6670; }
</style>