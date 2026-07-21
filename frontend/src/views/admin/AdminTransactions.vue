<template>
  <div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'金额',key:'amount_wsh'},{label:'类型',key:'type_wsh'},{label:'状态',key:'status_label_wsh'},{label:'用户ID',key:'user_id_wsh'},{label:'创建时间',key:'created_at_wsh'}]" :data="transactions" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTransactions } from '@/api/wallet'
import DataTable from '@/components/common/DataTable.vue'
import { TransactionStatus, enrichWithStatus } from '@/constants/statusMaps'

const transactions = ref([])

onMounted(async () => {
  try { const r = await getTransactions(); if (r.code === 200) transactions.value = (Array.isArray(r.data) ? r.data : []).map(t => enrichWithStatus(t, 'status_wsh', TransactionStatus)) }
  catch (e) {}
})
</script>