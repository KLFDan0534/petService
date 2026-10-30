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
          <AppIcon><Clock /></AppIcon>
          <span>刷新过期</span>
        </button>
        <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="loading" @click="loadAll">
          <AppIcon><Refresh /></AppIcon>
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
          <AppIcon><Search /></AppIcon>
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

// 这几个管理页都是同一套路：顶部筛选 → 请求列表 → 表格渲染 → 操作完再拉一遍
const appStore = useAppStore()
const loading = ref(false)
const usageLoading = ref(false)
// 两个筛选值：上面那张表按状态筛，下面那张按用户ID筛，空串都代表全部
const statusFilter = ref('')
const usageUserId = ref('')
const users = ref([])
const usages = ref([])

// 一进页面就把会员列表和权益流水都拉一遍
onMounted(loadAll)

// 会员列表跟着顶部状态筛选走，没选就查全部；拉完顺手刷新下面的流水
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

// 权益流水按用户ID查，输入框空着就查全部
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

// 手动让后端把已经到期的会员标成过期，刷完再重新读一遍列表
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

// 状态码翻成中文，碰到没见过的值就原样显示
function statusText(status) {
  return {
    active: '生效中',
    inactive: '未开通',
    expired: '已过期',
    cancelled: '已取消',
  }[status] || status || '-'
}

// 只有生效中给绿灯，其他状态都按灰色处理
function statusClass(status) {
  return {
    active: 'is-on',
    inactive: 'is-off',
    expired: 'is-off',
    cancelled: 'is-off',
  }[status] || 'is-off'
}

// 流水状态里「锁定」表示先占住额度还没真扣，「释放」表示额度退回去了
function usageStatusText(status) {
  return {
    locked: '已锁定',
    used: '已使用',
    released: '已释放',
  }[status] || status || '-'
}

// 流水徽章配色：锁定给黄色，已使用给绿色，释放给灰色
function usageStatusClass(status) {
  return {
    locked: 'is-wait',
    used: 'is-on',
    released: 'is-off',
  }[status] || 'is-off'
}

// 折扣率存的是 0~1 的小数，1 就是原价，小于 1 才换算成几折
function discountText(value) {
  const rate = Number(value || 1)
  return rate >= 1 ? '无折扣' : `${(rate * 10).toFixed(1)} 折`
}

// 起止时间拼成一段，两头都空就显示横杠
function periodText(startValue, endValue) {
  const start = dateText(startValue)
  const end = dateText(endValue)
  return start === '-' && end === '-' ? '-' : `${start} 至 ${end}`
}

// 后端返回的时间带个 T，去掉 T 再截到分钟就够了
function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

// 金额统一补两位小数，没值就当 0
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
  border-radius: var(--radius-pill);
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
