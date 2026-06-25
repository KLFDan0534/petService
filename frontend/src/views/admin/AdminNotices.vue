<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="showForm = true">+ 发布公告</button>
    <div v-for="n in notices" :key="n.id_wsh" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <strong>{{ n.title_wsh }}</strong>
        <span><button class="btn btn-sm btn-secondary" @click="editNotice(n)">编辑</button><button class="btn btn-sm btn-danger" style="margin-left:4px" @click="deleteNotice(n.id_wsh)">删除</button><span style="font-size:12px;color:var(--color-muted-foreground);margin-left:8px">{{ new Date(n.created_at_wsh).toLocaleDateString() }}</span></span>
      </div>
      <p style="margin-top:8px;font-size:14px;color:var(--color-muted-foreground)">{{ n.content_wsh }}</p>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>{{ editingId ? '编辑公告' : '发布公告' }}</h2>
        <form @submit.prevent="editingId ? updateNotice() : createNotice()">
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group"><label>内容</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">{{ editingId ? '保存' : '发布' }}</button>
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

const authStore = useAuthStore()
const appStore = useAppStore()
const notices = ref([])
const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ title_wsh: '', content_wsh: '', type_wsh: 'notice' })

async function loadNotices() {
  try {
    const r = await authStore.apiGet('/api/notices', { type: 'notice', _t: Date.now() })
    if (r.code === 200) notices.value = (r.data || []).filter(n => n.type_wsh === 'notice')
  }
  catch (e) {}
}

onMounted(loadNotices)

function resetForm() {
  editingId.value = null
  Object.assign(form, { title_wsh: '', content_wsh: '', type_wsh: 'notice' })
}

async function createNotice() {
  try {
    const payload = { title_wsh: form.title_wsh, content_wsh: form.content_wsh, type_wsh: 'notice' }
    const r = await authStore.apiPost('/api/notices', payload)
    if (r.code === 200) {
      appStore.addToast('发布成功', 'success')
      showForm.value = false
      resetForm()
      notices.value.unshift(r.data)
    }
  }
  catch (e) { appStore.addToast('发布失败', 'error') }
}

function editNotice(n) {
  editingId.value = n.id_wsh
  form.title_wsh = n.title_wsh
  form.content_wsh = n.content_wsh
  showForm.value = true
}

async function updateNotice() {
  try {
    const payload = { title_wsh: form.title_wsh, content_wsh: form.content_wsh, type_wsh: 'notice' }
    const r = await authStore.apiPost(`/api/notices/${editingId.value}`, payload)
    if (r.code === 200) {
      appStore.addToast('保存成功', 'success')
      showForm.value = false
      resetForm()
      await loadNotices()
    }
  }
  catch (e) { appStore.addToast('保存失败', 'error') }
}

async function deleteNotice(id) {
  if (!confirm('确定删除该公告？')) return
  try {
    await authStore.apiDelete(`/api/notices/${id}`)
    appStore.addToast('删除成功', 'success')
    await loadNotices()
  } catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
