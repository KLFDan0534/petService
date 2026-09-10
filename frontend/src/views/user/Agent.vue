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
          <p class="ag-sub">用一句话描述需求，助手会匹配门店与服务、生成方案供你确认，确认后才创建订单。默认只生成订单，不会自动付款。</p>
        </div>
        <div class="ag-actions">
          <router-link to="/orders" class="cta cta-outline">查看我的订单</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 描述需求 ═══ -->
      <section v-if="!plan" class="ag-section" aria-label="描述你的需求">
        <header class="ag-sec-head">
          <div class="ag-head-copy">
            <p class="ag-eyebrow ag-sec-eyebrow">
              <span class="ag-idx">01</span>
              <span class="ag-line" aria-hidden="true"></span>
              <span>任务</span>
            </p>
            <h2 class="ag-sec-title">描述你的需求</h2>
            <p class="ag-sec-desc">写清宠物、时间与偏好门店，匹配会更准。</p>
          </div>
        </header>

        <div class="ag-grid">
          <!-- 表单 -->
          <div class="ag-form">
            <label class="ag-field">
              <span class="ag-label">任务描述<span class="ag-req">*</span></span>
              <textarea
                v-model.trim="input"
                rows="5"
                maxlength="300"
                :disabled="busy"
                placeholder="例如：帮布丁订 9 月 15 日到 18 日的标准寄养，优先衡山路门店"
                class="ag-textarea"
              />
            </label>

            <div class="ag-examples">
              <button v-for="example in examples" :key="example" type="button" class="ag-chip" @click="input = example">
                {{ example }}
              </button>
            </div>

            <div class="ag-options">
              <label class="ag-check">
                <input v-model="autoPay" type="checkbox" :disabled="busy">
                <span class="ag-check-box" aria-hidden="true">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                </span>
                <span class="ag-check-copy">
                  <span class="ag-check-label">下单后自动支付</span>
                  <span class="ag-check-desc">开启后助手会用余额直接完成支付；关闭时只生成订单，由你手动确认。</span>
                </span>
              </label>

              <template v-if="autoPay">
                <label class="ag-field ag-field-password">
                  <span class="ag-label">支付密码</span>
                  <input
                    v-model.trim="paymentPassword"
                    type="password"
                    inputmode="numeric"
                    maxlength="6"
                    autocomplete="one-time-code"
                    placeholder="6 位数字"
                    :disabled="busy"
                    class="ag-input"
                  >
                  <span class="ag-hint">仅本次执行使用，不会保存在本地或服务端。</span>
                </label>
              </template>
            </div>

            <div class="ag-submit">
              <button type="button" class="cta cta-primary" :disabled="busy || !input" @click="submitPlan">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor" aria-hidden="true">
                  <path d="M6 4l14 8-14 8V4z" />
                </svg>
                {{ busy ? '生成中...' : '生成方案' }}
              </button>
            </div>
          </div>

          <!-- How it works -->
          <aside class="ag-steps">
            <p class="ag-steps-eyebrow">操作流程</p>
            <ol class="ag-step-list">
              <li v-for="(step, index) in steps" :key="step.title" class="ag-step">
                <span class="ag-step-index tabular">0{{ index + 1 }}</span>
                <span class="ag-step-copy">
                  <span class="ag-step-title">{{ step.title }}</span>
                  <span class="ag-step-desc">{{ step.desc }}</span>
                </span>
              </li>
            </ol>
          </aside>
        </div>
      </section>

      <!-- ═══ 02 · 方案确认 ═══ -->
      <section v-else-if="plan" class="ag-section" aria-label="方案确认">
        <header class="ag-sec-head">
          <div class="ag-head-copy">
            <p class="ag-eyebrow ag-sec-eyebrow">
              <span class="ag-idx">02</span>
              <span class="ag-line" aria-hidden="true"></span>
              <span>方案</span>
            </p>
            <h2 class="ag-sec-title">确认寄养方案</h2>
            <p class="ag-sec-desc">核对下方方案无误后确认下单；价格以订单中心最终实付为准。</p>
          </div>
        </header>

        <div class="ag-plan">
          <dl class="ag-plan-grid">
            <div class="ag-plan-cell">
              <dt>宠物</dt>
              <dd>{{ plan.pet_name_wsh || '—' }}</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>商家</dt>
              <dd>{{ plan.merchant_name_wsh || '—' }}</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>看护人</dt>
              <dd>{{ plan.keeper_name_wsh || '—' }}</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>服务</dt>
              <dd>{{ plan.service_name_wsh || '—' }}</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>日期</dt>
              <dd class="tabular">{{ plan.start_date_wsh }} ~ {{ plan.end_date_wsh }}（{{ plan.days_wsh }} 天）</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>距离</dt>
              <dd>{{ plan.distance_wsh != null ? `${plan.distance_wsh} 公里` : '—' }}</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>单价</dt>
              <dd class="tabular">¥{{ money(plan.unit_price_wsh) }}/天</dd>
            </div>
            <div class="ag-plan-cell">
              <dt>总价（预估）</dt>
              <dd class="tabular ag-plan-total">¥{{ money(plan.total_price_wsh) }}</dd>
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

            <template v-if="autoPay">
              <label class="ag-field ag-field-password">
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
            </template>
          </div>

          <div class="ag-planactions">
            <button type="button" class="cta cta-primary" :disabled="confirming" @click="confirmPlan">
              {{ confirming ? '提交中...' : autoPay ? '授权下单并支付' : '确认下单' }}
            </button>
            <button type="button" class="cta cta-outline" :disabled="confirming" @click="resetPlan">重新描述</button>
          </div>
          <p v-if="locating" class="ag-hint ag-locating">正在获取位置…</p>
        </div>
      </section>

      <!-- ═══ 03 · 执行失败 ═══ -->
      <section v-if="failed && !plan" class="ag-section" aria-label="执行失败">
        <header class="ag-sec-head">
          <div class="ag-head-copy">
            <p class="ag-eyebrow ag-sec-eyebrow">
              <span class="ag-idx">03</span>
              <span class="ag-line" aria-hidden="true"></span>
              <span>结果</span>
            </p>
            <h2 class="ag-sec-title">执行失败</h2>
          </div>
        </header>
        <div class="ag-failed">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3z" /><path d="M12 9v4" /><path d="M12 17h.01" />
          </svg>
          <div class="ag-failed-copy">
            <p class="ag-failed-text">{{ failed }}</p>
            <p class="ag-failed-desc">可以把需求写得更具体后重试，或直接到服务列表手动下单。</p>
          </div>
        </div>
      </section>

      <!-- ═══ 03 · 执行结果 ═══ -->
      <section v-else-if="result" class="ag-section" aria-label="执行结果">
        <header class="ag-sec-head">
          <div class="ag-head-copy">
            <p class="ag-eyebrow ag-sec-eyebrow">
              <span class="ag-idx">03</span>
              <span class="ag-line" aria-hidden="true"></span>
              <span>结果</span>
            </p>
            <h2 class="ag-sec-title">执行结果</h2>
            <p class="ag-sec-desc">每一步都记录在案，可与订单中心逐项核对。</p>
          </div>
        </header>

        <div class="ag-result-grid">
          <div class="ag-result-summary">
            <span class="ag-result-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="2" width="20" height="8" rx="2" /><rect x="2" y="14" width="20" height="8" rx="2" /><path d="M6 6h.01M6 18h.01" />
              </svg>
            </span>
            <p class="ag-result-text">{{ result.message_wsh || '助手已完成本次任务。' }}</p>

            <dl class="ag-result-facts">
              <div class="ag-result-fact">
                <dt>订单号</dt>
                <dd class="tabular">{{ result.orderNo || '未生成' }}</dd>
              </div>
              <div class="ag-result-fact">
                <dt>支付状态</dt>
                <dd>
                  <span v-if="result.paymentStatus" :class="['badge', result.paymentStatus === 'paid' ? 'badge-active' : 'badge-action']">
                    {{ paymentStatusText(result.paymentStatus) }}
                  </span>
                  <span v-else>未支付</span>
                </dd>
              </div>
              <div class="ag-result-fact">
                <dt>下一步</dt>
                <dd>{{ nextActionText(result.nextAction || '') }}</dd>
              </div>
            </dl>

            <div v-if="result.payNo && result.paymentStatus === 'pending'" class="ag-result-actions">
              <button type="button" class="cta cta-primary" :disabled="payingPayNo === result.payNo" @click="manualPay(result)">
                {{ payingPayNo === result.payNo ? '支付中...' : '手动支付' }}
              </button>
              <router-link to="/payments" class="cta cta-outline">查看支付记录</router-link>
            </div>
            <div v-else class="ag-result-actions">
              <router-link to="/orders" class="cta cta-outline">到订单中心确认</router-link>
            </div>
          </div>

          <div class="ag-result-logs">
            <header class="ag-logs-head">
              <h3 class="ag-logs-title">执行日志</h3>
            </header>
            <ol v-if="result.logs?.length" class="ag-log-list">
              <li v-for="(log, index) in result.logs" :key="index" class="ag-log">
                <span class="ag-log-index tabular">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="ag-log-text">{{ log }}</span>
              </li>
            </ol>
            <p v-else class="ag-logs-empty">本次执行没有返回日志。</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { executeAgentPlan, executeAgentConfirm } from '@/api/ai'
