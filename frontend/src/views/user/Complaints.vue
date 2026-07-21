<template>
  <div class="complaints-page">
    <PageHero title="投诉建议" subtitle="围绕订单提交问题，客服会结合聊天与照护记录处理" />

    <div class="page-actions">
      <button class="btn btn-primary" type="button" @click="openComplaintForm">+ 提交投诉</button>
      <button class="btn btn-outline" type="button" @click="showReportForm = !showReportForm">举报内容</button>
    </div>

    <section v-if="routeOrderId" class="card order-context">
      <div>
        <span>当前关联订单</span>
        <strong>{{ route.query.orderNo || `#${routeOrderId}` }}</strong>
      </div>
      <router-link class="btn btn-outline btn-sm" :to="{ name: 'OrderDetail', params: { id: routeOrderId } }">
        查看订单
      </router-link>
    </section>

    <div v-if="showReportForm" class="card report-card">
      <h3>举报内容</h3>
      <form @submit.prevent="submitReport">
        <div class="form-group"><label>目标ID</label><input v-model="reportForm.target_id_wsh" required></div>
        <div class="form-group">
          <label>目标类型</label>
          <select v-model="reportForm.target_type_wsh">
            <option value="keeper">寄养师</option>
            <option value="merchant">商家</option>
            <option value="pet">宠物</option>
            <option value="content">内容</option>
          </select>
        </div>
        <div class="form-group"><label>原因</label><textarea v-model="reportForm.reason_wsh" rows="3" required></textarea></div>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary btn-sm" @click="showReportForm = false">取消</button>
          <button type="submit" class="btn btn-primary btn-sm">提交</button>
        </div>
      </form>
    </div>

    <div class="complaint-list">
      <article v-for="c in complaints" :key="c.id_wsh" class="card complaint-card">
        <div class="complaint-header">
          <div>
            <strong>{{ c.title_wsh }}</strong>
            <small v-if="c.order_id_wsh">订单 {{ evidenceOrderNo(c) }}</small>
          </div>
          <span :class="['badge', statusBadge(c.status_wsh)]">{{ statusLabel(c.status_wsh) }}</span>
        </div>
        <p>{{ c.content_wsh }}</p>
        <div v-if="c.evidence_summary_wsh" class="evidence-line">
          聊天 {{ c.evidence_summary_wsh.chat_message_count_wsh || 0 }} 条 ·
          照护记录 {{ c.evidence_summary_wsh.care_record_count_wsh || 0 }} 条
        </div>
        <div v-if="c.result_wsh" class="result-line">处理意见：{{ c.result_wsh }}</div>
      </article>
    </div>

    <EmptyState v-if="!loading && complaints.length === 0" title="暂无投诉" icon="📢" />

    <div v-if="showForm" class="modal-overlay" @mousedown.self="closeComplaintForm">
      <div class="modal complaint-modal">
        <h2>提交投诉</h2>
        <form @submit.prevent="createComplaint">
          <div v-if="form.order_id_wsh" class="linked-order">
            <span>关联订单</span>
            <strong>{{ route.query.orderNo || `#${form.order_id_wsh}` }}</strong>
          </div>
          <div class="form-grid">
            <div class="form-group">
              <label>投诉对象</label>
              <select v-model="form.target_type_wsh">
                <option value="keeper">寄养师</option>
                <option value="merchant">商家</option>
              </select>
            </div>
            <div class="form-group">
              <label>对象ID</label>
              <input v-model="form.target_id_wsh" placeholder="可由订单自动带入">
            </div>
          </div>
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group"><label>问题描述</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="closeComplaintForm">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getMyComplaints, createComplaint as apiCreateComplaint } from '@/api/complaint'
import { createReview } from '@/api/rating'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const appStore = useAppStore()
const complaints = ref([])
const loading = ref(true)
const submitting = ref(false)
const showForm = ref(false)
const showReportForm = ref(false)
const form = reactive(defaultComplaintForm())
const reportForm = reactive({ target_id_wsh: '', target_type_wsh: 'pet', reason_wsh: '' })

const routeOrderId = computed(() => route.query.orderId ? Number(route.query.orderId) : null)

onMounted(async () => {
  applyRouteContext()
  if (routeOrderId.value) showForm.value = true
  await loadComplaints()
})

watch(() => route.query, () => {
  applyRouteContext()
}, { deep: true })

async function loadComplaints() {
  loading.value = true
  try {
    const response = await getMyComplaints()
    if (response.code === 200) complaints.value = response.data || []
  } catch {
    appStore.addToast('投诉记录加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function defaultComplaintForm() {
  return {
    order_id_wsh: null,
    target_id_wsh: '',
    target_type_wsh: 'keeper',
    title_wsh: '',
    content_wsh: '',
  }
}

function applyRouteContext() {
  if (!routeOrderId.value) return
  form.order_id_wsh = routeOrderId.value
  form.target_id_wsh = route.query.targetId ? String(route.query.targetId) : ''
  form.target_type_wsh = route.query.targetType === 'merchant' ? 'merchant' : 'keeper'
  if (!form.title_wsh) {
    form.title_wsh = `订单${route.query.orderNo ? ` ${route.query.orderNo}` : ''}服务问题`
  }
}

function openComplaintForm() {
  Object.assign(form, defaultComplaintForm())
  applyRouteContext()
  showForm.value = true
}

function closeComplaintForm() {
  showForm.value = false
}

async function createComplaint() {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      ...form,
      target_id_wsh: form.target_id_wsh ? Number(form.target_id_wsh) : null,
    }
    const response = await apiCreateComplaint(payload)
    if (response.code === 200) {
      appStore.addToast('提交成功', 'success')
      showForm.value = false
      Object.assign(form, defaultComplaintForm())
      applyRouteContext()
      await loadComplaints()
    }
  } catch (error) {
    appStore.addToast(error?.message || '提交失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function submitReport() {
  try {
    const response = await createReview({ ...reportForm })
    if (response.code === 200) {
      appStore.addToast('举报成功', 'success')
      showReportForm.value = false
      Object.assign(reportForm, { target_id_wsh: '', target_type_wsh: 'pet', reason_wsh: '' })
    }
  } catch {
    appStore.addToast('举报失败', 'error')
  }
}

function evidenceOrderNo(complaint) {
  return complaint.evidence_summary_wsh?.order_no_wsh || `#${complaint.order_id_wsh}`
}

function statusLabel(status) {
  return { resolved: '已处理', rejected: '已驳回', pending: '待处理' }[status] || status || '-'
}

function statusBadge(status) {
  return { resolved: 'badge-success', rejected: 'badge-danger', pending: 'badge-warning' }[status] || 'badge-info'
}
</script>

<style scoped>
.complaints-page {
  display: grid;
  gap: 16px;
}
.page-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.order-context,
.complaint-card,
.report-card {
  padding: 18px;
}
.order-context {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.order-context span,
.linked-order span {
  display: block;
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.complaint-list {
  display: grid;
  gap: 12px;
}
.complaint-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.complaint-header small {
  display: block;
  margin-top: 4px;
  color: var(--color-muted-foreground);
}
.complaint-card p {
  margin: 10px 0 0;
  color: var(--color-muted-foreground);
}
.evidence-line,
.result-line {
  margin-top: 10px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.linked-order {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-muted);
}
.complaint-modal {
  max-width: 560px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@media (max-width: 640px) {
  .order-context,
  .complaint-header {
    flex-direction: column;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
