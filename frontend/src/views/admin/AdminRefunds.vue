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
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getRefunds, approveRefund, rejectRefund, completeRefund as apiCompleteRefund } from '@/api/refund'
import { RefundStatus, enrichWithStatus } from '@/constants/statusMaps'


const appStore = useAppStore()
// 退款单只读展示，能做的动作只有通过、拒绝、完成这三个
const refunds = ref([])

function enrichRefund(r) {
  return enrichWithStatus(r, 'status_wsh', RefundStatus)
}

async function loadRefunds() {
  try { const r = await getRefunds(); if (r.code === 200) refunds.value = (Array.isArray(r.data) ? r.data : []).map(enrichRefund) }
  catch (e) {}
}
onMounted(loadRefunds)

// 退款分两步走：先审通过，再点「完成退款」才真的把钱退回去，中间那步别省
async function approve(id) { try { await approveRefund(id); appStore.addToast('已通过', 'success'); await loadRefunds() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await rejectRefund(id); appStore.addToast('已拒绝', 'success'); await loadRefunds() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function completeRefund(id) { try { await apiCompleteRefund(id); appStore.addToast('退款完成', 'success'); await loadRefunds() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