import { executePayment } from '@/api/payment'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { useAppStore } from '@/stores/app'

const examples = [
  '帮布丁订 9 月 15 日到 18 日的标准寄养，优先衡山路门店',
  '下周二开始给芋圆订 3 天寄养，价格不超过 150 一天',
  '帮我订一只金毛的寄养，5 天，本周五开始',
]

const steps = [
  { title: '解析需求', desc: 'AI 理解宠物、服务类型、日期与门店偏好。' },
  { title: '匹配与校验', desc: '匹配你的宠物档案与附近门店，校验档期与服务。' },
  { title: '生成方案', desc: '给出推荐商家、看护人与预估价格，由你确认。' },
  { title: '确认下单', desc: '确认后才创建订单；可开启自动支付。' },
]

const appStore = useAppStore()
const input = ref('')
const planning = ref(false)
const locating = ref(false)
const confirming = ref(false)
const autoPay = ref(false)
const paymentPassword = ref('')
const payingPayNo = ref('')
const plan = ref(null)
const result = ref(null)
const failed = ref('')

const busy = computed(() => planning.value || locating.value || confirming.value)
const hasResult = computed(() => Boolean(result.value) || Boolean(failed.value))

function money(value) {
  return Number(value ?? 0).toFixed(2)
}

function reset() {
  plan.value = null
  result.value = null
  failed.value = ''
  paymentPassword.value = ''
}

