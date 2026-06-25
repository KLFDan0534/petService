<template>
  <div>
    <div style="display:flex;gap:8px;margin-bottom:16px">
      <button v-for="t in tabs" :key="t.key" :class="['btn', activeTab === t.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        @click="activeTab = t.key">{{ t.label }}</button>
      <button class="btn btn-sm btn-success" style="margin-left:auto" :disabled="seeding" @click="seedTestOrders">{{ seeding ? '生成中...' : '生成测试订单' }}</button>
    </div>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'用户',key:'owner_name_wsh'},{label:'服务',key:'service_name_wsh'},{label:'金额',key:'total_amount_wsh'},{label:'状态',key:'status_label_wsh'},{label:'时间',key:'created_at_wsh'},]" :data="filteredOrders">
      <template #default="{ row }">
        <select class="form-control" style="width:120px;display:inline-block" :value="row.status_wsh" @change="changeStatus(row, $event.target.value)">
          <option v-for="s in statuses" :key="s" :value="s" :disabled="s === row.status_wsh">{{ statusLabels[s] || s }}</option>
        </select>
        <button class="btn btn-sm btn-danger" style="margin-left:4px" @click="cancelOrder(row.id_wsh)">取消</button>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const orders = ref([])
const activeTab = ref('all')
const seeding = ref(false)
const tabs = [
  { key: 'all', label: '全部' }, { key: 'pending', label: '待处理' },
  { key: 'paid', label: '已支付' },
  { key: 'confirmed', label: '已确认' }, { key: 'in_progress', label: '进行中' },
  { key: 'completed', label: '已完成' }, { key: 'cancelled', label: '已取消' },
]
const statuses = ['pending', 'paid', 'confirmed', 'in_progress', 'completed', 'cancelled']
const statusLabels = { pending: '待支付', paid: '已支付', confirmed: '已确认', in_progress: '进行中', completed: '已完成', cancelled: '已取消' }

const orderStatusMap = { pending: { text: '待支付', cls: 'badge-warning' }, paid: { text: '已支付', cls: 'badge-info' }, confirmed: { text: '已确认', cls: 'badge-info' }, in_progress: { text: '进行中', cls: 'badge-info' }, completed: { text: '已完成', cls: 'badge-success' }, cancelled: { text: '已取消', cls: 'badge-danger' } }

function enrichOrder(o) {
  const s = orderStatusMap[o.status_wsh]
  o.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : o.status_wsh
  return o
}

const filteredOrders = computed(() => activeTab.value === 'all' ? orders.value : orders.value.filter(o => o.status_wsh === activeTab.value))

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/orders'); if (r.code === 200) orders.value = (Array.isArray(r.data) ? r.data : []).map(enrichOrder) }
  catch (e) {}
})

async function seedTestOrders() {
  seeding.value = true
  try { const r = await authStore.apiPost('/api/orders/seed-test'); if (r.code === 200) { appStore.addToast('测试订单已生成', 'success'); location.reload() } else { appStore.addToast(r.message || '生成失败', 'error') } }
  catch (e) { appStore.addToast('生成失败', 'error') }
  finally { seeding.value = false }
}

async function changeStatus(order, newStatus) {
  try {
    const r = await authStore.apiPut(`/api/orders/${order.id_wsh}/status`, { status_wsh: newStatus })
    if (r.code === 200) { appStore.addToast(`状态已变更为 ${statusLabels[newStatus] || newStatus}`, 'success'); order.status_wsh = newStatus }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function cancelOrder(id) {
  try { await authStore.apiPost('/api/orders/cancel', { order_id_wsh: id }); appStore.addToast('已取消', 'success'); location.reload() }
  catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
