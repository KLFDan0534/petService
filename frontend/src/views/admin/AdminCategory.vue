<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="addCategory()">+ 新增分类</button>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'分类名称',key:'name_wsh'},{label:'父分类ID',key:'parent_id_wsh'},{label:'排序',key:'sort_order_wsh'}]" :data="categories">
      <template #default="{ row }">
        <button class="btn btn-sm btn-outline" @click="editCategory(row)">编辑</button>
        <button class="btn btn-sm btn-danger" @click="deleteCategory(row.id_wsh)">删除</button>
      </template>
    </DataTable>

    <AppDialog :visible="showForm" :title="editingCategory ? '编辑分类' : '新增分类'" @close="showForm = false">
      <form id="categoryForm" @submit.prevent="saveCategory">
        <div class="form-group"><label>分类名称</label><input v-model="form.name_wsh" required></div>
        <div class="form-group">
          <label>父分类</label>
          <select v-model.number="form.parent_id_wsh">
            <option :value="0">一级分类</option>
            <option v-for="c in topCategories" :key="c.id_wsh" :value="c.id_wsh">{{ c.name_wsh }}</option>
          </select>
        </div>
        <div class="form-group"><label>排序</label><input v-model.number="form.sort_order_wsh" type="number" min="0"></div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
        <button type="submit" form="categoryForm" class="btn btn-primary btn-sm">{{ editingCategory ? '保存' : '创建' }}</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getCategories, createCategory, updateCategory, deleteCategory as apiDeleteCategory } from '@/api/category'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
const appStore = useAppStore()
const categories = ref([])
const showForm = ref(false)
const editingCategory = ref(null)
const form = reactive({ name_wsh: '', parent_id_wsh: 0, sort_order_wsh: 0 })

const topCategories = computed(() => categories.value.filter(c => c.parent_id_wsh === 0))

onMounted(loadCategories)

async function loadCategories() {
  try { const r = await getCategories(); if (r.code === 200) categories.value = r.data }
  catch (e) {}
}

function addCategory() {
  editingCategory.value = null
  Object.assign(form, { name_wsh: '', parent_id_wsh: 0, sort_order_wsh: 0 })
  showForm.value = true
}

function editCategory(cat) {
  editingCategory.value = cat
  Object.assign(form, { name_wsh: cat.name_wsh, parent_id_wsh: cat.parent_id_wsh || 0, sort_order_wsh: cat.sort_order_wsh || 0 })
  showForm.value = true
}

async function saveCategory() {
  try {
    const payload = { name_wsh: form.name_wsh, parent_id_wsh: form.parent_id_wsh, sort_order_wsh: form.sort_order_wsh }
    if (editingCategory.value) {
      await updateCategory(editingCategory.value.id_wsh, payload)
      appStore.addToast('更新成功', 'success')
    } else {
      await createCategory(payload)
      appStore.addToast('创建成功', 'success')
    }
    showForm.value = false; editingCategory.value = null; loadCategories()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function deleteCategory(id) {
  if (!confirm('确定删除？')) return
  try { await apiDeleteCategory(id); appStore.addToast('删除成功', 'success'); loadCategories() }
  catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
