<template>
  <div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'投诉人',key:'owner_name_wsh'},{label:'标题',key:'title_wsh'},{label:'状态',key:'status_label_wsh'},{label:'时间',key:'created_at_wsh'}]" :data="complaints">
      <template #default="{ row }">
        <div style="display:flex;gap:4px">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="resolve(row.id_wsh)">处理</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="rejectComplaint(row.id_wsh)">驳回</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const complaints = ref([])

const complaintStatusMap = { pending: { text: '待处理', cls: 'badge-warning' }, resolved: { text: '已处理', cls: 'badge-success' }, rejected: { text: '已驳回', cls: 'badge-danger' } }

function enrichComplaint(c) {
  const s = complaintStatusMap[c.status_wsh]
  c.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : c.status_wsh
  return c
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/complaints/all'); if (r.code === 200) complaints.value = (r.data.list || r.data).map(enrichComplaint) }
  catch (e) {}
})

async function resolve(id) {
  try { const r = await authStore.apiPost(`/api/complaints/${id}/resolve`, {}); if (r.code === 200) { appStore.addToast('已处理', 'success'); location.reload() } }
  catch (e) { appStore.addToast('操作失败', 'error') }
}
async function rejectComplaint(id) {
  try { const r = await authStore.apiPost(`/api/complaints/${id}/reject`, {}); if (r.code === 200) { appStore.addToast('已驳回', 'success'); location.reload() } }
  catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
