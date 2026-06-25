<template>
  <div>
    <h2 style="margin-bottom:16px">订单管理</h2>
    <DataTable :columns="[{label:'订单号',key:'orderNo'},{label:'用户',key:'username'},{label:'金额',key:'amount'},{label:'状态',key:'status'},{label:'时间',key:'createTime'}]" :data="orders">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button class="btn btn-sm btn-primary">处理</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import request from '@/utils/request'

const orders = ref([])

onMounted(async () => {
  try {
    const r = await request.get('/orders/merchant?page=1&size=100')
    if (r.data.code === 200) orders.value = r.data.data?.list || r.data.data || []
  } catch (e) {}
})
</script>
