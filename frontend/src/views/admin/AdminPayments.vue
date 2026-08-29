<template>
  <div>
    <div class="admin-filters">
      <input
        v-model="filters.keyword"
        class="form-control"
        placeholder="搜索订单号/支付编号"
        @keyup.enter="applyFilters"
      >
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">搜索</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'订单号',key:'order_no_wsh'},
      {label:'支付编号',key:'pay_no_wsh'},
      {label:'金额',key:'amount_label_wsh'},
      {label:'支付方式',key:'method_wsh'},
      {label:'状态',key:'status_label_wsh'},
      {label:'创建时间',key:'created_at_wsh'}
    ]" :data="payments" />

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import { getAdminPayments } from '@/api/payment'
import { PaymentStatus, enrichWithStatus } from '@/constants/statusMaps'

const payments = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ keyword: '' })
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function fmtAmount(v) {
  if (v == null) return '-'
  return '¥' + Number(v).toFixed(2)
}

function enrich(p) {
  const e = enrichWithStatus(p, 'status_wsh', PaymentStatus)
  e.amount_label_wsh = fmtAmount(p.amount_wsh)
  return e
}

function buildParams() {
  const params = { page: currentPage.value, size: size.value }
  if (filters.keyword && filters.keyword.trim()) params.keyword = filters.keyword.trim()
  return params
}

async function load() {
  try {
    const r = await getAdminPayments(buildParams())
    if (r.code === 200) {
      payments.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { filters.keyword = ''; applyFilters() }
function changePage(page) { currentPage.value = page; load() }

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 220px; }
</style>