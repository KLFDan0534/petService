<template>
  <div>
    <DataTable :columns="[
      { key: 'id_wsh', label: 'ID' },
      { key: 'name_wsh', label: '商家名称' },
      { key: 'phone_wsh', label: '联系人' },
      { key: 'status_label_wsh', label: '状态' },
    ]" :data="merchants">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 0" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 0" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
            <select class="form-control" style="width:100px;display:inline-block" :value="row.status_wsh" @change="changeStatus(row.id_wsh, Number($event.target.value))">
            <option :value="0">待审核</option>
            <option :value="1">已通过</option>
            <option :value="2">已拒绝</option>
          </select>
          <button class="btn btn-sm btn-danger" @click="deleteMerchant(row.id_wsh)">删除</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getMerchants, approveMerchant, rejectMerchant, updateMerchantStatus, deleteMerchant as deleteMerchantApi } from '@/api/merchant'
import { MerchantStatus, enrichWithStatus } from '@/constants/statusMaps'
import DataTable from '@/components/common/DataTable.vue'

const appStore = useAppStore()
const merchants = ref([])

function enrichMerchant(m) {
  return enrichWithStatus(m, 'status_wsh', MerchantStatus)
}

async function loadMerchants() {
  try { const r = await getMerchants(); if (r.code === 200) merchants.value = (Array.isArray(r.data) ? r.data : []).map(enrichMerchant) }
  catch (e) {}
}
onMounted(loadMerchants)

async function approve(id) {
  try { const r = await approveMerchant(id); if (r.code === 200) { appStore.addToast('已通过', 'success'); await loadMerchants() } }
  catch (e) { appStore.addToast('操作失败', 'error') }
}

async function reject(id) {
  try { const r = await rejectMerchant(id); if (r.code === 200) { appStore.addToast('已拒绝', 'success'); await loadMerchants() } }
  catch (e) { appStore.addToast('操作失败', 'error') }
}

async function changeStatus(id, status) {
  const labels = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  if (!confirm(`确定将状态变更为"${labels[status]}"？`)) return
  try {
    const r = await updateMerchantStatus(id, status)
    if (r.code === 200) { appStore.addToast(`状态已变更为 ${labels[status]}`, 'success'); await loadMerchants() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function deleteMerchant(id) {
  if (!confirm('确定删除该商家？')) return
  try { await deleteMerchantApi(id); appStore.addToast('已删除', 'success'); await loadMerchants() }
  catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
