<template>
  <div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'标题',key:'title_wsh'},{label:'用户',key:'user_name_wsh'},{label:'状态',key:'status_label_wsh'}]" :data="tickets">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 'open'" class="btn btn-sm btn-info" @click="assignTicket(row.id_wsh)">分配</button>
          <button v-if="row.status_wsh === 'processing'" class="btn btn-sm btn-success" @click="resolve(row.id_wsh)">解决</button>
          <button v-if="row.status_wsh === 'resolved'" class="btn btn-sm btn-danger" @click="close(row.id_wsh)">关闭</button>
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
const tickets = ref([])

const ticketStatusMap = { open: { text: '待处理', cls: 'badge-warning' }, processing: { text: '处理中', cls: 'badge-info' }, resolved: { text: '已解决', cls: 'badge-success' }, closed: { text: '已关闭', cls: 'badge-secondary' } }

function enrichTicket(t) {
  const s = ticketStatusMap[t.status_wsh]
  t.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : t.status_wsh
  return t
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/tickets'); if (r.code === 200) tickets.value = (Array.isArray(r.data) ? r.data : (r.data.list || [])).map(enrichTicket) }
  catch (e) {}
})

async function assignTicket(id) { try { await authStore.apiPost(`/api/tickets/${id}/assign`, {}); appStore.addToast('已分配', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function resolve(id) { try { await authStore.apiPost(`/api/tickets/${id}/resolve`, {}); appStore.addToast('已解决', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function close(id) { try { await authStore.apiPost(`/api/tickets/${id}/close`, {}); appStore.addToast('已关闭', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
