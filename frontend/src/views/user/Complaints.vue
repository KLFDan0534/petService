<template>
  <div class="cp-page">
    <div class="cp-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="cp-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="cp-crumb-link">首页</router-link>
        <span class="cp-crumb-sep" aria-hidden="true">›</span>
        <span class="cp-crumb-here">我的投诉</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="cp-head">
        <div class="cp-head-copy">
          <p class="cp-eyebrow" aria-hidden="true">
            <span class="cp-eyebrow-line"></span>
            <span>Complaints</span>
          </p>
          <h1 class="cp-title">我的投诉</h1>
          <p class="cp-sub">投诉会由平台直接介入核查，48 小时内给出结论。门店无法自行关闭你的投诉。</p>
          <dl class="cp-facts" aria-label="投诉概览">
            <div>
              <dt>全部</dt>
              <dd>{{ loading ? '--' : complaints.length }}</dd>
            </div>
            <div class="cp-fact-bordered">
              <dt>处理中</dt>
              <dd>{{ loading ? '--' : complaints.filter(c => c.status_wsh === 'pending' || c.status_wsh === 'processing').length }}</dd>
            </div>
          </dl>
        </div>
        <div class="cp-actions">
          <button type="button" class="cta cta-primary" @click="openComplaintForm">发起投诉</button>
          <button type="button" class="cta cta-outline" @click="showReportForm = !showReportForm">举报对象</button>
        </div>
      </header>

      <!-- ═══ 关联订单上下文 ═══ -->
      <section v-if="routeOrderId" class="cp-order-ctx" aria-label="当前关联订单">
        <div class="cp-order-ctx-copy">
          <span class="cp-order-ctx-dot" aria-hidden="true"></span>
          <div class="cp-order-ctx-text">
            <p class="cp-order-ctx-label">当前关联订单</p>
            <p class="cp-order-ctx-name">{{ route.query.orderNo || `#${routeOrderId}` }}</p>
          </div>
        </div>
        <router-link class="cta cta-outline cta-sm" :to="{ name: 'OrderDetail', params: { id: routeOrderId } }">
          查看订单
        </router-link>
      </section>

      <!-- ═══ 举报对象 ═══ -->
      <section v-if="showReportForm" class="cp-report" aria-label="举报对象">
        <div class="cp-report-head">
          <p class="cp-eyebrow">
            <span class="cp-line" aria-hidden="true"></span>
            <span>Report</span>
          </p>
          <h2 class="cp-report-title">举报对象</h2>
          <p class="cp-report-desc">选择要举报的订单、商家或寄养师，平台管理员将进行审核。</p>
        </div>
        <form @submit.prevent="submitReport">
          <div class="cp-field">
            <label class="cp-label" for="cp-report-target">举报对象</label>
            <select id="cp-report-target" v-model="reportForm.targetKey" class="cp-input" required @change="onReportTargetChange">
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
            <small v-if="targetsLoading" class="cp-hint">正在加载可选对象...</small>
          </div>
          <div v-if="reportForm.targetLabel" class="cp-field">
            <label class="cp-label" for="cp-report-label">举报目标</label>
            <input id="cp-report-label" class="cp-input" :value="reportForm.targetLabel" disabled>
          </div>
          <div class="cp-field">
            <label class="cp-label" for="cp-report-reason">原因</label>
            <textarea id="cp-report-reason" v-model="reportForm.reason_wsh" class="cp-input cp-textarea" rows="3" required placeholder="请描述违规原因"></textarea>
          </div>
          <div class="cp-form-actions">
            <button type="button" class="cta cta-outline" @click="showReportForm = false">取消</button>
            <button type="submit" class="cta cta-primary">提交举报</button>
          </div>
        </form>
      </section>

      <!-- ═══ 01 · 投诉记录 ═══ -->
      <section class="cp-section" aria-label="投诉记录">
        <header class="cp-sec-head">
          <div class="cp-sec-copy">
            <p class="cp-eyebrow cp-sec-eyebrow">
              <span class="cp-idx">01</span>
              <span class="cp-line" aria-hidden="true"></span>
              <span>Cases</span>
            </p>
            <h2 class="cp-sec-title">投诉记录</h2>
            <p class="cp-sec-desc">点开任意一条查看凭证与平台沟通记录。</p>
          </div>
          <div class="cp-filters" role="group" aria-label="投诉状态筛选">
            <button
              v-for="tab in statusTabs"
              :key="tab.key"
              type="button"
              :aria-pressed="statusFilter === tab.key"
              class="cp-chip"
              :class="{ active: statusFilter === tab.key }"
              :disabled="loading"
              @click="statusFilter = tab.key"
            >{{ tab.label }} <span class="cp-chip-count tabular">{{ statusCounts[tab.key] }}</span></button>
          </div>
        </header>

        <div class="cp-body">
          <div v-if="loading" class="cp-skel-stack">
            <div v-for="i in 2" :key="i" class="cp-skeleton" />
          </div>

          <div v-else-if="filteredComplaints.length === 0" class="cp-empty">
            <p class="cp-empty-icon" aria-hidden="true">📢</p>
            <p class="cp-empty-title">{{ complaints.length ? '该状态下暂无投诉' : '没有投诉记录' }}</p>
            <p class="cp-empty-desc">{{ complaints.length ? '换一个状态看看。' : '这是好事。如果服务没达到预期，随时可以在这里发起投诉。' }}</p>
            <button v-if="complaints.length" type="button" class="cta cta-outline" @click="statusFilter = 'all'">查看全部</button>
            <button v-else type="button" class="cta cta-primary" @click="openComplaintForm">发起投诉</button>
          </div>

          <div v-else class="cp-list">
            <article v-for="c in filteredComplaints" :key="c.id_wsh" class="cp-card" @click="openDetail(c)">
              <div class="cp-card-top">
                <div class="cp-card-main">
                  <div class="cp-card-title-row">
                    <h3 class="cp-card-title">{{ c.title_wsh }}</h3>
                    <span :class="['badge', statusBadge(c.status_wsh)]">{{ statusLabel(c.status_wsh) }}</span>
                  </div>
                  <p class="cp-card-meta">
                    <span class="cp-id">#{{ c.id_wsh ?? '-' }}</span>
                    <span class="cp-sep" aria-hidden="true">·</span>
                    <span>{{ formatTime(c.created_at_wsh) }}</span>
                    <template v-if="c.order_id_wsh">
                      <span class="cp-sep" aria-hidden="true">·</span>
                      <span>订单 {{ evidenceOrderNo(c) }}</span>
                    </template>
                  </p>
                </div>
                <div class="cp-card-tags">
                  <span v-if="c.order_no_wsh" class="cp-tag">{{ c.order_no_wsh }}</span>
                  <span v-if="c.merchant_name_wsh" class="cp-tag">{{ c.merchant_name_wsh }}</span>
                  <span v-if="c.keeper_name_wsh" class="cp-tag">照护师 · {{ c.keeper_name_wsh }}</span>
                </div>
              </div>

              <p v-if="c.content_wsh" class="cp-card-content">{{ c.content_wsh }}</p>

              <div v-if="c.evidence_summary_wsh" class="cp-card-evidence">
                聊天 {{ c.evidence_summary_wsh.chat_message_count_wsh || 0 }} 条 ·
                照护记录 {{ c.evidence_summary_wsh.care_record_count_wsh || 0 }} 条
              </div>

              <p v-if="c.result_wsh" class="cp-card-result">
                <span class="cp-result-label">平台结论 · </span>{{ c.result_wsh }}
              </p>

              <div class="cp-card-foot">
                <span class="text-link">
                  <span class="tl-text">查看处理进展</span>
                  <span class="tl-arrow" aria-hidden="true">→</span>
                </span>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>

    <!-- ═══════════════════════════════════════════
         投诉详情弹窗
         ═══════════════════════════════════════════ -->
    <div v-if="selectedComplaint" class="dlg-overlay" @click.self="selectedComplaint = null">
      <div class="dlg-panel dlg-panel-lg" role="dialog" aria-modal="true" aria-label="投诉详情">
        <div class="dlg-head">
          <div class="cp-detail-head">
            <h3 class="dlg-title">{{ selectedComplaint.title_wsh }}</h3>
            <span :class="['badge', statusBadge(selectedComplaint.status_wsh)]" class="cp-detail-badge">{{ statusLabel(selectedComplaint.status_wsh) }}</span>
          </div>
          <button type="button" class="dlg-close" aria-label="关闭" @click="selectedComplaint = null">✕</button>
        </div>
        <div class="dlg-body">
          <p class="cp-detail-content">{{ selectedComplaint.content_wsh }}</p>

          <div v-if="parseImages(selectedComplaint.images_wsh).length" class="cp-detail-photos">
            <a
              v-for="(url, index) in parseImages(selectedComplaint.images_wsh)"
              :key="url"
              :href="url"
              target="_blank"
              rel="noreferrer"
              class="cp-detail-photo"
            >
              <img :src="url" :alt="`投诉照片 ${index + 1}`" loading="lazy" @error="$event.target.style.display = 'none'">
            </a>
          </div>

          <p v-if="selectedComplaint.result_wsh" class="cp-detail-result">
            <strong>处理意见：</strong>{{ selectedComplaint.result_wsh }}
          </p>

          <div class="cp-thread">
            <h4 class="cp-thread-title">沟通记录</h4>
            <p v-if="messagesLoading" class="cp-thread-empty">加载中...</p>
            <p v-else-if="detailMessages.length === 0" class="cp-thread-empty">暂无消息，可在下方留言</p>
            <div v-for="m in detailMessages" :key="m.id_wsh" class="cp-msg" :class="{ 'cp-msg--me': m.from_user_id_wsh === currentUserId }">
              <div class="cp-msg-bubble">
                <div class="cp-msg-meta">
                  <strong>{{ m.from_user_name_wsh || '客服' }}</strong>
                  <span>{{ formatTime(m.created_at_wsh) }}</span>
                </div>
                <div>{{ m.content_wsh }}</div>
              </div>
            </div>
          </div>

          <div class="cp-field">
            <textarea v-model="replyContent" class="cp-input cp-textarea" rows="2" maxlength="1000" placeholder="回复客服..."></textarea>
          </div>
          <div class="cp-form-actions">
            <button class="cta cta-primary" type="button" :disabled="!replyContent.trim() || replySubmitting" @click="sendReply">
              {{ replySubmitting ? '发送中...' : '发送' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════
         提交投诉弹窗
         ═══════════════════════════════════════════ -->
    <div v-if="showForm" class="dlg-overlay" @click.self="closeComplaintForm">
      <div class="dlg-panel dlg-panel-form" role="dialog" aria-modal="true" aria-label="提交投诉">
        <div class="dlg-head">
          <h3 class="dlg-title">提交投诉</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="closeComplaintForm">✕</button>
        </div>
        <form @submit.prevent="createComplaint">
          <div class="dlg-body">
            <div class="dlg-field">
              <label class="dlg-label" for="cp-target">投诉对象</label>
              <select id="cp-target" v-model="form.targetKey" class="dlg-input" required @change="syncFromTargetKey">
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
              <small v-if="targetsLoading" class="cp-hint">正在加载可选对象...</small>
              <small v-else-if="targetEmpty" class="cp-hint">暂无可投诉的订单或商家，可在完成一次服务后进行反馈。</small>
            </div>
            <div v-if="form.order_id_wsh" class="cp-linked">
              <span>关联订单</span>
              <strong>{{ route.query.orderNo || `#${form.order_id_wsh}` }}</strong>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="cp-title">标题</label>
              <input id="cp-title" v-model="form.title_wsh" class="dlg-input" required maxlength="100" placeholder="一句话概括问题">
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="cp-content">问题描述</label>
              <textarea id="cp-content" v-model="form.content_wsh" class="dlg-textarea" rows="4" required maxlength="2000" placeholder="请描述发生了什么、您的诉求"></textarea>
            </div>
          </div>
          <div class="dlg-foot">
            <button type="button" class="dlg-cancel" @click="closeComplaintForm">取消</button>
            <button type="submit" class="cta cta-primary" :disabled="submitting">
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

const statusFilter = ref('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待处理' },
  { key: 'processing', label: '处理中' },
  { key: 'resolved', label: '已处理' },
  { key: 'rejected', label: '已驳回' },
]
const statusCounts = computed(() => {
  const all = complaints.value
  return {
    all: all.length,
    pending: all.filter((c) => c.status_wsh === 'pending').length,
    processing: all.filter((c) => c.status_wsh === 'processing').length,
    resolved: all.filter((c) => c.status_wsh === 'resolved').length,
    rejected: all.filter((c) => c.status_wsh === 'rejected').length,
  }
})
const filteredComplaints = computed(() => {
  if (statusFilter.value === 'all') return complaints.value
  return complaints.value.filter((c) => c.status_wsh === statusFilter.value)
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
/* ═══════════════════════════════════════════════════════
   Editorial warm — uses shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode is handled globally
   via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.cp-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.cp-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.cp-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.cp-crumb-link { color: var(--ref-muted); text-decoration: none; }
.cp-crumb-link:hover { color: var(--ref-ink); }
.cp-crumb-sep { color: var(--ref-line); }
.cp-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.cp-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.cp-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.cp-idx { font-variant-numeric: tabular-nums; }
.cp-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Header ═══ */
.cp-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 30px;
}
.cp-head-copy { min-width: 0; }
.cp-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.cp-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.cp-facts {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0 28px;
  margin: 28px 0 0;
}
.cp-facts div { display: flex; flex-direction: column; }
.cp-fact-bordered { border-left: 1px solid var(--ref-line); padding-left: 28px; }
.cp-facts dt {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.cp-facts dd {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 26px;
  font-weight: 400;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: var(--ref-ink);
}
.cp-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ Filter chips ═══ */
.cp-filters { display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.cp-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.cp-chip:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: color-mix(in srgb, var(--ref-sand) 60%, transparent); }
.cp-chip:disabled { opacity: 0.6; cursor: not-allowed; }
.cp-chip.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }
.cp-chip-count { opacity: 0.72; font-variant-numeric: tabular-nums; font-size: 11px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-sm { height: 38px; padding: 0 16px; font-size: 12.5px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Text link ═══ */
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
  text-decoration: none;
  transition: color 0.15s;
  cursor: pointer;
}
.text-link:hover { color: var(--ref-brand); }
.tl-text { border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent); padding-bottom: 2px; }
.text-link:hover .tl-text { border-color: var(--ref-brand); }
.tl-arrow { transition: transform 0.15s; }
.text-link:hover .tl-arrow { transform: translateX(4px); }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-warning { background: var(--ref-brand); color: #fff; }
.badge-info {
  background: var(--ref-surface);
  color: var(--ref-ink);
  border-color: color-mix(in srgb, var(--ref-ink) 20%, transparent);
}
.badge-success {
  background: color-mix(in srgb, var(--color-success) 13%, var(--ref-surface));
  color: color-mix(in srgb, var(--color-success) 72%, var(--ref-ink));
  border-color: color-mix(in srgb, var(--color-success) 28%, transparent);
}
.badge-secondary { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-danger,
.badge-error {
  background: color-mix(in srgb, var(--color-danger) 11%, var(--ref-surface));
  color: color-mix(in srgb, var(--color-danger) 78%, var(--ref-ink));
  border-color: color-mix(in srgb, var(--color-danger) 28%, transparent);
}

/* ═══ Order context ═══ */
.cp-order-ctx {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 20px;
  margin-top: 8px;
  padding: 16px 20px;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 26%, var(--ref-line));
  border-radius: 16px;
  background: color-mix(in srgb, var(--ref-brand) 7%, var(--ref-surface));
}
.cp-order-ctx-copy { display: flex; align-items: center; gap: 14px; min-width: 0; }
.cp-order-ctx-dot {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--ref-brand);
}
.cp-order-ctx-text { min-width: 0; }
.cp-order-ctx-label {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ref-brand-deep);
}
.cp-order-ctx-name {
  margin: 4px 0 0;
  font-family: var(--ref-font-display);
  font-size: 16px;
  font-weight: 500;
  color: var(--ref-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ═══ Report card ═══ */
.cp-report {
  margin-top: 20px;
  padding: 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
}
.cp-report-head { margin-bottom: 20px; }
.cp-report-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.cp-report-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.cp-field { display: grid; gap: 8px; }
.cp-field + .cp-field { margin-top: 14px; }
.cp-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.cp-input,
.cp-textarea {
  width: 100%;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.cp-input { height: 42px; padding: 0 14px; }
.cp-textarea { padding: 12px 14px; resize: vertical; line-height: 1.6; }
.cp-input::placeholder,
.cp-textarea::placeholder { color: var(--ref-muted); }
.cp-input:focus,
.cp-textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.cp-input:disabled { background: color-mix(in srgb, var(--ref-sand) 55%, var(--ref-surface)); color: var(--ref-muted); }
.cp-hint {
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--ref-muted);
}
.cp-form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }

/* ═══ Section ═══ */
.cp-section { margin-top: 48px; }
.cp-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.cp-sec-copy { min-width: 0; }
.cp-sec-eyebrow { margin: 0; }
.cp-sec-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.cp-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Body / skeleton / empty ═══ */
.cp-body { margin-top: 24px; }
.cp-skel-stack { display: grid; gap: 14px; }
.cp-skeleton {
  height: 190px;
  border-radius: 16px;
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: cp-shimmer 1.3s linear infinite;
}
.cp-empty {
  padding: 64px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  text-align: center;
}
.cp-empty-icon { margin: 0 0 14px; font-size: 38px; }
.cp-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.cp-empty-desc { margin: 10px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.cp-empty .cta { margin-top: 22px; }

/* ═══ Cards ═══ */
.cp-list { display: grid; gap: 14px; }
.cp-card {
  padding: 22px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  cursor: pointer;
  transition: border-color 0.18s, transform 0.18s, box-shadow 0.18s;
}
.cp-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.cp-card-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 24px;
}
.cp-card-main { min-width: 0; flex: 1; }
.cp-card-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.cp-card-title {
  margin: 0;
  min-width: 0;
  font-family: var(--ref-font-display);
  font-size: 18px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  transition: color 0.15s;
}
.cp-card:hover .cp-card-title { color: var(--ref-brand); }
.cp-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
}
.cp-id { font-variant-numeric: tabular-nums; }
.cp-sep { color: var(--ref-line); }
.cp-card-tags { display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.cp-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
  white-space: nowrap;
}
.cp-card-content {
  margin: 14px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cp-card-evidence {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ref-muted);
}
.cp-card-result {
  margin: 12px 0 0;
  padding: 12px 16px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.cp-result-label { color: var(--ref-muted); }
.cp-card-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--ref-line);
}

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(10, 8, 6, 0.5);
  backdrop-filter: blur(2px);
  animation: cp-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 440px;
  max-height: min(90vh, 760px);
  overflow-y: auto;
  border-radius: 20px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent);
  animation: cp-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-panel-lg { max-width: 640px; }
