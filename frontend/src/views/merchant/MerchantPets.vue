<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>宠物管理</h2>
      <button class="btn btn-secondary btn-sm">发布宠物</button>
    </div>

    <DataTable
      :columns="[
        { label: '名称', key: 'name_wsh' },
        { label: '种类', key: 'type_wsh' },
        { label: '年龄', key: 'age_wsh' },
        { label: '体重', key: 'weight_wsh' }
      ]"
      :data="pets"
    >
      <template #default>
        <div style="display:flex;gap:8px">
          <button class="btn btn-sm btn-secondary">编辑</button>
          <button class="btn btn-sm btn-danger">下架</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import { getMerchantPets } from '@/api/pet'

// 商家名下的宠物，模板里直接丢给表格显示
const pets = ref([])

// 接口有时返回 {list:[...]} 有时直接给数组，这里两种都认
onMounted(async () => {
  try {
    const r = await getMerchantPets()
    if (r.code === 200) pets.value = r.data?.list || r.data || []
  } catch (e) {}
})
</script>
