<template>
  <div>
    <section class="toolbar-card">
      <div>
        <h2>提现审核</h2>
        <p>申请时冻结余额，通过后等待线下打款，完成打款后才正式扣减钱包余额。</p>
      </div>
      <button class="btn btn-outline btn-sm" type="button" @click="loadWithdrawals">刷新</button>
    </section>

    <DataTable
      :columns="[
        { key: 'id_wsh', label: 'ID' },
        { key: 'user_id_wsh', label: '用户' },
        { key: 'amount_wsh', label: '金额' },
        { key: 'bank_card_wsh', label: '提现方式' },
        { key: 'status_label_wsh', label: '状态' }
      ]"
      :data="withdrawals"
    >
      <template #default="{ row }">
        <div class="actions">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" type="button" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" type="button" @click="reject(row.id_wsh)">拒绝</button>
          <button v-if="row.status_wsh === 'approved'" class="btn btn-sm btn-primary" type="button" @click="complete(row.id_wsh)">完成打款</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getWithdrawals, approveWithdrawal, rejectWithdrawal, completeWithdrawal } from '@/api/wallet'
import { WithdrawalStatus, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
const withdrawals = ref([])

function enrichWithdrawal(w) {
  return enrichWithStatus(w, 'status_wsh', WithdrawalStatus)
}

async function loadWithdrawals() {
  try {
    const r = await getWithdrawals()
    if (r.code === 200) withdrawals.value = (r.data.list || []).map(enrichWithdrawal)
  } catch (e) {
    appStore.addToast(e?.message || '提现记录加载失败', 'error')
  }
}

onMounted(loadWithdrawals)

async function approve(id) {
  try {
    await approveWithdrawal(id)
    appStore.addToast('已通过', 'success')
    await loadWithdrawals()
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
  }
}

async function reject(id) {
  try {
    await rejectWithdrawal(id)
    appStore.addToast('已拒绝', 'success')
    await loadWithdrawals()
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
  }
}

async function complete(id) {
  try {
    await completeWithdrawal(id)
    appStore.addToast('提现已完成', 'success')
    await loadWithdrawals()
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
  }
}
</script>

<style scoped>
.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
}
.toolbar-card h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.toolbar-card p {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
