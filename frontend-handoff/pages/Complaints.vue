<template>
  <div class="complaints-page">
    <div class="page-actions">
      <button class="btn btn-primary" type="button" @click="openComplaintForm">+ 提交投诉</button>
      <button class="btn btn-outline" type="button" @click="showReportForm = !showReportForm">举报对象</button>
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
      <h3>举报对象</h3>
      <p class="form-hint">选择要举报的订单、商家或寄养师，平台管理员将进行审核。</p>
      <form @submit.prevent="submitReport">
        <div class="form-group">
          <label>举报对象</label>
          <select v-model="reportForm.targetKey" required @change="onReportTargetChange">
            <option value="" disabled>请选择要举报的对象</option>
            <optgroup label="我的订单">
              <option v-for="o in targets.orders_wsh || []" :key="'o' + o.id_wsh" :value="`order:${o.id_wsh}`">
                {{ o.order_no_wsh || ('订单 #' + o.id_wsh) }} · {{ o.merchant_name_wsh || '商家' }}
              </option>
            </optgroup>
            <optgroup label="我消费的商家">
              <option v-for="m in targets.merchants_wsh || []" :key="'m' + m.id_wsh" :value="`merchant:${m.id_wsh}`">
                {{ m.name_wsh }}（{{ m.order_count_wsh }} 单）
              </option>
            </optgroup>
            <optgroup label="服务我的寄养师">
              <option v-for="k in targets.keepers_wsh || []" :key="'k' + k.id_wsh" :value="`keeper:${k.id_wsh}`">
                {{ k.name_wsh }} · {{ k.merchant_name_wsh || '寄养师' }}
              </option>
            </optgroup>
          </select>
          <small v-if="targetsLoading" class="form-hint">正在加载可选对象...</small>
        </div>
        <div v-if="reportForm.targetLabel" class="form-group">
          <label>举报目标</label>
          <input :value="reportForm.targetLabel" disabled>
        </div>
        <div class="form-group"><label>原因</label><textarea v-model="reportForm.reason_wsh" rows="3" required placeholder="请描述违规原因"></textarea></div>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary btn-sm" @click="showReportForm = false">取消</button>
          <button type="submit" class="btn btn-primary btn-sm">提交举报</button>
        </div>
      </form>
    </div>

    <div class="complaint-list">
      <article v-for="c in complaints" :key="c.id_wsh" class="card complaint-card" @click="openDetail(c)">
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
        <div class="detail-hint">点击查看沟通记录 →</div>
      </article>
    </div>

    <EmptyState v-if="!loading && complaints.length === 0" title="暂无投诉" icon="📢" />

    <div v-if="selectedComplaint" class="modal-overlay" @mousedown.self="selectedComplaint = null">
      <div class="modal" style="max-width:640px">
        <div class="detail-header">
          <div>
            <h2 style="margin:0">{{ selectedComplaint.title_wsh }}</h2>
            <span :class="['badge', statusBadge(selectedComplaint.status_wsh)]" style="margin-top:8px">{{ statusLabel(selectedComplaint.status_wsh) }}</span>
          </div>
          <button class="btn btn-secondary btn-sm" type="button" @click="selectedComplaint = null">关闭</button>
        </div>
        <p class="detail-content">{{ selectedComplaint.content_wsh }}</p>
        <div v-if="parseImages(selectedComplaint.images_wsh).length" class="detail-photos">
          <a
            v-for="(url, index) in parseImages(selectedComplaint.images_wsh)"
            :key="url"
            :href="url"
            target="_blank"
            rel="noreferrer"
            class="detail-photo"
          >
            <img :src="url" :alt="`投诉照片 ${index + 1}`" loading="lazy" @error="$event.target.style.display = 'none'">
          </a>
        </div>
        <div v-if="selectedComplaint.result_wsh" class="alert alert-success" style="margin-bottom:14px">
          <strong>处理意见：</strong>{{ selectedComplaint.result_wsh }}
        </div>

        <h4 style="margin-bottom:8px">沟通记录</h4>
        <div v-if="messagesLoading" class="loading" style="padding:16px">加载中...</div>
        <div v-else-if="detailMessages.length === 0" class="empty-msg">暂无消息，可在下方留言</div>
        <div v-for="m in detailMessages" :key="m.id_wsh" class="msg-row" :class="{ 'msg-row--me': m.from_user_id_wsh === currentUserId }">
          <div class="msg-bubble">
            <div class="msg-meta">{{ m.from_user_name_wsh || '客服' }} · {{ formatTime(m.created_at_wsh) }}</div>
            <div>{{ m.content_wsh }}</div>
          </div>
        </div>

        <div class="form-group" style="margin-top:14px">
          <textarea v-model="replyContent" rows="2" maxlength="1000" placeholder="回复客服..."></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-primary btn-sm" type="button" :disabled="!replyContent.trim() || replySubmitting" @click="sendReply">
            {{ replySubmitting ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="closeComplaintForm">
      <div class="modal complaint-modal">
        <h2>提交投诉</h2>
        <form @submit.prevent="createComplaint">
          <div class="form-group">
            <label>投诉对象</label>
            <select v-model="form.targetKey" required @change="syncFromTargetKey">
              <option value="" disabled>请选择要投诉的对象</option>
              <optgroup label="我的订单">
                <option v-for="o in targets.orders_wsh || []" :key="'o' + o.id_wsh" :value="`order:${o.id_wsh}`">
                  {{ o.order_no_wsh || ('订单 #' + o.id_wsh) }} · {{ o.merchant_name_wsh || '商家' }}（{{ orderStatusLabel(o.status_wsh) }}）
                </option>
              </optgroup>
              <optgroup label="我消费的商家">
                <option v-for="m in targets.merchants_wsh || []" :key="'m' + m.id_wsh" :value="`merchant:${m.id_wsh}`">
                  {{ m.name_wsh }}（{{ m.order_count_wsh }} 单）
                </option>
              </optgroup>
              <optgroup label="服务我的寄养师">
                <option v-for="k in targets.keepers_wsh || []" :key="'k' + k.id_wsh" :value="`keeper:${k.id_wsh}`">
                  {{ k.name_wsh }} · {{ k.merchant_name_wsh || '寄养师' }}
                </option>
              </optgroup>
            </select>
            <small v-if="targetsLoading" class="form-hint">正在加载可选对象...</small>
            <small v-else-if="targetEmpty" class="form-hint">暂无可投诉的订单或商家，可在完成一次服务后进行反馈。</small>
          </div>
          <div v-if="form.order_id_wsh" class="linked-order">
            <span>关联订单</span>
            <strong>{{ route.query.orderNo || `#${form.order_id_wsh}` }}</strong>
          </div>
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required maxlength="100" placeholder="一句话概括问题"></div>
          <div class="form-group"><label>问题描述</label><textarea v-model="form.content_wsh" rows="4" required maxlength="2000" placeholder="请描述发生了什么、您的诉求"></textarea></div>
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
import { getMyComplaints, createComplaint as apiCreateComplaint, getComplaintTargets, getComplaintMessages, sendComplaintMessage } from '@/api/complaint'
import { createReview } from '@/api/rating'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()
const complaints = ref([])
const loading = ref(true)
const submitting = ref(false)
const showForm = ref(false)
const showReportForm = ref(false)
const targets = ref({ orders_wsh: [], merchants_wsh: [], keepers_wsh: [] })
const targetsLoading = ref(true)
const form = reactive(defaultComplaintForm())
const reportForm = reactive({ targetKey: '', targetLabel: '', reason_wsh: '' })
const selectedComplaint = ref(null)
const detailMessages = ref([])
const messagesLoading = ref(false)
const replyContent = ref('')
const replySubmitting = ref(false)
const currentUserId = computed(() => authStore.user?.id_wsh || null)

const routeOrderId = computed(() => route.query.orderId ? Number(route.query.orderId) : null)
const targetEmpty = computed(() => {
  const t = targets.value
  return !(t.orders_wsh?.length || t.merchants_wsh?.length || t.keepers_wsh?.length)
})

onMounted(async () => {
  await loadTargets()
  applyRouteContext()
  await loadComplaints()
})

watch(() => route.query, () => {
  applyRouteContext()
}, { deep: true })

async function loadTargets() {
  targetsLoading.value = true
  try {
    const r = await getComplaintTargets()
    if (r.code === 200) {
      targets.value = {
        orders_wsh: r.data?.orders_wsh || [],
        merchants_wsh: r.data?.merchants_wsh || [],
        keepers_wsh: r.data?.keepers_wsh || [],
      }
      syncFromTargetKey()
    }
  } catch (e) {
    targets.value = { orders_wsh: [], merchants_wsh: [], keepers_wsh: [] }
  } finally {
    targetsLoading.value = false
  }
}

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
    merchant_id_wsh: null,
    target_id_wsh: null,
    target_type_wsh: 'keeper',
    targetKey: '',
    title_wsh: '',
    content_wsh: '',
  }
}

