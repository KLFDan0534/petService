<template>
  <div>
    <PageHero title="文件管理" subtitle="管理您的文件上传" />
    <button class="btn btn-primary" style="margin-bottom:24px" @click="triggerUpload">+ 上传文件</button>
    <input type="file" ref="fileInput" style="display:none" @change="uploadFile">
    <div v-for="f in files" :key="f.id_wsh" class="card" style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center">
      <div>
        <div style="font-weight:600">{{ f.original_name_wsh }}</div>
        <div style="font-size:12px;color:var(--color-muted-foreground)">{{ (f.size_wsh / 1024).toFixed(1) }} KB · {{ new Date(f.created_at_wsh).toLocaleDateString() }}</div>
      </div>
      <button class="btn btn-sm btn-outline" @click="downloadFile(f.id_wsh)">下载</button>
    </div>
    <EmptyState v-if="!loading && files.length === 0" title="暂无文件" icon="📁" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const files = ref([])
const loading = ref(true)
const fileInput = ref(null)

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/files'); if (r.code === 200) files.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

function triggerUpload() { fileInput.value?.click() }

async function uploadFile(e) {
  const file = e.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const r = await authStore.apiPost('/api/files/upload', formData)
    if (r.code === 200) { appStore.addToast('上传成功', 'success'); files.value.push(r.data) }
  } catch (e) { appStore.addToast('上传失败', 'error') }
}

function downloadFile(id) { window.open(`/api/files/${id}/download`, '_blank') }
</script>
