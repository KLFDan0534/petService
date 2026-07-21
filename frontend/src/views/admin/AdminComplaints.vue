<template>
  <div class="admin-complaints-page">
    <DataTable :columns="columns" :data="complaints">
      <template #default="{ row }">
        <div class="row-actions">
          <button
            v-if="row.order_id_wsh"
            class="btn btn-sm btn-info"
            type="button"
            @click="openEvidence(row)"
          >
            查看证据
          </button>
          <button
            v-if="row.status_wsh === 'pending'"
            class="btn btn-sm btn-success"
            type="button"
            @click="openReview(row, 'resolve')"
          >
            处理
          </button>
          <button
            v-if="row.status_wsh === 'pending'"
            class="btn btn-sm btn-danger"
            type="button"
            @click="openReview(row, 'reject')"
          >
            驳回
          </button>
        </div>
      </template>
    </DataTable>

    <div v-if="pendingReview" class="modal-overlay" @mousedown.self="cancelReview">
      <div class="modal" style="max-width:500px">
        <h3>{{ pendingReview.action === 'resolve' ? '处理投诉' : '驳回投诉' }}</h3>
        <p style="margin:10px 0 0;color:var(--color-muted-foreground)">{{ pendingReview.title }}</p>
        <div class="form-group" style="margin-top:16px">
          <label>处理意见</label>
          <textarea v-model="reviewResult" rows="4" placeholder="请输入处理意见"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" type="button" @click="cancelReview">取消</button>
          <button
            :class="['btn', 'btn-sm', pendingReview.action === 'resolve' ? 'btn-success' : 'btn-danger']"
            type="button"
            :disabled="!reviewResult.trim()"
            @click="submitReview"
          >
            {{ pendingReview.action === 'resolve' ? '确认处理' : '确认驳回' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="evidenceVisible" class="modal-overlay" @mousedown.self="closeEvidence">
      <div class="modal evidence-modal">
        <div class="modal-header">
          <div>
            <h2>投诉证据</h2>
            <p v-if="selectedEvidence?.summary_wsh">
              订单 {{ selectedEvidence.summary_wsh.order_no_wsh || `#${selectedEvidence.summary_wsh.order_id_wsh}` }}
            </p>
          </div>
          <button class="btn btn-outline btn-sm" type="button" @click="closeEvidence">关闭</button>
        </div>

        <div v-if="evidenceLoading" class="loading">加载中...</div>
        <div v-else-if="selectedEvidence" class="evidence-content">
          <section class="evidence-summary">
            <div>
              <span>订单状态</span>
              <strong>{{ selectedEvidence.summary_wsh?.order_status_wsh || '-' }}</strong>
            </div>
            <div>
              <span>聊天</span>
              <strong>{{ selectedEvidence.summary_wsh?.chat_message_count_wsh || 0 }} 条</strong>
            </div>
            <div>
              <span>照护记录</span>
              <strong>{{ selectedEvidence.summary_wsh?.care_record_count_wsh || 0 }} 条</strong>
            </div>
          </section>

          <section class="evidence-section">
            <h3>最近聊天</h3>
            <div v-if="selectedEvidence.chat_messages_wsh?.length" class="evidence-list">
              <article v-for="message in selectedEvidence.chat_messages_wsh" :key="message.id_wsh" class="evidence-item">
                <div class="item-meta">
                  <span>{{ message.from_user_id_wsh }} → {{ message.to_user_id_wsh }}</span>
                  <time>{{ formatTime(message.created_at_wsh) }}</time>
                </div>
                <p v-if="message.content_wsh">{{ message.content_wsh }}</p>
                <a v-if="message.file_url_wsh" :href="message.file_url_wsh" target="_blank" rel="noreferrer">查看附件</a>
              </article>
            </div>
            <div v-else class="empty-inline">暂无聊天记录</div>
          </section>

          <section class="evidence-section">
            <h3>照护记录</h3>
            <div v-if="selectedEvidence.care_records_wsh?.length" class="evidence-list">
              <article v-for="record in selectedEvidence.care_records_wsh" :key="record.id_wsh" class="evidence-item">
                <div class="item-meta">
                  <span>{{ record.type_wsh || '记录' }}</span>
                  <time>{{ formatTime(record.record_time_wsh || record.created_at_wsh) }}</time>
                </div>
                <p v-if="record.content_wsh">{{ record.content_wsh }}</p>
                <div v-if="parseImages(record.images_wsh).length" class="photo-links">
                  <a
                    v-for="(url, index) in parseImages(record.images_wsh)"
                    :key="`${url}-${index}`"
                    :href="url"
                    target="_blank"
                    rel="noreferrer"
                  >
                    照片 {{ index + 1 }}
                  </a>
                </div>
              </article>
            </div>
            <div v-else class="empty-inline">暂无照护记录</div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import {
  getComplaints,
  getComplaintEvidence,
  resolveComplaint,
  rejectComplaint as rejectComplaintApi,
} from '@/api/complaint'
import { ComplaintStatus, enrichWithStatus } from '@/constants/statusMaps'
import DataTable from '@/components/common/DataTable.vue'

const appStore = useAppStore()
const complaints = ref([])
const evidenceVisible = ref(false)
const evidenceLoading = ref(false)
const selectedEvidence = ref(null)
const pendingReview = ref(null)
const reviewResult = ref('')

const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '投诉人', key: 'owner_name_wsh' },
  { label: '订单', key: 'order_label_wsh' },
  { label: '标题', key: 'title_wsh' },
  { label: '证据摘要', key: 'evidence_label_wsh' },
  { label: '状态', key: 'status_label_wsh' },
  { label: '时间', key: 'created_at_wsh' },
]