function resetPlan() {
  plan.value = null
  failed.value = ''
  paymentPassword.value = ''
}

async function submitPlan() {
  if (!input.value || busy.value) return
  failed.value = ''
  plan.value = null

  let coords = null
  locating.value = true
  try {
    const loc = await getCurrentAddress()
    coords = loc && loc.latitude_wsh != null ? loc : null
  } catch (e) {
    appStore.addToast('未获取到位置，将使用档案坐标', 'info')
    coords = null
  } finally {
    locating.value = false
  }

  planning.value = true
  try {
    const r = await executeAgentPlan({
      input_wsh: input.value,
      latitude_wsh: coords?.latitude_wsh,
      longitude_wsh: coords?.longitude_wsh,
      address_wsh: coords?.address_wsh,
    })
    if (r.code === 200) {
      const data = r.data || {}
      if (data.status_wsh === 'plan_generated') {
        plan.value = data
      } else {
        failed.value = data.message_wsh || '方案生成失败，请稍后重试。'
        const hint = nextActionText(data.next_action_wsh)
        if (hint && hint !== '无需操作') failed.value += `（${hint}）`
      }
    } else {
      failed.value = r.message || '方案生成失败，请稍后重试。'
    }
  } catch (e) {
    failed.value = '方案生成失败，请检查网络后重试。'
  } finally {
    planning.value = false
  }
}

