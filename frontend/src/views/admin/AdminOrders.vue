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

    <div class="admin-order-filters">
      <input
        v-model="keyword"
        class="form-control"
        placeholder="订单号搜索"
        @keyup.enter="applyFilter"
      />
      <input v-model="startDate" type="date" class="form-control" title="创建日期起" />
      <span class="filter-sep">至</span>
      <input v-model="endDate" type="date" class="form-control" title="创建日期止" />
      <select v-model.number="size" class="form-control filter-size" @change="changeSize">
        <option v-for="option in pageSizes" :key="option" :value="option">{{ option }} 条/页</option>
      </select>
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilter">查询</button>
      <button class="btn btn-sm btn-outline" type="button" @click="resetFilter">重置</button>
    </div>

    <DataTable
      :columns="columns"
      :data="filteredOrders"
    >
      <template #default="{ row }">
        <select
          class="form-control order-status-select"
          :value="row.status_wsh"
          :disabled="allowedStatuses(row.status_wsh).length <= 1"
          :title="allowedStatuses(row.status_wsh).length <= 1 ? '该状态只能通过支付、取消、退款、完成等专用入口变更' : ''"
          @change="changeStatus(row, $event)"
        >
          <option
            v-for="status in allowedStatuses(row.status_wsh)"
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

    <div class="admin-order-pager">
      <span class="pager-info">共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页</span>
      <button
        class="btn btn-sm btn-outline"
        type="button"
        :disabled="loading || page <= 1"
        @click="goPage(page - 1)"
      >
        上一页
      </button>
      <button
        class="btn btn-sm btn-outline"
        type="button"
        :disabled="loading || pages === 0 || page >= pages"
        @click="goPage(page + 1)"
      >
        下一页
      </button>
    </div>

    <AppDialog
      v-if="showDetailModal && detailOrder"
      :visible="showDetailModal && detailOrder"
      :width="760"
      :title="`订单详情 #${detailOrder?.id_wsh || '-'}`"
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
import { computed, onMounted, ref, watch } from 'vue'
import { useAppStore } from '@/stores/app'
import { pageAdminOrders, getOrder, updateOrderStatus, deleteOrder as apiDeleteOrder } from '@/api/order'
import { OrderStatus, enrichWithStatus, getStatusBadge, getStatusLabel, makeStatusBadge } from '@/constants/statusMaps'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { billingText } from '@/domain/BookingUnit'

const appStore = useAppStore()
// 当前页数据、筛选条件、分页状态
const orders = ref([])
const activeTab = ref('all')
const showDetailModal = ref(false)
const detailOrder = ref(null)

// 分页与筛选：全部交给后端，前端只保存当前条件
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const pageSizes = [20, 50, 100]

const columns = [
  { label: 'ID', key: 'id_wsh' },
  { label: '用户', key: 'owner_name_wsh' },
  { label: '服务', key: 'service_name_wsh' },
  { label: '金额', key: 'total_amount_wsh' },
  { label: '状态', key: 'status_label_wsh' },
  { label: '时间', key: 'created_at_wsh' },
]

// 这些标签只是在前端挑数据，点来点去不会重新请求后端
const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待付款' },
  { key: 'paid', label: '已支付' },
  { key: 'confirmed', label: '待接收' },
  { key: 'in_progress', label: '服务中' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
]

// 状态下拉只列「当前状态 + 允许的下一个状态」。
// 这张表必须与后端 OrderStatus.MANUAL_TRANSITIONS 保持一致：
// 手动修正只能沿履约链向前一步，跳步会绕过交接码/开始照片，资金态必须走专用接口。
const MANUAL_TRANSITIONS = {
  confirmed: ['received'],
  received: ['in_progress'],
}
function allowedStatuses(current) {
  return [current, ...(MANUAL_TRANSITIONS[current] || [])]
}
const statuses = Object.keys(OrderStatus)
const statusLabels = Object.fromEntries(statuses.map(status => [status, getStatusLabel(OrderStatus, status)]))

// 状态筛选已下推到后端：当前页就是筛选后的结果，前端不再二次过滤
const filteredOrders = computed(() => orders.value)

const detailStatus = computed(() => ({
  label: getStatusLabel(OrderStatus, detailOrder.value?.status_wsh),
  badge: getStatusBadge(OrderStatus, detailOrder.value?.status_wsh),
}))

// 把状态码换成「中文名 + 徽章样式」挂到每条订单上，表格直接拿来显示
function enrichOrder(order) {
  return enrichWithStatus(order, 'status_wsh', OrderStatus)
}

// 分页查询：状态/订单号/日期都交给后端，前端只负责传条件和页码。
// 后端不传日期时默认只看最近 30 天，翻页深度超过 2000 条会直接拒绝。
async function loadOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeTab.value !== 'all') params.status = activeTab.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (startDate.value) params.start_date = startDate.value
    if (endDate.value) params.end_date = endDate.value

    const response = await pageAdminOrders(params)
    if (response.code === 200) {
      const data = response.data || {}
      orders.value = (Array.isArray(data.list) ? data.list : []).map(enrichOrder)
      total.value = data.total || 0
      pages.value = data.pages || 0
      if (data.page) page.value = data.page
    } else {
      appStore.addToast(response.message || '获取订单列表失败', 'error')
    }
  } catch (_) {
    appStore.addToast('获取订单列表失败', 'error')
  } finally {
    loading.value = false
  }
}

// 改筛选条件一定回到第 1 页，否则会停在一个已经不存在的页码上
function applyFilter() {
  page.value = 1
  loadOrders()
}

function resetFilter() {
  keyword.value = ''
  startDate.value = ''
  endDate.value = ''
  activeTab.value = 'all'
  page.value = 1
  loadOrders()
}

// 切换每页条数同样回到第 1 页
function changeSize() {
  page.value = 1
  loadOrders()
}

function goPage(target) {
  if (target < 1 || target === page.value) return
  if (pages.value && target > pages.value) return
  page.value = target
  loadOrders()
}

// 标签切换即重新查询
watch(activeTab, () => {
  page.value = 1
  loadOrders()
})

onMounted(loadOrders)

// 管理员手动修正状态：只能沿履约链向前一步（与后端转移表一致）。
// 待付款 → 已支付 → 待接收 → 已接收 → 服务中 → 已完成；取消与退款走各自的专用接口。
async function changeStatus(order, event) {
  const newStatus = event.target.value
  try {
    const response = await updateOrderStatus(order.id_wsh, { status_wsh: newStatus })
    if (response.code === 200) {
      // 只改本地这一条，不重新拉列表，页面不会跳、滚动位置也不会丢
      order.status_wsh = newStatus
      order.status_label_wsh = makeStatusBadge(OrderStatus, newStatus)
      appStore.addToast(`状态已变更为 ${statusLabels[newStatus] || newStatus}`, 'success')
    } else {
      // 后端拒绝时把下拉恢复成原值，否则界面会显示一个并不存在的状态
      event.target.value = order.status_wsh
    }
  } catch (e) {
    appStore.addToast(e?.message || '操作失败', 'error')
    event.target.value = order.status_wsh
  }
}

// 列表里没有宠物、看护人、商家这些细项，所以详情要单独再请求一次
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

// 删除是不可逆的，先弹浏览器自带的确认框，删完重新拉一遍列表
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

.admin-order-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.admin-order-filters .form-control {
  width: auto;
  min-width: 140px;
}

.filter-sep {
  color: var(--color-text-secondary);
}

.filter-size {
  min-width: 104px;
}

.admin-order-pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.pager-info {
  margin-right: auto;
  color: var(--color-text-secondary);
  font-size: 13px;
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
