<template>
  <div>
    <div style="display:flex;gap:8px;margin-bottom:16px">
      <button v-for="t in tabs" :key="t.key" :class="['btn', activeTab === t.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        @click="activeTab = t.key; loadKeepers()">{{ t.label }}</button>
    </div>
    <DataTable :columns="[
      { key: 'id_wsh', label: 'ID' },
      { key: 'name_wsh', label: '姓名' },
      { key: 'review_status_label_wsh', label: '审核状态' },
      { key: 'online_status_label_wsh', label: '在线状态' },
    ]" :data="filteredKeepers">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 0" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 0" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
          <select v-model="row.status_wsh" class="form-control" style="width:100px" @change="updateOnlineStatus(row)">
            <option :value="1">在线</option>
            <option :value="3">离线</option>
            <option :value="4">忙碌</option>
          </select>
          <span v-if="row.status_wsh !== 0" :class="['badge', row.status_wsh === 1 ? 'badge-success' : 'badge-danger']">
            {{ row.status_wsh === 1 ? '已通过' : '已拒绝' }}
          </span>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
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
  if (activeTab.value === 'approved') return keepers.value.filter(k => k.status_wsh === 1)
  return keepers.value.filter(k => k.status_wsh === 2)
})

onMounted(loadKeepers)

const reviewStatusMap = { 0: { text: '待审核', cls: 'badge-warning' }, 1: { text: '已通过', cls: 'badge-success' }, 2: { text: '已拒绝', cls: 'badge-danger' } }
const onlineStatusMap = { 1: { text: '在线', cls: 'badge-success' }, 3: { text: '离线', cls: 'badge-secondary' }, 4: { text: '忙碌', cls: 'badge-warning' } }

function enrichKeeper(k) {
  const r = reviewStatusMap[k.status_wsh]
  k.review_status_label_wsh = r ? `<span class="badge ${r.cls}">${r.text}</span>` : k.status_wsh
  const o = onlineStatusMap[k.status_wsh]
  k.online_status_label_wsh = o ? `<span class="badge ${o.cls}">${o.text}</span>` : '<span class="badge badge-disabled">-</span>'
  return k
}

async function loadKeepers() {
  try {
    const url = activeTab.value === 'pending' ? '/api/keepers/pending' : '/api/keepers'
    const r = await authStore.apiGet(url)
    if (r.code === 200) {
      const raw = r.data.list || r.data
      keepers.value = Array.isArray(raw) ? raw.map(enrichKeeper) : []
    }
  } catch (e) {}
}

async function approve(id) {
  try {
    const r = await authStore.apiPost(`/api/keepers/${id}/approve`, {})
    if (r.code === 200) { appStore.addToast('已通过', 'success'); loadKeepers() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function reject(id) {
  try {
    const r = await authStore.apiPost(`/api/keepers/${id}/reject`, {})
    if (r.code === 200) { appStore.addToast('已拒绝', 'success'); loadKeepers() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function updateOnlineStatus(keeper) {
  try {
    const statusMap = { 1: 'active', 3: 'offline', 4: 'busy' }
    const status = statusMap[keeper.status_wsh] || String(keeper.status_wsh)
    const r = await authStore.apiPatch(`/api/keepers/${keeper.id_wsh}/online-status`, { status })
    if (r.code === 200) appStore.addToast('状态已更新', 'success')
  } catch (e) { appStore.addToast('更新失败', 'error') }
}
</script>
