<template>
  <div>
    <div class="admin-filters">
      <select v-model="filters.status_wsh" class="form-control" @change="applyFilters">
        <option value="">全部状态</option>
        <option value="pending">待审批</option>
        <option value="approved">已通过</option>
        <option value="rejected">已拒绝</option>
      </select>
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">筛选</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'寄养员',key:'keeper_name_wsh'},
      {label:'商家',key:'merchant_name_wsh'},
      {label:'请假原因',key:'reason_wsh'},
      {label:'开始时间',key:'start_date_wsh'},
      {label:'结束时间',key:'end_date_wsh'},
      {label:'状态',key:'status_label_wsh'}
    ]" :data="leaves">
      <template #default="{ row }">
        <div style="display:flex;gap:4px">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="doApprove(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="openReject(row)">拒绝</button>
        </div>
      </template>
    </DataTable>

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>

    <div v-if="rejectTarget" class="modal-overlay" @mousedown.self="rejectTarget = null">
      <div class="modal" style="max-width:440px">
        <h3>驳回请假申请</h3>
        <div class="form-group" style="margin-top:16px">
          <label>驳回原因</label>
          <textarea v-model="rejectReason" rows="3" placeholder="请输入驳回原因"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" type="button" @click="rejectTarget = null">取消</button>
          <button class="btn btn-danger btn-sm" type="button" @click="doReject">确认驳回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getAdminLeaves, approveLeave, rejectLeave } from '@/api/attendance'
import { LeaveApprovalStatus, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
const leaves = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ status_wsh: '' })
const rejectTarget = ref(null)
const rejectReason = ref('')
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrich(l) {
  const e = enrichWithStatus(l, 'status_wsh', LeaveApprovalStatus)
  e.keeper_name_wsh = e.keeper_name_wsh || '-'
  e.merchant_name_wsh = e.merchant_name_wsh || '-'
  return e
}

function buildParams() {
  const params = { page: currentPage.value, size: size.value }
  if (filters.status_wsh) params.status_wsh = filters.status_wsh
  return params
}

async function load() {
  try {
    const r = await getAdminLeaves(buildParams())
    if (r.code === 200) {
      leaves.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { filters.status_wsh = ''; applyFilters() }
function changePage(page) { currentPage.value = page; load() }

async function doApprove(id) {
  try {
    await approveLeave(id)
    appStore.addToast('已通过', 'success')
    await load()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

function openReject(row) { rejectTarget.value = row; rejectReason.value = '' }

async function doReject() {
  try {
    await rejectLeave(rejectTarget.value.id_wsh, rejectReason.value.trim())
    appStore.addToast('已驳回', 'success')
    rejectTarget.value = null
    await load()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 140px; }
</style>