<template>
  <div>
    <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap">
      <input v-model="query" placeholder="搜索文档..." style="max-width:300px" @keyup.enter="loadDocuments">
      <button class="btn btn-primary" @click="loadDocuments">搜索</button>
      <button class="btn btn-success" @click="openCreate">+ 新增文档</button>
    </div>

    <div v-if="loading" class="loading"><LoadingSpinner /></div>
    <DataTable v-else :columns="columns" :data="documents">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button class="btn btn-sm btn-outline" @click="openEdit(row.id_wsh)">编辑</button>
          <button class="btn btn-sm btn-danger" @click="handleDelete(row.id_wsh)">删除</button>
        </div>
      </template>
    </DataTable>
    <p v-if="!loading" class="pager-hint">共 {{ total }} 条文档</p>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>{{ editing ? '编辑文档' : '新增文档' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group"><label>分类</label>
            <select v-model="form.category_wsh">
              <option value="">无</option>
              <option value="饮食">饮食</option>
              <option value="健康">健康</option>
              <option value="行为">行为</option>
              <option value="护理">护理</option>
              <option value="医疗">医疗</option>
              <option value="疫苗">疫苗</option>
              <option value="训练">训练</option>
            </select>
          </div>
          <div class="form-group">
            <label>输入方式</label>
            <div style="display:flex;gap:12px">
              <label><input type="radio" value="manual" v-model="inputMode"> 手动输入</label>
              <label><input type="radio" value="upload" v-model="inputMode"> 上传文件</label>
            </div>
          </div>
          <div class="form-group" v-if="inputMode === 'manual'">
            <label>内容</label>
            <textarea v-model="form.content_wsh" rows="6" required></textarea>
          </div>
          <div class="form-group" v-if="inputMode === 'upload' && !editing">
            <label>文件</label>
            <input type="file" ref="fileInput" accept=".txt,.docx" @change="onFileChange" required>
            <div v-if="selectedFile" style="margin-top:4px;font-size:12px;color:var(--color-muted-foreground)">
              已选择: {{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
            </div>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="submitting">{{ submitting ? '提交中...' : (editing ? '保存' : '创建') }}</button>
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
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { getRagDocuments, searchRag, createRagDocument, uploadRagDocument, deleteRagDocument, updateRagDocument, getRagDocument } from '@/api/ai'


const appStore = useAppStore()
const documents = ref([])
const loading = ref(true)
const query = ref('')
const showForm = ref(false)
const editing = ref(false)
const submitting = ref(false)
const inputMode = ref('manual')
const selectedFile = ref(null)
const fileInput = ref(null)
const form = reactive({ title_wsh: '', content_wsh: '', category_wsh: '', source_type_wsh: '' })
const editingId = ref(null)
const total = ref(0)

const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '标题', key: 'title_wsh' },
  { label: '分类', key: 'category_wsh' },
  { label: '字数', key: 'word_count_wsh' },
  { label: '创建时间', key: 'created_at_wsh' },
]

onMounted(loadDocuments)

async function loadDocuments() {
  loading.value = true
  try {
    if (query.value.trim()) {
      const r = await searchRag({ query: query.value, limit: 100 })
      if (r.code === 200) {
        documents.value = r.data || []
        total.value = documents.value.length
      }
    } else {
      const r = await getRagDocuments({ page: 1, size: 100 })
      if (r.code === 200) {
        documents.value = r.data?.list || r.data?.records || (Array.isArray(r.data) ? r.data : [])
        total.value = r.data?.total ?? documents.value.length
      }
    }
  } catch (e) {}
  loading.value = false
}

function openCreate() {
  Object.assign(form, { title_wsh: '', content_wsh: '', category_wsh: '', source_type_wsh: '' })
  editing.value = false
  editingId.value = null
  submitting.value = false
  inputMode.value = 'manual'
  selectedFile.value = null
  showForm.value = true
}

async function openEdit(id) {
  try {
    const r = await getRagDocument(id)
    if (r.code === 200) {
      Object.assign(form, {
        title_wsh: r.data.title_wsh || '',
        content_wsh: r.data.content_wsh || '',
        category_wsh: r.data.category_wsh || '',
        source_type_wsh: r.data.source_type_wsh || '',
      })
      editingId.value = id
      editing.value = true
      submitting.value = false
      inputMode.value = 'manual'
      selectedFile.value = null
      showForm.value = true
    }
  } catch (e) {
    appStore.addToast('加载文档失败', 'error')
  }
}

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (inputMode.value === 'upload' && selectedFile.value && !editing.value) {
      const fd = new FormData()
      fd.append('file', selectedFile.value)
      if (form.title_wsh) fd.append('title', form.title_wsh)
      if (form.category_wsh) fd.append('category', form.category_wsh)
      const r = await uploadRagDocument(fd)
      if (r.code === 200) {
        appStore.addToast('创建成功', 'success')
        showForm.value = false
        loadDocuments()
      }
    } else if (editing.value && editingId.value) {
      const r = await updateRagDocument(editingId.value, { ...form })
      if (r.code === 200) {
        appStore.addToast('保存成功', 'success')
        showForm.value = false
        loadDocuments()
      }
    } else {
      const r = await createRagDocument({ ...form })
      if (r.code === 200) {
        appStore.addToast('创建成功', 'success')
        showForm.value = false
        loadDocuments()
      }
    }
  } catch (e) { appStore.addToast('创建失败', 'error') }
  finally { submitting.value = false }
}

async function handleDelete(id) {
  if (!confirm('确认删除此文档？')) return
  try {
    const r = await deleteRagDocument(id)
    if (r.code === 200) {
      appStore.addToast('删除成功', 'success')
      loadDocuments()
    }
  } catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>

<style scoped>
.pager-hint {
  margin-top: 12px;
  color: var(--color-muted-foreground);
  font-size: 12px;
}
</style>
