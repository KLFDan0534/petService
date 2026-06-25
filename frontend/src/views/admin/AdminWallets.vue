<template>
  <div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'余额',key:'balance_wsh'},{label:'用户ID',key:'user_id_wsh'},{label:'创建时间',key:'created_at_wsh'},{label:'更新时间',key:'updated_at_wsh'}]" :data="wallets" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const wallets = ref([])

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/wallet'); if (r.code === 200) wallets.value = r.data }
  catch (e) {}
})
</script>