function applyRouteContext() {
  if (!routeOrderId.value) return
  form.order_id_wsh = routeOrderId.value
  form.targetKey = `order:${routeOrderId.value}`
  form.target_type_wsh = route.query.targetType === 'merchant' ? 'merchant' : 'keeper'
  if (!form.title_wsh) {
    form.title_wsh = `订单${route.query.orderNo ? ` ${route.query.orderNo}` : ''}服务问题`
  }
  syncFromTargetKey()
}

function openComplaintForm() {
  Object.assign(form, defaultComplaintForm())
  applyRouteContext()
  showForm.value = true
}

function closeComplaintForm() {
  showForm.value = false
}

function syncFromTargetKey() {
  if (!form.targetKey) {
    form.order_id_wsh = null
    form.merchant_id_wsh = null
    form.target_id_wsh = null
    form.target_type_wsh = 'merchant'
    return
  }
  const [type, idStr] = form.targetKey.split(':')
  const id = Number(idStr)
  if (type === 'order') {
    const o = (targets.value.orders_wsh || []).find(x => x.id_wsh === id)
    form.order_id_wsh = id
    form.merchant_id_wsh = o?.merchant_id_wsh ?? null
    if (o?.keeper_id_wsh) {
      form.target_id_wsh = o.keeper_id_wsh
      form.target_type_wsh = 'keeper'
    } else {
      form.target_id_wsh = o?.merchant_id_wsh ?? null
      form.target_type_wsh = 'merchant'
    }
  } else if (type === 'merchant') {
    const m = (targets.value.merchants_wsh || []).find(x => x.id_wsh === id)
    form.order_id_wsh = null
    form.merchant_id_wsh = id
    form.target_id_wsh = id
    form.target_type_wsh = 'merchant'
  } else if (type === 'keeper') {
    const k = (targets.value.keepers_wsh || []).find(x => x.id_wsh === id)
    form.order_id_wsh = null
    form.merchant_id_wsh = k?.merchant_id_wsh ?? null
    form.target_id_wsh = id
    form.target_type_wsh = 'keeper'
  }
}

