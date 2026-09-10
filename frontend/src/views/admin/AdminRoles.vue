<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="openCreate">+ 创建角色</button>
    <DataTable :columns="columns" :data="roles">
      <template #default="{ row }">
        <button v-if="!isManagedCustomerServiceRole(row)" class="btn btn-sm btn-outline" @click="editRole(row)">编辑</button>
        <button v-if="!isManagedCustomerServiceRole(row)" class="btn btn-sm btn-danger" @click="deleteRole(row.id_wsh)">删除</button>
      </template>
    </DataTable>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="closeForm">
      <div class="modal">
        <h2>{{ editingRole ? '编辑角色' : '创建角色' }}</h2>
        <form @submit.prevent="saveRole">
          <div class="form-group"><label>角色名</label><input v-model="form.name_wsh" required></div>
          <div class="form-group"><label>编码</label><input v-model="form.code_wsh" required :disabled="!!editingRole"></div>
          <div class="form-group"><label>描述</label><input v-model="form.description_wsh"></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="closeForm">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">{{ editingRole ? '保存' : '创建' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getRoles, createRole, updateRole, deleteRole as apiDeleteRole } from '@/api/admin'


const appStore = useAppStore()
const roles = ref([])
const showForm = ref(false)
const editingRole = ref(null)
const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '角色名', key: 'name_wsh' },
  { label: 'Code', key: 'code_wsh' },
  { label: '描述', key: 'description_wsh' },
  { label: '用户数', key: 'user_count_wsh' },
]
const form = reactive({ name_wsh: '', code_wsh: '', description_wsh: '' })

onMounted(loadRoles)

async function loadRoles() {
  try { const r = await getRoles(); if (r.code === 200) roles.value = r.data }
  catch (e) {}
}

function resetForm() {
  Object.assign(form, { name_wsh: '', code_wsh: '', description_wsh: '' })
}

function openCreate() {
  editingRole.value = null
  resetForm()
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingRole.value = null
  resetForm()
}

function editRole(role) {
  editingRole.value = role
  Object.assign(form, { name_wsh: role.name_wsh, code_wsh: role.code_wsh, description_wsh: role.description_wsh })
  showForm.value = true
}

function isManagedCustomerServiceRole(role) {
  return String(role?.code_wsh || '').trim().toUpperCase() === 'CUSTOMER_SERVICE'
}

async function saveRole() {
  try {
    if (!editingRole.value && isManagedCustomerServiceRole(form)) {
      appStore.addToast('客服角色由商家申请流程自动管理', 'error')
      return
    }
    if (editingRole.value) {
      await updateRole(editingRole.value.id_wsh, { name_wsh: form.name_wsh, description_wsh: form.description_wsh })
      appStore.addToast('更新成功', 'success')
    } else {
      await createRole({ name_wsh: form.name_wsh, code_wsh: form.code_wsh, description_wsh: form.description_wsh })
      appStore.addToast('创建成功', 'success')
    }
    closeForm()
    loadRoles()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function deleteRole(id) {
  if (!confirm('确定删除？')) return
  try { await apiDeleteRole(id); appStore.addToast('删除成功', 'success'); loadRoles() }
  catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
