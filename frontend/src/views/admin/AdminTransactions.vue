<template>
  <div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'金额',key:'amount_wsh'},{label:'类型',key:'type_wsh'},{label:'状态',key:'status_label_wsh'},{label:'用户ID',key:'user_id_wsh'},{label:'创建时间',key:'created_at_wsh'}]" :data="transactions" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const transactions = ref([])

const txStatusMap = { pending: { text: '待处理', cls: 'badge-warning' }, completed: { text: '已完成', cls: 'badge-success' }, failed: { text: '失败', cls: 'badge-danger' } }

function enrichTx(t) {
  const s = txStatusMap[t.status_wsh]
  t.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : t.status_wsh
  return t
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/transactions'); if (r.code === 200) transactions.value = (Array.isArray(r.data) ? r.data : []).map(enrichTx) }
  catch (e) {}
})
</script>