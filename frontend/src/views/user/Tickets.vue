<template>
  <div class="tk-page">
    <div class="tk-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="tk-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="tk-crumb-link">首页</router-link>
        <span class="tk-crumb-sep" aria-hidden="true">›</span>
        <span class="tk-crumb-here">我的工单</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="tk-head">
        <div class="tk-head-copy">
          <p class="tk-eyebrow" aria-hidden="true">
            <span class="tk-eyebrow-line"></span>
            <span>服务工单</span>
          </p>
          <h1 class="tk-title">我的工单</h1>
          <p class="tk-sub">订单申诉、使用问题与产品建议都在这里跟进。客服的每一次回复都留在工单里，可随时回看。</p>
          <dl class="tk-facts" aria-label="工单概览">
            <div>
              <dt>全部</dt>
              <dd>{{ loading ? '--' : tickets.length }}</dd>
            </div>
            <div class="tk-fact-bordered">
              <dt>未结</dt>
              <dd>{{ loading ? '--' : tickets.filter(t => t.status_wsh === 'pending' || t.status_wsh === 'processing').length }}</dd>
            </div>
          </dl>
        </div>
        <div class="tk-actions">
          <button type="button" class="cta cta-primary" @click="showForm = true">提交工单</button>
        </div>
      </header>

      <!-- ═══ 01 · 工单列表 ═══ -->
      <section class="tk-section" aria-label="工单列表">
        <header class="tk-sec-head">
          <div class="tk-sec-copy">
            <p class="tk-eyebrow tk-sec-eyebrow">
              <span class="tk-idx">01</span>
              <span class="tk-line" aria-hidden="true"></span>
              <span>工单</span>
            </p>
            <h2 class="tk-sec-title">工单列表</h2>
            <p class="tk-sec-desc">点开任意一条查看完整沟通记录。</p>
          </div>
        </header>

        <div class="tk-body">
          <div v-if="loading" class="tk-skel-stack">
            <div v-for="i in 3" :key="i" class="tk-skeleton" />
          </div>

          <div v-else-if="tickets.length === 0" class="tk-empty">
            <p class="tk-empty-icon" aria-hidden="true">🎫</p>
            <p class="tk-empty-title">还没有提交过工单</p>
            <p class="tk-empty-desc">遇到订单或使用上的问题，提交工单后客服会在这里回复你。</p>
            <button type="button" class="cta cta-primary" @click="showForm = true">提交工单</button>
          </div>

          <div v-else class="tk-list">
            <article
              v-for="t in tickets"
              :key="t.id_wsh"
              class="tk-card"
              @click="selectTicket(t)"
            >
              <div class="tk-card-top">
                <div class="tk-card-main">
                  <div class="tk-card-title-row">
                    <h3 class="tk-card-title">{{ t.title_wsh }}</h3>
                    <span :class="['badge', statusBadge(t.status_wsh)]">{{ statusLabel(t.status_wsh) }}</span>
                  </div>
                  <p class="tk-card-meta">
                    <span class="tk-id">#{{ t.id_wsh ?? '-' }}</span>
                    <span class="tk-sep" aria-hidden="true">·</span>
                    <span>{{ new Date(t.created_at_wsh).toLocaleString() }}</span>
                  </p>
                </div>
                <div class="tk-card-tags">
                  <span class="tk-tag">{{ categoryLabel(t.category_wsh) }}</span>
                  <span v-if="t.priority_wsh === 'high'" class="tk-urgent">{{ priorityLabel(t.priority_wsh) }}</span>
                  <span v-else class="tk-tag">{{ priorityLabel(t.priority_wsh) }}</span>
                </div>
              </div>

              <p v-if="t.content_wsh" class="tk-card-content">{{ t.content_wsh }}</p>

              <p v-if="t.result_wsh" class="tk-card-result">
                <span class="tk-result-label">处理结论 · </span>{{ t.result_wsh }}
              </p>

              <div class="tk-card-foot">
                <span class="text-link">
                  <span class="tl-text">查看沟通记录</span>
                  <span class="tl-arrow" aria-hidden="true">→</span>
                </span>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>

    <!-- ═══════════════════════════════════════════
         新建工单弹窗
         ═══════════════════════════════════════════ -->
    <div v-if="showForm" class="dlg-overlay" @click.self="showForm = false">
      <div class="dlg-panel" role="dialog" aria-modal="true" aria-label="提交工单">
        <div class="dlg-head">
          <h3 class="dlg-title">提交工单</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="showForm = false">✕</button>
        </div>
        <form @submit.prevent="createTicket">
          <div class="dlg-body">
            <div class="dlg-field">
              <label class="dlg-label" for="tk-title">问题标题</label>
              <input id="tk-title" v-model="form.title_wsh" class="dlg-input" required placeholder="例如：寄养期间监控画面无法查看">
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="tk-category">工单类型</label>
              <select id="tk-category" v-model="form.category_wsh" class="dlg-input" required>
                <option value="appeal">订单申诉</option>
                <option value="complaint">服务投诉</option>
                <option value="other">其他</option>
              </select>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="tk-priority">紧急程度</label>
              <select id="tk-priority" v-model="form.priority_wsh" class="dlg-input" required>
                <option value="medium">普通</option>
                <option value="high">紧急</option>
                <option value="low">低</option>
              </select>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="tk-merchant">关联门店</label>
              <select id="tk-merchant" v-model="form.merchant_id_wsh" class="dlg-input" required>
                <option :value="null" disabled>请选择商家</option>
                <option v-for="m in approvedMerchants" :key="m.id_wsh" :value="m.id_wsh">{{ m.name_wsh }}</option>
              </select>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="tk-content">详细说明</label>
              <textarea id="tk-content" v-model="form.content_wsh" class="dlg-textarea" rows="4" required placeholder="发生了什么、涉及哪笔订单、期望怎么处理"></textarea>
            </div>
          </div>
          <div class="dlg-foot">
            <button type="button" class="dlg-cancel" @click="showForm = false">取消</button>
            <button type="submit" class="cta cta-primary">提交工单</button>
          </div>
        </form>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════
         工单详情弹窗
         ═══════════════════════════════════════════ -->
    <div v-if="selectedTicket" class="dlg-overlay" @click.self="selectedTicket = null">
      <div class="dlg-panel dlg-panel-lg" role="dialog" aria-modal="true" aria-label="工单详情">
        <div class="dlg-head">
          <h3 class="dlg-title">{{ selectedTicket.title_wsh }}</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="selectedTicket = null">✕</button>
        </div>
        <div class="dlg-body">
          <div class="tk-detail-badges">
            <span :class="['badge', categoryBadge(selectedTicket.category_wsh)]">{{ categoryLabel(selectedTicket.category_wsh) }}</span>
            <span :class="['badge', statusBadge(selectedTicket.status_wsh)]">{{ statusLabel(selectedTicket.status_wsh) }}</span>
            <span :class="['badge', 'badge-info']">{{ priorityLabel(selectedTicket.priority_wsh) }}</span>
          </div>

          <p class="tk-detail-content">{{ selectedTicket.content_wsh }}</p>

          <p v-if="selectedTicket.status_wsh === 'resolved' && selectedTicket.result_wsh" class="tk-detail-result">
            <strong>处理结果：</strong>{{ selectedTicket.result_wsh }}
          </p>

          <div class="tk-thread">
            <h4 class="tk-thread-title">沟通记录</h4>
            <p v-if="messages.length === 0" class="tk-thread-empty">暂无消息</p>
            <div v-for="m in messages" :key="m.id_wsh" class="tk-msg" :class="{ 'tk-msg--me': m.user_id_wsh === currentUserId }">
              <div class="tk-msg-bubble">
                <div class="tk-msg-meta">
                  <strong>{{ m.user_id_wsh === currentUserId ? '我' : '客服' }}</strong>
                  <span>{{ new Date(m.created_at_wsh).toLocaleString() }}</span>
                </div>
                <div>{{ m.content_wsh }}</div>
              </div>
            </div>
          </div>

          <div v-if="editing" class="tk-evidence">
            <label class="dlg-label" for="tk-evidence">上传证据 / 回复</label>
            <textarea id="tk-evidence" v-model="evidenceContent" class="dlg-textarea" rows="3" placeholder="请输入证据描述或回复内容"></textarea>
          </div>
          <div class="tk-evidence-actions">
            <button v-if="!editing" type="button" class="cta cta-outline" @click="editing = true">上传证据</button>
            <template v-if="editing">
              <button type="button" class="cta cta-primary" :disabled="!evidenceContent.trim()" @click="submitEvidence">提交</button>
              <button type="button" class="cta cta-outline" @click="cancelEvidence">取消</button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyTickets, createTicket as apiCreateTicket, getTicketMessages, sendTicketMessage } from '@/api/ticket'
