<template>
  <div>
    <div class="coupon-toolbar">
      <div>
        <h2>优惠券管理</h2>
        <p>平台券由平台承担优惠成本，商家结算不被扣减。</p>
      </div>
      <button class="btn btn-primary btn-sm" type="button" :disabled="submitting" @click="showCreate = true">新建优惠券</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>编号</th>
            <th>名称</th>
            <th>类型</th>
            <th>门槛</th>
            <th>优惠</th>
            <th>库存</th>
            <th>有效期</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in templates" :key="item.id_wsh">
            <td>{{ item.id_wsh }}</td>
            <td>{{ item.name_wsh }}</td>
            <td>{{ item.type_wsh === 'percent' ? '折扣券' : '满减券' }}</td>
            <td>¥{{ money(item.threshold_amount_wsh) }}</td>
            <td>{{ discountText(item) }}</td>
            <td>{{ stockText(item) }}</td>
            <td>{{ dateText(item.valid_from_wsh) }} 至 {{ dateText(item.valid_to_wsh) }}</td>
            <td>
              <span :class="['coupon-status', item.status_wsh === 1 ? 'is-on' : 'is-off']">
                {{ item.status_wsh === 1 ? '启用' : '停用' }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="btn btn-sm btn-outline" type="button" :disabled="submitting" @click="openGrant(item)">发放</button>
                <button
                  class="btn btn-sm"
                  :class="item.status_wsh === 1 ? 'btn-danger' : 'btn-success'"
                  type="button"
                  :disabled="submitting"
                  @click="toggleStatus(item)"
                >
                  {{ item.status_wsh === 1 ? '停用' : '启用' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="templates.length === 0">
            <td colspan="9" class="empty-cell">暂无优惠券</td>
          </tr>
        </tbody>
      </table>
    </div>

    <AppDialog
      :visible="showCreate"
      :width="760"
      title="新建优惠券"
      @close="showCreate = false"
    >
      <div class="form-grid">
          <label>
            名称
            <input v-model.trim="createForm.name_wsh" class="form-control" maxlength="100" placeholder="例如：平台新人券">
          </label>
          <label>
            类型
            <select v-model="createForm.type_wsh" class="form-control">
              <option value="amount">满减券</option>
              <option value="percent">折扣券</option>
            </select>
          </label>
          <label>
            使用门槛
            <input v-model.number="createForm.threshold_amount_wsh" class="form-control" min="0" step="0.01" type="number">
          </label>
          <label v-if="createForm.type_wsh === 'amount'">
            减免金额
            <input v-model.number="createForm.discount_amount_wsh" class="form-control" min="0.01" step="0.01" type="number">
          </label>
          <label v-else>
            折扣率
            <input v-model.number="createForm.discount_rate_wsh" class="form-control" max="0.99" min="0.01" step="0.01" type="number">
          </label>
          <label>
            最大减免
            <input v-model.number="createForm.max_discount_amount_wsh" class="form-control" min="0" step="0.01" type="number" placeholder="折扣券可填">
          </label>
          <label>
            总库存
            <input v-model.number="createForm.total_quantity_wsh" class="form-control" min="0" step="1" type="number" placeholder="0 或空为不限">
          </label>
          <label>
            每人限领
            <input v-model.number="createForm.per_user_limit_wsh" class="form-control" min="1" step="1" type="number">
          </label>
          <label>
            生效时间
            <input v-model="createForm.valid_from_wsh" class="form-control" type="datetime-local">
          </label>
          <label>
            失效时间
            <input v-model="createForm.valid_to_wsh" class="form-control" type="datetime-local">
          </label>
          <label>
            适用范围
            <select v-model="createForm.scope_type_wsh" class="form-control">
              <option value="platform">全平台</option>
              <option value="merchant">指定商家</option>
            </select>
          </label>
          <label v-if="createForm.scope_type_wsh === 'merchant'">
            商家ID
            <input v-model.number="createForm.merchant_id_wsh" class="form-control" min="1" step="1" type="number">
          </label>
          <label class="form-grid__wide">
            备注
            <textarea v-model.trim="createForm.remark_wsh" class="form-control" maxlength="500" rows="3"></textarea>
          </label>
        </div>
      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="showCreate = false">取消</button>
        <button class="btn btn-primary btn-sm" type="button" :disabled="submitting" @click="submitCreate">
          {{ submitting ? '保存中...' : '保存' }}
        </button>
      </template>
    </AppDialog>

    <AppDialog
      :visible="grantTarget"
      :width="680"
      title="发放优惠券"
      @close="closeGrant"
    >
      <p class="muted-text">{{ grantTarget.name_wsh }}</p>

        <div class="grant-mode-tabs">
          <button
            type="button"
            :class="['grant-mode-tab', grantMode === 'selected' ? 'active' : '']"
            @click="grantMode = 'selected'"
          >
            选择用户
          </button>
          <button
            type="button"
            :class="['grant-mode-tab', grantMode === 'all' ? 'active' : '']"
            @click="grantMode = 'all'"
          >
            全部正常用户
          </button>
          <button
            type="button"
            :class="['grant-mode-tab', grantMode === 'condition' ? 'active' : '']"
            @click="grantMode = 'condition'"
          >
            条件发放
          </button>
        </div>

        <div class="grant-layout">
          <label class="form-field grant-quantity">
            数量
            <input v-model.number="grantForm.quantity_wsh" class="form-control" min="1" step="1" type="number">
          </label>

          <template v-if="grantMode === 'selected'">
            <div class="grant-search">
              <input
                v-model.trim="userSearch"
                class="form-control"
                placeholder="搜索用户名、昵称、手机号或真实姓名"
                @keyup.enter="searchGrantUsers"
              >
              <button class="btn btn-outline btn-sm" type="button" :disabled="userSearchLoading" @click="searchGrantUsers">
                {{ userSearchLoading ? '搜索中...' : '搜索' }}
              </button>
            </div>

            <div v-if="selectedUsers.length" class="selected-users">
              <span v-for="user in selectedUsers" :key="user.id_wsh" class="selected-user">
                {{ userLabel(user) }}
                <button type="button" @click="removeSelectedUser(user.id_wsh)">×</button>
              </span>
            </div>

            <div class="grant-user-list">
              <div v-if="userSearchLoading" class="grant-empty">搜索中...</div>
              <div v-else-if="userOptions.length === 0" class="grant-empty">请输入关键词搜索用户</div>
              <label v-else v-for="user in userOptions" :key="user.id_wsh" class="grant-user-row">
                <input
                  type="checkbox"
                  :checked="isUserSelected(user.id_wsh)"
                  @change="toggleSelectedUser(user)"
                >
                <span>
                  <strong>{{ userLabel(user) }}</strong>
                  <small>ID {{ user.id_wsh }} · {{ user.phone_wsh || '无手机号' }} · {{ user.status_wsh === 1 ? '正常' : '禁用' }}</small>
                </span>
              </label>
            </div>
          </template>

          <template v-else-if="grantMode === 'condition'">
            <div class="grant-condition-note">
              已填写且大于 0 的数值条件、已选择的时间范围、指定用户列表之间为“满足任一项即发放”；0 或留空不限制，负数不发放。
            </div>
            <div class="condition-grid">
              <label class="form-field">
                注册开始
                <input v-model="grantForm.registered_from_wsh" class="form-control" type="datetime-local">
              </label>
              <label class="form-field">
                注册结束
                <input v-model="grantForm.registered_to_wsh" class="form-control" type="datetime-local">
              </label>
              <label class="form-field">
                累计消费不少于
                <input v-model.number="grantForm.min_total_spend_wsh" class="form-control" step="0.01" type="number" placeholder="0 或空不限制">
              </label>
              <label class="form-field">
                完成订单不少于
                <input v-model.number="grantForm.min_order_count_wsh" class="form-control" step="1" type="number" placeholder="0 或空不限制">
              </label>
              <label class="form-field">
                宠物数不少于
                <input v-model.number="grantForm.min_pet_count_wsh" class="form-control" step="1" type="number" placeholder="0 或空不限制">
              </label>
              <label class="form-field">
                最近下单开始
                <input v-model="grantForm.last_order_from_wsh" class="form-control" type="datetime-local">
              </label>
              <label class="form-field">
                最近下单结束
                <input v-model="grantForm.last_order_to_wsh" class="form-control" type="datetime-local">
              </label>
              <label class="form-field">
                指定用户ID
                <input v-model.trim="grantForm.include_user_ids_text" class="form-control" placeholder="逗号分隔，作为任一命中条件">
              </label>
              <label class="form-field">
                排除用户ID
                <input v-model.trim="grantForm.exclude_user_ids_text" class="form-control" placeholder="逗号分隔，始终不发">
              </label>
            </div>
          </template>

          <div v-else class="grant-all-panel">
          将发给所有正常状态用户，仍受库存和每人限领限制。
        </div>
      </div>

      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="closeGrant">取消</button>
        <button
          v-if="grantMode === 'selected'"
          class="btn btn-primary btn-sm"
          type="button"
          :disabled="submitting || selectedUsers.length === 0"
          @click="submitGrantSelected"
        >
          {{ submitting ? '发放中...' : `发给已选用户(${selectedUsers.length})` }}
        </button>
        <button
          v-else-if="grantMode === 'condition'"
          class="btn btn-primary btn-sm"
          type="button"
          :disabled="submitting"
          @click="submitGrantCondition"
        >
          {{ submitting ? '发放中...' : '按条件发放' }}
        </button>
        <button
          v-else
          class="btn btn-primary btn-sm"
          type="button"
          :disabled="submitting"
          @click="confirmGrantAll"
        >
          发给全部正常用户
        </button>
      </template>
    </AppDialog>

    <AppDialog
      :visible="grantAllConfirming"
      :width="420"
      title="确认发给全部正常用户"
      @close="grantAllConfirming = false"
    >
      <p class="confirm-text">
        将把「{{ grantTarget?.name_wsh }}」按每人 {{ Number(grantForm.quantity_wsh || 1) }} 张发放给所有正常状态用户。
      </p>
      <template #footer>
        <button class="btn btn-secondary btn-sm" type="button" @click="grantAllConfirming = false">取消</button>
        <button class="btn btn-primary btn-sm" type="button" :disabled="submitting" @click="submitGrantAll">
          {{ submitting ? '发放中...' : '确认发放' }}
        </button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import {
  createCouponTemplate,
  getCouponTemplates,
  grantCouponByCondition,
  grantCouponToAll,
  grantCouponToUser,
  updateCouponTemplateStatus,
} from '@/api/coupon'
import { getUsers } from '@/api/admin'
import AppDialog from '@/components/common/AppDialog.vue'

const appStore = useAppStore()
const loading = ref(false)
const submitting = ref(false)
const showCreate = ref(false)
const templates = ref([])
const grantTarget = ref(null)
const grantMode = ref('selected')
const grantAllConfirming = ref(false)
const userSearch = ref('')
const userSearchLoading = ref(false)
const userOptions = ref([])
const selectedUsers = ref([])
const createForm = reactive(defaultCreateForm())
const grantForm = reactive(defaultGrantForm())

onMounted(loadTemplates)

async function loadTemplates() {
  loading.value = true
  try {
    const res = await getCouponTemplates()
    if (res.code === 200) templates.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    appStore.addToast('优惠券列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (submitting.value) return
  const error = validateCreateForm()
  if (error) return appStore.addToast(error, 'warning')
  submitting.value = true
  try {
    const res = await createCouponTemplate(buildCreatePayload())
    if (res.code === 200) {
      appStore.addToast('优惠券已创建', 'success')
      Object.assign(createForm, defaultCreateForm())
      showCreate.value = false
      await loadTemplates()
    }
  } catch (e) {
    appStore.addToast('优惠券保存失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(item) {
  if (submitting.value) return
  submitting.value = true
  try {
    const nextStatus = item.status_wsh === 1 ? 0 : 1
    const res = await updateCouponTemplateStatus(item.id_wsh, nextStatus)
    if (res.code === 200) {
      item.status_wsh = nextStatus
      appStore.addToast('状态已更新', 'success')
    }
  } catch (e) {
    appStore.addToast('状态更新失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openGrant(item) {
  grantTarget.value = item
  grantMode.value = 'selected'
  grantAllConfirming.value = false
  userSearch.value = ''
  userOptions.value = []
  selectedUsers.value = []
  Object.assign(grantForm, defaultGrantForm())
}

function closeGrant() {
  grantTarget.value = null
  grantAllConfirming.value = false
  selectedUsers.value = []
  userOptions.value = []
  userSearch.value = ''
}

async function searchGrantUsers() {
  if (!userSearch.value) {
    userOptions.value = []
    return appStore.addToast('请输入用户名、昵称或手机号', 'warning')
  }
  userSearchLoading.value = true
  try {
    const res = await getUsers({ page: 1, size: 20, q: userSearch.value })
    if (res.code === 200) {
      const data = Array.isArray(res.data?.list || res.data) ? (res.data.list || res.data) : []
      userOptions.value = data
    }
  } catch {
    appStore.addToast('用户搜索失败', 'error')
  } finally {
    userSearchLoading.value = false
  }
}

function toggleSelectedUser(user) {
  if (isUserSelected(user.id_wsh)) {
    removeSelectedUser(user.id_wsh)
    return
  }
  selectedUsers.value = [...selectedUsers.value, user]
}

function removeSelectedUser(userId) {
  selectedUsers.value = selectedUsers.value.filter(user => Number(user.id_wsh) !== Number(userId))
}

function isUserSelected(userId) {
  return selectedUsers.value.some(user => Number(user.id_wsh) === Number(userId))
}

function userLabel(user) {
  return user.nickname_wsh || user.username_wsh || `用户 ${user.id_wsh}`
}

async function submitGrantSelected() {
  if (!grantTarget.value) return
  if (selectedUsers.value.length === 0) return appStore.addToast('请选择用户', 'warning')
  await submitGrant(async () => {
    let issued = 0
    for (const user of selectedUsers.value) {
      const res = await grantCouponToUser(grantTarget.value.id_wsh, {
        user_id_wsh: Number(user.id_wsh),
        quantity_wsh: Number(grantForm.quantity_wsh || 1),
      })
      if (res.code !== 200) {
        throw new Error(res.message || '优惠券发放失败')
      }
      if (res.code === 200) issued += Number(res.data || 0)
    }
    return { code: 200, data: issued }
  })
}

function confirmGrantAll() {
  if (!grantTarget.value) return
  if (Number(grantForm.quantity_wsh || 0) <= 0) return appStore.addToast('数量必须大于0', 'warning')
  grantAllConfirming.value = true
}

async function submitGrantAll() {
  if (!grantTarget.value) return
  await submitGrant(() => grantCouponToAll(grantTarget.value.id_wsh, {
    quantity_wsh: Number(grantForm.quantity_wsh || 1),
  }))
}

async function submitGrantCondition() {
  if (!grantTarget.value) return
  await submitGrant(() => grantCouponByCondition(grantTarget.value.id_wsh, buildGrantConditionPayload()))
}

async function submitGrant(action) {
  if (submitting.value) return
  if (Number(grantForm.quantity_wsh || 0) <= 0) return appStore.addToast('数量必须大于0', 'warning')
  submitting.value = true
  try {
    const res = await action()
    if (res.code === 200) {
      appStore.addToast(`已发放 ${res.data || 0} 张`, 'success')
      closeGrant()
      await loadTemplates()
    } else {
      appStore.addToast(res.message || '优惠券发放失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '优惠券发放失败', 'error')
  } finally {
    submitting.value = false
  }
}

function validateCreateForm() {
  if (!createForm.name_wsh) return '请填写优惠券名称'
  if (!createForm.valid_from_wsh || !createForm.valid_to_wsh) return '请选择有效期'
  if (new Date(createForm.valid_to_wsh) <= new Date(createForm.valid_from_wsh)) return '失效时间必须晚于生效时间'
  if (createForm.type_wsh === 'amount' && Number(createForm.discount_amount_wsh || 0) <= 0) return '减免金额必须大于0'
  if (createForm.type_wsh === 'percent') {
    const rate = Number(createForm.discount_rate_wsh || 0)
    if (rate <= 0 || rate >= 1) return '折扣率必须在0到1之间'
  }
  if (createForm.scope_type_wsh === 'merchant' && !createForm.merchant_id_wsh) return '指定商家券需要填写商家ID'
  return ''
}

function buildCreatePayload() {
  return {
    name_wsh: createForm.name_wsh,
    type_wsh: createForm.type_wsh,
    threshold_amount_wsh: moneyNumber(createForm.threshold_amount_wsh),
    discount_amount_wsh: createForm.type_wsh === 'amount' ? moneyNumber(createForm.discount_amount_wsh) : null,
    discount_rate_wsh: createForm.type_wsh === 'percent' ? Number(createForm.discount_rate_wsh) : null,
    max_discount_amount_wsh: createForm.max_discount_amount_wsh === '' ? null : moneyNumber(createForm.max_discount_amount_wsh),
    total_quantity_wsh: createForm.total_quantity_wsh === '' ? null : Number(createForm.total_quantity_wsh),
    per_user_limit_wsh: Number(createForm.per_user_limit_wsh || 1),
    valid_from_wsh: toApiDateTime(createForm.valid_from_wsh),
    valid_to_wsh: toApiDateTime(createForm.valid_to_wsh),
    status_wsh: 1,
    scope_type_wsh: createForm.scope_type_wsh,
    merchant_id_wsh: createForm.scope_type_wsh === 'merchant' ? Number(createForm.merchant_id_wsh) : null,
    remark_wsh: createForm.remark_wsh || null,
  }
}

function defaultCreateForm() {
  const now = new Date()
  const end = new Date(now)
  end.setDate(end.getDate() + 30)
  return {
    name_wsh: '',
    type_wsh: 'amount',
    threshold_amount_wsh: 0,
    discount_amount_wsh: 10,
    discount_rate_wsh: 0.9,
    max_discount_amount_wsh: '',
    total_quantity_wsh: '',
    per_user_limit_wsh: 1,
    valid_from_wsh: toInputDateTime(now),
    valid_to_wsh: toInputDateTime(end),
    scope_type_wsh: 'platform',
    merchant_id_wsh: '',
    remark_wsh: '',
  }
}

function defaultGrantForm() {
  return {
    quantity_wsh: 1,
    registered_from_wsh: '',
    registered_to_wsh: '',
    min_total_spend_wsh: '',
    min_order_count_wsh: '',
    min_pet_count_wsh: '',
    last_order_from_wsh: '',
    last_order_to_wsh: '',
    include_user_ids_text: '',
    exclude_user_ids_text: '',
  }
}

function buildGrantConditionPayload() {
  return {
    quantity_wsh: Number(grantForm.quantity_wsh || 1),
    registered_from_wsh: toApiDateTime(grantForm.registered_from_wsh),
    registered_to_wsh: toApiDateTime(grantForm.registered_to_wsh),
    min_total_spend_wsh: emptyToNull(grantForm.min_total_spend_wsh),
    min_order_count_wsh: emptyToNull(grantForm.min_order_count_wsh),
    min_pet_count_wsh: emptyToNull(grantForm.min_pet_count_wsh),
    last_order_from_wsh: toApiDateTime(grantForm.last_order_from_wsh),
    last_order_to_wsh: toApiDateTime(grantForm.last_order_to_wsh),
    include_user_ids_wsh: parseIdList(grantForm.include_user_ids_text),
    exclude_user_ids_wsh: parseIdList(grantForm.exclude_user_ids_text),
  }
}

function parseIdList(value) {
  if (!value) return []
  return String(value)
    .split(/[,\s，]+/)
    .map(item => Number(item))
    .filter(item => Number.isFinite(item) && item > 0)
}

function emptyToNull(value) {
  return value === '' || value === null || value === undefined ? null : Number(value)
}

function discountText(item) {
  if (item.type_wsh === 'percent') {
    const rate = Number(item.discount_rate_wsh || 0)
    const max = Number(item.max_discount_amount_wsh || 0)
    return `${(rate * 10).toFixed(1)}折${max > 0 ? `，最高减¥${money(max)}` : ''}`
  }
  return `减¥${money(item.discount_amount_wsh)}`
}

function stockText(item) {
  const total = Number(item.total_quantity_wsh || 0)
  if (!total) return `已发 ${item.issued_quantity_wsh || 0} / 不限`
  return `已发 ${item.issued_quantity_wsh || 0} / ${total}`
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function moneyNumber(value) {
  return Number(Number(value || 0).toFixed(2))
}

function toApiDateTime(value) {
  return value ? `${value}:00` : null
}

function toInputDateTime(date) {
  const pad = value => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style scoped>
.coupon-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.coupon-toolbar h2 {
  margin: 0 0 4px;
}
.coupon-toolbar p,
.muted-text {
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin: 0;
}
.coupon-modal {
  max-width: 760px;
}
.grant-modal {
  max-width: 680px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-top: 16px;
}
.form-grid label,
.form-field {
  display: grid;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.form-grid__wide {
  grid-column: 1 / -1;
}
.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.grant-mode-tabs {
  background: var(--color-muted);
  border-radius: 8px;
  display: inline-flex;
  gap: 4px;
  margin-top: 16px;
  padding: 4px;
}
.grant-mode-tab {
  background: transparent;
  border-radius: var(--radius-inline);
  color: var(--color-muted-foreground);
  font-size: 13px;
  font-weight: 700;
  padding: 8px 12px;
}
.grant-mode-tab.active {
  background: var(--color-card);
  color: var(--color-foreground);
  box-shadow: var(--shadow-sm);
}
.grant-layout {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}
.grant-quantity {
  max-width: 180px;
}
.grant-search {
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
}
.condition-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
.grant-condition-note {
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.6;
}
.selected-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.selected-user {
  align-items: center;
  background: rgba(249, 115, 22, 0.12);
  border-radius: var(--radius-pill);
  color: var(--color-primary);
  display: inline-flex;
  font-size: 12px;
  font-weight: 700;
  gap: 6px;
  padding: 4px 8px;
}
.selected-user button {
  background: transparent;
  color: inherit;
  font-size: 16px;
  line-height: 1;
  padding: 0;
}
.grant-user-list {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  max-height: 260px;
  overflow-y: auto;
}
.grant-user-row {
  align-items: center;
  border-bottom: 1px solid var(--color-border);
  display: grid;
  gap: 10px;
  grid-template-columns: auto minmax(0, 1fr);
  padding: 10px 12px;
}
.grant-user-row:last-child {
  border-bottom: 0;
}
.grant-user-row input {
  width: auto;
}
.grant-user-row small {
  color: var(--color-muted-foreground);
  display: block;
  font-size: 12px;
  margin-top: 2px;
}
.grant-empty,
.grant-all-panel,
.confirm-text {
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.grant-empty,
.grant-all-panel {
  padding: 16px;
}
.coupon-status {
  display: inline-flex;
  min-width: 48px;
  justify-content: center;
  border-radius: var(--radius-pill);
  padding: 3px 8px;
  font-size: 12px;
}
.coupon-status.is-on {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}
.coupon-status.is-off {
  background: rgba(100, 116, 139, 0.14);
  color: var(--color-muted-foreground);
}
.empty-cell {
  color: var(--color-muted-foreground);
  padding: 32px;
  text-align: center;
}
@media (max-width: 640px) {
  .grant-search {
    grid-template-columns: 1fr;
  }
  .grant-quantity {
    max-width: none;
  }
}
</style>
