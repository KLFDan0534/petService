<template>
  <div>
    <div class="admin-filters">
      <input
        v-model="filters.keyword"
        class="form-control"
        placeholder="搜索文件名"
        @keyup.enter="applyFilters"
      >
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">搜索</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'文件名',key:'original_name_wsh'},
      {label:'类型',key:'file_type_wsh'},
      {label:'大小',key:'size_label_wsh'},
      {label:'上传时间',key:'created_at_wsh'}
    ]" :data="files">
      <template #default="{ row }">
        <button class="btn btn-sm btn-danger" @click="confirmDelete(row)">删除</button>
      </template>
    </DataTable>

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>

    <AppDialog :visible="!!pendingDelete" :width="420" title="删除文件" @close="pendingDelete = null">
      <p style="margin-top:12px;font-size:14px">确定要删除文件「{{ pendingDelete?.original_name_wsh || pendingDelete?.id_wsh }}」吗？此操作不可恢复。</p>
      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="pendingDelete = null">取消</button>
        <button class="btn btn-danger btn-sm" type="button" @click="doDelete">确认删除</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { getAdminFiles, deleteFile } from '@/api/file'

const appStore = useAppStore()
const files = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ keyword: '' })
const pendingDelete = ref(null)
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function fmtSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

function enrich(f) {
  return { ...f, size_label_wsh: fmtSize(f.file_size_wsh) }
}

function buildParams() {
  const params = { page: currentPage.value, size: size.value }
  if (filters.keyword && filters.keyword.trim()) params.keyword = filters.keyword.trim()
  return params
}

async function load() {
  try {
    const r = await getAdminFiles(buildParams())
    if (r.code === 200) {
      files.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { filters.keyword = ''; applyFilters() }
function changePage(page) { currentPage.value = page; load() }

function confirmDelete(row) { pendingDelete.value = row }

async function doDelete() {
  try {
    await deleteFile(pendingDelete.value.id_wsh)
    appStore.addToast('已删除', 'success')
    pendingDelete.value = null
    await load()
  } catch (e) { appStore.addToast('删除失败', 'error') }
}

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 220px; }
</style>