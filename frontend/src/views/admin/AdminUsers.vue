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

    <AppDialog
      :visible="!!pendingToggle"
      :width="400"
      :title="'确认' + (pendingToggle?.newStatus === 0 ? '禁用' : '启用')"
      @close="pendingToggle = null"
    >
      <p style="margin:16px 0">
        确定{{ pendingToggle?.newStatus === 0 ? '禁用' : '启用' }}该用户吗？
      </p>
      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="pendingToggle = null">取消</button>
        <button
          :class="['btn', pendingToggle?.newStatus === 0 ? 'btn-danger' : 'btn-success']"
          type="button"
          @click="doToggle"
        >
          确认{{ pendingToggle?.newStatus === 0 ? '禁用' : '启用' }}
        </button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { useAppStore } from '@/stores/app'
import { getUsers as apiGetUsers, updateUserStatus } from '@/api/admin'
import { UserStatus, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
// pendingToggle 存着"要改谁、改成什么"，等弹窗确认了才真发请求
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

// 按搜索词拉用户，拉回来先补上人能看懂的角色和状态文字
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

// 后端给的是状态码和角色原串，这里转成表格能直接显示的文字
function enrichUser(user) {
  const enriched = enrichWithStatus(user, 'status_wsh', UserStatus)
  enriched.role_label_wsh = normalizeRoles(enriched.roles_wsh).join(', ') || '-'
  return enriched
}

// roles 有时候给数组，有时候给逗号串，两种都得能读
function normalizeRoles(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.map(cleanRole).filter(Boolean)
  return String(value).split(',').map(cleanRole).filter(Boolean)
}

// 去掉 ROLE_ 前缀再统一大写，显示成 ADMIN 这种
function cleanRole(role) {
  return String(role || '').replace('ROLE_', '').trim().toUpperCase()
}

// 只记下这次要改什么，先弹窗问一句，别点了就立刻生效
function confirmToggle(id, newStatus) {
  pendingToggle.value = { id, newStatus }
}

// 先把弹窗关掉再发请求，用户连点也不会重复提交
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
