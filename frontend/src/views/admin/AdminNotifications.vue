<template>
  <div>
    <div class="admin-filters">
      <select v-model="filters.type_wsh" class="form-control" @change="applyFilters">
        <option value="">全部类型</option>
        <option value="system">系统</option>
        <option value="order">订单</option>
        <option value="notice">公告</option>
      </select>
      <select v-model="filters.is_read_wsh" class="form-control" @change="applyFilters">
        <option value="">全部状态</option>
        <option value="0">未读</option>
        <option value="1">已读</option>
      </select>
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">筛选</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'标题',key:'title_wsh'},
      {label:'内容',key:'content_wsh'},
      {label:'类型',key:'type_label_wsh'},
      {label:'目标用户',key:'user_name_wsh'},
      {label:'已读状态',key:'read_label_wsh'},
      {label:'发送时间',key:'created_at_wsh'}
    ]" :data="notifications" />

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import { getAdminNotifications } from '@/api/notification'
import { NotificationTypeMap, ReadStatus, enrichWithStatus } from '@/constants/statusMaps'

const notifications = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ type_wsh: '', is_read_wsh: '' })
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrich(n) {
  const e = enrichWithStatus(n, 'type_wsh', NotificationTypeMap, 'type_label_wsh')
  e.read_label_wsh = { label: ReadStatus[e.is_read_wsh]?.label || (e.is_read_wsh ? '已读' : '未读'), badge: ReadStatus[e.is_read_wsh]?.badge || 'badge-info' }
  e.user_name_wsh = e.user_name_wsh || '-'
  return e
}

function buildParams() {
  const params = { page: currentPage.value, size: size.value }
  if (filters.type_wsh) params.type_wsh = filters.type_wsh
  if (filters.is_read_wsh !== '') params.is_read_wsh = filters.is_read_wsh
  return params
}

async function load() {
  try {
    const r = await getAdminNotifications(buildParams())
    if (r.code === 200) {
      notifications.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { filters.type_wsh = ''; filters.is_read_wsh = ''; applyFilters() }
function changePage(page) { currentPage.value = page; load() }

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 120px; }
</style>