<template>
  <div class="ag-page">
    <div class="ag-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ag-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ag-crumb-link">首页</router-link>
        <span class="ag-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/ai" class="ag-crumb-link">AI 助手</router-link>
        <span class="ag-crumb-sep" aria-hidden="true">›</span>
        <span class="ag-crumb-here">智能下单</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ag-head">
        <div class="ag-head-copy">
          <div class="ag-eyebrow" aria-hidden="true">
            <span class="ag-eyebrow-line"></span>
            <span>行动</span>
          </div>
          <h1 class="ag-title">智能下单</h1>
          <p class="ag-sub">像聊天一样把寄养方案聊清楚：说一句就更新一版，宠物、日期、门店都会记住。只有你点「确认下单」，才会真正创建订单。</p>
        </div>
        <div class="ag-actions">
          <router-link to="/orders" class="cta cta-outline">查看我的订单</router-link>
        </div>
      </header>

      <!-- ═══ 会话区 ═══ -->
      <div class="ag-chat">
        <!-- 左：历史会话 -->
        <aside class="ag-side" aria-label="历史会话">
          <button type="button" class="ag-new" :disabled="sending || confirming" @click="newSession">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            新会话
          </button>
          <p class="ag-side-label">历史会话</p>
          <ul class="ag-side-list">
            <li
              v-for="item in sessions"
              :key="item.session_id_wsh"
              :class="['ag-side-item', { 'is-active': item.session_id_wsh === sessionId }]"
            >
              <button type="button" class="ag-side-open" @click="selectSession(item)">
                <span class="ag-side-title">{{ item.title_wsh }}</span>
                <span class="ag-side-meta">
                  <span :class="['ag-dot', item.status_wsh === 'CONFIRMED' ? 'is-done' : item.status_wsh === 'AWAIT_CONFIRM' ? 'is-ready' : '']"></span>
                  {{ statusText(item.status_wsh) }}
                </span>
              </button>
              <button type="button" class="ag-side-del" title="删除会话" @click.stop="removeSession(item)">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </li>
            <li v-if="!sessions.length" class="ag-side-empty">还没有会话，说一句话就会自动保存。</li>
          </ul>
        </aside>

        <!-- 右：对话流 -->
        <section class="ag-main" aria-label="对话">
          <div ref="streamRef" class="ag-stream">
            <div v-if="!messages.length" class="ag-hello">
              <p class="ag-hello-title">先说说你的需求</p>
              <p class="ag-hello-desc">写清宠物、时间与偏好门店，匹配会更准。之后想改，直接说「改成 5 晚」就行。</p>
              <div class="ag-examples">
                <button v-for="example in examples" :key="example" type="button" class="ag-chip" @click="useExample(example)">
                  {{ example }}
                </button>
              </div>
            </div>

            <template v-for="(item, index) in messages" :key="index">
              <!-- 文本气泡 -->
              <div v-if="item.kind === 'text'" :class="['ag-msg', item.role]">
                <span class="ag-avatar" aria-hidden="true">{{ item.role === 'user' ? '我' : 'AI' }}</span>
                <div class="ag-bubble">
                  <p class="ag-bubble-text">{{ item.content }}</p>
                </div>
              </div>

              <!-- 方案卡片 -->
              <div v-else-if="item.kind === 'plan'" class="ag-plan">
                <header class="ag-plan-head">
                  <span class="ag-plan-tag">方案</span>
                  <span class="ag-plan-hint">核对无误再确认；想改直接说，例如「改成 5 晚」</span>
                </header>
                <dl class="ag-plan-grid">
                  <div class="ag-plan-cell">
                    <dt>宠物</dt>
                    <dd>{{ item.plan.pet_name_wsh || '—' }}</dd>
                  </div>
                  <div class="ag-plan-cell">
                    <dt>商家</dt>
                    <dd>{{ item.plan.merchant_name_wsh || '—' }}</dd>
                  </div>
                  <div class="ag-plan-cell">
                    <dt>看护人</dt>
                    <dd>{{ item.plan.keeper_name_wsh || '—' }}</dd>
                  </div>
                  <div class="ag-plan-cell">
                    <dt>服务</dt>
                    <dd>{{ item.plan.service_name_wsh || '—' }}</dd>
                  </div>
                  <div class="ag-plan-cell ag-plan-wide">
                    <dt>日期</dt>
                    <dd class="tabular">{{ item.plan.start_date_wsh }} ~ {{ item.plan.end_date_wsh }}（{{ item.plan.days_wsh }} {{ unitLabel(item.plan.unit_wsh) }}）</dd>
                  </div>
                  <div class="ag-plan-cell">
                    <dt>距离</dt>
                    <dd>{{ item.plan.distance_wsh != null ? `${item.plan.distance_wsh} 公里` : '—' }}</dd>
                  </div>
                  <div class="ag-plan-cell">
                    <dt>单价</dt>
                    <dd class="tabular">¥{{ money(item.plan.unit_price_wsh) }}/{{ unitLabel(item.plan.unit_wsh) }}</dd>
                  </div>
                  <div class="ag-plan-cell">
                    <dt>总价（预估）</dt>
                    <dd class="tabular ag-plan-total">¥{{ money(item.plan.total_price_wsh) }}</dd>
                  </div>
                </dl>

                <div class="ag-options">
                  <label class="ag-check">
                    <input v-model="autoPay" type="checkbox" :disabled="confirming">
                    <span class="ag-check-box" aria-hidden="true">
                      <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6 9 17l-5-5" />
                      </svg>
                    </span>
                    <span class="ag-check-copy">
                      <span class="ag-check-label">确认后自动支付</span>
                      <span class="ag-check-desc">开启后会用余额直接完成支付；关闭时只创建待支付订单。</span>
                    </span>
                  </label>
                  <label v-if="autoPay" class="ag-field-password">
                    <span class="ag-label">支付密码</span>
                    <input
                      v-model.trim="paymentPassword"
                      type="password"
                      inputmode="numeric"
                      maxlength="6"
                      autocomplete="one-time-code"
                      placeholder="6 位数字"
                      :disabled="confirming"
                      class="ag-input"
                    >
                    <span class="ag-hint">仅本次执行使用，不会保存在本地或服务端。</span>
                  </label>
                </div>

                <div class="ag-plan-actions">
                  <button type="button" class="cta cta-primary" :disabled="confirming" @click="confirmPlan(item.plan)">
                    {{ confirming ? '提交中...' : autoPay ? '授权下单并支付' : '确认下单' }}
                  </button>
                </div>
              </div>

              <!-- 发送失败：用同一个 request_id 重试，服务端若已处理过会直接回放结果，不会重复记一遍 -->
              <div v-else-if="item.kind === 'retry'" class="ag-msg assistant">
                <span class="ag-avatar" aria-hidden="true">AI</span>
                <div class="ag-bubble">
                  <p class="ag-bubble-text">{{ item.content }}</p>
                  <button type="button" class="ag-chip ag-retry" :disabled="sending" @click="retryLast()">
                    重试这一句
                  </button>
                </div>
              </div>

              <!-- 执行结果卡片 -->
              <div v-else class="ag-result">
                <p class="ag-result-text">{{ item.result.message_wsh }}</p>
                <dl class="ag-result-facts">
                  <div class="ag-result-fact">
                    <dt>订单号</dt>
                    <dd class="tabular">{{ item.result.orderNo || '未生成' }}</dd>
                  </div>
                  <div class="ag-result-fact">
                    <dt>支付状态</dt>
                    <dd>
                      <span v-if="item.result.paymentStatus" :class="['badge', item.result.paymentStatus === 'paid' ? 'badge-active' : 'badge-action']">
                        {{ paymentStatusText(item.result.paymentStatus) }}
                      </span>
                      <span v-else>未支付</span>
                    </dd>
                  </div>
                </dl>
                <div class="ag-result-actions">
                  <button
                    v-if="item.result.payNo && item.result.paymentStatus === 'pending'"
                    type="button"
                    class="cta cta-primary"
                    :disabled="payingPayNo === item.result.payNo"
                    @click="manualPay(item.result)"
                  >
                    {{ payingPayNo === item.result.payNo ? '支付中...' : '手动支付' }}
                  </button>
                  <router-link to="/orders" class="cta cta-outline">到订单中心查看</router-link>
                </div>
              </div>
            </template>

            <div v-if="sending" class="ag-msg assistant">
              <span class="ag-avatar" aria-hidden="true">AI</span>
              <div class="ag-bubble"><p class="ag-bubble-text ag-typing">正在匹配门店与服务…</p></div>
            </div>
          </div>

          <footer class="ag-compose">
            <div v-if="locked" class="ag-locked">
              这个会话已经下单了。要再订一次，请点左上角「新会话」。
            </div>
            <template v-else>
              <textarea
                v-model.trim="input"
                rows="2"
                maxlength="500"
                :disabled="sending || confirming"
                class="ag-textarea"
                placeholder="说说你的需求，或者直接说要改什么，例如：帮布丁订 3 晚寄养，下周二入住"
                @keydown.enter.exact.prevent="send"
              />
              <div class="ag-compose-row">
                <p class="ag-hint">
                  <template v-if="locating">正在获取位置…</template>
                  <template v-else>Enter 发送，Shift + Enter 换行。价格由后端报价给出，助手不会自己算。</template>
                </p>
                <button type="button" class="cta cta-primary" :disabled="sending || confirming || !input" @click="send">
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor" aria-hidden="true">
                    <path d="M6 4l14 8-14 8V4z" />
                  </svg>
                  {{ sending ? '处理中...' : '发送' }}
                </button>
              </div>
            </template>
          </footer>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import {
  sendAgentMessage,
  executeAgentConfirm,
  listAgentSessions,
  getAgentSession,
  deleteAgentSession,
} from '@/api/ai'
import { executePayment } from '@/api/payment'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { useAppStore } from '@/stores/app'
import { unitLabel } from '@/domain/BookingUnit'

