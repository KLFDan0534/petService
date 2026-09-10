<template>
  <div>
    <div v-if="!selectedOrder" class="card" style="padding:24px;margin-bottom:24px">
      <div class="form-group">
        <label>选择订单</label>
        <select v-model="selectedOrderId" class="form-control">
          <option value="">-- 请选择订单 --</option>
          <option v-for="o in orders" :key="o.id_wsh" :value="o.id_wsh">
            订单 #{{ o.id_wsh }} - {{ o.status_wsh }}
          </option>
        </select>
        <button class="btn btn-primary btn-sm" style="margin-top:8px" @click="loadOrder">确认选择</button>
      </div>
    </div>
    <div v-if="selectedOrder" style="margin-bottom:24px">
      <p style="margin-bottom:8px">当前订单: #{{ selectedOrder.id_wsh }} | 宠物ID: {{ selectedPetId }}</p>
      <button class="btn btn-primary" @click="openForm('health')">健康异常上报</button>
      <button class="btn btn-primary" style="margin-left:8px" @click="openForm('diet')">饮食异常上报</button>
      <button class="btn btn-primary" style="margin-left:8px" @click="openForm('behavior')">行为异常上报</button>
      <button class="btn btn-danger" style="margin-left:8px" @click="openForm('emergency')">紧急情况上报</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!selectedOrder" class="empty-state">
      <h3>请先选择一个订单</h3>
    </div>
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

    <AppDialog :visible="showForm" :title="typeLabel(form.type_wsh) + '上报'" @close="showForm = false">
      <form id="anomaly-form" @submit.prevent="submitRecord">
        <div class="form-group"><label>内容描述</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
        <div class="form-group"><label>图片URL（可选）</label><input v-model="form.images_wsh"></div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
        <button type="submit" form="anomaly-form" class="btn btn-primary btn-sm">提交</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { getCareRecordsByOrder, createCareRecord } from '@/api/careRecord'
import { useAppStore } from '@/stores/app'

const authStore = useAuthStore()
const appStore = useAppStore()
const records = ref([])
const orders = ref([])
const loading = ref(true)
const showForm = ref(false)
const selectedOrderId = ref('')
const selectedOrder = ref(null)
const selectedPetId = ref(null)
const form = ref({ type_wsh: 'health', content_wsh: '', images_wsh: '' })
const typeMap = { health: '健康异常', diet: '饮食异常', behavior: '行为异常', emergency: '紧急情况' }

function typeLabel(t) { return typeMap[t] || t }
function typeClass(t) {
  return t === 'emergency' ? 'text-danger' : t === 'health' ? 'text-warning' : ''
}

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/orders')
    if (r.code === 200) orders.value = (r.data || []).filter(o =>
      ['pending', 'confirmed', 'in_progress'].includes(o.status_wsh))
  } catch (e) {}
  finally { loading.value = false }
})

async function loadOrder() {
  if (!selectedOrderId.value) return
  selectedOrder.value = orders.value.find(o => o.id_wsh === selectedOrderId.value)
  selectedPetId.value = selectedOrder.value?.pet_id_wsh || selectedOrder.value?.service_id_wsh
  loading.value = true
  try {
    const r = await getCareRecordsByOrder(selectedOrderId.value)
    if (r.code === 200) records.value = r.data
  } catch (e) {}
  finally { loading.value = false }
}

function openForm(type) {
  form.value = { type_wsh: type, content_wsh: '', images_wsh: '' }
  showForm.value = true
}

async function submitRecord() {
  if (!selectedOrder.value) return
  try {
    const r = await createCareRecord({
      type_wsh: form.value.type_wsh,
      content_wsh: form.value.content_wsh,
      images_wsh: form.value.images_wsh,
      order_id_wsh: selectedOrder.value.id_wsh,
    })
    if (r.code === 200) { appStore.addToast('上报成功', 'success'); showForm.value = false; records.value.push(r.data) }
  } catch (e) { appStore.addToast('上报失败', 'error') }
}
</script>
