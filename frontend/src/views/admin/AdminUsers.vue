<template>
  <div>
    <div style="margin-bottom:16px;display:flex;gap:12px">
      <input v-model="searchQuery" placeholder="搜索用户..." style="max-width:300px" @keyup.enter="loadUsers">
    </div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'用户名',key:'username_wsh'},{label:'昵称',key:'nickname_wsh'},{label:'角色',key:'roles_wsh'},{label:'状态',key:'status_label_wsh'},]" :data="users" :renderHtml="true">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button :class="['btn', 'btn-sm', row.status_wsh === 1 ? 'btn-danger' : 'btn-success']"
            @click="confirmToggle(row.id_wsh, row.status_wsh === 1 ? 0 : 1)">
            {{ row.status_wsh === 1 ? '封禁' : '启用' }}
          </button>
        </div>
      </template>
    </DataTable>

    <div v-if="pendingToggle" class="modal-overlay" @mousedown.self="pendingToggle = null">
      <div class="modal" style="max-width:400px">
        <h3>确认{{ pendingToggle.newStatus === 0 ? '封禁' : '启用' }}</h3>
        <p style="margin:16px 0">确定{{ pendingToggle.newStatus === 0 ? '封禁' : '启用' }}该用户吗？</p>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="pendingToggle = null">取消</button>
          <button class="btn" :class="pendingToggle.newStatus === 0 ? 'btn-danger' : 'btn-success'" @click="doToggle">确认{{ pendingToggle.newStatus === 0 ? '封禁' : '启用' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const users = ref([])
const searchQuery = ref('')

onMounted(loadUsers)

async function loadUsers() {
  try {
    const r = await authStore.apiGet(`/api/users?page=1&size=100${searchQuery.value ? `&q=${searchQuery.value}` : ''}`)
    if (r.code === 200) users.value = Array.isArray(r.data.list || r.data) ? (r.data.list || r.data).map(enrichUser) : []
  } catch (e) {}
}

const statusMap = { 0: { text: '封禁', cls: 'badge-danger' }, 1: { text: '正常', cls: 'badge-success' } }

function enrichUser(u) {
  const s = statusMap[u.status_wsh]
  u.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : `<span class="badge badge-disabled">${u.status_wsh}</span>`
  return u
}

const pendingToggle = ref(null)

function confirmToggle(id, newStatus) {
  pendingToggle.value = { id, newStatus }
}

async function doToggle() {
  if (!pendingToggle.value) return
  const { id, newStatus } = pendingToggle.value
  pendingToggle.value = null
  try {
    const r = await authStore.apiPut(`/api/users/${id}/status`, { status_wsh: newStatus })
    if (r.code === 200) { appStore.addToast('操作成功', 'success'); loadUsers() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