async function confirmPlan() {
  if (!plan.value || confirming.value) return
  if (autoPay.value && !/^\d{6}$/.test(paymentPassword.value)) {
    appStore.addToast('请输入 6 位支付密码，或关闭自动支付授权', 'error')
    return
  }

  const token = plan.value.plan_token_wsh
  failed.value = ''
  confirming.value = true

  try {
    const r = await executeAgentConfirm({
      plan_token_wsh: token,
      auto_pay_wsh: autoPay.value,
      payment_password_wsh: autoPay.value ? paymentPassword.value : undefined,
    })
    if (r.code === 200) {
      const data = r.data || {}
      result.value = toAgentResult(data)
      plan.value = null
      paymentPassword.value = ''
      if (data.status === 'success') {
        appStore.addToast('订单已支付', 'success')
      } else if (data.status === 'pending_payment') {
        appStore.addToast('订单已创建，等待手动支付', 'success')
      }
    } else {
      failed.value = r.message || '任务执行失败，请稍后重试。'
      plan.value = null
    }
  } catch (e) {
    failed.value = '任务执行失败，请检查网络后重试。'
    plan.value = null
  } finally {
    confirming.value = false
  }
}

async function manualPay(msg) {
  if (!msg.payNo || payingPayNo.value) return
  payingPayNo.value = msg.payNo
  try {
    const r = await executePayment({ pay_no_wsh: msg.payNo })
    if (r.code === 200) {
      msg.paymentStatus = 'paid'
      msg.message_wsh = '支付已完成，订单已进入后续处理。'
      appStore.addToast('支付成功', 'success')
    }
  } catch (e) {
    appStore.addToast('支付失败，请稍后重试', 'error')
  } finally {
    payingPayNo.value = ''
  }
}

function toAgentResult(data = {}) {
  return {
    message_wsh: data.message_wsh || data.error || '任务已处理。',
    orderNo: data.orderNo || data.order_no_wsh,
    payNo: data.payNo || data.pay_no_wsh,
    logs: Array.isArray(data.logs) ? data.logs : [],
    nextAction: data.requires_user_input_wsh ? data.next_action_wsh : '',
    paymentStatus: data.payment_status_wsh,
  }
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
    ask_days: '请补充寄养天数。',
    ask_location: '请提供或授权位置信息。',
    ask_payment_password: '请补充支付密码，或关闭自动支付授权后只创建待支付订单。',
    unsupported_service_type: '智能下单目前仅支持寄养类日间服务，请到服务列表手动下单。',
    plan_expired: '该方案已过期或无效，请重新生成方案后再试。',
    plan_busy: '该方案正在处理或已提交，请勿重复点击。',
    complete_profile: '请先完善个人资料。',
    create_pet_profile: '请先添加宠物档案。',
    retry_later: '请稍后再试。',
  }
  return map[action] || action || '无需操作'
}
</script>

<style scoped>
.ag-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ag-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
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

/* ═══ Eyebrow ═══ */
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
.ag-idx { font-variant-numeric: tabular-nums; }
.ag-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ag-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.ag-head-copy { min-width: 0; }
.ag-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ag-sub {
  margin: 14px 0 0;
  max-width: 640px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ag-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
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
.badge-action { background: var(--ref-brand); color: #fff; }
.badge-active { background: color-mix(in srgb, var(--color-success) 12%, transparent); color: var(--color-success); border-color: color-mix(in srgb, var(--color-success) 30%, transparent); }

/* ═══ Section ═══ */
.ag-section { margin-top: 56px; }
.ag-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.ag-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ag-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ag-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ 01 · 表单 ═══ */
.ag-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
  margin-top: 20px;
  align-items: start;
}
.ag-form {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px;
}
.ag-field { display: block; }
.ag-label {
  display: block;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ref-ink-soft);
}
.ag-req { margin-left: 4px; color: var(--ref-brand); }
.ag-textarea {
  width: 100%;
  margin-top: 8px;
  resize: vertical;
  min-height: 118px;
  padding: 12px 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.7;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ag-textarea::placeholder { color: color-mix(in srgb, var(--ref-muted) 75%, transparent); }
.ag-textarea:focus {
  outline: none;
  border-color: var(--ref-brand);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 18%, transparent);
}
.ag-textarea:disabled { opacity: 0.6; }
.ag-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
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

