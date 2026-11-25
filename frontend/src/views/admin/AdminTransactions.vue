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

    <div class="pager-bar">
      <div class="pager-size">
        <span>每页</span>
        <select v-model.number="size" :disabled="loading" @change="changeSize">
          <option v-for="n in pageSizes" :key="n" :value="n">{{ n }} 条</option>
        </select>
      </div>
      <span class="pager-info">共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页</span>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || page <= 1" @click="goPage(page - 1)">上一页</button>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || pages === 0 || page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTransactionsPage } from '@/api/wallet'
import DataTable from '@/components/common/DataTable.vue'
import { TransactionStatus, enrichWithStatus } from '@/constants/statusMaps'

const transactions = ref([])
const type = ref('')
// 分页状态：流水表无限增长，必须服务端分页
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const pageSizes = [20, 50, 100]
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

// 类型码对照表，后端以后加了新类型也不会显示成空白
const TYPE_LABELS = {
  recharge: '充值',
  admin_adjust: '余额调整',
  payment: '订单支付',
  coupon_subsidy: '优惠补贴',
  withdrawal: '提现',
}

// 没登记过的类型直接显示原值，总比空着好
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

// 点筛选项就换类型，并回到第一页
async function setType(t) {
  type.value = t
  page.value = 1
  await load()
}

// 切换每页条数同样回第一页
async function changeSize() {
  page.value = 1
  await load()
}

async function goPage(target) {
  if (target < 1 || target === page.value) return
  if (pages.value && target > pages.value) return
  page.value = target
  await load()
}

// 选"全部"时类型传 undefined，让后端别加这个筛选条件
async function load() {
  loading.value = true
  try {
    const r = await getTransactionsPage({
      type: type.value || undefined,
      page: page.value,
      size: size.value,
    })
    if (r.code === 200) {
      const data = r.data || {}
      transactions.value = (Array.isArray(data.list) ? data.list : [])
        .map(t => enrichWithStatus(t, 'status_wsh', TransactionStatus))
      total.value = data.total || 0
      pages.value = data.pages || 0
      if (data.page) page.value = data.page
    }
  } catch (e) {
    // 失败时保留上一次结果，避免表格瞬间清空
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.pager-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.pager-size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: auto;
}
.pager-size select {
  width: auto;
  min-width: 84px;
}
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