import { getMerchants } from '@/api/merchant'
import { TicketStatus, TicketCategoryMap, getStatusLabel, getStatusBadge } from '@/constants/statusMaps'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const authStore = useAuthStore()
const appStore = useAppStore()
const tickets = ref([])
const loading = ref(true)
const showForm = ref(false)
const selectedTicket = ref(null)
const messages = ref([])
const editing = ref(false)
const evidenceContent = ref('')
const approvedMerchants = ref([])

const form = reactive({ title_wsh: '', content_wsh: '', category_wsh: 'appeal', priority_wsh: 'medium', merchant_id_wsh: null })

const currentUserId = authStore.user?.id_wsh

function categoryLabel(cat) { return getStatusLabel(TicketCategoryMap, cat) }
function categoryBadge(cat) { return getStatusBadge(TicketCategoryMap, cat) }

function priorityLabel(p) {
  return { high: '紧急', medium: '普通', low: '低' }[p] || p
}

function statusLabel(s) { return getStatusLabel(TicketStatus, s) }
function statusBadge(s) { return getStatusBadge(TicketStatus, s) }

onMounted(async () => {
  try {
    const r = await getMyTickets()
    if (r.code === 200) tickets.value = r.data
  } catch (e) {}
  finally { loading.value = false }
  try {
    const m = await getMerchants()
    if (m.code === 200) approvedMerchants.value = (m.data || []).filter(x => x.status_wsh === 1)
  } catch (e) {}
})

