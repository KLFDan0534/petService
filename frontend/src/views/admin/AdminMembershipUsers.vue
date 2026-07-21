<template>
  <div class="membership-user-page">
    <div class="membership-user-toolbar">
      <h2>会员用户管理</h2>
      <div class="toolbar-actions">
        <select v-model="statusFilter" class="filter-select" @change="loadAll">
          <option value="">全部状态</option>
          <option value="active">生效中</option>
          <option value="inactive">未开通</option>
          <option value="expired">已过期</option>
          <option value="cancelled">已取消</option>
        </select>
        <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="loading" @click="expireNow">
          <el-icon><Clock /></el-icon>
          <span>刷新过期</span>
        </button>
        <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="loading" @click="loadAll">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </button>
      </div>
    </div>

    <section>
      <div class="section-title">
        <h3>会员列表</h3>
        <span>{{ users.length }} 条记录</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户ID</th>
              <th>套餐</th>
              <th>等级</th>
              <th>折扣</th>
              <th>状态</th>
              <th>有效期</th>
              <th>剩余天数</th>
              <th>来源</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users" :key="item.id_wsh || item.user_id_wsh">
              <td>{{ item.user_id_wsh }}</td>
              <td>
                <div class="plan-cell">
                  <strong>{{ item.plan_name_wsh || item.plan_code_wsh || '-' }}</strong>
                  <small>{{ item.plan_code_wsh || '-' }}</small>
                </div>
              </td>
              <td>Lv{{ item.level_wsh || 0 }}</td>
              <td>{{ discountText(item.discount_rate_wsh) }}</td>
              <td>
                <span :class="['status-badge', statusClass(item.status_wsh)]">
                  {{ statusText(item.status_wsh) }}
                </span>
              </td>
              <td>{{ periodText(item.started_at_wsh, item.expires_at_wsh) }}</td>
              <td>{{ item.remaining_days_wsh || 0 }}</td>
              <td>{{ item.source_wsh || '-' }}</td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="8" class="empty-cell">暂无会员用户</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section>
      <div class="section-title">
        <h3>权益流水</h3>
        <span>{{ usages.length }} 条记录</span>
      </div>
      <div class="usage-filter">
        <input v-model.trim="usageUserId" class="filter-input" placeholder="按用户ID筛选">
        <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="usageLoading" @click="loadUsages">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </button>
      </div>
      <div v-if="usageLoading" class="loading">加载中...</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户ID</th>
              <th>会员ID</th>
              <th>业务</th>
              <th>权益</th>
              <th>金额</th>
              <th>状态</th>
              <th>使用时间</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="usage in usages" :key="usage.id_wsh || usage.request_id_wsh">
              <td>{{ usage.user_id_wsh }}</td>
              <td>{{ usage.membership_id_wsh || '-' }}</td>
              <td>{{ usage.business_type_wsh }} #{{ usage.business_id_wsh }}</td>
              <td>{{ usage.benefit_code_wsh || usage.benefit_type_wsh || '-' }}</td>
              <td>¥{{ money(usage.amount_wsh) }}</td>
              <td>
                <span :class="['status-badge', usageStatusClass(usage.usage_status_wsh)]">
                  {{ usageStatusText(usage.usage_status_wsh) }}
                </span>
              </td>
              <td>{{ dateText(usage.used_at_wsh) }}</td>
              <td>{{ dateText(usage.created_at_wsh) }}</td>
            </tr>
            <tr v-if="usages.length === 0">
              <td colspan="8" class="empty-cell">暂无权益流水</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Clock, Refresh, Search } from '@element-plus/icons-vue'
import {
  expireMembershipsForAdmin,
  getAdminMembershipUsages,
  getAdminMembershipUsers,
} from '@/api/membership'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const usageLoading = ref(false)
const statusFilter = ref('')
const usageUserId = ref('')
const users = ref([])
const usages = ref([])

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const params = statusFilter.value ? { status_wsh: statusFilter.value } : {}
    const res = await getAdminMembershipUsers(params)
    if (res.code === 200) {
      users.value = Array.isArray(res.data) ? res.data : []
    } else {
      appStore.addToast(res.message || '会员用户加载失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员用户加载失败', 'error')
  } finally {
    loading.value = false
  }
  await loadUsages()
}

async function loadUsages() {
  usageLoading.value = true
  try {
    const params = usageUserId.value ? { user_id_wsh: Number(usageUserId.value) } : {}
    const res = await getAdminMembershipUsages(params)
    if (res.code === 200) {
      usages.value = Array.isArray(res.data) ? res.data : []
    } else {
      appStore.addToast(res.message || '权益流水加载失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '权益流水加载失败', 'error')
  } finally {
    usageLoading.value = false
  }
}

async function expireNow() {
  try {
    const res = await expireMembershipsForAdmin()
    if (res.code === 200) {
      appStore.addToast(`已刷新 ${res.data || 0} 条过期会员`, 'success')
      await loadAll()
    } else {
      appStore.addToast(res.message || '过期会员刷新失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '过期会员刷新失败', 'error')
  }
}

function statusText(status) {
  return {
    active: '生效中',
    inactive: '未开通',
    expired: '已过期',
    cancelled: '已取消',
  }[status] || status || '-'
}

function statusClass(status) {
  return {
    active: 'is-on',
    inactive: 'is-off',
    expired: 'is-off',
    cancelled: 'is-off',
  }[status] || 'is-off'
}

function usageStatusText(status) {
  return {
    locked: '已锁定',
    used: '已使用',
    released: '已释放',
  }[status] || status || '-'
}

function usageStatusClass(status) {
  return {
    locked: 'is-wait',
    used: 'is-on',
    released: 'is-off',
  }[status] || 'is-off'
}

function discountText(value) {
  const rate = Number(value || 1)
  return rate >= 1 ? '无折扣' : `${(rate * 10).toFixed(1)} 折`
}

function periodText(startValue, endValue) {
  const start = dateText(startValue)
  const end = dateText(endValue)
  return start === '-' && end === '-' ? '-' : `${start} 至 ${end}`
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.membership-user-page {
  display: grid;
  gap: 24px;
}

.membership-user-toolbar,
.toolbar-actions,
.section-title,
.usage-filter {
  align-items: center;
  display: flex;
  gap: 12px;
  min-width: 0;
}

.membership-user-toolbar,
.section-title {
  justify-content: space-between;
  flex-wrap: wrap;
}

.membership-user-toolbar h2,
.section-title h3 {
  margin: 0;
  min-width: 0;
}

.membership-user-toolbar h2 {
  font-size: 22px;
}

.toolbar-actions,
.usage-filter {
  flex-wrap: wrap;
}

.section-title {
  margin-bottom: 12px;
}

.section-title span,
.plan-cell small,
.empty-cell {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.filter-select,
.filter-input {
  min-height: 36px;
  padding: 8px 12px;
}

.filter-select {
  width: 120px;
}

.filter-input {
  min-width: 180px;
}

.plan-cell {
  display: grid;
  gap: 3px;
}

.status-badge {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-size: 12px;
  justify-content: center;
  min-width: 58px;
  padding: 4px 8px;
}

.status-badge.is-on {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.status-badge.is-wait {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.status-badge.is-off {
  background: rgba(100, 116, 139, 0.14);
  color: var(--color-muted-foreground);
}

.empty-cell {
  padding: 32px;
  text-align: center;
}

@media (max-width: 720px) {
  .membership-user-toolbar,
  .toolbar-actions,
  .section-title,
  .usage-filter {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-select,
  .filter-input,
  .toolbar-actions .btn,
  .usage-filter .btn {
    width: 100%;
  }
}
</style>
