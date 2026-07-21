<template>
  <div class="merchant-cs-page">
    <section class="panel">
      <div class="section-header">
        <div>
          <h1>客服管理</h1>
          <p>审核用户的客服申请，客服通过后只服务当前商家的工单和投诉。</p>
        </div>
        <button class="btn btn-outline btn-sm" type="button" @click="loadData">刷新</button>
      </div>

      <DataTable :columns="pendingColumns" :data="pendingApplications">
        <template #default="{ row }">
          <div class="row-actions">
            <button class="btn btn-success btn-sm" type="button" @click="openReview(row, 'approve')">通过</button>
            <button class="btn btn-danger btn-sm" type="button" @click="openReview(row, 'reject')">拒绝</button>
          </div>
        </template>
      </DataTable>
    </section>

    <section class="panel">
      <div class="section-header">
        <div>
          <h2>已通过客服</h2>
          <p>这些客服可以处理当前商家的支持任务。</p>
        </div>
      </div>

      <DataTable :columns="staffColumns" :data="staffList" />
    </section>

    <div v-if="pendingReview" class="modal-overlay" @mousedown.self="cancelReview">
      <div class="modal">
        <h3>{{ pendingReview.action === 'approve' ? '通过客服申请' : '拒绝客服申请' }}</h3>
        <p class="review-target">{{ pendingReview.name }}</p>
        <div class="form-group">
          <label>审核意见</label>
          <textarea v-model="reviewNote" rows="4" placeholder="填写给申请人的审核意见" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" type="button" @click="cancelReview">取消</button>
          <button
            :class="['btn', 'btn-sm', pendingReview.action === 'approve' ? 'btn-success' : 'btn-danger']"
            type="button"
            @click="submitReview"
          >
            确认{{ pendingReview.action === 'approve' ? '通过' : '拒绝' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import {
  approveCustomerServiceApplication,
  getMerchantCustomerServiceStaff,
  getPendingCustomerServiceApplications,
  rejectCustomerServiceApplication,
} from '@/api/merchantCustomerService'

const appStore = useAppStore()
const pendingApplications = ref([])
const staffList = ref([])
const pendingReview = ref(null)
const reviewNote = ref('')

const pendingColumns = [
  { label: '申请人', key: 'display_name_wsh' },
  { label: '账号', key: 'username_wsh' },
  { label: '申请说明', key: 'applicant_note_wsh' },
  { label: '申请时间', key: 'created_at_wsh' },
]

const staffColumns = [
  { label: '客服', key: 'display_name_wsh' },
  { label: '账号', key: 'username_wsh' },
  { label: '审核意见', key: 'review_note_wsh' },
  { label: '通过时间', key: 'reviewed_at_wsh' },
]

async function loadData() {
  const [pendingResponse, staffResponse] = await Promise.all([
    getPendingCustomerServiceApplications(),
    getMerchantCustomerServiceStaff(),
  ])
  if (pendingResponse.code === 200) {
    pendingApplications.value = (pendingResponse.data || []).map(enrichApplication)
  }
  if (staffResponse.code === 200) {
    staffList.value = (staffResponse.data || []).map(enrichApplication)
  }
}

function enrichApplication(item) {
  const displayName = item.nickname_wsh || item.username_wsh || `用户 #${item.user_id_wsh}`
  return {
    ...item,
    display_name_wsh: displayName,
    applicant_note_wsh: item.applicant_note_wsh || '-',
    review_note_wsh: item.review_note_wsh || '-',
  }
}

function openReview(row, action) {
  pendingReview.value = {
    id: row.id_wsh,
    action,
    name: row.display_name_wsh,
  }
  reviewNote.value = action === 'approve' ? '欢迎加入客服团队' : ''
}

function cancelReview() {
  pendingReview.value = null
  reviewNote.value = ''
}

async function submitReview() {
  if (!pendingReview.value) return
  const action = pendingReview.value.action
  try {
    const response = action === 'approve'
      ? await approveCustomerServiceApplication(pendingReview.value.id, reviewNote.value.trim())
      : await rejectCustomerServiceApplication(pendingReview.value.id, reviewNote.value.trim())
    if (response.code === 200) {
      appStore.addToast(action === 'approve' ? '已通过客服申请' : '已拒绝客服申请', 'success')
      cancelReview()
      await loadData()
    }
  } catch {
    appStore.addToast('审核操作失败', 'error')
  }
}

onMounted(loadData)
</script>

<style scoped>
.merchant-cs-page {
  display: grid;
  gap: 20px;
}
.panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}
.section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}
.section-header h1,
.section-header h2 {
  font-size: 20px;
  line-height: 1.3;
  margin: 0 0 4px;
}
.section-header p,
.review-target {
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin: 0;
}
.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
@media (max-width: 700px) {
  .section-header {
    flex-direction: column;
  }
  .panel {
    padding: 18px;
  }
}
</style>
