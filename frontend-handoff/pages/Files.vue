<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:24px" @click="triggerUpload">+ 上传文件</button>
    <input type="file" ref="fileInput" style="display:none" @change="uploadFileHandler">
    <div v-for="f in files" :key="f.id_wsh" class="card" style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center">
      <div>
        <div style="font-weight:600">{{ f.original_name_wsh }}</div>
        <div style="font-size:12px;color:var(--color-muted-foreground)">{{ (f.size_wsh / 1024).toFixed(1) }} KB · {{ new Date(f.created_at_wsh).toLocaleDateString() }}</div>
      </div>
      <button class="btn btn-sm btn-outline" @click="downloadFileHandler(f.id_wsh)">下载</button>
    </div>
    <EmptyState v-if="!loading && files.length === 0" title="暂无文件" icon="📁" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import EmptyState from '@/components/common/EmptyState.vue'
import { getFiles, uploadFile, downloadFile } from '@/api/file'

const appStore = useAppStore()
const files = ref([])
const loading = ref(true)
const fileInput = ref(null)

onMounted(async () => {
  try { const r = await getFiles(); if (r.code === 200) files.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

function triggerUpload() { fileInput.value?.click() }

async function uploadFileHandler(e) {
  const file = e.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const r = await uploadFile(formData)
    if (r.code === 200) { appStore.addToast('上传成功', 'success'); files.value.push(r.data) }
  } catch (e) { appStore.addToast('上传失败', 'error') }
}

async function downloadFileHandler(id) {
  try {
    const res = await downloadFile(id)
    const disposition = res.headers['content-disposition']
    const match = disposition && disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i)
    const filename = match ? decodeURIComponent(match[1]) : `file-${id}`
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url; a.download = filename; a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    appStore.addToast('下载失败：' + (e.message || '权限不足或文件不存在'), 'error')
  }
}
</script>
