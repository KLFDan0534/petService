<template>
  <div>
    <DataTable :columns="[{key:'id_wsh',label:'ID'},{key:'user_id_wsh',label:'用户'},{key:'amount_wsh',label:'金额'},{key:'bank_card_wsh',label:'提现方式'},{key:'status_label_wsh',label:'状态'}]" :data="withdrawals">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
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
const withdrawals = ref([])

const withdrawalStatusMap = { pending: { text: '待审核', cls: 'badge-warning' }, approved: { text: '已通过', cls: 'badge-success' }, rejected: { text: '已拒绝', cls: 'badge-danger' } }

function enrichWithdrawal(w) {
  const s = withdrawalStatusMap[w.status_wsh]
  w.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : w.status_wsh
  return w
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/withdrawals'); if (r.code === 200) withdrawals.value = (r.data.list || []).map(enrichWithdrawal) }
  catch (e) {}
})

async function approve(id) { try { await authStore.apiPost(`/api/withdrawals/${id}/approve`, {}); appStore.addToast('已通过', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await authStore.apiPost(`/api/withdrawals/${id}/reject`, {}); appStore.addToast('已拒绝', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