.dlg-panel-form { max-width: 560px; }
.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px 0;
}
.dlg-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.dlg-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--ref-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-input,
.dlg-textarea {
  width: 100%;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-input { height: 42px; padding: 0 14px; }
.dlg-textarea { padding: 12px 14px; resize: vertical; line-height: 1.6; }
.dlg-input::placeholder,
.dlg-textarea::placeholder { color: var(--ref-muted); }
.dlg-input:focus,
.dlg-textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.dlg-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══ Complaint detail ═══ */
.cp-detail-head { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; min-width: 0; }
.cp-detail-badge { flex-shrink: 0; }
.cp-detail-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ref-ink-soft);
}
.cp-detail-photos { display: flex; flex-wrap: wrap; gap: 10px; }
.cp-detail-photo { display: inline-block; line-height: 0; }
.cp-detail-photo img {
  width: 128px;
  height: 128px;
  object-fit: cover;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  cursor: zoom-in;
  transition: transform 0.2s;
}
.cp-detail-photo img:hover { transform: scale(1.04); }
.cp-detail-result {
  margin: 0;
  padding: 12px 16px;
  border: 1px solid color-mix(in srgb, var(--color-success) 28%, var(--ref-line));
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-success) 8%, var(--ref-surface));
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.cp-thread { display: grid; gap: 10px; }
.cp-thread-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
}
.cp-thread-empty {
  margin: 0;
  padding: 12px;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.cp-msg { display: flex; }
.cp-msg--me { justify-content: flex-end; }
.cp-msg-bubble {
  max-width: 85%;
  padding: 10px 12px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-sand) 55%, var(--ref-surface));
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.7;
}
.cp-msg--me .cp-msg-bubble {
  background: color-mix(in srgb, var(--ref-brand) 10%, var(--ref-surface));
  border-color: color-mix(in srgb, var(--ref-brand) 26%, var(--ref-line));
}
.cp-msg-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--ref-muted);
}
.cp-msg-meta strong { color: var(--ref-ink-soft); }

/* ═══ Linked order ═══ */
.cp-linked {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-sand) 45%, var(--ref-surface));
}
.cp-linked span { font-size: 12px; color: var(--ref-muted); }
.cp-linked strong {
  font-family: var(--ref-font-display);
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
}

/* ═══ Animations ═══ */
@keyframes cp-shimmer { to { background-position: -200% 0; } }
@keyframes cp-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes cp-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ Responsive ═══ */
@media (max-width: 760px) {
  .cp-card-top { flex-direction: column; }
}
@media (max-width: 560px) {
  .cp-shell { padding: 0 16px; }
  .cp-head { padding: 30px 0 22px; }
  .cp-sub { font-size: 13.5px; }
  .cp-facts { gap: 0 20px; }
  .cp-fact-bordered { padding-left: 20px; }
  .cp-facts dd { font-size: 22px; }
  .cp-actions { width: 100%; }
  .cp-actions .cta { flex: 1; }
  .cp-report { padding: 20px 18px; }
  .cp-card { padding: 18px 18px; }
  .cp-card-foot { justify-content: flex-start; }
}
@media (prefers-reduced-motion: reduce) {
  .cp-skeleton { animation: none; }
}
</style>
