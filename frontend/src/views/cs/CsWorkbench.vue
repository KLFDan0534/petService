<template>
  <div class="cs-workbench">
    <div class="wb-header">
      <h2>客服工作台</h2>
      <p>欢迎回来，{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else>
      <!-- 统计卡片 -->
      <section class="stat-grid">
        <div class="stat-card" @click="goTickets('pending')">
          <span class="stat-value">{{ stats.pending_tickets }}</span>
          <span class="stat-label">待处理工单</span>
        </div>
        <div class="stat-card" @click="goTickets('processing', true)">
          <span class="stat-value">{{ stats.my_processing_tickets }}</span>
          <span class="stat-label">我处理中</span>
        </div>
        <div class="stat-card" @click="goComplaints('pending')">
          <span class="stat-value">{{ stats.pending_complaints }}</span>
          <span class="stat-label">待处理投诉</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ stats.resolved_tickets_today + stats.resolved_complaints_today }}</span>
          <span class="stat-label">今日解决</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ stats.merchant_count }}</span>
          <span class="stat-label">服务商家</span>
        </div>
      </section>

      <div class="wb-columns">
        <!-- 服务商家（多商家入口） -->
        <section class="panel">
          <h3>我服务的商家</h3>
          <div v-if="!merchants.length" class="empty-inline">暂无服务商家，请先通过客服申请并获商家审核</div>
          <div v-for="m in merchants" :key="m.merchant_id_wsh" class="merchant-row">
            <div class="merchant-info">
              <strong>{{ m.merchant_name_wsh }}</strong>
              <span class="merchant-badges">
                <span class="badge badge-warning">工单 {{ m.pending_ticket_count }}</span>
                <span class="badge badge-danger">投诉 {{ m.pending_complaint_count }}</span>
              </span>
            </div>
            <div class="merchant-actions">
              <router-link
                class="btn btn-sm btn-outline"
                :to="{ path: '/merchant/support/tickets', query: { merchant_id_wsh: m.merchant_id_wsh } }"
              >工单</router-link>
              <router-link
                class="btn btn-sm btn-outline"
                :to="{ path: '/merchant/support/complaints', query: { merchant_id_wsh: m.merchant_id_wsh } }"
              >投诉</router-link>
            </div>
          </div>
        </section>

        <!-- 最近会话（实时聊天入口） -->
        <section class="panel">
          <h3>最近会话 <router-link class="btn btn-sm" to="/merchant/support/chat">进入聊天</router-link></h3>
          <div v-if="!conversations.length" class="empty-inline">暂无会话</div>
          <router-link
            v-for="c in conversations.slice(0, 5)"
            :key="c.other_user_id_wsh"
            class="conv-row"
            :to="{ path: '/merchant/support/chat', query: { userId: c.other_user_id_wsh } }"
          >
            <div class="conv-info">
              <strong>{{ c.other_user_name_wsh }}</strong>
              <span class="conv-preview">{{ c.last_message_wsh || '(空)' }}</span>
            </div>
            <span v-if="c.unread_count > 0" class="badge badge-danger">{{ c.unread_count }}</span>
          </router-link>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getCsStats, getCsMerchants, getCsConversations } from '@/api/csWorkbench'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const loading = ref(true)
const stats = ref({ pending_tickets: 0, my_processing_tickets: 0, pending_complaints: 0, resolved_tickets_today: 0, resolved_complaints_today: 0, merchant_count: 0 })
const merchants = ref([])
const conversations = ref([])

async function load() {
  loading.value = true
  try {
    const [s, m, c] = await Promise.all([getCsStats(), getCsMerchants(), getCsConversations()])
    if (s.code === 200) stats.value = s.data || {}
    if (m.code === 200) merchants.value = m.data || []
    if (c.code === 200) conversations.value = c.data || []
  } catch (e) {
    appStore.addToast('工作台加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function goTickets(status, onlyMine = false) {
  router.push({ path: '/merchant/support/tickets', query: { status_wsh: status, ...(onlyMine ? { assignee_id_wsh: authStore.user?.id_wsh } : {}) } })
}

function goComplaints(status) {
  router.push({ path: '/merchant/support/complaints', query: { status_wsh: status } })
}

onMounted(load)
</script>

<style scoped>
.cs-workbench {
  display: grid;
  gap: 20px;
}
.wb-header h2 {
  margin: 0 0 4px;
}
.wb-header p {
  margin: 0;
  color: var(--color-muted-foreground);
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.stat-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 16px;
  background: var(--color-card);
  cursor: pointer;
  transition: box-shadow 180ms ease, transform 180ms ease;
}
.stat-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
}
.stat-label {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.wb-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 900px) {
  .wb-columns {
    grid-template-columns: 1fr;
  }
}
.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 16px;
  background: var(--color-card);
}
.panel h3 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 12px;
}
.merchant-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
}
.merchant-row:last-child {
  border-bottom: none;
}
.merchant-info {
  display: grid;
  gap: 4px;
}
.merchant-badges {
  display: flex;
  gap: 6px;
}
.merchant-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.conv-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
  color: inherit;
  text-decoration: none;
}
.conv-row:last-child {
  border-bottom: none;
}
.conv-info {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.conv-preview {
  font-size: 12px;
  color: var(--color-muted-foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty-inline {
  color: var(--color-muted-foreground);
  font-size: 13px;
  padding: 12px 0;
}
</style>