.ag-options {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--ref-line);
  display: grid;
  gap: 16px;
}
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
  width: 20px;
  height: 20px;
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
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ag-check-desc {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ref-muted);
}
.ag-field-password { display: grid; gap: 8px; }
.ag-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  letter-spacing: 0.2em;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ag-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.ag-hint {
  font-size: 11.5px;
  color: var(--ref-muted);
}
.ag-locating { margin-top: 12px; }
.ag-submit {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}
.ag-planactions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

/* ═══ 01 · 流程说明 ═══ */
.ag-steps {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: color-mix(in srgb, var(--ref-cream) 40%, var(--ref-surface));
  padding: 24px;
}
.ag-steps-eyebrow {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ag-step-list {
  list-style: none;
  margin: 20px 0 0;
  padding: 0;
  display: grid;
  gap: 20px;
}
.ag-step {
  display: flex;
  gap: 14px;
}
.ag-step-index {
  padding-top: 2px;
  font-size: 12px;
  color: var(--ref-muted);
}
.ag-step-copy {
  min-width: 0;
  padding-left: 14px;
  border-left: 1px solid var(--ref-line);
}
.ag-step-title {
  display: block;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ag-step-desc {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ref-muted);
}

/* ═══ 02 · 方案确认 ═══ */
.ag-plan {
  margin-top: 20px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px;
}
.ag-plan-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px 24px;
}
.ag-plan-cell dt {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ref-muted);
}
.ag-plan-cell dd {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ag-plan-cell dd.ag-plan-total {
  font-size: 17px;
  font-weight: 600;
  color: var(--ref-brand);
}
.ag-plan .ag-options { margin-top: 24px; }

/* ═══ 03 · 失败 ═══ */
.ag-failed {
  margin-top: 20px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px 24px;
  border: 1px solid color-mix(in srgb, var(--ref-brand-deep) 20%, transparent);
  border-radius: 16px;
  background: color-mix(in srgb, var(--ref-brand-deep) 5%, var(--ref-surface));
  color: var(--ref-brand-deep);
}
.ag-failed-copy { min-width: 0; }
.ag-failed-text {
  margin: 0;
  font-size: 13.5px;
  color: var(--ref-ink);
}
.ag-failed-desc {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}

/* ═══ 03 · 结果 ═══ */
.ag-result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-top: 20px;
  align-items: stretch;
}
.ag-result-summary {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px;
}
.ag-result-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--ref-sand);
  color: var(--ref-brand);
}
.ag-result-text {
  margin: 18px 0 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 90%, transparent);
}
.ag-result-facts {
  margin: 20px 0 0;
  padding-top: 18px;
  border-top: 1px solid var(--ref-line);
  display: grid;
  gap: 16px;
}
.ag-result-fact {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}
.ag-result-fact dt {
  flex: 0 0 auto;
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ag-result-fact dd {
  margin: 0;
  max-width: 60%;
  text-align: right;
  font-size: 13px;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ag-result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}
.ag-result-logs {
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.ag-logs-head {
  padding: 16px 20px;
  border-bottom: 1px solid var(--ref-line);
}
.ag-logs-title {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ag-log-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  max-height: 22rem;
}
.ag-log {
  display: flex;
  gap: 12px;
  padding: 13px 20px;
  border-bottom: 1px solid var(--ref-line);
}
.ag-log:last-child { border-bottom: none; }
.ag-log-index {
  flex: 0 0 auto;
  padding-top: 1px;
  font-size: 11px;
  color: var(--ref-muted);
}
.ag-log-text {
  min-width: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.ag-logs-empty {
  margin: 0;
  padding: 34px 20px;
  text-align: center;
  font-size: 12.5px;
  color: var(--ref-muted);
}

/* ═══ Responsive ═══ */
@media (max-width: 980px) {
  .ag-grid, .ag-result-grid { grid-template-columns: 1fr; }
  .ag-plan-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .ag-shell { padding: 0 16px; }
  .ag-head { padding: 30px 0 22px; }
  .ag-sub { font-size: 13.5px; }
  .ag-section { margin-top: 44px; }
  .ag-form, .ag-steps, .ag-plan { padding: 18px; }
  .ag-plan-grid { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .cta { transition: none; }
}
</style>