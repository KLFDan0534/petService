<template>
  <div class="admin-users-page">
    <div class="toolbar">
      <input v-model="searchQuery" placeholder="搜索用户..." style="max-width:300px" @keyup.enter="loadUsers">
      <button class="btn btn-outline btn-sm" type="button" @click="loadUsers">搜索</button>
    </div>

    <DataTable :columns="columns" :data="users">
      <template #default="{ row }">
        <button
          :class="['btn', 'btn-sm', row.status_wsh === 1 ? 'btn-danger' : 'btn-success']"
          type="button"
          @click="confirmToggle(row.id_wsh, row.status_wsh === 1 ? 0 : 1)"
        >
          {{ row.status_wsh === 1 ? '禁用' : '启用' }}
        </button>
      </template>
    </DataTable>

    <div v-if="pendingToggle" class="modal-overlay" @mousedown.self="pendingToggle = null">
      <div class="modal" style="max-width:400px">
        <h3>确认{{ pendingToggle.newStatus === 0 ? '禁用' : '启用' }}</h3>
        <p style="margin:16px 0">
          确定{{ pendingToggle.newStatus === 0 ? '禁用' : '启用' }}该用户吗？
        </p>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" type="button" @click="pendingToggle = null">取消</button>
          <button
            :class="['btn', pendingToggle.newStatus === 0 ? 'btn-danger' : 'btn-success']"
            type="button"
            @click="doToggle"
          >
            确认{{ pendingToggle.newStatus === 0 ? '禁用' : '启用' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import { useAppStore } from '@/stores/app'
import { getUsers as apiGetUsers, updateUserStatus } from '@/api/admin'
import { UserStatus, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
const users = ref([])
const searchQuery = ref('')
const pendingToggle = ref(null)

const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '用户名', key: 'username_wsh' },
  { label: '昵称', key: 'nickname_wsh' },
  { label: '角色', key: 'role_label_wsh' },
  { label: '状态', key: 'status_label_wsh' },
]

async function loadUsers() {
  try {
    const response = await apiGetUsers({ page: 1, size: 100, q: searchQuery.value || undefined })
    if (response.code === 200) {
      const list = response.data?.list || response.data || []
      users.value = Array.isArray(list) ? list.map(enrichUser) : []
    }
  } catch {
    appStore.addToast('用户列表加载失败', 'error')
  }
}

function enrichUser(user) {
  const enriched = enrichWithStatus(user, 'status_wsh', UserStatus)
  enriched.role_label_wsh = normalizeRoles(enriched.roles_wsh).join(', ') || '-'
  return enriched
}

function normalizeRoles(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.map(cleanRole).filter(Boolean)
  return String(value).split(',').map(cleanRole).filter(Boolean)
}

function cleanRole(role) {
  return String(role || '').replace('ROLE_', '').trim().toUpperCase()
}

function confirmToggle(id, newStatus) {
  pendingToggle.value = { id, newStatus }
}

async function doToggle() {
  if (!pendingToggle.value) return
  const { id, newStatus } = pendingToggle.value
  pendingToggle.value = null
  try {
    const response = await updateUserStatus(id, newStatus)
    if (response.code === 200) {
      appStore.addToast('操作成功', 'success')
      await loadUsers()
    }
  } catch {
    appStore.addToast('操作失败', 'error')
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-users-page {
  display: grid;
  gap: 16px;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
