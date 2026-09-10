<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="showForm = true">+ 发布公告</button>
    <div v-for="n in notices" :key="n.id_wsh" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <strong>{{ n.title_wsh }}</strong>
        <span>
          <button class="btn btn-sm btn-secondary" @click="editNotice(n)">编辑</button>
          <button class="btn btn-sm btn-danger" style="margin-left:4px" @click="deleteNotice(n.id_wsh)">删除</button>
          <span style="font-size:12px;color:var(--color-muted-foreground);margin-left:8px">{{ deliveryLabel(n.delivery_type_wsh) }} · {{ new Date(n.created_at_wsh).toLocaleDateString() }}</span>
        </span>
      </div>
      <p style="margin-top:8px;font-size:14px;color:var(--color-muted-foreground)">{{ n.content_wsh }}</p>
    </div>

    <AppDialog :visible="showForm" :title="editingId ? '编辑公告' : '发布公告'" @close="showForm = false">
        <form id="noticeForm" @submit.prevent="editingId ? updateNotice() : createNotice()">
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group"><label>内容</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
          <div class="form-group">
            <label>发送方式</label>
            <div style="display:flex;flex-wrap:wrap;gap:12px;margin-top:8px">
              <label
                v-for="option in deliveryOptions"
                :key="option.value"
                style="display:inline-flex;align-items:center;gap:6px;font-size:14px"
              >
                <input v-model="form.delivery_types_wsh" type="checkbox" :value="option.value">
                <span>{{ option.label }}</span>
              </label>
            </div>
            <div style="margin-top:6px;color:var(--color-muted-foreground);font-size:12px">至少选择一项，可同时勾选。</div>
          </div>
        </form>
        <template #footer>
          <button type="button" class="btn btn-secondary btn-sm" @click="cancelForm">取消</button>
          <button type="submit" form="noticeForm" class="btn btn-primary btn-sm" :disabled="!form.delivery_types_wsh.length">{{ editingId ? '保存' : '发布' }}</button>
        </template>
      </AppDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getNotices, createNotice as apiCreateNotice, updateNotice as apiUpdateNotice, deleteNotice as apiDeleteNotice } from '@/api/notice'
import AppDialog from '@/components/common/AppDialog.vue'

const appStore = useAppStore()
const deliveryOptions = [
  { value: 'notification', label: '消息通知' },
  { value: 'popup', label: '弹窗' },
]
const notices = ref([])
const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ title_wsh: '', content_wsh: '', type_wsh: 'notice', delivery_types_wsh: [] })

function deliveryLabel(type) {
  const parsed = parseDeliveryTypes(type)
  const selected = new Set(parsed)
  const labels = deliveryOptions
    .filter(option => selected.has(option.value))
    .map(option => option.label)
  const extra = parsed.filter(value => !deliveryOptions.some(option => option.value === value))
  return [...labels, ...extra].join('、') || '公告'
}

function parseDeliveryTypes(type) {
  return String(type || '')
    .split(',')
    .map(item => item.trim().toLowerCase())
    .filter(Boolean)
}

function buildDeliveryTypeValue() {
  return [...new Set(form.delivery_types_wsh.map(item => item.trim().toLowerCase()).filter(Boolean))].join(',')
}

function getDeliveryPayload() {
  const delivery_type_wsh = buildDeliveryTypeValue()
  if (!delivery_type_wsh) {
    throw new Error('请至少选择一种发送方式')
  }
  return delivery_type_wsh
}

async function loadNotices() {
  try {
    const r = await getNotices({ type: 'notice', _t: Date.now() })
    if (r.code === 200) notices.value = (r.data || []).filter(n => n.type_wsh === 'notice')
  }
  catch (e) {}
}

onMounted(loadNotices)

function resetForm() {
  editingId.value = null
  Object.assign(form, { title_wsh: '', content_wsh: '', type_wsh: 'notice', delivery_types_wsh: [] })
}

function cancelForm() {
  showForm.value = false
  resetForm()
}

async function createNotice() {
  try {
    const payload = { title_wsh: form.title_wsh, content_wsh: form.content_wsh, type_wsh: 'notice', delivery_type_wsh: getDeliveryPayload() }
    const r = await apiCreateNotice(payload)
    if (r.code === 200) {
      appStore.addToast('发布成功', 'success')
      showForm.value = false
      resetForm()
      notices.value.unshift(r.data)
    }
  }
  catch (e) { appStore.addToast(e?.message || '发布失败', 'error') }
}

function editNotice(n) {
  editingId.value = n.id_wsh
  form.title_wsh = n.title_wsh
  form.content_wsh = n.content_wsh
  form.delivery_types_wsh = parseDeliveryTypes(n.delivery_type_wsh)
  showForm.value = true
}

async function updateNotice() {
  try {
    const payload = { title_wsh: form.title_wsh, content_wsh: form.content_wsh, type_wsh: 'notice', delivery_type_wsh: getDeliveryPayload() }
    const r = await apiUpdateNotice(editingId.value, payload)
    if (r.code === 200) {
      appStore.addToast('保存成功', 'success')
      showForm.value = false
      resetForm()
      await loadNotices()
    }
  }
  catch (e) { appStore.addToast(e?.message || '保存失败', 'error') }
}

async function deleteNotice(id) {
  if (!confirm('确定删除该公告？')) return
  try {
    await apiDeleteNotice(id)
    appStore.addToast('删除成功', 'success')
    await loadNotices()
  }
  catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
