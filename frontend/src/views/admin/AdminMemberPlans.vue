<template>
  <div class="member-plan-page">
    <div class="member-plan-toolbar">
      <h2>会员套餐管理</h2>
      <div class="toolbar-actions">
        <select v-model="statusFilter" class="filter-select" @change="loadPlans">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </select>
        <button class="btn btn-outline btn-sm icon-btn" type="button" :disabled="loading" @click="loadPlans">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </button>
        <button class="btn btn-primary btn-sm icon-btn" type="button" :disabled="submitting" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新建套餐</span>
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>编码</th>
            <th>名称</th>
            <th>会员等级</th>
            <th>价格</th>
            <th>有效期</th>
            <th>折扣</th>
            <th>配置</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="plan in plans" :key="plan.id_wsh">
            <td><code>{{ plan.code_wsh }}</code></td>
            <td>{{ plan.name_wsh }}</td>
            <td>{{ levelText(plan.level_wsh) }}</td>
            <td>¥{{ money(plan.price_wsh) }}</td>
            <td>{{ plan.duration_days_wsh }} 天</td>
            <td>{{ discountText(plan.discount_rate_wsh) }}</td>
            <td>
              <div class="config-tags">
                <span v-if="monthlyCouponIds(plan.monthly_coupon_config_wsh).length" class="config-tag">
                  月券 {{ monthlyCouponSummary(plan.monthly_coupon_config_wsh) }}
                </span>
                <span v-if="Number(plan.discount_rate_wsh || 1) < 1" class="config-tag">会员折扣</span>
                <span
                  v-if="!monthlyCouponIds(plan.monthly_coupon_config_wsh).length && Number(plan.discount_rate_wsh || 1) >= 1"
                  class="muted"
                >
                  -
                </span>
              </div>
            </td>
            <td>
              <span :class="['badge', plan.status_wsh === 1 ? 'badge-success' : 'badge-disabled']">
                {{ plan.status_wsh === 1 ? '启用' : '停用' }}
              </span>
            </td>
            <td>{{ dateText(plan.updated_at_wsh || plan.created_at_wsh) }}</td>
            <td>
              <div class="row-actions">
                <button class="btn btn-outline btn-sm square-btn" type="button" title="编辑" :disabled="submitting" @click="openEdit(plan)">
                  <el-icon><Edit /></el-icon>
                </button>
                <button
                  class="btn btn-sm square-btn"
                  :class="plan.status_wsh === 1 ? 'btn-danger' : 'btn-success'"
                  type="button"
                  :title="plan.status_wsh === 1 ? '停用' : '启用'"
                  :disabled="submitting"
                  @click="toggleStatus(plan)"
                >
                  <el-icon><SwitchButton /></el-icon>
                </button>
                <button class="btn btn-danger btn-sm square-btn" type="button" title="删除" :disabled="submitting" @click="removePlan(plan)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="plans.length === 0">
            <td colspan="10" class="empty-cell">暂无会员套餐</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="closeForm">
      <div class="modal member-plan-modal">
        <h2>{{ editingPlan ? '编辑会员套餐' : '新建会员套餐' }}</h2>
        <form @submit.prevent="submitForm">
          <div class="form-grid">
            <label class="form-field">
              套餐编码
              <input v-model.trim="form.code_wsh" required maxlength="50" placeholder="GOLD_MONTH" @input="normalizeCodeInput">
            </label>
            <label class="form-field">
              套餐名称
              <input v-model.trim="form.name_wsh" required maxlength="100" placeholder="月度黄金会员">
            </label>
            <label class="form-field">
              会员等级
              <input v-model.number="form.level_wsh" required min="1" step="1" type="number">
              <small class="field-help">管理员手动设置，数字越大等级越高。</small>
            </label>
            <label class="form-field">
              价格
              <input v-model.number="form.price_wsh" required min="0" step="0.01" type="number">
            </label>
            <label class="form-field">
              有效天数
              <input v-model.number="form.duration_days_wsh" required min="1" step="1" type="number">
            </label>
            <label class="form-field">
              会员倍率
              <input v-model.number="form.discount_rate_wsh" required max="1" min="0.01" step="0.01" type="number" placeholder="1 或 0.9">
              <small class="field-help">管理员手动设置：1 表示无折扣，0.9 表示 9 折。</small>
            </label>
            <label class="form-field">
              排序
              <input v-model.number="form.sort_order_wsh" min="0" step="1" type="number">
            </label>
            <label class="form-field">
              状态
              <select v-model.number="form.status_wsh">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
            <section class="form-section form-field--wide">
              <div class="section-heading">
                <div>
                  <h3>赠送优惠券</h3>
                  <p>直接选优惠券管理里的券；没有合适的券，可以在这里快速创建。</p>
                </div>
                <div class="section-actions">
                  <button class="btn btn-outline btn-sm" type="button" :disabled="couponLoading" @click="loadCouponTemplates">
                    {{ couponLoading ? '加载中...' : '刷新券库' }}
                  </button>
                  <button class="btn btn-outline btn-sm" type="button" @click="quickCouponVisible = !quickCouponVisible">
                    {{ quickCouponVisible ? '收起创建' : '直接创建优惠券' }}
                  </button>
                  <router-link class="btn btn-secondary btn-sm" to="/admin/coupons">优惠券管理</router-link>
                </div>
              </div>

              <div v-if="selectedCouponTemplates.length" class="selected-coupons">
                <span v-for="coupon in selectedCouponTemplates" :key="coupon.id_wsh" class="selected-coupon">
                  {{ coupon.name_wsh }}
                </span>
              </div>

              <div v-if="quickCouponVisible" class="quick-coupon-panel">
                <div class="quick-coupon-grid">
                  <label class="form-field">
                    优惠券名称
                    <input v-model.trim="quickCouponForm.name_wsh" maxlength="100" placeholder="例如：会员专享券">
                  </label>
                  <label class="form-field">
                    类型
                    <select v-model="quickCouponForm.type_wsh">
                      <option value="amount">满减券</option>
                      <option value="percent">折扣券</option>
                    </select>
                  </label>
                  <label class="form-field">
                    使用门槛
                    <input v-model.number="quickCouponForm.threshold_amount_wsh" min="0" step="0.01" type="number">
                  </label>
                  <label v-if="quickCouponForm.type_wsh === 'amount'" class="form-field">
                    减免金额
                    <input v-model.number="quickCouponForm.discount_amount_wsh" min="0.01" step="0.01" type="number">
                  </label>
                  <label v-else class="form-field">
                    折扣
                    <select v-model.number="quickCouponForm.discount_rate_wsh">
                      <option :value="0.95">9.5 折</option>
                      <option :value="0.9">9 折</option>
                      <option :value="0.85">8.5 折</option>
                      <option :value="0.8">8 折</option>
                    </select>
                  </label>
                  <label class="form-field">
                    有效天数
                    <input v-model.number="quickCouponForm.valid_days_wsh" min="1" step="1" type="number">
                  </label>
                  <label class="form-field">
                    总库存
                    <input v-model.number="quickCouponForm.total_quantity_wsh" min="0" step="1" type="number" placeholder="空为不限">
                  </label>
                  <label class="form-field">
                    每人限领
                    <input v-model.number="quickCouponForm.per_user_limit_wsh" min="1" step="1" type="number">
                  </label>
                </div>
                <div class="quick-coupon-actions">
                  <button class="btn btn-primary btn-sm" type="button" :disabled="quickCouponSubmitting" @click="submitQuickCoupon">
                    {{ quickCouponSubmitting ? '创建中...' : '创建并选中' }}
                  </button>
                </div>
              </div>

              <div v-if="couponLoading" class="coupon-empty">优惠券加载中...</div>
              <div v-else-if="couponTemplates.length === 0" class="coupon-empty">暂无优惠券，请先创建一张。</div>
              <div v-else class="coupon-picker">
                <label
                  v-for="coupon in couponTemplates"
                  :key="coupon.id_wsh"
                  :class="['coupon-option', form.coupon_template_ids_wsh.includes(Number(coupon.id_wsh)) ? 'is-selected' : '']"
                >
                  <input v-model="form.coupon_template_ids_wsh" type="checkbox" :value="Number(coupon.id_wsh)">
                  <span>
                    <strong>{{ coupon.name_wsh }}</strong>
                    <small>{{ couponTemplateText(coupon) }} · {{ couponStatusText(coupon) }}</small>
                  </span>
                </label>
              </div>
            </section>
            <label class="form-field form-field--wide">
              备注
              <textarea v-model.trim="form.remark_wsh" maxlength="500" rows="3"></textarea>
            </label>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary btn-sm" type="button" @click="closeForm">取消</button>
            <button class="btn btn-primary btn-sm" type="submit" :disabled="submitting">
              {{ submitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { createCouponTemplate, getCouponTemplates } from '@/api/coupon'
import {
  createMemberPlan,
  deleteMemberPlan,
  getAdminMemberPlans,
  updateMemberPlan,
  updateMemberPlanStatus,
} from '@/api/membership'

const appStore = useAppStore()
const loading = ref(false)
const submitting = ref(false)
const showForm = ref(false)
const editingPlan = ref(null)
const statusFilter = ref('')
const plans = ref([])
const couponTemplates = ref([])
const couponLoading = ref(false)
const quickCouponVisible = ref(false)
const quickCouponSubmitting = ref(false)
const form = reactive(defaultForm())
const quickCouponForm = reactive(defaultQuickCouponForm())

const selectedCouponTemplates = computed(() => {
  const selectedIds = new Set((form.coupon_template_ids_wsh || []).map(id => Number(id)))
  return couponTemplates.value.filter(coupon => selectedIds.has(Number(coupon.id_wsh)))
})

onMounted(() => {
  loadPlans()
  loadCouponTemplates()
})

async function loadPlans() {
  loading.value = true
  try {
    const params = statusFilter.value === '' ? {} : { status_wsh: Number(statusFilter.value) }
    const res = await getAdminMemberPlans(params)
    if (res.code === 200) {
      plans.value = Array.isArray(res.data) ? res.data : []
    } else {
      appStore.addToast(res.message || '会员套餐加载失败', 'error')
    }
  } catch (e) {
    appStore.addToast('会员套餐加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadCouponTemplates() {
  couponLoading.value = true
  try {
    const res = await getCouponTemplates()
    if (res.code === 200) {
      couponTemplates.value = Array.isArray(res.data) ? res.data : []
    } else {
      appStore.addToast(res.message || '优惠券加载失败', 'error')
    }
  } catch (e) {
    appStore.addToast('优惠券加载失败', 'error')
  } finally {
    couponLoading.value = false
  }
}

function openCreate() {
  editingPlan.value = null
  Object.assign(form, defaultForm())
  Object.assign(quickCouponForm, defaultQuickCouponForm())
  quickCouponVisible.value = false
  showForm.value = true
}

function openEdit(plan) {
  editingPlan.value = plan
  Object.assign(form, {
    code_wsh: plan.code_wsh || '',
    name_wsh: plan.name_wsh || '',
    level_wsh: plan.level_wsh || 1,
    price_wsh: Number(plan.price_wsh || 0),
    duration_days_wsh: plan.duration_days_wsh || 30,
    discount_rate_wsh: Number(plan.discount_rate_wsh || 1),
    monthly_coupon_config_wsh: plan.monthly_coupon_config_wsh || '',
    benefit_config_wsh: plan.benefit_config_wsh || '',
    coupon_template_ids_wsh: parseCouponTemplateIds(plan.monthly_coupon_config_wsh),
    status_wsh: plan.status_wsh ?? 1,
    sort_order_wsh: plan.sort_order_wsh || 0,
    remark_wsh: plan.remark_wsh || '',
  })
  Object.assign(quickCouponForm, defaultQuickCouponForm())
  quickCouponVisible.value = false
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingPlan.value = null
}

async function submitForm() {
  if (submitting.value) return
  const error = validateForm()
  if (error) {
    appStore.addToast(error, 'warning')
    return
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    const res = editingPlan.value
      ? await updateMemberPlan(editingPlan.value.id_wsh, payload)
      : await createMemberPlan(payload)
    if (res.code === 200) {
      appStore.addToast(editingPlan.value ? '会员套餐已更新' : '会员套餐已创建', 'success')
      closeForm()
      await loadPlans()
    } else {
      appStore.addToast(res.message || '会员套餐保存失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '会员套餐保存失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(plan) {
  if (submitting.value) return
  submitting.value = true
  try {
    const nextStatus = plan.status_wsh === 1 ? 0 : 1
    const res = await updateMemberPlanStatus(plan.id_wsh, nextStatus)
    if (res.code === 200) {
      plan.status_wsh = nextStatus
      appStore.addToast(nextStatus === 1 ? '会员套餐已启用' : '会员套餐已停用', 'success')
    } else {
      appStore.addToast(res.message || '状态更新失败', 'error')
    }
  } catch (e) {
    appStore.addToast('状态更新失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function removePlan(plan) {
  if (!confirm(`确定删除会员套餐“${plan.name_wsh}”吗？已有会员或订单引用时后端会拒绝删除。`)) return
  submitting.value = true
  try {
    const res = await deleteMemberPlan(plan.id_wsh)
    if (res.code === 200) {
      appStore.addToast('会员套餐已删除', 'success')
      await loadPlans()
    } else {
      appStore.addToast(res.message || '删除失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '删除失败', 'error')
  } finally {
    submitting.value = false
  }
}

function validateForm() {
  if (!/^[A-Z0-9_]{2,50}$/.test(form.code_wsh || '')) return '套餐编码只能使用大写字母、数字和下划线'
  if (!form.name_wsh) return '请填写套餐名称'
  if (Number(form.level_wsh || 0) <= 0) return '等级必须大于0'
  if (Number(form.price_wsh) < 0) return '价格不能为负数'
  if (Number(form.duration_days_wsh || 0) <= 0) return '有效天数必须大于0'
  const rate = Number(form.discount_rate_wsh)
  if (rate <= 0 || rate > 1) return '订单折扣率必须大于0且不超过1'
  return ''
}

function buildPayload() {
  return {
    code_wsh: form.code_wsh,
    name_wsh: form.name_wsh,
    level_wsh: Number(form.level_wsh),
    price_wsh: moneyNumber(form.price_wsh),
    duration_days_wsh: Number(form.duration_days_wsh),
    discount_rate_wsh: moneyNumber(form.discount_rate_wsh),
    monthly_coupon_config_wsh: buildMonthlyCouponConfig(),
    benefit_config_wsh: buildBenefitConfig(),
    status_wsh: Number(form.status_wsh),
    sort_order_wsh: Number(form.sort_order_wsh || 0),
    remark_wsh: blankToNull(form.remark_wsh),
  }
}

function defaultForm() {
  return {
    code_wsh: '',
    name_wsh: '',
    level_wsh: 1,
    price_wsh: 0,
    duration_days_wsh: 30,
    discount_rate_wsh: 1,
    monthly_coupon_config_wsh: '',
    benefit_config_wsh: '',
    coupon_template_ids_wsh: [],
    status_wsh: 1,
    sort_order_wsh: 0,
    remark_wsh: '',
  }
}

function defaultQuickCouponForm() {
  return {
    name_wsh: '',
    type_wsh: 'amount',
    threshold_amount_wsh: 0,
    discount_amount_wsh: 10,
    discount_rate_wsh: 0.9,
    valid_days_wsh: 30,
    total_quantity_wsh: '',
    per_user_limit_wsh: 1,
  }
}

function normalizeCodeInput() {
  form.code_wsh = String(form.code_wsh || '').toUpperCase().replace(/[^A-Z0-9_]/g, '')
}

function blankToNull(value) {
  return value && String(value).trim() ? String(value).trim() : null
}

async function submitQuickCoupon() {
  if (quickCouponSubmitting.value) return
  const error = validateQuickCouponForm()
  if (error) {
    appStore.addToast(error, 'warning')
    return
  }
  quickCouponSubmitting.value = true
  try {
    const res = await createCouponTemplate(buildQuickCouponPayload())
    if (res.code === 200) {
      appStore.addToast('优惠券已创建并选中', 'success')
      await loadCouponTemplates()
      const createdId = Number(res.data?.id_wsh)
      if (createdId && !form.coupon_template_ids_wsh.includes(createdId)) {
        form.coupon_template_ids_wsh.push(createdId)
      }
      Object.assign(quickCouponForm, defaultQuickCouponForm())
      quickCouponVisible.value = false
    } else {
      appStore.addToast(res.message || '优惠券创建失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.message || '优惠券创建失败', 'error')
  } finally {
    quickCouponSubmitting.value = false
  }
}

function validateQuickCouponForm() {
  if (!quickCouponForm.name_wsh) return '请填写优惠券名称'
  if (Number(quickCouponForm.threshold_amount_wsh || 0) < 0) return '使用门槛不能为负数'
  if (quickCouponForm.type_wsh === 'amount' && Number(quickCouponForm.discount_amount_wsh || 0) <= 0) return '减免金额必须大于0'
  if (quickCouponForm.type_wsh === 'percent') {
    const rate = Number(quickCouponForm.discount_rate_wsh || 0)
    if (rate <= 0 || rate >= 1) return '折扣必须大于0且小于10折'
  }
  if (Number(quickCouponForm.valid_days_wsh || 0) <= 0) return '有效天数必须大于0'
  if (quickCouponForm.total_quantity_wsh !== '' && Number(quickCouponForm.total_quantity_wsh) < 0) return '总库存不能为负数'
  if (Number(quickCouponForm.per_user_limit_wsh || 0) <= 0) return '每人限领必须大于0'
  return ''
}

function buildQuickCouponPayload() {
  const now = new Date()
  const end = new Date(now)
  end.setDate(end.getDate() + Number(quickCouponForm.valid_days_wsh || 30))
  return {
    name_wsh: quickCouponForm.name_wsh,
    type_wsh: quickCouponForm.type_wsh,
    threshold_amount_wsh: moneyNumber(quickCouponForm.threshold_amount_wsh),
    discount_amount_wsh: quickCouponForm.type_wsh === 'amount' ? moneyNumber(quickCouponForm.discount_amount_wsh) : null,
    discount_rate_wsh: quickCouponForm.type_wsh === 'percent' ? Number(quickCouponForm.discount_rate_wsh) : null,
    max_discount_amount_wsh: null,
    total_quantity_wsh: quickCouponForm.total_quantity_wsh === '' ? null : Number(quickCouponForm.total_quantity_wsh),
    per_user_limit_wsh: Number(quickCouponForm.per_user_limit_wsh || 1),
    valid_from_wsh: toApiDateTime(now),
    valid_to_wsh: toApiDateTime(end),
    status_wsh: 1,
    scope_type_wsh: 'platform',
    merchant_id_wsh: null,
    remark_wsh: '会员套餐赠券',
  }
}

function buildMonthlyCouponConfig() {
  const config = parseJsonObject(form.monthly_coupon_config_wsh)
  const ids = uniquePositiveIds(form.coupon_template_ids_wsh)
  if (ids.length > 0) {
    config.coupon_template_ids = ids
  } else {
    delete config.coupon_template_ids
  }
  return Object.keys(config).length ? JSON.stringify(config) : null
}

function buildBenefitConfig() {
  const config = parseJsonObject(form.benefit_config_wsh)
  if (Number(form.discount_rate_wsh || 1) < 1) {
    config.order_discount = true
  } else {
    delete config.order_discount
  }
  return Object.keys(config).length ? JSON.stringify(config) : null
}

function parseCouponTemplateIds(configText) {
  const config = parseJsonObject(configText)
  return uniquePositiveIds(config.coupon_template_ids || [])
}

function parseJsonObject(value) {
  if (!value) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? { ...parsed } : {}
  } catch (_) {
    return {}
  }
}

function uniquePositiveIds(value) {
  return [...new Set((Array.isArray(value) ? value : [])
    .map(item => Number(item))
    .filter(item => Number.isFinite(item) && item > 0))]
}

function monthlyCouponIds(configText) {
  return parseCouponTemplateIds(configText)
}

function monthlyCouponSummary(configText) {
  const ids = monthlyCouponIds(configText)
  if (!ids.length) return ''
  const names = ids
    .map(id => couponTemplates.value.find(coupon => Number(coupon.id_wsh) === Number(id))?.name_wsh)
    .filter(Boolean)
  if (!names.length) return `${ids.length} 张`
  if (names.length <= 2) return names.join('、')
  return `${names.slice(0, 2).join('、')}等 ${ids.length} 张`
}

function levelText(value) {
  const level = Number(value || 1)
  return `Lv${level}`
}

function couponTemplateText(coupon) {
  if (coupon.type_wsh === 'percent') {
    const rate = Number(coupon.discount_rate_wsh || 0)
    const max = Number(coupon.max_discount_amount_wsh || 0)
    return `${(rate * 10).toFixed(1)}折${max > 0 ? `，最高减¥${money(max)}` : ''}`
  }
  return `满¥${money(coupon.threshold_amount_wsh)}减¥${money(coupon.discount_amount_wsh)}`
}

function couponStatusText(coupon) {
  return coupon.status_wsh === 1 ? '启用' : '停用'
}

function moneyNumber(value) {
  return Number(Number(value || 0).toFixed(2))
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function discountText(value) {
  const rate = Number(value || 1)
  return rate >= 1 ? '无折扣' : `${(rate * 10).toFixed(1)} 折`
}

function dateText(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function toApiDateTime(date) {
  const pad = value => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}
</script>

<style scoped>
.member-plan-page {
  display: grid;
  gap: 16px;
}

.member-plan-toolbar {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  flex-wrap: wrap;
  min-width: 0;
}

.member-plan-toolbar h2 {
  font-size: 22px;
  margin: 0;
  min-width: 0;
}

.toolbar-actions,
.row-actions,
.config-tags {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.filter-select {
  min-height: 36px;
  padding: 8px 12px;
  width: 120px;
}

.icon-btn :deep(.el-icon),
.square-btn :deep(.el-icon) {
  font-size: 16px;
}

.square-btn {
  height: 34px;
  min-height: 34px;
  padding: 0;
  width: 34px;
}

.member-plan-modal {
  max-width: 860px;
}

.form-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.form-field {
  color: var(--color-muted-foreground);
  display: grid;
  font-size: 13px;
  gap: 6px;
}

.form-field--wide {
  grid-column: 1 / -1;
}

.field-help {
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
}

.form-section {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  display: grid;
  gap: 14px;
  padding: 16px;
}

.section-heading {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.section-heading h3 {
  color: var(--color-foreground);
  font-size: 16px;
  margin: 0 0 4px;
}

.section-heading p,
.coupon-empty {
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}

.section-actions,
.quick-coupon-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-coupons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-coupon {
  background: rgba(249, 115, 22, 0.12);
  border-radius: 999px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
}

.quick-coupon-panel {
  background: var(--color-muted);
  border-radius: var(--radius-md);
  display: grid;
  gap: 12px;
  padding: 14px;
}

.quick-coupon-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.coupon-picker {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.coupon-option {
  align-items: flex-start;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-foreground);
  cursor: pointer;
  display: grid;
  gap: 10px;
  grid-template-columns: auto minmax(0, 1fr);
  padding: 12px;
}

.coupon-option.is-selected {
  background: rgba(249, 115, 22, 0.08);
  border-color: var(--color-primary);
}

.coupon-option input {
  margin-top: 3px;
  padding: 0;
  width: auto;
}

.coupon-option strong {
  display: block;
  font-size: 13px;
  line-height: 1.4;
}

.coupon-option small {
  color: var(--color-muted-foreground);
  display: block;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 2px;
}

.config-tag {
  background: rgba(37, 99, 235, 0.12);
  border-radius: 999px;
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
  padding: 3px 8px;
}

.muted,
.empty-cell {
  color: var(--color-muted-foreground);
}

.empty-cell {
  padding: 32px;
  text-align: center;
}

code {
  background: var(--color-muted);
  border-radius: 6px;
  color: var(--color-foreground);
  font-size: 12px;
  padding: 3px 6px;
}

@media (max-width: 720px) {
  .member-plan-toolbar,
  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-select,
  .toolbar-actions .btn {
    width: 100%;
  }

  .section-heading,
  .section-actions,
  .quick-coupon-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
