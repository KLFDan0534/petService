<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom: 16px" @click="addCategory()">+ 新增服务分类</button>

    <div v-if="loading" class="loading"><LoadingSpinner /></div>
    <div v-else-if="categories.length === 0" class="empty-state">暂无服务分类，请先创建一级分类。</div>
    <div v-else class="tree-list">
      <template v-for="parent in treeData" :key="parent.id_wsh">
        <div class="tree-row" :class="{ 'has-children': parent.children.length }" @click="toggleExpand(parent.id_wsh)">
          <span class="tree-toggle" :class="{ expanded: expandedIds.has(parent.id_wsh) }">▶</span>
          <span class="tree-name">{{ parent.name_wsh }}</span>
          <span class="tree-code">{{ parent.code_wsh }}</span>
          <span class="tree-status" :class="{ active: parent.status_wsh === 1 }">
            {{ parent.status_wsh === 1 ? '启用' : '停用' }}
          </span>
          <div class="tree-actions" @click.stop>
            <button class="btn btn-sm btn-outline" @click="addSubCategory(parent)">+ 子分类</button>
            <button class="btn btn-sm btn-outline" @click="editCategory(parent)">编辑</button>
            <button class="btn btn-sm btn-outline" @click="toggleStatus(parent)">
              {{ parent.status_wsh === 1 ? '停用' : '启用' }}
            </button>
            <button class="btn btn-sm btn-danger" @click="deleteCategory(parent)">删除</button>
          </div>
        </div>
        <div v-show="expandedIds.has(parent.id_wsh)" class="tree-children">
          <div v-for="child in parent.children" :key="child.id_wsh" class="tree-row child-row">
            <span class="tree-name">{{ child.name_wsh }}</span>
            <span class="tree-code">{{ child.code_wsh }}</span>
            <span class="tree-status" :class="{ active: child.status_wsh === 1 }">
              {{ child.status_wsh === 1 ? '启用' : '停用' }}
            </span>
            <div class="tree-actions">
              <button class="btn btn-sm btn-outline" @click="editCategory(child)">编辑</button>
              <button class="btn btn-sm btn-outline" @click="toggleStatus(child)">
                {{ child.status_wsh === 1 ? '停用' : '启用' }}
              </button>
              <button class="btn btn-sm btn-danger" @click="deleteCategory(child)">删除</button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <AppDialog :visible="showForm" :title="editingCategory ? '编辑分类' : (form.parent_id_wsh ? '新增子分类' : '新增一级分类')" @close="showForm = false">
        <form id="serviceCategoryForm" @submit.prevent="saveCategory">
          <div class="form-group">
            <label>分类名称</label>
            <input v-model="form.name_wsh" required placeholder="例如：寄养服务">
          </div>
          <div class="form-group">
            <label>分类编码</label>
            <input v-model="form.code_wsh" required :disabled="!!editingCategory" placeholder="例如：BOARDING">
            <small v-if="!editingCategory" style="color: var(--color-muted-foreground); font-size: 12px">
              大写英文字母、下划线，创建后不可修改
            </small>
          </div>
          <div class="form-group">
            <label>父级分类</label>
            <select v-model.number="form.parent_id_wsh">
              <option :value="0">一级分类（顶级）</option>
              <option
                v-for="node in flattenedTreeData"
                :key="node.id_wsh"
                :value="node.id_wsh"
                :disabled="isInvalidParent(node.id_wsh)"
              >
                {{ formatCategoryOption(node) }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="form.sort_order_wsh" type="number" min="0">
          </div>
        </form>
        <template #footer>
          <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
          <button type="submit" form="serviceCategoryForm" class="btn btn-primary btn-sm">{{ editingCategory ? '保存' : '创建' }}</button>
        </template>
      </AppDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { useCategoryStore } from '@/stores/category'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import {
  getAdminServiceCategoryList,
  createServiceCategory,
  updateServiceCategory,
  deleteServiceCategory
} from '@/api/serviceCategory'

const appStore = useAppStore()
const categoryStore = useCategoryStore()
const categories = ref([])
const loading = ref(true)
const showForm = ref(false)
const editingCategory = ref(null)
const form = reactive({ name_wsh: '', code_wsh: '', parent_id_wsh: 0, sort_order_wsh: 0 })
const expandedIds = ref(new Set())

function toggleExpand(id) {
  const s = new Set(expandedIds.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  expandedIds.value = s
}

function sortByOrder(list = []) {
  return [...list].sort((a, b) => {
    const orderDiff = (a.sort_order_wsh || 0) - (b.sort_order_wsh || 0)
    if (orderDiff !== 0) return orderDiff
    return (a.id_wsh || 0) - (b.id_wsh || 0)
  })
}

function buildTree(nodes) {
  const childrenMap = new Map()
  nodes.forEach(node => {
    const parentId = node.parent_id_wsh || 0
    if (!childrenMap.has(parentId)) childrenMap.set(parentId, [])
    childrenMap.get(parentId).push(node)
  })

  const buildBranch = (parentId = 0, level = 0) =>
    sortByOrder(childrenMap.get(parentId) || []).map(node => ({
      ...node,
      level,
      children: buildBranch(node.id_wsh, level + 1)
    }))

  return buildBranch()
}

const treeData = computed(() => buildTree(categories.value))

const flattenedTreeData = computed(() => {
  const rows = []
  const walk = (nodes) => {
    nodes.forEach(node => {
      rows.push(node)
      if (node.children.length) walk(node.children)
    })
  }
  walk(treeData.value)
  return rows
})

const invalidParentIds = computed(() => {
  if (!editingCategory.value) return new Set()

  const ids = new Set([editingCategory.value.id_wsh])
  const walk = (parentId) => {
    categories.value
      .filter(cat => (cat.parent_id_wsh || 0) === parentId)
      .forEach(child => {
        if (!ids.has(child.id_wsh)) {
          ids.add(child.id_wsh)
          walk(child.id_wsh)
        }
      })
  }

  walk(editingCategory.value.id_wsh)
  return ids
})

onMounted(loadCategories)

async function loadCategories() {
  loading.value = true
  try {
    const r = await getAdminServiceCategoryList()
    if (r.code === 200) categories.value = r.data || []
    expandedIds.value = new Set()
  } catch (e) {
    appStore.addToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function addCategory() {
  editingCategory.value = null
  Object.assign(form, { name_wsh: '', code_wsh: '', parent_id_wsh: 0, sort_order_wsh: 0 })
  showForm.value = true
}

function addSubCategory(parent) {
  editingCategory.value = null
  Object.assign(form, {
    name_wsh: '',
    code_wsh: `${parent.code_wsh}_`,
    parent_id_wsh: parent.id_wsh,
    sort_order_wsh: 0
  })
  showForm.value = true
}

function editCategory(cat) {
  editingCategory.value = cat
  Object.assign(form, {
    name_wsh: cat.name_wsh,
    code_wsh: cat.code_wsh,
    parent_id_wsh: cat.parent_id_wsh || 0,
    sort_order_wsh: cat.sort_order_wsh || 0
  })
  showForm.value = true
}

function formatCategoryOption(node) {
  return `${'\u00A0\u00A0'.repeat(node.level)}${node.name_wsh}`
}

function isInvalidParent(id) {
  return invalidParentIds.value.has(id)
}

async function saveCategory() {
  try {
    const payload = {
      name_wsh: form.name_wsh,
      code_wsh: form.code_wsh,
      parent_id_wsh: form.parent_id_wsh || null,
      sort_order_wsh: form.sort_order_wsh || 0
    }

    if (editingCategory.value) {
      const r = await updateServiceCategory(editingCategory.value.id_wsh, payload)
      if (r.code === 200) appStore.addToast('更新成功', 'success')
      else appStore.addToast(r.message || '更新失败', 'error')
    } else {
      const r = await createServiceCategory(payload)
      if (r.code === 200) appStore.addToast('创建成功', 'success')
      else appStore.addToast(r.message || '创建失败', 'error')
    }

    showForm.value = false
    editingCategory.value = null
    await loadCategories()
    categoryStore.loadCategories(true)
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  }
}

async function deleteCategory(cat) {
  if (!confirm(`确定删除分类“${cat.name_wsh}”吗？如果有子分类或关联服务项目，将无法删除。`)) return
  try {
    const r = await deleteServiceCategory(cat.id_wsh)
    if (r.code === 200) {
      appStore.addToast('删除成功', 'success')
      await loadCategories()
      categoryStore.loadCategories(true)
    } else {
      appStore.addToast(r.message || '删除失败', 'error')
    }
  } catch (e) {
    appStore.addToast('删除失败，请检查是否有子分类或关联服务', 'error')
  }
}

async function toggleStatus(cat) {
  const newStatus = cat.status_wsh === 1 ? 0 : 1
  try {
    const r = await updateServiceCategory(cat.id_wsh, { status_wsh: newStatus })
    if (r.code === 200) {
      appStore.addToast(newStatus === 1 ? '已启用' : '已停用', 'success')
      await loadCategories()
      categoryStore.loadCategories(true)
    } else {
      appStore.addToast(r.message || '操作失败', 'error')
    }
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  }
}
</script>

<style scoped>
.tree-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tree-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  cursor: default;
}

.has-children {
  cursor: pointer;
  font-weight: 600;
}

.tree-children .tree-row {
  margin-left: 28px;
  margin-top: 4px;
  border-color: var(--color-border-light, var(--color-border));
  font-weight: 400;
}

.tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  font-size: 10px;
  color: var(--color-muted-foreground);
  transition: transform 0.2s;
  flex-shrink: 0;
  user-select: none;
}

.tree-toggle.expanded {
  transform: rotate(90deg);
}

.tree-name {
  flex: 1;
  min-width: 0;
}

.tree-code {
  width: 180px;
  color: var(--color-muted-foreground);
  font-family: monospace;
  font-size: 13px;
}

.tree-status {
  width: 40px;
  font-size: 12px;
  color: var(--color-muted-foreground);
  text-align: center;
}

.tree-status.active {
  color: var(--color-primary);
  font-weight: 600;
}

.tree-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: var(--color-muted-foreground);
}
</style>
