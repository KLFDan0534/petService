<template>
  <div>
    <div class="ticket-filters">
      <select v-model="filters.status_wsh" class="form-control" @change="applyFilters">
        <option value="">全部状态</option>
        <option value="pending">待处理</option>
        <option value="processing">处理中</option>
        <option value="resolved">已解决</option>
        <option value="closed">已关闭</option>
      </select>
      <select v-model="filters.category_wsh" class="form-control" @change="applyFilters">
        <option value="">全部分类</option>
        <option value="complaint">投诉</option>
        <option value="question">咨询</option>
        <option value="suggestion">建议</option>
        <option value="other">其他</option>
      </select>
      <select v-model="filters.priority_wsh" class="form-control" @change="applyFilters">
        <option value="">全部优先级</option>
        <option value="low">低</option>
        <option value="medium">中</option>
        <option value="high">高</option>
        <option value="urgent">紧急</option>
      </select>
      <input
        v-model="filters.keyword"
        class="form-control"
        placeholder="搜索标题/内容关键字"
        @keyup.enter="applyFilters"
      >
      <label class="my-only">
        <input v-model="filters.onlyMine" type="checkbox" @change="applyFilters">
        只看我的
      </label>
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">筛选</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'标题',key:'title_wsh'},
      {label:'优先级',key:'priority_label_wsh'},
      {label:'分类',key:'category_label_wsh'},
      {label:'用户',key:'user_name_wsh'},
      {label:'商家',key:'merchant_name_wsh'},
      {label:'状态',key:'status_label_wsh'},
      {label:'创建时间',key:'created_at_wsh'}
    ]" :data="tickets">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-info" @click="acceptTicket(row.id_wsh)">受理</button>
          <button v-if="row.status_wsh === 'processing'" class="btn btn-sm btn-success" @click="resolve(row.id_wsh)">解决</button>
          <button v-if="row.status_wsh === 'resolved'" class="btn btn-sm btn-danger" @click="close(row.id_wsh)">关闭</button>
          <button class="btn btn-sm btn-info" @click="goChat(row)">回复用户</button>
        </div>
      </template>
    </DataTable>

    <AppDialog
      :visible="pendingResolve"
      :width="460"
      title="解决工单"
      @close="cancelResolve"
    >
      <div class="form-group" style="margin-top:16px">
        <label>处理结果</label>
        <textarea v-model="resolveResult" rows="4" placeholder="请输入处理结果"></textarea>
      </div>
      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="cancelResolve">取消</button>
        <button class="btn btn-primary btn-sm" type="button" :disabled="!resolveResult.trim()" @click="confirmResolve">确认解决</button>
      </template>
    </AppDialog>

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { getTickets, assignTicket as apiAssignTicket, resolveTicket, closeTicket } from '@/api/ticket'
import { TicketStatus, TicketCategory, TicketPriority, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
const authStore = useAuthStore()
const tickets = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const pendingResolve = ref(null)
const resolveResult = ref('')
const filters = reactive({ status_wsh: '', category_wsh: '', priority_wsh: '', keyword: '', onlyMine: false, merchant_id_wsh: null })
const route = useRoute()
const router = useRouter()
const currentUserId = computed(() => authStore.user?.id_wsh)
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrichTicket(t) {
  const enriched = enrichWithStatus(t, 'status_wsh', TicketStatus)
  enriched.priority_label_wsh = TicketPriority[t.priority_wsh]?.label || t.priority_wsh || '-'
  enriched.category_label_wsh = TicketCategory[t.category_wsh]?.label || t.category_wsh || '-'
  return enriched
}

function buildFilterParams() {
  const params = {}
  if (filters.status_wsh) params.status_wsh = filters.status_wsh
  if (filters.category_wsh) params.category_wsh = filters.category_wsh
  if (filters.priority_wsh) params.priority_wsh = filters.priority_wsh
  if (filters.keyword && filters.keyword.trim()) params.keyword = filters.keyword.trim()
  if (filters.onlyMine) params.assignee_id_wsh = currentUserId.value
  if (filters.merchant_id_wsh) params.merchant_id_wsh = filters.merchant_id_wsh
  return params
}

async function loadTickets() {
  try {
    const r = await getTickets(currentPage.value, size.value, buildFilterParams())
    if (r.code === 200) {
      tickets.value = (r.data.list || []).map(enrichTicket)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() {
  currentPage.value = 1
  loadTickets()
}

function resetFilters() {
  filters.status_wsh = ''
  filters.category_wsh = ''
  filters.priority_wsh = ''
  filters.keyword = ''
  filters.onlyMine = false
  applyFilters()
}

function changePage(page) {
  currentPage.value = page
  loadTickets()
}

onMounted(() => {
  if (route.query.status_wsh) filters.status_wsh = String(route.query.status_wsh)
  if (route.query.merchant_id_wsh) filters.merchant_id_wsh = Number(route.query.merchant_id_wsh)
  if (route.query.assignee_id_wsh && Number(route.query.assignee_id_wsh) === currentUserId.value) filters.onlyMine = true
  loadTickets()
})

async function acceptTicket(id) {
  try {
    await apiAssignTicket(id, currentUserId.value)
    appStore.addToast('已受理', 'success')
    await loadTickets()
  } catch (e) { appStore.addToast('受理失败', 'error') }
}

async function assignTicket(id) {
  try {
    await apiAssignTicket(id, currentUserId.value)
    appStore.addToast('已分配', 'success')
    await loadTickets()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

function resolve(id) {
  pendingResolve.value = id
  resolveResult.value = '工单已处理完成'
}

function cancelResolve() {
  pendingResolve.value = null
  resolveResult.value = ''
}

async function confirmResolve() {
  if (!pendingResolve.value || !resolveResult.value.trim()) return
  try {
    await resolveTicket(pendingResolve.value, resolveResult.value.trim())
    appStore.addToast('已解决', 'success')
    cancelResolve()
    await loadTickets()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function close(id) {
  try {
    await closeTicket(id)
    appStore.addToast('已关闭', 'success')
    await loadTickets()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

function goChat(row) {
  router.push({
    path: '/merchant/support/chat',
    query: {
      bizType: 'ticket',
      bizId: row.id_wsh,
      userId: row.user_id_wsh || '',
      name: row.user_name_wsh || '',
      title: row.title_wsh || '',
      status: row.status_wsh || '',
    },
  })
}
</script>

<style scoped>
.ticket-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 14px;
}
.ticket-filters .form-control {
  width: auto;
  min-width: 120px;
}
.my-only {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  white-space: nowrap;
}
.empty-inline {
  color: var(--color-muted-foreground);
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}
</style>
