<template>
  <div>
    <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;align-items:center">
      <select v-model="filter.module" class="input" style="width:140px">
        <option value="">全部模块</option>
        <option value="pet">宠物</option>
        <option value="order">订单</option>
        <option value="merchant">商家</option>
        <option value="user">用户</option>
        <option value="notice">公告</option>
      </select>
      <select v-model="filter.operation" class="input" style="width:140px">
        <option value="">全部操作</option>
        <option value="create">新增</option>
        <option value="update">更新</option>
        <option value="delete">删除</option>
        <option value="cancel">取消</option>
        <option value="approve">审核通过</option>
        <option value="reject">驳回</option>
      </select>
      <select v-model="filter.status" class="input" style="width:120px">
        <option value="">全部状态</option>
        <option value="1">成功</option>
        <option value="0">失败</option>
      </select>
      <button class="btn btn-primary btn-sm" @click="load">查询</button>
    </div>
    <div style="overflow-x:auto">
      <table class="table">
        <thead><tr><th>编号</th><th>用户</th><th>模块</th><th>操作</th><th>描述</th><th>接口地址</th><th>耗时</th><th>状态</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id_wsh" :class="{'row-error': log.status_wsh === 0}">
            <td>{{ log.id_wsh }}</td>
            <td>{{ log.username_wsh }}</td>
            <td><span class="tag">{{ log.module_wsh }}</span></td>
            <td>{{ log.operation_wsh }}</td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ log.description_wsh }}</td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:12px">{{ log.request_url_wsh }}</td>
            <td>{{ log.duration_wsh }}毫秒</td>
            <td><span :class="log.status_wsh === 1 ? 'text-success' : 'text-danger'">{{ log.status_wsh === 1 ? '成功' : '失败' }}</span></td>
            <td style="font-size:12px">{{ log.created_at_wsh }}</td>
          </tr>
          <tr v-if="logs.length === 0"><td colspan="9" style="text-align:center">暂无数据</td></tr>
        </tbody>
      </table>
    </div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
      <span style="font-size:13px;color:var(--color-muted-foreground)">共 {{ total }} 条</span>
      <div style="display:flex;gap:8px">
        <button class="btn btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span style="line-height:32px">第 {{ page }} / {{ pages }} 页</span>
        <button class="btn btn-sm" :disabled="page >= pages" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs } from '@/api/admin'


const logs = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const filter = reactive({ module: '', operation: '', status: '' })

onMounted(() => { load() })

async function load() {
  try {
    const params = { page: page.value, size: size.value }
    if (filter.module) params.module = filter.module
    if (filter.operation) params.operation = filter.operation
    if (filter.status !== '') params.status = filter.status
    const r = await getOperationLogs(params)
    if (r.code === 200) {
      logs.value = r.data?.list || r.data?.records || []
      total.value = r.data.total || 0
      pages.value = r.data.pages || r.data.pages_wsh || 0
    }
  } catch (e) { /* ignore */ }
}
</script>

<style scoped>
.table { width:100%; border-collapse:collapse; font-size:13px }
.table th, .table td { padding:8px 10px; border-bottom:1px solid var(--color-border); text-align:left }
.table th { background:var(--color-muted); font-weight:600; color:var(--color-muted-foreground) }
.row-error { background:rgba(229,62,62,0.05) }
.tag { display:inline-block; padding:2px 8px; border-radius:4px; background:var(--color-primary); color:#fff; font-size:11px }
.text-success { color:var(--color-success) }
.text-danger { color:var(--color-danger) }
.input { padding:6px 10px; border:1px solid var(--color-border); border-radius:var(--radius-inline); background:var(--color-background); color:var(--color-foreground); font-size:13px }
</style>
