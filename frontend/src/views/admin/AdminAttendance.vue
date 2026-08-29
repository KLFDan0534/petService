<template>
  <div>
    <div class="admin-filters">
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">筛选</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'寄养员',key:'keeper_name_wsh'},
      {label:'商家',key:'merchant_name_wsh'},
      {label:'签到时间',key:'check_in_at_wsh'},
      {label:'签退时间',key:'check_out_at_wsh'},
      {label:'状态',key:'duty_label_wsh'}
    ]" :data="attendance" />

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
import { getAdminAttendance } from '@/api/attendance'

const attendance = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({})
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrich(a) {
  const onDuty = a.on_duty_wsh === true
  return {
    ...a,
    keeper_name_wsh: a.keeper_name_wsh || '-',
    merchant_name_wsh: a.merchant_name_wsh || '-',
    check_out_at_wsh: a.check_out_at_wsh || '-',
    duty_label_wsh: onDuty
      ? { label: '在岗', badge: 'badge-success' }
      : { label: '已签退', badge: 'badge-secondary' },
  }
}

async function load() {
  try {
    const r = await getAdminAttendance({ page: currentPage.value, size: size.value })
    if (r.code === 200) {
      attendance.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { applyFilters() }
function changePage(page) { currentPage.value = page; load() }

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 120px; }
</style>