function onReportTargetChange() {
  const [type, idStr] = reportForm.targetKey.split(':')
  const id = Number(idStr)
  if (type === 'order') {
    const o = (targets.value.orders_wsh || []).find(x => x.id_wsh === id)
    reportForm.targetLabel = o ? `${o.order_no_wsh || ('订单 #' + o.id_wsh)} · ${o.merchant_name_wsh || '商家'}` : ''
  } else if (type === 'merchant') {
    const m = (targets.value.merchants_wsh || []).find(x => x.id_wsh === id)
    reportForm.targetLabel = m ? `商家 · ${m.name_wsh}` : ''
  } else if (type === 'keeper') {
    const k = (targets.value.keepers_wsh || []).find(x => x.id_wsh === id)
    reportForm.targetLabel = k ? `寄养师 · ${k.name_wsh}` : ''
  }
}

async function createComplaint() {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      title_wsh: form.title_wsh,
      content_wsh: form.content_wsh,
      order_id_wsh: form.order_id_wsh || undefined,
      merchant_id_wsh: form.merchant_id_wsh || undefined,
      target_id_wsh: form.target_id_wsh || undefined,
      target_type_wsh: form.target_type_wsh,
    }
    const response = await apiCreateComplaint(payload)
    if (response.code === 200) {
      appStore.addToast('提交成功', 'success')
      showForm.value = false
      Object.assign(form, defaultComplaintForm())
      applyRouteContext()
      await loadComplaints()
    } else {
      appStore.addToast(response.message || '提交失败', 'error')
    }
  } catch (error) {
    appStore.addToast(error?.message || '提交失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function submitReport() {
  if (!reportForm.targetKey) return
  try {
    const [type, idStr] = reportForm.targetKey.split(':')
    const response = await createReview({
      target_type_wsh: type,
      target_id_wsh: Number(idStr),
      reason_wsh: reportForm.reason_wsh,
    })
    if (response.code === 200) {
      appStore.addToast('举报成功', 'success')
      showReportForm.value = false
      Object.assign(reportForm, { targetKey: '', targetLabel: '', reason_wsh: '' })
    } else {
      appStore.addToast(response.message || '举报失败', 'error')
    }
  } catch {
    appStore.addToast('举报失败', 'error')
  }
}

async function openDetail(c) {
  selectedComplaint.value = c
  detailMessages.value = []
  replyContent.value = ''
  messagesLoading.value = true
  try {
    const r = await getComplaintMessages(c.id_wsh)
    if (r.code === 200) detailMessages.value = r.data || []
  } catch (e) {
    appStore.addToast('沟通记录加载失败', 'error')
  } finally {
    messagesLoading.value = false
  }
}

async function sendReply() {
  if (!replyContent.value.trim() || replySubmitting.value) return
  replySubmitting.value = true
  try {
    const r = await sendComplaintMessage(selectedComplaint.value.id_wsh, replyContent.value.trim())
    if (r.code === 200) {
      detailMessages.value.push(r.data)
      replyContent.value = ''
    } else {
      appStore.addToast(r.message || '发送失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '发送失败', 'error')
  } finally {
    replySubmitting.value = false
  }
}

function formatTime(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString()
}

function parseImages(value) {
  if (!value) return []
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

function evidenceOrderNo(complaint) {
  return complaint.evidence_summary_wsh?.order_no_wsh || `#${complaint.order_id_wsh}`
}

function statusLabel(status) {
  return { resolved: '已处理', rejected: '已驳回', pending: '待处理', processing: '处理中' }[status] || status || '-'
}

function statusBadge(status) {
  return { resolved: 'badge-success', rejected: 'badge-danger', pending: 'badge-warning', processing: 'badge-info' }[status] || 'badge-info'
}

function orderStatusLabel(status) {
  return {
    pending: '待支付',
    confirmed: '已确认',
    paid: '已支付',
    in_progress: '服务中',
    completed: '已完成',
    cancelled: '已取消',
    refunded: '已退款',
  }[status] || status || ''
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
.form-hint {
  margin: 0 0 12px;
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
.complaint-card {
  cursor: pointer;
}
.complaint-card:hover {
  border-color: var(--color-primary);
}
.complaint-card p {
  margin: 10px 0 0;
  color: var(--color-muted-foreground);
}
.detail-hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--color-muted-foreground);
  text-align: right;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.detail-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
}
.detail-photo {
  display: inline-block;
  line-height: 0;
}
.detail-photo img {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: zoom-in;
}
.detail-content {
  margin: 0 0 14px;
  color: var(--color-muted-foreground);
}
.empty-msg {
  padding: 12px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.msg-row {
  display: flex;
  margin-bottom: 10px;
}
.msg-row--me {
  justify-content: flex-end;
}
.msg-bubble {
  max-width: 85%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-muted);
  color: var(--color-foreground);
  font-size: 14px;
}
.msg-row--me .msg-bubble {
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-card));
  border-color: color-mix(in srgb, var(--color-primary) 30%, var(--color-border));
}
.msg-meta {
  margin-bottom: 4px;
  font-size: 11px;
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
@media (max-width: 640px) {
  .order-context,
  .complaint-header {
    flex-direction: column;
  }
}
</style>
