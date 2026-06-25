<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="showForm = true">+ 创建角色</button>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'角色名',key:'name_wsh'},{label:'描述',key:'description_wsh'},{label:'用户数',key:'user_count_wsh'}]" :data="roles">
      <template #default="{ row }">
        <button class="btn btn-sm btn-outline" @click="editRole(row)">编辑</button>
        <button class="btn btn-sm btn-danger" @click="deleteRole(row.id_wsh)">删除</button>
      </template>
    </DataTable>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>{{ editingRole ? '编辑角色' : '创建角色' }}</h2>
        <form @submit.prevent="saveRole">
          <div class="form-group"><label>角色名</label><input v-model="form.name_wsh" required></div>
          <div class="form-group"><label>描述</label><input v-model="form.description_wsh"></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">{{ editingRole ? '保存' : '创建' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const roles = ref([])
const showForm = ref(false)
const editingRole = ref(null)
const form = reactive({ name_wsh: '', description_wsh: '' })

onMounted(loadRoles)

async function loadRoles() {
  try { const r = await authStore.apiGet('/api/roles'); if (r.code === 200) roles.value = r.data }
  catch (e) {}
}

function editRole(role) {
  editingRole.value = role
  Object.assign(form, { name_wsh: role.name_wsh, description_wsh: role.description_wsh })
  showForm.value = true
}

async function saveRole() {
  try {
    if (editingRole.value) {
      await authStore.apiPut(`/api/roles/${editingRole.value.id_wsh}`, { name_wsh: form.name_wsh, description_wsh: form.description_wsh })
      appStore.addToast('更新成功', 'success')
    } else {
      await authStore.apiPost('/api/roles', { name_wsh: form.name_wsh, description_wsh: form.description_wsh })
      appStore.addToast('创建成功', 'success')
    }
    showForm.value = false; editingRole.value = null; loadRoles()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function deleteRole(id) {
  if (!confirm('确定删除？')) return
  try { await authStore.apiDelete(`/api/roles/${id}`); appStore.addToast('删除成功', 'success'); loadRoles() }
  catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
