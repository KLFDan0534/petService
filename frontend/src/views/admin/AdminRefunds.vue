<template>
  <div>
    <DataTable :columns="[{ label: 'ID', key: 'id_wsh' }, { label: '金额', key: 'amount_wsh' }, { label: '原因', key: 'reason_wsh' }, { label: '状态', key: 'status_label_wsh' }]" :data="refunds">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
          <button v-if="row.status_wsh === 'approved'" class="btn btn-sm btn-primary" @click="completeRefund(row.id_wsh)">完成退款</button>

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
const refunds = ref([])

const refundStatusMap = { pending: { text: '待审核', cls: 'badge-info' }, approved: { text: '已通过(待退款)', cls: 'badge-primary' }, completed: { text: '已完成', cls: 'badge-success' }, rejected: { text: '已拒绝', cls: 'badge-danger' } }

function enrichRefund(r) {
  const s = refundStatusMap[r.status_wsh]
  r.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : r.status_wsh
  return r
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/refunds/all'); if (r.code === 200) refunds.value = (Array.isArray(r.data) ? r.data : []).map(enrichRefund) }
  catch (e) {}
})

async function approve(id) { try { await authStore.apiPost(`/api/refunds/${id}/approve`, {}); appStore.addToast('已通过', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await authStore.apiPost(`/api/refunds/${id}/reject`, {}); appStore.addToast('已拒绝', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function completeRefund(id) { try { await authStore.apiPost(`/api/refunds/${id}/complete`, {}); appStore.addToast('退款完成', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
