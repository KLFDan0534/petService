<template>
  <div>
    <PageHero title="异常管理" subtitle="健康、饮食、行为异常及紧急情况上报" />
    <div style="margin-bottom:24px">
      <button class="btn btn-primary" @click="openForm('health')">健康异常上报</button>
      <button class="btn btn-primary" style="margin-left:8px" @click="openForm('diet')">饮食异常上报</button>
      <button class="btn btn-primary" style="margin-left:8px" @click="openForm('behavior')">行为异常上报</button>
      <button class="btn btn-danger" style="margin-left:8px" @click="openForm('emergency')">紧急情况上报</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="records.length === 0" class="empty-state">
      <h3>暂无异常记录</h3>
    </div>
    <div v-else class="card" style="margin-bottom:12px" v-for="r in records" :key="r.id_wsh">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <strong :class="typeClass(r.type_wsh)">{{ typeLabel(r.type_wsh) }}</strong>
        <span style="font-size:12px;color:var(--color-muted-foreground)">{{ new Date(r.record_time_wsh || r.created_at_wsh).toLocaleString() }}</span>
      </div>
      <p style="margin-top:8px">{{ r.content_wsh }}</p>
      <div v-if="r.images_wsh" style="margin-top:8px;font-size:12px;color:var(--color-muted-foreground)">附件: {{ r.images_wsh }}</div>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>{{ typeLabel(form.type_wsh) }}上报</h2>
        <form @submit.prevent="submitRecord">
          <div class="form-group"><label>内容描述</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
          <div class="form-group"><label>图片URL（可选）</label><input v-model="form.images_wsh"></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">提交</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const records = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = ref({ type: 'health', content: '', images: '' })
const typeMap = { health: '健康异常', diet: '饮食异常', behavior: '行为异常', emergency: '紧急情况' }

function typeLabel(t) { return typeMap[t] || t }
function typeClass(t) {
  return t === 'emergency' ? 'text-danger' : t === 'health' ? 'text-warning' : ''
}

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/api/care-records/order/1')
    if (r.code === 200) records.value = r.data
  } catch (e) {}
  finally { loading.value = false }
})

function openForm(type) {
  form.value = { type, content: '', images: '' }
  showForm.value = true
}

async function submitRecord() {
  try {
    const r = await authStore.apiPost('/api/care-records', { ...form.value, orderId: 1, petId: 1, keeperId: 1 })
    if (r.code === 200) { appStore.addToast('上报成功', 'success'); showForm.value = false; records.value.push(r.data) }
  } catch (e) { appStore.addToast('上报失败', 'error') }
}
</script>
