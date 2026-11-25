<template>
  <div>
    <section class="toolbar-card">
      <div>
        <h2>提现审核</h2>
        <p>申请时冻结余额，通过后等待线下打款，完成打款后才正式扣减钱包余额。</p>
      </div>
      <button class="btn btn-outline btn-sm" type="button" @click="loadWithdrawals">刷新</button>
    </section>

    <DataTable
      :columns="[
        { key: 'id_wsh', label: 'ID' },
        { key: 'user_id_wsh', label: '用户' },
        { key: 'amount_wsh', label: '金额' },
        { key: 'bank_card_wsh', label: '提现方式' },
        { key: 'status_label_wsh', label: '状态' }
      ]"
      :data="withdrawals"
    >
      <template #default="{ row }">
        <div class="actions">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" type="button" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" type="button" @click="reject(row.id_wsh)">拒绝</button>
          <button v-if="row.status_wsh === 'approved'" class="btn btn-sm btn-primary" type="button" @click="complete(row.id_wsh)">完成打款</button>
        </div>
      </template>
    </DataTable>

    <div class="pager-bar">
      <div class="pager-size">
        <span>每页</span>
        <select v-model.number="size" :disabled="loading" @change="changeSize">
          <option v-for="n in pageSizes" :key="n" :value="n">{{ n }} 条</option>
        </select>
      </div>
      <span class="pager-info">共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页</span>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || page <= 1" @click="goPage(page - 1)">上一页</button>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || pages === 0 || page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getWithdrawals, approveWithdrawal, rejectWithdrawal, completeWithdrawal } from '@/api/wallet'
import { WithdrawalStatus, enrichWithStatus } from '@/constants/statusMaps'

const appStore = useAppStore()
const withdrawals = ref([])

// 分页状态：提现记录持续增长，后端已支持分页，这里补上翻页控件
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const pageSizes = [20, 50, 100]

// 补上状态文字，表格状态列才有中文可看
function enrichWithdrawal(w) {
  return enrichWithStatus(w, 'status_wsh', WithdrawalStatus)
}

// 接口回来的是分页结构，真正的数组在 list 里
async function loadWithdrawals() {
  loading.value = true
  try {
    const r = await getWithdrawals({ page: page.value, size: size.value })
    if (r.code === 200) {
      const data = r.data || {}
      withdrawals.value = (data.list || []).map(enrichWithdrawal)
      total.value = data.total || 0
      pages.value = data.pages || 0
      if (data.page) page.value = data.page
    }
  } catch (e) {
    appStore.addToast(e?.message || '提现记录加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function changeSize() {
  page.value = 1
  await loadWithdrawals()
}

async function goPage(target) {
  if (target < 1 || target === page.value) return
  if (pages.value && target > pages.value) return
  page.value = target
  await loadWithdrawals()
}

onMounted(loadWithdrawals)

// 通过只是同意申请，钱还冻着，要等线下打款完才算扣
async function approve(id) {
  try {
    await approveWithdrawal(id)
    appStore.addToast('已通过', 'success')
    await loadWithdrawals()
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
  }
}

async function reject(id) {
  try {
    await rejectWithdrawal(id)
    appStore.addToast('已拒绝', 'success')
    await loadWithdrawals()
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
  }
}

// 这一步才真正从钱包里扣钱，所以单独一个按钮，跟"通过"分开
async function complete(id) {
  try {
    await completeWithdrawal(id)
    appStore.addToast('提现已完成', 'success')
    await loadWithdrawals()
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
  }
}
</script>

<style scoped>
.pager-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.pager-size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: auto;
}
.pager-size select {
  width: auto;
  min-width: 84px;
}
.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
}
.toolbar-card h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.toolbar-card p {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