const STORAGE_KEY = 'agent_session_id'

const examples = [
  '帮布丁订 9 月 15 日到 18 日的标准寄养，优先衡山路门店',
  '下周二开始给芋圆订 3 晚寄养，价格不超过 150 一晚',
  '帮我订一只金毛的寄养，5 晚，本周五开始',
]

const appStore = useAppStore()

const sessions = ref([])
const sessionId = ref('')
/** 首轮之前就生成好的会话ID：服务端用它入库，重发也不会多建会话 */
const pendingSessionId = ref('')
/** 上一句的发送凭据（requestId + 原文 + 会话ID），用于「重试这一句」时带同一个 request_id */
const pendingTurn = ref(null)
const sessionStatus = ref('')
const messages = ref([])
const input = ref('')
const sending = ref(false)
const locating = ref(false)
const confirming = ref(false)
const autoPay = ref(false)
const paymentPassword = ref('')
const payingPayNo = ref('')
const streamRef = ref(null)

const locked = computed(() => sessionStatus.value === 'CONFIRMED')

watch(() => messages.value.length, () => scrollToBottom())

onMounted(async () => {
  await refreshSessions()
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    await openSession(saved, { silent: true })
  }
})

function scrollToBottom() {
  nextTick(() => {
    const el = streamRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function money(value) {
  return Number(value ?? 0).toFixed(2)
}

function statusText(status) {
  const map = { COLLECTING: '收集中', AWAIT_CONFIRM: '待确认', CONFIRMED: '已下单' }
  return map[status] || '新的咨询'
}

function paymentStatusText(status) {
  const map = { pending: '待支付', paid: '已支付', success: '已支付' }
  return map[status] || status
}

function nextActionText(action) {
  const map = {
    ask_requirement: '请补充下单需求。',
    ask_pet_type: '请补充宠物类型或品种。',
    ask_pet_identity: '请补充宠物昵称，便于精确匹配档案。',
    ask_days: '请补充预订夜数。',
    ask_location: '请提供或授权位置信息。',
    ask_payment_password: '请补充支付密码，或关闭自动支付后只创建待支付订单。',
    unsupported_service_type: '智能下单目前仅支持按夜寄养，请到服务列表手动下单。',
    complete_profile: '请先完善个人资料。',
    create_pet_profile: '请先添加宠物档案。',
    retry_later: '请稍后再试。',
  }
  return map[action] || ''
}

function useExample(text) {
  input.value = text
}

function pushText(role, content) {
  messages.value.push({ kind: 'text', role, content })
}

/**
 * 方案卡片全局只保留最新一张。
 * <p>多轮修改后旧卡片上的「确认下单」按钮仍指向同一会话，用户点旧卡片会以为下的是旧方案，
 * 实际下的是最新方案。与其解释，不如直接移除旧卡片，只留当前这一张。</p>
 */
function pushPlan(plan) {
  removePlanCards()
  messages.value.push({ kind: 'plan', plan })
}

/** 前端预生成会话ID：首轮之前就有值，网络重发时不会再多建一个会话。 */
function newClientSessionId() {
  const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}${Math.random().toString(16).slice(2, 10)}`
  return uuid.replace(/-/g, '')
}

function pushResult(result) {
  messages.value.push({ kind: 'result', result })
}

function pushRetry(content) {
  messages.value.push({ kind: 'retry', content })
}

function newSession() {
  // 有请求在途时切走会让响应落到空白会话上（甚至把已下单状态写到新会话，锁死输入框）
  if (sending.value || confirming.value) return
  sessionId.value = ''
  pendingSessionId.value = ''
  pendingTurn.value = null
  sessionStatus.value = ''
  messages.value = []
  input.value = ''
  autoPay.value = false
  paymentPassword.value = ''
  localStorage.removeItem(STORAGE_KEY)
}

async function refreshSessions() {
  try {
    const r = await listAgentSessions()
    if (r.code === 200) sessions.value = r.data || []
  } catch (e) {
    /* 列表失败不阻塞对话 */
  }
}

async function selectSession(item) {
  if (sending.value || confirming.value) return
  if (item.session_id_wsh === sessionId.value) return
  await openSession(item.session_id_wsh)
}

async function openSession(id, { silent = false } = {}) {
  try {
    const r = await getAgentSession(id)
    if (r.code !== 200) {
      localStorage.removeItem(STORAGE_KEY)
      return
    }
    const data = r.data || {}
    sessionId.value = data.session_id_wsh
    pendingSessionId.value = ''
    sessionStatus.value = data.status_wsh || ''
    localStorage.setItem(STORAGE_KEY, data.session_id_wsh)
    messages.value = []
    for (const msg of data.messages || []) {
      if (msg.content_wsh) pushText(msg.role_wsh, msg.content_wsh)
    }
    // 只有「待确认」才展示可操作的方案卡片；已下单的会话不再给确认入口
    if (data.plan_card && data.status_wsh === 'AWAIT_CONFIRM') {
      pushPlan(data.plan_card)
    }
    autoPay.value = false
    paymentPassword.value = ''
    scrollToBottom()
  } catch (e) {
    if (!silent) appStore.addToast('会话加载失败，请稍后重试', 'error')
    localStorage.removeItem(STORAGE_KEY)
  }
}

async function removeSession(item) {
  if (sending.value || confirming.value) return
  try {
    const r = await deleteAgentSession(item.session_id_wsh)
    if (r.code === 200) {
      if (item.session_id_wsh === sessionId.value) newSession()
      await refreshSessions()
      appStore.addToast('会话已删除', 'success')
    }
  } catch (e) {
    appStore.addToast('删除失败，请稍后重试', 'error')
  }
}

async function send() {
  const text = input.value
  if (!text || sending.value || confirming.value || locked.value) return
  pendingTurn.value = null
  await doSend(text, { requestId: newClientSessionId(), pushUserBubble: true })
}

/**
 * 用同一个 request_id 重发上一句。
 * <p>服务端把最近处理过的 request_id 存在会话状态里，命中就直接回放结果——
 * 所以「其实已经处理成功、只是响应超时」的情况下，重试不会把同一句话记两遍。</p>
 */
async function retryLast() {
  const turn = pendingTurn.value
  if (!turn || sending.value || confirming.value) return
  messages.value = messages.value.filter(m => m.kind !== 'retry')
  await doSend(turn.text, {
    requestId: turn.requestId,
    pushUserBubble: false,
    forcedSessionId: turn.sessionId,
  })
}

async function doSend(text, { requestId, pushUserBubble, forcedSessionId = '' }) {
  // 首轮之前就把会话ID定下来并一直沿用：超时重发不会再多建一个会话
  if (!sessionId.value && !forcedSessionId && !pendingSessionId.value) {
    pendingSessionId.value = newClientSessionId()
  }
  const clientSessionId = forcedSessionId || sessionId.value || pendingSessionId.value
  const isFirstRound = !sessionId.value
  pendingTurn.value = { requestId, text, sessionId: clientSessionId }

  if (pushUserBubble) {
    pushText('user', text)
    input.value = ''
  }
  sending.value = true

  let coords = null
  if (isFirstRound) {
    locating.value = true
    try {
      const loc = await getCurrentAddress()
      coords = loc && loc.latitude_wsh != null ? loc : null
    } catch (e) {
      coords = null
    } finally {
      locating.value = false
    }
  }

  try {
    const r = await sendAgentMessage({
      session_id_wsh: clientSessionId,
      request_id_wsh: requestId,
      content_wsh: text,
      latitude_wsh: coords?.latitude_wsh,
      longitude_wsh: coords?.longitude_wsh,
      address_wsh: coords?.address_wsh,
    })
    const data = r.data || {}
    // 响应回来时用户可能已经切到别的会话/新建了会话，这一轮的落点已经不是当前视图，丢弃
    if (sessionId.value && sessionId.value !== clientSessionId) return

    if (data.session_id_wsh) {
      sessionId.value = data.session_id_wsh
      pendingSessionId.value = ''
      localStorage.setItem(STORAGE_KEY, data.session_id_wsh)
    }
    if (data.reply_wsh) pushText('assistant', data.reply_wsh)

    // 方案卡片严格跟随服务端：非 await_confirm 一律移除，
    // 否则会留下一张「点了必然失败」的旧卡片（服务端已把 plan 清空或日期已过期）
    removePlanCards()
    if (data.status_wsh === 'await_confirm') {
      sessionStatus.value = 'AWAIT_CONFIRM'
      if (data.plan_card) pushPlan(data.plan_card)
    } else if (data.status_wsh === 'collecting') {
      sessionStatus.value = 'COLLECTING'
      const hint = nextActionText(data.next_action_wsh)
      if (hint) pushText('assistant', hint)
    } else {
      // failed：首轮不会入库，续轮方案保持不变
      if (!data.session_id_wsh) sessionStatus.value = ''
    }
    pendingTurn.value = null
    await refreshSessions()
  } catch (e) {
    // 区分业务错误与网络异常：400/403/404 这类重试没有意义，只有网络异常和 409（上一句还在处理）
    // 才值得用同一个 request_id 重试。统一说成「网络异常」会让用户以为没发出去，从而重复发送。
    const status = e?.response?.status
    const msg = e?.response?.data?.message
    if (msg && status && status >= 400 && status < 500 && status !== 409) {
      pushText('assistant', msg)
    } else {
      pushRetry(msg || '网络异常，这句话可能没发出去。点下面可以安全重试。')
    }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

async function confirmPlan(plan) {
  if (!plan || confirming.value || !sessionId.value) return
  if (autoPay.value && !/^\d{6}$/.test(paymentPassword.value)) {
    appStore.addToast('请输入 6 位支付密码，或关闭自动支付', 'error')
    return
  }

  confirming.value = true
  try {
    const r = await executeAgentConfirm({
      session_id_wsh: sessionId.value,
      auto_pay_wsh: autoPay.value,
      payment_password_wsh: autoPay.value ? paymentPassword.value : undefined,
    })
    if (r.code !== 200) {
      pushText('assistant', r.message || '下单失败，请稍后重试。')
      return
    }
    const data = r.data || {}
    if (data.status === 'success' || data.status === 'pending_payment') {
      sessionStatus.value = 'CONFIRMED'
      removePlanCards()
      pushResult({
        message_wsh: data.message_wsh || '订单已创建。',
        orderNo: data.orderNo || data.order_no_wsh,
        payNo: data.payNo || data.pay_no_wsh,
        paymentStatus: data.payment_status_wsh,
      })
      paymentPassword.value = ''
      autoPay.value = false
      appStore.addToast(data.status === 'success' ? '订单已支付' : '订单已创建，等待支付', 'success')
      await refreshSessions()
    } else {
      // 方案里的入住日期已过期：这张卡片再点也只会失败，直接移除，让用户重新给日期
      if (data.next_action_wsh === 'ask_days') removePlanCards()
      pushText('assistant', data.message_wsh || '这一步没完成，请补充信息后重试。')
    }
  } catch (e) {
    // 区分业务错误与网络异常：业务错误重试没有意义，网络异常才值得重试
    const msg = e?.response?.data?.message || '下单请求没送达，请检查网络后重试。'
    pushText('assistant', msg)
  } finally {
    confirming.value = false
    scrollToBottom()
  }
}

function removePlanCards() {
  messages.value = messages.value.filter(m => m.kind !== 'plan')
}

async function manualPay(result) {
  if (!result.payNo || payingPayNo.value) return
  payingPayNo.value = result.payNo
  try {
    const r = await executePayment({ pay_no_wsh: result.payNo })
    if (r.code === 200) {
      result.paymentStatus = 'paid'
      result.message_wsh = '支付已完成，订单已进入后续处理。'
      appStore.addToast('支付成功', 'success')
    }
  } catch (e) {
    appStore.addToast('支付失败，请稍后重试', 'error')
  } finally {
    payingPayNo.value = ''
  }
}
</script>

<style scoped>
.ag-page {
  width: 100%;
  padding: 6px 0 48px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.ag-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* Breadcrumb */
.ag-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ag-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ag-crumb-link:hover { color: var(--ref-ink); }
.ag-crumb-sep { color: var(--ref-line); }
.ag-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow */
.ag-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ag-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }

/* Hero */
.ag-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 34px 0 22px;
}
.ag-head-copy { min-width: 0; }
.ag-title {
  margin: 16px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(32px, 4vw, 46px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
}
.ag-sub {
  margin: 12px 0 0;
  max-width: 660px;
  font-size: 13.5px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ag-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* CTA */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  padding: 0 18px;
  border-radius: 11px;
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

/* 会话区骨架 */
.ag-chat {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 16px;
  margin-top: 8px;
  align-items: stretch;
}

/* 左：历史会话 */
.ag-side {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 14px;
  display: flex;
  flex-direction: column;
  min-height: 420px;
  max-height: 620px;
}
.ag-new {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 38px;
  border-radius: 10px;
  border: 1px dashed color-mix(in srgb, var(--ref-ink) 22%, transparent);
  background: transparent;
  color: var(--ref-ink);
  font-size: 12.5px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.ag-new:hover:not(:disabled) {
  border-color: var(--ref-brand);
  background: color-mix(in srgb, var(--ref-brand) 6%, transparent);
}
.ag-side-label {
  margin: 18px 0 8px;
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ag-side-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  display: grid;
  gap: 2px;
}
.ag-side-item {
  display: flex;
  align-items: center;
  gap: 2px;
  border-radius: 10px;
  transition: background 0.15s;
}
.ag-side-item:hover { background: color-mix(in srgb, var(--ref-sand) 55%, transparent); }
.ag-side-item.is-active { background: color-mix(in srgb, var(--ref-brand) 9%, transparent); }
.ag-side-open {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 4px;
  padding: 9px 8px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.ag-side-title {
  font-size: 12.5px;
  color: var(--ref-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ag-side-meta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  color: var(--ref-muted);
}
.ag-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--ref-muted);
}
.ag-dot.is-ready { background: var(--ref-brand); }
.ag-dot.is-done { background: var(--color-success); }
.ag-side-del {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-right: 4px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--ref-muted);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}
.ag-side-item:hover .ag-side-del { opacity: 1; }
.ag-side-del:hover { color: var(--ref-brand-deep); background: color-mix(in srgb, var(--ref-brand) 10%, transparent); }
.ag-side-empty {
  padding: 12px 8px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}

/* 右：对话流 */
.ag-main {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  display: flex;
  flex-direction: column;
  min-height: 420px;
  max-height: 620px;
  overflow: hidden;
}
.ag-stream {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 空态 */
.ag-hello {
  margin: auto 0;
  padding: 8px 4px 16px;
}
.ag-hello-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 400;
  color: var(--ref-ink);
}
.ag-hello-desc {
  margin: 8px 0 0;
  max-width: 460px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ref-muted);
}
.ag-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
.ag-chip {
  padding: 7px 13px;
  border-radius: 999px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}
.ag-chip:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent);
  background: color-mix(in srgb, var(--ref-sand) 50%, transparent);
  color: var(--ref-ink);
}
/* 发送失败气泡里的「重试这一句」 */
.ag-retry {
  margin-top: 8px;
  align-self: flex-start;
}
.ag-retry:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* 气泡 */
.ag-msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 100%;
}
.ag-msg.user { flex-direction: row-reverse; }
.ag-avatar {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--ref-sand);
  color: var(--ref-brand-deep);
  font-size: 10px;
  font-weight: 600;
}
.ag-msg.user .ag-avatar {
  background: color-mix(in srgb, var(--ref-brand) 12%, transparent);
  color: var(--ref-brand-deep);
}
.ag-bubble {
  max-width: 78%;
  padding: 11px 14px;
  border-radius: 14px;
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 45%, var(--ref-surface));
}
.ag-msg.user .ag-bubble {
  background: var(--ref-brand);
  border-color: var(--ref-brand);
}
.ag-bubble-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--ref-ink);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.ag-msg.user .ag-bubble-text { color: #fff; }
.ag-typing { color: var(--ref-muted); }

/* 方案卡片 */
.ag-plan {
  border: 1px solid color-mix(in srgb, var(--ref-brand) 22%, var(--ref-line));
  border-radius: 16px;
  background: color-mix(in srgb, var(--ref-brand) 3%, var(--ref-surface));
  padding: 16px 18px;
}
.ag-plan-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.ag-plan-tag {
  padding: 3px 9px;
  border-radius: 6px;
  background: var(--ref-brand);
  color: #fff;
  font-size: 10.5px;
  letter-spacing: 0.06em;
}
.ag-plan-hint {
  font-size: 11.5px;
  color: var(--ref-muted);
}
.ag-plan-grid {
  margin: 14px 0 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 18px;
}
.ag-plan-cell dt {
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--ref-muted);
}
.ag-plan-cell dd {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ag-plan-cell.ag-plan-wide { grid-column: span 2; }
.ag-plan-cell dd.ag-plan-total {
  font-size: 15px;
  font-weight: 600;
  color: var(--ref-brand);
}
.ag-plan .ag-options {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--ref-line);
  display: grid;
  gap: 12px;
}
.ag-plan-actions { margin-top: 16px; }

/* 选项 / 输入 */
.ag-check {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
}
.ag-check input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
.ag-check-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 19px;
  height: 19px;
  flex: 0 0 auto;
  margin-top: 1px;
  border: 1px solid var(--ref-line);
  border-radius: 6px;
  background: var(--ref-surface);
  color: transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.ag-check input:checked + .ag-check-box {
  background: var(--ref-brand);
  border-color: var(--ref-brand);
  color: #fff;
}
.ag-check input:focus-visible + .ag-check-box {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 20%, transparent);
}
.ag-check-copy { min-width: 0; }
.ag-check-label {
  display: block;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ag-check-desc {
  display: block;
  margin-top: 2px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--ref-muted);
}
.ag-field-password { display: grid; gap: 6px; max-width: 240px; }
.ag-label { font-size: 12px; font-weight: 500; color: var(--ref-ink-soft); }
.ag-input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--ref-line);
  border-radius: 9px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  letter-spacing: 0.2em;
}
.ag-input:focus {
  outline: none;
  border-color: var(--ref-brand);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.ag-hint {
  margin: 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--ref-muted);
}

/* 结果卡片 */
.ag-result {
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  padding: 16px 18px;
}
.ag-result-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 92%, transparent);
}
.ag-result-facts {
  margin: 14px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--ref-line);
  display: grid;
  gap: 10px;
}
.ag-result-fact {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}
.ag-result-fact dt {
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ag-result-fact dd {
  margin: 0;
  max-width: 62%;
  text-align: right;
  font-size: 12.5px;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ag-result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

/* Badge */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  border: 1px solid transparent;
  padding: 3px 9px;
  font-size: 10.5px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-action { background: var(--ref-brand); color: #fff; }
.badge-active {
  background: color-mix(in srgb, var(--color-success) 12%, transparent);
  color: var(--color-success);
  border-color: color-mix(in srgb, var(--color-success) 30%, transparent);
}

/* 输入区 */
.ag-compose {
  border-top: 1px solid var(--ref-line);
  padding: 14px 18px 16px;
  background: var(--ref-surface);
}
.ag-textarea {
  width: 100%;
  resize: none;
  min-height: 60px;
  padding: 11px 13px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.7;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ag-textarea::placeholder { color: color-mix(in srgb, var(--ref-muted) 78%, transparent); }
.ag-textarea:focus {
  outline: none;
  border-color: var(--ref-brand);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.ag-textarea:disabled { opacity: 0.6; }
.ag-compose-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 10px;
}
.ag-locked {
  padding: 10px 14px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-success) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-success) 26%, transparent);
  font-size: 12.5px;
  color: var(--ref-ink-soft);
}

/* Responsive */
@media (max-width: 980px) {
  .ag-chat { grid-template-columns: 1fr; }
  .ag-side { max-height: 220px; min-height: 0; }
  .ag-main { max-height: none; }
  .ag-plan-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ag-plan-cell.ag-plan-wide { grid-column: span 2; }
}
@media (max-width: 640px) {
  .ag-shell { padding: 0 16px; }
  .ag-head { padding: 26px 0 18px; }
  .ag-plan { padding: 14px; }
  .ag-plan-grid { grid-template-columns: 1fr; }
  .ag-plan-cell.ag-plan-wide { grid-column: span 1; }
  .ag-bubble { max-width: 86%; }
  .ag-compose-row { flex-direction: column; align-items: stretch; }
}
@media (prefers-reduced-motion: reduce) {
  .cta { transition: none; }
}
</style>
