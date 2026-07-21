<template>
  <div class="agent-page">
    <PageHero title="AI 智能体" subtitle="自然语言创建寄养订单，默认停在待支付状态" />

    <section class="agent-shell">
      <div class="chat-messages">
        <article v-for="msg in messages" :key="msg.id" :class="['message-row', `message-row--${msg.role}`]">
          <div class="message-bubble">
            <p>{{ msg.content }}</p>

            <div v-if="msg.orderNo || msg.payNo" class="result-meta">
              <span v-if="msg.orderNo">订单号：{{ msg.orderNo }}</span>
              <span v-if="msg.payNo">支付号：{{ msg.payNo }}</span>
              <span v-if="msg.paymentStatus">支付状态：{{ paymentStatusText(msg.paymentStatus) }}</span>
            </div>

            <div v-if="msg.nextAction" class="next-action">
              {{ nextActionText(msg.nextAction) }}
            </div>

            <div v-if="msg.payNo && msg.paymentStatus === 'pending'" class="message-actions">
              <button class="btn btn-primary btn-sm" :disabled="payingPayNo === msg.payNo" @click="manualPay(msg)">
                {{ payingPayNo === msg.payNo ? '支付中...' : '手动支付' }}
              </button>
              <router-link class="btn btn-outline btn-sm" to="/payments">查看支付记录</router-link>
            </div>

            <details v-if="msg.logs?.length" class="agent-logs">
              <summary>执行日志</summary>
              <ol>
                <li v-for="(log, i) in msg.logs" :key="i">{{ log }}</li>
              </ol>
            </details>
          </div>
        </article>

        <div v-if="loading" class="loading">智能体执行中...</div>
      </div>

      <form class="agent-input-panel" @submit.prevent="executeTask">
        <textarea
          v-model.trim="input"
          rows="3"
          maxlength="500"
          placeholder="例如：帮我的 golden 寄养 3 天，找附近评分高的看护人"
          :disabled="loading"
        />

        <div class="auth-row">
          <label class="switch-line">
            <input v-model="autoPay" type="checkbox" :disabled="loading">
            <span>授权 Agent 自动支付本次订单</span>
          </label>
          <router-link to="/profile/payment-password">设置支付密码</router-link>
        </div>

        <input
          v-if="autoPay"
          v-model.trim="paymentPassword"
          class="payment-password"
          type="password"
          inputmode="numeric"
          maxlength="6"
          autocomplete="one-time-code"
          placeholder="输入 6 位支付密码"
          :disabled="loading"
        >

        <div class="submit-row">
          <span>{{ input.length }}/500</span>
          <button class="btn btn-primary" type="submit" :disabled="loading || !input">
            {{ loading ? '执行中...' : autoPay ? '授权下单并支付' : '创建待支付订单' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { executeAgent } from '@/api/ai'
import { executePayment } from '@/api/payment'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const appStore = useAppStore()
const messages = ref([
  {
    id: 0,
    role: 'ai',
    content: '我可以帮你选择宠物、匹配附近商家和看护人，并创建待支付订单。支付默认由你手动确认。',
  },
])
const input = ref('')
const loading = ref(false)
const autoPay = ref(false)
const paymentPassword = ref('')
const payingPayNo = ref('')

async function executeTask() {
  if (!input.value || loading.value) return
  if (autoPay.value && !/^\d{6}$/.test(paymentPassword.value)) {
    appStore.addToast('请输入 6 位支付密码，或关闭自动支付授权', 'error')
    return
  }

  const task = input.value
  const password = paymentPassword.value
  messages.value.push({ id: Date.now(), role: 'user', content: task })
  input.value = ''
  paymentPassword.value = ''
  loading.value = true

  try {
    const r = await executeAgent({
      input_wsh: task,
      auto_pay_wsh: autoPay.value,
      payment_password_wsh: autoPay.value ? password : undefined,
    })
    if (r.code === 200) {
      messages.value.push(toAgentMessage(r.data))
      if (r.data?.status === 'success') {
        appStore.addToast('订单已支付', 'success')
      } else if (r.data?.status === 'pending_payment') {
        appStore.addToast('订单已创建，等待手动支付', 'success')
      }
    } else {
      messages.value.push({
        id: Date.now(),
        role: 'ai',
        content: r.message || '任务执行失败，请稍后重试。',
      })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: '任务执行失败，请检查网络后重试。' })
  } finally {
    loading.value = false
  }
}

async function manualPay(msg) {
  if (!msg.payNo || payingPayNo.value) return
  payingPayNo.value = msg.payNo
  try {
    const r = await executePayment({ pay_no_wsh: msg.payNo })
    if (r.code === 200) {
      msg.paymentStatus = 'paid'
      msg.content = '支付已完成，订单已进入后续处理。'
      appStore.addToast('支付成功', 'success')
    }
  } catch (e) {
    appStore.addToast('支付失败，请稍后重试', 'error')
  } finally {
    payingPayNo.value = ''
  }
}

function toAgentMessage(data = {}) {
  return {
    id: Date.now() + Math.random(),
    role: 'ai',
    content: data.message_wsh || data.error || '任务已处理。',
    orderNo: data.orderNo,
    payNo: data.payNo,
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
    ask_days: '请补充寄养天数。',
    ask_location: '请提供或授权位置信息。',
    ask_payment_password: '请补充支付密码，或关闭自动支付授权后只创建待支付订单。',
    complete_profile: '请先完善个人资料。',
    create_pet_profile: '请先添加宠物档案。',
    retry_later: '请稍后再试。',
  }
  return map[action] || action
}
</script>

<style scoped>
.agent-page {
  display: grid;
  gap: 16px;
}

.agent-shell {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface, #fff);
  min-height: 620px;
  display: grid;
  grid-template-rows: 1fr auto;
}

.chat-messages {
  padding: 18px;
  overflow-y: auto;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(760px, 88%);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px 14px;
  background: var(--color-muted);
  color: var(--color-foreground);
}

.message-row--user .message-bubble {
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-color: transparent;
}

.message-bubble p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-meta,
.message-actions,
.auth-row,
.submit-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.result-meta {
  margin-top: 10px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.next-action {
  margin-top: 10px;
  color: #ad6800;
  font-size: 13px;
}

.message-actions {
  margin-top: 12px;
}

.agent-logs {
  margin-top: 12px;
  font-size: 13px;
}

.agent-logs ol {
  margin: 8px 0 0 18px;
  padding: 0;
  color: var(--color-muted-foreground);
}

.agent-input-panel {
  border-top: 1px solid var(--color-border);
  padding: 14px;
  display: grid;
  gap: 12px;
}

.agent-input-panel textarea,
.payment-password {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 10px 12px;
  resize: vertical;
  background: var(--color-background, #fff);
  color: var(--color-foreground);
}

.auth-row,
.submit-row {
  justify-content: space-between;
}

.switch-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-foreground);
}

.submit-row span {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

@media (max-width: 640px) {
  .agent-shell {
    min-height: 560px;
  }

  .message-bubble {
    max-width: 100%;
  }

  .auth-row,
  .submit-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
