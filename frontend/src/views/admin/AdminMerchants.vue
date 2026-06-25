<template>
  <div>
    <DataTable :columns="[
      { key: 'id_wsh', label: 'ID' },
      { key: 'name_wsh', label: '商家名称' },
      { key: 'phone_wsh', label: '联系人' },
      { key: 'status_label_wsh', label: '状态' },
    ]" :data="merchants">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button v-if="row.status_wsh === 0" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 0" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
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
const merchants = ref([])

const merchantStatusMap = { 0: { text: '待审核', cls: 'badge-warning' }, 1: { text: '已通过', cls: 'badge-success' }, 2: { text: '已拒绝', cls: 'badge-danger' } }

function enrichMerchant(m) {
  const s = merchantStatusMap[m.status_wsh]
  m.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : m.status_wsh
  return m
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/merchants'); if (r.code === 200) merchants.value = (Array.isArray(r.data) ? r.data : []).map(enrichMerchant) }
  catch (e) {}
})

async function approve(id) {
  try { const r = await authStore.apiPost(`/api/merchants/${id}/approve`, {}); if (r.code === 200) { appStore.addToast('已通过', 'success'); location.reload() } }
  catch (e) { appStore.addToast('操作失败', 'error') }
}

async function reject(id) {
  try { const r = await authStore.apiPost(`/api/merchants/${id}/reject`, {}); if (r.code === 200) { appStore.addToast('已拒绝', 'success'); location.reload() } }
  catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