function enrichComplaint(complaint) {
  const enriched = enrichWithStatus(complaint, 'status_wsh', ComplaintStatus)
  const evidence = complaint.evidence_summary_wsh
  enriched.order_label_wsh = evidence?.order_no_wsh || (complaint.order_id_wsh ? `#${complaint.order_id_wsh}` : '-')
  enriched.evidence_label_wsh = evidence
    ? `聊天 ${evidence.chat_message_count_wsh || 0} 条 / 照护 ${evidence.care_record_count_wsh || 0} 条`
    : '-'
  return enriched
}

async function loadComplaints() {
  try {
    const response = await getComplaints()
    if (response.code === 200) {
      complaints.value = (response.data.list || response.data || []).map(enrichComplaint)
    }
  } catch {
    appStore.addToast('投诉列表加载失败', 'error')
  }
}

async function openEvidence(row) {
  evidenceVisible.value = true
  evidenceLoading.value = true
  selectedEvidence.value = null
  try {
    const response = await getComplaintEvidence(row.id_wsh)
    if (response.code === 200) selectedEvidence.value = response.data
  } catch {
    appStore.addToast('证据加载失败', 'error')
  } finally {
    evidenceLoading.value = false
  }
}

function closeEvidence() {
  evidenceVisible.value = false
  selectedEvidence.value = null
}

function openReview(row, action) {
  pendingReview.value = {
    id: row.id_wsh,
    action,
    title: row.title_wsh || `投诉 #${row.id_wsh}`,
  }
  reviewResult.value = action === 'resolve' ? '投诉已处理' : '投诉证据不足，已驳回'
}

function cancelReview() {
  pendingReview.value = null
  reviewResult.value = ''
}

async function submitReview() {
  if (!pendingReview.value || !reviewResult.value.trim()) return
  try {
    const response = pendingReview.value.action === 'resolve'
      ? await resolveComplaint(pendingReview.value.id, reviewResult.value.trim())
      : await rejectComplaintApi(pendingReview.value.id, reviewResult.value.trim())
    if (response.code === 200) {
      appStore.addToast(pendingReview.value.action === 'resolve' ? '已处理' : '已驳回', 'success')
      cancelReview()
      await loadComplaints()
    }
  } catch {
    appStore.addToast('操作失败', 'error')
  }
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function parseImages(value) {
  if (!value) return []
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

onMounted(loadComplaints)
</script>

<style scoped>
.admin-complaints-page {
  display: grid;
  gap: 16px;
}
.row-actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.evidence-modal {
  width: min(920px, calc(100vw - 32px));
  max-height: calc(100vh - 64px);
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.modal-header p {
  margin: 4px 0 0;
  color: var(--color-muted-foreground);
}
.evidence-content {
  display: grid;
  gap: 18px;
}
.evidence-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.evidence-summary > div {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-muted);
}
.evidence-summary span,
.item-meta {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.evidence-summary strong {
  display: block;
  margin-top: 4px;
}
.evidence-section h3 {
  margin-bottom: 10px;
}
.evidence-list {
  display: grid;
  gap: 10px;
}
.evidence-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
}
.item-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.evidence-item p {
  margin: 0 0 6px;
}
.photo-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.empty-inline {
  color: var(--color-muted-foreground);
  font-size: 13px;
}
@media (max-width: 700px) {
  .evidence-summary {
    grid-template-columns: 1fr;
  }
  .item-meta {
    flex-direction: column;
  }
}
</style>
