<template>
  <div>
    <div class="admin-order-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['btn', activeTab === tab.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        type="button"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <DataTable
      :columns="columns"
      :data="filteredOrders"
    >
      <template #default="{ row }">
        <select
          class="form-control order-status-select"
          :value="row.status_wsh"
          @change="changeStatus(row, $event.target.value)"
        >
          <option
            v-for="status in statuses"
            :key="status"
            :value="status"
            :disabled="status === row.status_wsh"
          >
            {{ statusLabels[status] || status }}
          </option>
        </select>
        <button class="btn btn-sm btn-info" type="button" @click="showDetail(row.id_wsh)">详情</button>
        <button class="btn btn-sm btn-danger" type="button" @click="deleteOrder(row.id_wsh)">删除</button>
      </template>
    </DataTable>

    <AppDialog
      :visible="showDetailModal && detailOrder"
      :width="760"
      :title="`订单详情 #${detailOrder.id_wsh}`"
      @close="showDetailModal = false"
    >
      <section class="detail-section">
          <div class="detail-grid">
            <div class="form-group"><label>订单编号</label><div>{{ detailOrder.order_no_wsh || '-' }}</div></div>
            <div class="form-group">
              <label>状态</label>
              <div>
                <span :class="['badge', detailStatus.badge]">{{ detailStatus.label }}</span>
              </div>
            </div>
            <div class="form-group"><label>订单金额</label><div>¥{{ detailOrder.total_amount_wsh ?? '-' }}</div></div>
            <div class="form-group"><label>优惠</label><div>¥{{ detailOrder.discount_wsh || 0 }}</div></div>
            <div class="form-group"><label>实付金额</label><div>¥{{ detailOrder.final_amount_wsh ?? '-' }}</div></div>
            <div class="form-group"><label>计费</label><div>{{ billingText(detailOrder) }}</div></div>
            <div class="form-group"><label>开始日期</label><div>{{ detailOrder.start_date_wsh || '-' }}</div></div>
            <div class="form-group"><label>结束日期</label><div>{{ detailOrder.end_date_wsh || '-' }}</div></div>
          </div>
        </section>

        <section class="detail-section">
          <h3>用户信息</h3>
          <div class="detail-grid">
            <div class="form-group"><label>用户名</label><div>{{ detailOrder.owner_name_wsh || '-' }}</div></div>
          </div>
        </section>

        <section v-if="detailOrder.service_name_wsh" class="detail-section">
          <h3>服务信息</h3>
          <div class="detail-grid">
            <div class="form-group"><label>服务名称</label><div>{{ detailOrder.service_name_wsh }}</div></div>
            <div class="form-group"><label>服务描述</label><div>{{ detailOrder.service_description_wsh || '-' }}</div></div>
          </div>
        </section>

        <section v-if="detailOrder.pet_name_wsh" class="detail-section">
          <h3>宠物信息</h3>
          <div class="detail-grid detail-grid-three">
            <div class="form-group"><label>名称</label><div>{{ detailOrder.pet_name_wsh }}</div></div>
            <div class="form-group"><label>种类</label><div>{{ detailOrder.pet_type_wsh || '-' }}</div></div>
            <div class="form-group"><label>品种</label><div>{{ detailOrder.pet_breed_wsh || '-' }}</div></div>
            <div class="form-group"><label>年龄</label><div>{{ detailOrder.pet_age_wsh ?? '-' }} 岁</div></div>
            <div class="form-group"><label>体重</label><div>{{ detailOrder.pet_weight_wsh ?? '-' }} kg</div></div>
          </div>
        </section>

        <section v-if="detailOrder.keeper_name_wsh" class="detail-section">
          <h3>看护人信息</h3>
          <div class="detail-grid">
            <div class="form-group"><label>姓名</label><div>{{ detailOrder.keeper_name_wsh }}</div></div>
            <div class="form-group"><label>电话</label><div>{{ detailOrder.keeper_phone_wsh || '-' }}</div></div>
          </div>
        </section>

        <section v-if="detailOrder.merchant_name_wsh" class="detail-section">
          <h3>商家信息</h3>
          <div class="detail-grid">
            <div class="form-group"><label>名称</label><div>{{ detailOrder.merchant_name_wsh }}</div></div>
            <div class="form-group"><label>电话</label><div>{{ detailOrder.merchant_phone_wsh || '-' }}</div></div>
          </div>
        </section>

      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="showDetailModal = false">关闭</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getOrders, getOrder, updateOrderStatus, deleteOrder as apiDeleteOrder } from '@/api/order'
