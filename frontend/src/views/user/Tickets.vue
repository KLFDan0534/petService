<template>
  <div>
    <PageHero title="工单管理" subtitle="提交和查看您的申诉与投诉" />

    <div style="margin-bottom:24px">
      <button class="btn btn-primary" @click="showForm = true">+ 新建申诉</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="tickets.length === 0" class="empty-state">
      <div class="icon">🎫</div>
      <h3>暂无工单</h3>
      <p>当您对订单有疑问时，可以提交申诉或投诉</p>
      <button class="btn btn-primary" @click="showForm = true">新建申诉</button>
    </div>
    <div v-else>
      <div
        v-for="t in tickets"
        :key="t.id_wsh"
        class="card"
        style="margin-bottom:12px;cursor:pointer"
        @click="selectTicket(t)"
      >
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
          <div style="display:flex;align-items:center;gap:8px">
            <span :class="['badge', categoryBadge(t.category_wsh)]">{{ categoryLabel(t.category_wsh) }}</span>
            <strong>{{ t.title_wsh }}</strong>
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            <span :class="['badge', t.priority_wsh === 'high' ? 'badge-error' : t.priority_wsh === 'medium' ? 'badge-warning' : 'badge-info']">{{ priorityLabel(t.priority_wsh) }}</span>
            <span :class="['badge', statusBadge(t.status_wsh)]">{{ statusLabel(t.status_wsh) }}</span>
          </div>
        </div>
        <p style="margin-top:8px;font-size:14px;color:var(--color-muted-foreground)">{{ t.content_wsh }}</p>
        <div style="font-size:12px;color:var(--color-muted-foreground);margin-top:4px">{{ new Date(t.created_at_wsh).toLocaleString() }}</div>
      </div>
    </div>

    <!-- 新建工单弹窗 -->
    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>新建申诉</h2>
        <form @submit.prevent="createTicket">
          <div class="form-group">
            <label>类型</label>
            <select v-model="form.category_wsh" required>
              <option value="appeal">订单申诉</option>
              <option value="complaint">服务投诉</option>
              <option value="other">其他</option>
            </select>
          </div>
          <div class="form-group">
            <label>优先级</label>
            <select v-model="form.priority_wsh" required>
              <option value="medium">普通</option>
              <option value="high">紧急</option>
              <option value="low">低</option>
            </select>
          </div>
          <div class="form-group">
            <label>所属商家</label>
            <select v-model="form.merchant_id_wsh" required>
              <option :value="null" disabled>请选择商家</option>
              <option v-for="m in approvedMerchants" :key="m.id_wsh" :value="m.id_wsh">{{ m.name_wsh }}</option>
            </select>
          </div>
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group"><label>描述</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">提交</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 工单详情弹窗 -->
    <div v-if="selectedTicket" class="modal-overlay" @mousedown.self="selectedTicket = null">
      <div class="modal" style="max-width:600px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
          <h2 style="margin:0">{{ selectedTicket.title_wsh }}</h2>
          <button class="btn btn-secondary btn-sm" @click="selectedTicket = null">关闭</button>
        </div>

        <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
          <span :class="['badge', categoryBadge(selectedTicket.category_wsh)]">{{ categoryLabel(selectedTicket.category_wsh) }}</span>
          <span :class="['badge', statusBadge(selectedTicket.status_wsh)]">{{ statusLabel(selectedTicket.status_wsh) }}</span>
          <span :class="['badge', 'badge-info']">{{ priorityLabel(selectedTicket.priority_wsh) }}</span>
        </div>

        <p style="margin-bottom:16px">{{ selectedTicket.content_wsh }}</p>

        <div v-if="selectedTicket.status_wsh === 'resolved' && selectedTicket.result_wsh" class="alert alert-success" style="margin-bottom:16px">
          <strong>处理结果：</strong>{{ selectedTicket.result_wsh }}
        </div>

        <h4 style="margin-bottom:8px">沟通记录</h4>
        <div v-if="messages.length === 0" style="color:var(--color-muted-foreground);font-size:14px;margin-bottom:12px">暂无消息</div>
        <div v-for="m in messages" :key="m.id_wsh" class="card" style="margin-bottom:8px;padding:12px;font-size:14px">
          <div style="display:flex;justify-content:space-between;margin-bottom:4px">
            <strong style="font-size:12px">{{ m.user_id_wsh === currentUserId ? '我' : '客服' }}</strong>
            <span style="font-size:11px;color:var(--color-muted-foreground)">{{ new Date(m.created_at_wsh).toLocaleString() }}</span>
          </div>
          <div>{{ m.content_wsh }}</div>
        </div>

        <div v-if="editing" class="form-group" style="margin-top:12px">
          <label>上传证据 / 回复</label>
          <textarea v-model="evidenceContent" rows="3" placeholder="请输入证据描述或回复内容"></textarea>
        </div>
        <div style="display:flex;gap:8px;margin-top:12px">
          <button v-if="!editing" class="btn btn-primary btn-sm" @click="editing = true">上传证据</button>
          <template v-if="editing">
            <button class="btn btn-primary btn-sm" @click="submitEvidence" :disabled="!evidenceContent.trim()">提交</button>
            <button class="btn btn-secondary btn-sm" @click="cancelEvidence">取消</button>
          </template>
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
import PageHero from '@/components/common/PageHero.vue'

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
