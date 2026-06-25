<template>
  <div>
    <PageHero title="AI 助手" subtitle="宠物护理智能问答 / 培养建议 / 寄养报告" />
    <div class="card" style="margin-bottom:16px;padding:16px">
      <div style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <div class="form-group" style="margin-bottom:0;min-width:180px">
          <label style="font-size:13px;margin-bottom:4px;display:block">选择宠物</label>
          <select v-model="selectedPetId" class="form-control" @change="onPetChange">
            <option value="">-- 请选择 --</option>
            <option v-for="p in pets" :key="p.id_wsh" :value="p.id_wsh">{{ p.name_wsh || p.pet_name_wsh }}</option>
          </select>
        </div>
        <div class="form-group" style="margin-bottom:0;min-width:200px">
          <label style="font-size:13px;margin-bottom:4px;display:block">已完成订单</label>
          <select v-model="selectedOrderId" class="form-control" :disabled="!selectedPetId">
            <option value="">-- 请选择 --</option>
            <option v-for="o in completedOrders" :key="o.id_wsh" :value="o.id_wsh">
              #{{ o.id_wsh }} {{ formatDate(o.created_at_wsh) }}
            </option>
          </select>
        </div>
        <button class="btn btn-primary" :disabled="!selectedPetId || !selectedOrderId || reportLoading" @click="generateReport('care_suggestion')">
          生成培养建议
        </button>
        <button class="btn btn-success" :disabled="!selectedPetId || !selectedOrderId || reportLoading" @click="generateReport('boarding_report')">
          生成培养完成报告
        </button>
      </div>
    </div>
    <div class="chat-container">
      <div class="chat-conversation">
        <div class="chat-messages">
          <div v-for="msg in messages" :key="msg.id" style="margin-bottom:16px">
            <div :style="{ textAlign: msg.role === 'user' ? 'right' : 'left' }">
              <span class="card" :style="{ display:'inline-block', padding:'8px 16px', borderRadius:'12px',
                background: msg.role === 'user' ? 'var(--color-primary)' : 'var(--color-muted)',
                color: msg.role === 'user' ? 'var(--color-on-primary)' : 'var(--color-foreground)' }">
                {{ msg.content }}
              </span>
            </div>
            <div v-if="msg.reportType" style="margin-top:8px;margin-left:16px">
              <span class="badge" :class="msg.reportType === 'care_suggestion' ? 'badge-info' : 'badge-success'">
                {{ msg.reportType === 'care_suggestion' ? '培养建议' : '寄养报告' }}
              </span>
            </div>
          </div>
          <div v-if="loading" class="loading">AI 思考中</div>
          <div v-if="reportLoading" class="loading">生成报告中...</div>
        </div>
        <div class="chat-input-area">
          <input v-model="question" @keyup.enter="ask" placeholder="输入您的问题..." style="flex:1">
          <button class="btn btn-primary btn-sm" @click="ask">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()

const messages = ref([{ id: 0, role: 'ai', content: '您好！我是宠物护理 AI 助手' }])
const question = ref('')
const loading = ref(false)
const reportLoading = ref(false)

const pets = ref([])
const orders = ref([])
const selectedPetId = ref('')
const selectedOrderId = ref('')

const completedOrders = computed(() => {
  if (!selectedPetId.value) return []
  return orders.value.filter(o =>
    o.status_wsh === 'completed' && String(o.pet_id_wsh) === String(selectedPetId.value)
  )
})

function formatDate(dt) {
  if (!dt) return ''
  return dt.slice(0, 10)
}

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/api/pets')
    if (r.code === 200) pets.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {}
  try {
    const r = await authStore.apiGet('/api/orders')
    if (r.code === 200) orders.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {}
})

function onPetChange() {
  selectedOrderId.value = ''
}

async function generateReport(type) {
  if (!selectedPetId.value || !selectedOrderId.value) return
  reportLoading.value = true
  const label = type === 'care_suggestion' ? '培养建议' : '培养完成报告'
  messages.value.push({ id: Date.now(), role: 'user', content: `为宠物 #${selectedPetId.value} 订单 #${selectedOrderId.value} 生成${label}` })
  try {
    const endpoint = type === 'care_suggestion' ? '/api/ai/care-suggestion' : '/api/ai/boarding-report'
    const r = await authStore.apiPost(endpoint, { pet_id_wsh: Number(selectedPetId.value), order_id_wsh: Number(selectedOrderId.value) })
    if (r.code === 200) {
      messages.value.push({ id: Date.now(), role: 'ai', content: r.data.content_wsh || '生成成功', reportType: type })
    } else {
      messages.value.push({ id: Date.now(), role: 'ai', content: '生成失败：' + (r.message || '未知错误') })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: '生成报告失败，请稍后重试。' })
  }
  reportLoading.value = false
}

async function ask() {
  if (!question.value.trim()) return
  const q = question.value
  messages.value.push({ id: Date.now(), role: 'user', content: q })
  question.value = ''
  loading.value = true
  try {
    const r = await authStore.apiPost('/api/rag/ask', { question: q })
    if (r.code === 200) {
      messages.value.push({ id: Date.now(), role: 'ai', content: r.data.reply_wsh || r.data.answer || '暂无回复' })
    }
  } catch (e) {
    messages.value.push({ id: Date.now(), role: 'ai', content: '请求失败，请稍后重试。' })
  }
  loading.value = false
}
</script>
