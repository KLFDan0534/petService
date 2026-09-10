<template>
  <section class="records-grid">
    <aside class="card section-card">
      <h3>选择订单</h3>
      <select :value="orderId" class="form-control" @change="$emit('update:orderId', $event.target.value)">
        <option value="">请选择订单</option>
        <option v-for="order in myOrders" :key="order.id_wsh" :value="String(order.id_wsh)">
          {{ order.service_name_wsh || '订单#' + order.id_wsh }} · {{ statusMap[order.status_wsh] || order.status_wsh }}
        </option>
      </select>
      <div v-if="dailyStatus" class="daily-card">
        <strong>每日上传进度</strong>
        <p>已上传 {{ dailyStatus.uploaded_days_wsh?.length || 0 }}/{{ dailyStatus.required_days_wsh?.length || 0 }}</p>
        <p v-if="dailyStatus.missing_days_wsh?.length">缺少：{{ dailyStatus.missing_days_wsh.join('、') }}</p>
      </div>
    </aside>

    <main v-if="orderId" class="card section-card">
      <h3>上传今日动态</h3>
      <div class="record-tabs">
        <button v-for="tab in recordTabs" :key="tab.key" :class="['btn', recordType === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']" @click="recordType = tab.key">
          {{ tab.label }}
        </button>
      </div>
      <textarea v-model="recordForm.content_wsh" class="form-control" rows="4" :placeholder="recordPlaceholder"></textarea>
      <div class="upload-field">
        <input ref="recordFileInput" type="file" accept="image/*" multiple style="display:none" @change="onRecordFilesChange">
        <button class="btn btn-outline btn-sm" @click="recordFileInput?.click()">选择照片</button>
        <span>{{ recordFiles.length ? `已选择 ${recordFiles.length} 张` : '未选择照片' }}</span>
      </div>
      <div v-if="recordFilePreviews.length" class="photo-preview-grid">
        <img v-for="preview in recordFilePreviews" :key="preview" :src="preview" alt="动态照片预览">
      </div>
      <div class="order-actions">
        <button class="btn btn-primary btn-sm" :disabled="recordUploading" @click="submitRecord">{{ recordUploading ? '上传中...' : '提交动态' }}</button>
        <button class="btn btn-outline btn-sm" @click="$emit('refresh')">刷新</button>
      </div>

      <div class="timeline">
        <div v-if="careRecords.length === 0" class="empty-inline">暂无动态</div>
        <article v-for="record in careRecords" :key="record.id_wsh" class="timeline-item">
          <div class="timeline-item__meta">{{ recordTypeMap[record.type_wsh] || record.type_wsh }} · {{ formatDateTime(record.record_time_wsh || record.created_at_wsh) }}</div>
          <p>{{ record.content_wsh }}</p>
          <div v-if="record.images_wsh" class="photo-preview-grid timeline-photos">
            <img v-for="url in parseImageUrls(record.images_wsh)" :key="url" :src="url" alt="动态照片">
          </div>
        </article>
      </div>
    </main>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { formatDateTime as utilFormatDateTime } from '@/utils/format'

const props = defineProps({
  orderId: { type: String, default: '' },
  myOrders: { type: Array, default: () => [] },
  dailyStatus: { type: Object, default: null },
  careRecords: { type: Array, default: () => [] },
  recordUploading: { type: Boolean, default: false },
  submitKey: { type: Number, default: 0 },
})

watch(() => props.submitKey, () => {
  recordForm.content_wsh = ''
  clearRecordFiles()
})
const emit = defineEmits(['update:orderId', 'upload-timeline', 'refresh'])

const statusMap = {
  pending: '待付款', paid: '已支付', confirmed: '待送达',
  delivered: '已送达', received: '已接收', in_progress: '培养中',
  completed: '已完成', cancelled: '已取消',
}

const recordType = ref('feed')
const recordFileInput = ref(null)
const recordFiles = ref([])
const recordFilePreviews = ref([])
const recordForm = reactive({ content_wsh: '' })

const recordTabs = [
  { key: 'feed', label: '喂食' },
  { key: 'activity', label: '活动' },
  { key: 'medication', label: '用药' },
  { key: 'health', label: '健康' },
  { key: 'note', label: '日志' },
]

const recordTypeMap = { feed: '喂食', activity: '活动', medication: '用药', health: '健康', note: '日志' }

const recordPlaceholder = computed(() => ({
  feed: '例如：上午 8 点喂食 100g，食欲正常',
  activity: '例如：下午散步 40 分钟，精神状态良好',
  medication: '例如：已按主人要求服药',
  health: '例如：体温正常，无异常情况',
  note: '例如：今日护理日志',
}[recordType.value]))

function onRecordFilesChange(event) {
  const files = Array.from(event.target.files || [])
  recordFiles.value = files
  recordFilePreviews.value = files.map(file => URL.createObjectURL(file))
}

function clearRecordFiles() {
  recordFiles.value = []
  recordFilePreviews.value = []
  if (recordFileInput.value) recordFileInput.value.value = ''
}

function submitRecord() {
  if (!props.orderId || !recordForm.content_wsh.trim() || recordFiles.value.length === 0) return
  emit('upload-timeline', {
    orderId: props.orderId,
    type: recordType.value,
    content: recordForm.content_wsh,
    files: [...recordFiles.value],
  })
}

function formatDateTime(value) {
  return utilFormatDateTime(value, { fallback: '-' })
}

function parseImageUrls(value) {
  return String(value || '').split(',').map(url => url.trim()).filter(Boolean)
}
</script>

<style scoped>
.records-grid { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 16px; align-items: start; }
.section-card { padding: 22px; display: grid; gap: 14px; }
.daily-card { border: 1px solid var(--color-border); border-radius: var(--radius-inline); padding: 12px; background: var(--color-muted); }
.daily-card p { margin: 6px 0 0; font-size: 13px; }
.record-tabs, .order-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.upload-field { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--color-muted-foreground); font-size: 13px; }
.photo-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 8px; }
.photo-preview-grid img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: var(--radius-inline); border: 1px solid var(--color-border); background: var(--color-muted); }
.timeline-photos { margin-top: 8px; max-width: 420px; }
.timeline { display: grid; gap: 10px; margin-top: 8px; }
.timeline-item { border: 1px solid var(--color-border); border-radius: var(--radius-inline); padding: 12px; }
.timeline-item p { margin: 6px 0; }
.timeline-item small, .timeline-item__meta { color: var(--color-muted-foreground); font-size: 12px; word-break: break-all; }
.empty-inline { color: var(--color-muted-foreground); font-size: 13px; }
@media (max-width: 760px) {
  .records-grid { grid-template-columns: 1fr; }
}
</style>
