<template>
  <div>
    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'标题',key:'title_wsh'},
      {label:'优先级',key:'priority_wsh'},
      {label:'用户',key:'user_name_wsh'},
      {label:'状态',key:'status_label_wsh'},
      {label:'创建时间',key:'created_at_wsh'}
    ]" :data="tickets">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-info" @click="assignTicket(row.id_wsh)">分配</button>
          <button v-if="row.status_wsh === 'processing'" class="btn btn-sm btn-success" @click="resolve(row.id_wsh)">解决</button>
          <button v-if="row.status_wsh === 'resolved'" class="btn btn-sm btn-danger" @click="close(row.id_wsh)">关闭</button>
        </div>
      </template>
    </DataTable>

    <div v-if="pendingResolve" class="modal-overlay" @mousedown.self="cancelResolve">
      <div class="modal" style="max-width:460px">
        <h3>解决工单</h3>
        <div class="form-group" style="margin-top:16px">
          <label>处理结果</label>
          <textarea v-model="resolveResult" rows="4" placeholder="请输入处理结果"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" type="button" @click="cancelResolve">取消</button>
          <button class="btn btn-primary btn-sm" type="button" :disabled="!resolveResult.trim()" @click="confirmResolve">确认解决</button>
        </div>
      </div>
    </div>

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import DataTable from '@/components/common/DataTable.vue'
import { getTickets, assignTicket as apiAssignTicket, resolveTicket, closeTicket } from '@/api/ticket'
import { TicketStatus, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
const authStore = useAuthStore()
const tickets = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const pendingResolve = ref(null)
const resolveResult = ref('')
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrichTicket(t) {
  return enrichWithStatus(t, 'status_wsh', TicketStatus)
}

async function loadTickets() {
  try {
    const r = await getTickets(currentPage.value, size.value)
    if (r.code === 200) {
      tickets.value = (r.data.list || []).map(enrichTicket)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function changePage(page) {
  currentPage.value = page
  loadTickets()
}

onMounted(loadTickets)

async function assignTicket(id) {
  try {
    await apiAssignTicket(id, authStore.user?.id_wsh)
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
</script>