async function createTicket() {
  try {
    const r = await apiCreateTicket({ ...form })
    if (r.code === 200) {
      appStore.addToast('创建成功', 'success')
      showForm.value = false
      tickets.value.push(r.data)
      form.title_wsh = ''; form.content_wsh = ''; form.category_wsh = 'appeal'; form.priority_wsh = 'medium'; form.merchant_id_wsh = null
    }
  } catch (e) { appStore.addToast('创建失败', 'error') }
}

async function selectTicket(t) {
  selectedTicket.value = t
  editing.value = false
  evidenceContent.value = ''
  messages.value = []
  try {
    const r = await getTicketMessages(t.id_wsh)
    if (r.code === 200) messages.value = r.data
  } catch (e) {}
}

async function submitEvidence() {
  try {
    const r = await sendTicketMessage(selectedTicket.value.id_wsh, { content_wsh: evidenceContent.value })
    if (r.code === 200) {
      appStore.addToast('证据已提交', 'success')
      messages.value.push(r.data)
      evidenceContent.value = ''
      editing.value = false
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}

function cancelEvidence() {
  editing.value = false
  evidenceContent.value = ''
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Editorial warm — uses shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode is handled globally
   via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.tk-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.tk-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.tk-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.tk-crumb-link { color: var(--ref-muted); text-decoration: none; }
.tk-crumb-link:hover { color: var(--ref-ink); }
.tk-crumb-sep { color: var(--ref-line); }
.tk-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.tk-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.tk-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.tk-idx { font-variant-numeric: tabular-nums; }
.tk-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Header ═══ */
.tk-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 30px;
}
.tk-head-copy { min-width: 0; }
.tk-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.tk-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.tk-facts {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0 28px;
  margin: 28px 0 0;
}
.tk-facts div { display: flex; flex-direction: column; }
.tk-fact-bordered { border-left: 1px solid var(--ref-line); padding-left: 28px; }
.tk-facts dt {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.tk-facts dd {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 26px;
  font-weight: 400;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: var(--ref-ink);
}
.tk-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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
  border-radius: var(--radius-inline);
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

/* ═══ Section ═══ */
.tk-section { margin-top: 44px; }
.tk-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.tk-sec-copy { min-width: 0; }
.tk-sec-eyebrow { margin: 0; }
.tk-sec-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.tk-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Body / skeleton / empty ═══ */
.tk-body { margin-top: 24px; }
.tk-skel-stack { display: grid; gap: 14px; }
.tk-skeleton {
  height: 168px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: tk-shimmer 1.3s linear infinite;
}
.tk-empty {
  padding: 64px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  text-align: center;
}
.tk-empty-icon { margin: 0 0 14px; font-size: 38px; }
.tk-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.tk-empty-desc { margin: 10px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.tk-empty .cta { margin-top: 22px; }

/* ═══ Cards ═══ */
.tk-list { display: grid; gap: 14px; }
.tk-card {
  padding: 22px 24px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
  cursor: pointer;
  transition: border-color 0.18s, transform 0.18s, box-shadow 0.18s;
}
.tk-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lift);
}
.tk-card-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 24px;
}
.tk-card-main { min-width: 0; flex: 1; }
.tk-card-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.tk-card-title {
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
.tk-card:hover .tk-card-title { color: var(--ref-brand); }
.tk-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
}
.tk-id { font-variant-numeric: tabular-nums; }
.tk-sep { color: var(--ref-line); }
.tk-card-tags { display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.tk-tag {
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-inline);
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
  white-space: nowrap;
}
.tk-urgent {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ref-brand-deep);
}
.tk-card-content {
  margin: 14px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tk-card-result {
  margin: 14px 0 0;
  padding: 12px 16px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-cream) 55%, var(--ref-surface));
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.tk-result-label { color: var(--ref-muted); }
.tk-card-foot {
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
  animation: tk-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 440px;
  max-height: min(90vh, 720px);
  overflow-y: auto;
  border-radius: 20px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: var(--shadow-pop);
  animation: tk-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-panel-lg { max-width: 600px; }
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
.dlg-input {
  height: 42px;
  padding: 0 14px;
}
.dlg-textarea {
  padding: 12px 14px;
  resize: vertical;
  line-height: 1.6;
}
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

/* ═══ Ticket detail ═══ */
.tk-detail-badges { display: flex; flex-wrap: wrap; gap: 8px; }
.tk-detail-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ref-ink-soft);
}
.tk-detail-result {
  margin: 0;
  padding: 12px 16px;
  border: 1px solid color-mix(in srgb, var(--color-success) 28%, var(--ref-line));
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-success) 8%, var(--ref-surface));
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.tk-thread { display: grid; gap: 10px; }
.tk-thread-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
}
.tk-thread-empty {
  margin: 0;
  padding: 12px;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.tk-msg { display: flex; }
.tk-msg--me { justify-content: flex-end; }
.tk-msg-bubble {
  max-width: 85%;
  padding: 10px 12px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-sand) 55%, var(--ref-surface));
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.7;
}
.tk-msg--me .tk-msg-bubble {
  background: color-mix(in srgb, var(--ref-brand) 10%, var(--ref-surface));
  border-color: color-mix(in srgb, var(--ref-brand) 26%, var(--ref-line));
}
.tk-msg-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--ref-muted);
}
.tk-msg-meta strong { color: var(--ref-ink-soft); }
.tk-evidence { display: grid; gap: 8px; }
.tk-evidence-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ Animations ═══ */
@keyframes tk-shimmer { to { background-position: -200% 0; } }
@keyframes tk-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes tk-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ Responsive ═══ */
@media (max-width: 760px) {
  .tk-card-top { flex-direction: column; }
}
@media (max-width: 560px) {
  .tk-shell { padding: 0 16px; }
  .tk-head { padding: 30px 0 22px; }
  .tk-sub { font-size: 13.5px; }
  .tk-facts { gap: 0 20px; }
  .tk-fact-bordered { padding-left: 20px; }
  .tk-facts dd { font-size: 22px; }
  .tk-card { padding: 18px 18px; }
  .tk-actions { width: 100%; }
  .tk-actions .cta { flex: 1; }
  .tk-card-foot { justify-content: flex-start; }
}
@media (prefers-reduced-motion: reduce) {
  .tk-skeleton { animation: none; }
}
</style>
