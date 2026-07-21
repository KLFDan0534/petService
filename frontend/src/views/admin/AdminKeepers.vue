<template>
  <div>
    <div style="display:flex;gap:8px;margin-bottom:16px">
      <button v-for="t in tabs" :key="t.key" :class="['btn', activeTab === t.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        @click="activeTab = t.key; loadKeepers()">{{ t.label }}</button>
    </div>
    <DataTable :columns="[
      { key: 'id_wsh', label: 'ID' },
      { key: 'name_wsh', label: '姓名' },
      { key: 'status_label_wsh', label: '状态' },
    ]" :data="filteredKeepers">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap;align-items:center">
          <span v-if="row.status_wsh === 0" class="badge badge-warning">待审核</span>
          <span v-else-if="row.status_wsh === 2" class="badge badge-danger">已拒绝</span>
          <span v-else class="badge badge-success">已通过</span>
          <select class="form-control" style="width:110px;margin-left:4px" @change="changeStatus(row, $event.target.value)">
            <option value="">修改状态...</option>
            <option value="0">待审核</option>
            <option value="1">已通过</option>
            <option value="2">已拒绝</option>
          </select>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getKeepers, approveKeeper, rejectKeeper } from '@/api/keeper'
import DataTable from '@/components/common/DataTable.vue'
import request from '@/utils/request'

const appStore = useAppStore()
const keepers = ref([])
const activeTab = ref('all')
const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待审核' },
  { key: 'approved', label: '已通过' },
  { key: 'rejected', label: '已拒绝' },
]

const filteredKeepers = computed(() => {
  if (activeTab.value === 'all') return keepers.value
  if (activeTab.value === 'pending') return keepers.value.filter(k => k.status_wsh === 0)
  if (activeTab.value === 'approved') return keepers.value.filter(k => k.status_wsh === 1 || k.status_wsh === 3 || k.status_wsh === 4)
  return keepers.value.filter(k => k.status_wsh === 2)
})

onMounted(loadKeepers)

function enrichKeeper(k) {
  const st = k.status_wsh
  if (st === 0) k.status_label_wsh = '待审核'
  else if (st === 2) k.status_label_wsh = '已拒绝'
  else k.status_label_wsh = '已通过'
  return k
}

async function loadKeepers() {
  try {
    const r = await getKeepers()
    if (r.code === 200) {
      const raw = r.data.list || r.data
      keepers.value = Array.isArray(raw) ? raw.map(enrichKeeper) : []
    }
  } catch (e) {}
}

const statusMap = { 0: '待审核', 1: '已通过', 2: '已拒绝' }

async function changeStatus(row, newStatus) {
  if (!newStatus) return
  try {
    const target = Number(newStatus)
    let r
    if (target === 1) {
      r = await approveKeeper(row.id_wsh)
    } else if (target === 2) {
      r = await rejectKeeper(row.id_wsh)
    } else {
      r = await request.put(`/api/keepers/${row.id_wsh}`, { status_wsh: target })
    }
    if (r.code === 200) { appStore.addToast(`状态已改为${statusMap[target]}`, 'success'); loadKeepers() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
