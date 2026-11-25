<template>
  <div>
    <DataTable :columns="[{ label: 'ID', key: 'id_wsh' }, { label: '金额', key: 'amount_wsh' }, { label: '原因', key: 'reason_wsh' }, { label: '状态', key: 'status_label_wsh' }]" :data="refunds">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
          <button v-if="row.status_wsh === 'approved'" class="btn btn-sm btn-primary" @click="completeRefund(row.id_wsh)">完成退款</button>

        </div>
      </template>
    </DataTable>

    <div style="display:flex;align-items:center;justify-content:flex-end;gap:8px;flex-wrap:wrap;margin-top:12px;font-size:13px">
      <div style="display:inline-flex;align-items:center;gap:6px;margin-right:auto">
        <span>每页</span>
        <select v-model.number="size" :disabled="loading" style="width:auto;min-width:84px" @change="changeSize">
          <option v-for="n in pageSizes" :key="n" :value="n">{{ n }} 条</option>
        </select>
      </div>
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页</span>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || page <= 1" @click="goPage(page - 1)">上一页</button>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || pages === 0 || page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getRefundsPage, approveRefund, rejectRefund, completeRefund as apiCompleteRefund } from '@/api/refund'
import { RefundStatus, enrichWithStatus } from '@/constants/statusMaps'


const appStore = useAppStore()
// 退款单只读展示，能做的动作只有通过、拒绝、完成这三个
const refunds = ref([])
// 分页状态：后端已提供 /refunds/all/page
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const pageSizes = [20, 50, 100]

function enrichRefund(r) {
  return enrichWithStatus(r, 'status_wsh', RefundStatus)
}

async function loadRefunds() {
  loading.value = true
  try {
    const r = await getRefundsPage({ page: page.value, size: size.value })
    if (r.code === 200) {
      const data = r.data || {}
      refunds.value = (data.list || []).map(enrichRefund)
      total.value = data.total || 0
      pages.value = data.pages || 0
      if (data.page) page.value = data.page
    }
  } catch (e) {
    // 失败保留上一次结果
  } finally {
    loading.value = false
  }
}

async function changeSize() {
  page.value = 1
  await loadRefunds()
}

async function goPage(target) {
  if (target < 1 || target === page.value) return
  if (pages.value && target > pages.value) return
  page.value = target
  await loadRefunds()
}
onMounted(loadRefunds)

// 退款分两步走：先审通过，再点「完成退款」才真的把钱退回去，中间那步别省
async function approve(id) { try { await approveRefund(id); appStore.addToast('已通过', 'success'); await loadRefunds() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await rejectRefund(id); appStore.addToast('已拒绝', 'success'); await loadRefunds() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function completeRefund(id) { try { await apiCompleteRefund(id); appStore.addToast('退款完成', 'success'); await loadRefunds() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