import { OrderStatus, enrichWithStatus, getStatusBadge, getStatusLabel, makeStatusBadge } from '@/constants/statusMaps'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { billingText } from '@/domain/BookingUnit'

const appStore = useAppStore()
const orders = ref([])
const activeTab = ref('all')
const showDetailModal = ref(false)
const detailOrder = ref(null)

const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '用户', key: 'owner_name_wsh' },
  { label: '服务', key: 'service_name_wsh' },
  { label: '金额', key: 'total_amount_wsh' },
  { label: '状态', key: 'status_label_wsh' },
  { label: '时间', key: 'created_at_wsh' },
]

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待付款' },
  { key: 'paid', label: '已支付' },
  { key: 'confirmed', label: '待送达' },
  { key: 'in_progress', label: '服务中' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
]

const statuses = Object.keys(OrderStatus)
const statusLabels = Object.fromEntries(statuses.map(status => [status, getStatusLabel(OrderStatus, status)]))

const filteredOrders = computed(() =>
  activeTab.value === 'all'
    ? orders.value
    : orders.value.filter(order => order.status_wsh === activeTab.value)
)

const detailStatus = computed(() => ({
  label: getStatusLabel(OrderStatus, detailOrder.value?.status_wsh),
  badge: getStatusBadge(OrderStatus, detailOrder.value?.status_wsh),
}))

function enrichOrder(order) {
  return enrichWithStatus(order, 'status_wsh', OrderStatus)
}

async function loadOrders() {
  try {
    const response = await getOrders()
    if (response.code === 200) {
      orders.value = (Array.isArray(response.data) ? response.data : []).map(enrichOrder)
    }
  } catch (_) {
    appStore.addToast('获取订单列表失败', 'error')
  }
}

onMounted(loadOrders)

async function changeStatus(order, newStatus) {
  try {
    const response = await updateOrderStatus(order.id_wsh, { status_wsh: newStatus })
    if (response.code === 200) {
      order.status_wsh = newStatus
      order.status_label_wsh = makeStatusBadge(OrderStatus, newStatus)
      appStore.addToast(`状态已变更为 ${statusLabels[newStatus] || newStatus}`, 'success')
    }
  } catch (_) {
    appStore.addToast('操作失败', 'error')
  }
}

async function showDetail(id) {
  try {
    const response = await getOrder(id)
    if (response.code === 200) {
      detailOrder.value = response.data
      showDetailModal.value = true
    }
  } catch (_) {
    appStore.addToast('获取详情失败', 'error')
  }
}

async function deleteOrder(id) {
  if (!confirm('确定删除该订单？')) return
  try {
    await apiDeleteOrder(id)
    appStore.addToast('已删除', 'success')
    await loadOrders()
  } catch (_) {
    appStore.addToast('删除失败', 'error')
  }
}
</script>

<style scoped>
.admin-order-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.order-status-select {
  display: inline-block;
  margin-right: 4px;
  min-width: 128px;
  width: auto;
}

.order-detail-modal {
  max-width: 760px;
}

.detail-section {
  border-top: 1px solid var(--color-border);
  margin-top: 16px;
  padding-top: 12px;
}

.detail-section:first-of-type {
  border-top: 0;
}

.detail-section h3 {
  font-size: 16px;
  margin-bottom: 8px;
}

.detail-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-grid-three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 768px) {
  .detail-grid,
  .detail-grid-three {
    grid-template-columns: 1fr;
  }

  .order-status-select {
    display: block;
    margin: 0 0 8px;
    width: 100%;
  }
}
</style>
