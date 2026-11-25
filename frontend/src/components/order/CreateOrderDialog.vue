<template>
  <AppDialog :visible="visible" :width="960" @close="handleClose">
    <template #header>
      <div class="order-modal__header">
        <h2>创建订单</h2>
        <p class="order-modal__subtitle">先选好宠物，再安排送达、接回时间（看护人由商家自动分配）</p>
      </div>
    </template>

    <!-- ========== 服务信息（只读） ========== -->
    <div class="service-info-card">
      <div class="service-info-row">
        <span class="service-info-label">服务</span>
        <span class="service-info-value">{{ serviceName || '加载中...' }}</span>
      </div>
      <div class="service-info-row">
        <span class="service-info-label">计价</span>
        <span class="service-info-value">￥{{ price || '--' }} / {{ unitLabel(serviceUnit) }}</span>
      </div>
    </div>

    <div class="form-grid">
      <!-- ========== 第一步：宠物与负责人 ========== -->
      <div class="form-section-title form-grid__wide">
        <span class="form-step">1</span>
        <span>宠物与负责人</span>
      </div>

      <label>
        宠物
        <select v-model="form.pet_id_wsh" class="form-control">
          <option value="">请选择宠物</option>
          <option v-for="pet in pets" :key="pet.id_wsh" :value="pet.id_wsh">
            {{ pet.name_wsh }} {{ pet.breed_wsh ? `(${pet.breed_wsh})` : '' }}
          </option>
        </select>
      </label>

      <!-- 商家由当前服务指定，客户不可修改 -->
      <div class="readonly-field">
        <span class="readonly-label">商家</span>
        <div class="readonly-value-row">
          <span class="readonly-value">{{ merchantDisplayName }}</span>
          <span class="readonly-badge">由当前服务指定</span>
        </div>
      </div>

      <!-- 看护人不在这里指定：支付后由看护者抢单 / 商家指派 / 超时自动分配 -->
      <div class="readonly-field">
        <span class="readonly-label">看护人</span>
        <div class="readonly-value-row">
          <span class="readonly-value">由商家自动分配</span>
          <span class="readonly-badge">支付后 30 分钟内确定</span>
        </div>
      </div>

      <!-- ========== 第二步：预约时间 ========== -->
      <div class="form-section-title form-grid__wide">
        <span class="form-step">2</span>
        <span>预约时间</span>
      </div>

      <!-- 可约日期由服务端 availability 决定；min/max 只是浏览器软约束 -->
      <label>
        送达日期
        <input v-model="form.delivery_date_wsh" type="date" class="form-control" :min="minDate()" :max="maxDate()" :disabled="availabilityLoading || bookableDates.length === 0" @change="onDeliveryDateChange">
        <span class="field-hint">{{ availabilityHint }}</span>
      </label>
      <label>
        {{ slotMode ? '开始时间' : '送达时间' }}
        <select v-model="form.delivery_slot_wsh" class="form-control" :disabled="!form.delivery_date_wsh || deliverySlotOptions.length === 0" @change="onDeliverySlotChange">
          <option value="">{{ deliverySlotPlaceholder }}</option>
          <option v-for="slot in deliverySlotOptions" :key="slot" :value="slot">{{ displaySlot(slot) }}</option>
        </select>
      </label>

      <!-- 按小时计费：数量 × 计费粒度 = 总时长 -->
      <label v-if="isHourUnit">
        {{ slotQuantityLabel }}
        <input data-testid="quantity-input" v-model.number="slotQuantity" type="number" class="form-control" min="1" :max="slotMaxQuantity">
        <span class="field-hint">{{ slotQuantityHint }}</span>
      </label>
      <!-- 按次/按场计费：固定 1 个单位 -->
      <label v-if="slotMode && !isHourUnit">
        服务数量
        <input class="form-control" :value="1" disabled>
        <span class="field-hint">每次服务一个连续时段</span>
      </label>

      <!-- 按夜寄养：用户选夜数，离店日由夜数推导 -->
      <label v-if="isNightUnit">
        预订夜数
        <input data-testid="nights-input" v-model.number="form.nights_wsh" type="number" class="form-control" min="1" :max="MAX_NIGHTS">
        <span class="field-hint">￥{{ price || '--' }} / {{ unitLabel(serviceUnit) }}；最终金额以服务端报价为准</span>
      </label>
      <label v-if="isNightUnit">
        离店日期（自动推导）
        <input class="form-control" :value="nightCheckoutDate || '--'" disabled data-testid="checkout-date">
        <span class="field-hint">离店日 14:00 标准离店，14:30 前免费；提前送达只加收费用、不顺延离店</span>
      </label>

      <!-- 非 slot 模式才需要单独选接回时间 -->
      <label v-if="!slotMode">
        接回时间
        <select v-model="form.pickup_slot_wsh" class="form-control" :disabled="!form.pickup_date_wsh || pickupSlotOptions.length === 0">
          <option value="">{{ pickupSlotPlaceholder }}</option>
          <option v-for="slot in pickupSlotOptions" :key="slot" :value="slot">{{ displaySlot(slot) }}</option>
        </select>
      </label>

      <!-- ========== 第三步：到店交接 ========== -->
      <div class="form-section-title form-grid__wide">
        <span class="form-step">3</span>
        <span>到店交接</span>
      </div>
      <!-- 地址固定为门店，值由 fillDeliveryFromMerchant() 写入，客户不可改 -->
      <label class="form-grid__wide">
        <span>宠物送达地址</span>
        <input
          class="form-control"
          type="text"
          :value="form.delivery_address_wsh || '请先选择门店'"
          readonly
          data-testid="delivery-address"
        >
        <span class="field-hint">宠物统一送至门店，由您当面交给看护者并出示交接码</span>
      </label>

      <!-- ========== 第四步：联系信息与备注 ========== -->
      <div class="form-section-title form-grid__wide">
        <span class="form-step">4</span>
        <span>联系信息与备注</span>
      </div>
      <label>
        紧急联系人
        <input v-model="form.emergency_contact_name_wsh" class="form-control" placeholder="联系人姓名">
      </label>
      <label>
        紧急联系电话
        <input v-model="form.emergency_contact_phone_wsh" class="form-control" placeholder="联系人手机号" type="tel">
      </label>
      <!-- 后端已按门槛过滤并排序，首张即最优；用户手动改过后不再自动覆盖 -->
      <label class="form-grid__wide">
        优惠券
        <select v-model="form.user_coupon_id_wsh" class="form-control" :disabled="couponLoading || availableCoupons.length === 0" @change="onCouponChange">
          <option value="">{{ couponLoading ? '正在加载优惠券...' : (availableCoupons.length === 0 ? '暂无优惠券' : '不使用优惠券') }}</option>
          <option v-for="coupon in availableCoupons" :key="coupon.id_wsh" :value="coupon.id_wsh">
            {{ coupon.name_wsh || `优惠券 #${coupon.id_wsh}` }} · {{ couponText(coupon) }}
          </option>
        </select>
      </label>
      <label class="form-grid__wide">
        备注
        <textarea v-model="form.remark_wsh" class="form-control" rows="3" placeholder="饮食、过敏、用药、性格等注意事项"></textarea>
      </label>
    </div>

    <!-- ========== 报价区（金额唯一来源：后端 preview） ========== -->
    <!-- 优先级：单位异常 > 报价失败 > 计算中 > 正常展示 -->
    <div class="estimate estimate--error" v-if="unitDataError">
      {{ unitDataError }}
    </div>
    <div class="estimate estimate--error" v-else-if="quoteState.error">
      价格计算失败：{{ quoteState.error }}
    </div>
    <div class="estimate" v-else-if="quoteState.loading">
      正在计算价格...
    </div>
    <div class="estimate" v-else>
      <!-- total 为 0 视为「尚无有效报价」，而非真的 0 元订单 -->
      <template v-if="estimatedAmount.total && estimatedAmount.total !== '0.00'">
        <div>本次预付：￥{{ estimatedAmount.final }}</div>
        <div style="font-weight:400;font-size:13px;margin-top:4px">
          原价 ￥{{ estimatedAmount.total }}
          <span v-if="Number(estimatedAmount.discount) > 0" style="color:var(--color-danger);margin-left:8px">优惠 ￥{{ estimatedAmount.discount }}</span>
          <span v-if="Number(estimatedAmount.couponDiscount) > 0" style="color:var(--color-danger);margin-left:8px">券减 ￥{{ estimatedAmount.couponDiscount }}</span>
          <span v-if="Number(estimatedAmount.membershipDiscount) > 0" style="color:var(--color-danger);margin-left:8px">会员减 ￥{{ estimatedAmount.membershipDiscount }}</span>
        </div>
        <!-- 晚接费仅预估，不进本次预付 -->
        <div v-if="isNightUnit" class="estimate-note" data-testid="overtime-estimate">
          预计晚接费：￥{{ estimatedAmount.overtime }}（{{ estimatedAmount.overtimeHours }} 小时）
          —— 仅预估，实际接走时按真实时间结算，不在本次预付内
        </div>
        <div v-if="Number(estimatedAmount.platformSubsidy) > 0" class="estimate-note">
          优惠券由平台补贴，商家结算不受影响
        </div>
      </template>
      <template v-else>选择宠物与服务时间后显示价格</template>
    </div>

    <template #footer>
      <button class="btn btn-secondary btn-sm" type="button" @click="handleClose">取消</button>
      <!-- 只按「加载/错误态」禁用；表单不完整时仍可点，由 submitBlockReason 给出具体提示 -->
      <button class="btn btn-primary btn-sm" type="button" :disabled="Boolean(submitLoadingReason)"
              @click="submitOrder">

        {{ submitting ? '提交中...' : '确认下单' }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup>
// 依赖
import { ref, reactive, computed, watch } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { getPets } from '@/api/pet'
import { getMerchants, getMerchant } from '@/api/merchant'
import { getServiceDetail, getServiceAvailability } from '@/api/service'
import { createOrder, previewOrder as fetchOrderQuote } from '@/api/order'
import { getAvailableCoupons } from '@/api/coupon'
import { unitLabel, standardCheckoutDate, isStandardUnit } from '@/domain/BookingUnit'
import { AVAILABILITY_REQUEST_WINDOW_DAYS } from '@/domain/BookingWindow'

// Props / Emits / Store
const props = defineProps({
  visible: Boolean,
  initialServiceId: { type: String, default: '' },
})
const emit = defineEmits(['close', 'created'])

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

// 基础状态
const submitting = ref(false)
const merchantLoading = ref(false)
const pets = ref([])
const merchants = ref([])
const serviceName = ref('')
const serviceId = ref('')
const serviceVersion = ref('')
const price = ref('')
// 打开弹窗时快照当前时间；提交时会再刷新一次，避免用户开着弹窗很久后才提交
const nowTick = ref(Date.now())

// 优惠券状态
const availableCoupons = ref([])
const couponLoading = ref(false)
// 用户手动改过券后，不再自动勾选最优券
const couponTouched = ref(false)
// 并发守卫：只有最后一次拉券结果生效
let couponSeq = 0
// 券列表按哪个金额基数拉的；金额没变就不重复拉，避免「报价↔拉券」循环
let couponLoadedAmount = null

/* ============================================================================
 * 报价状态（全前端唯一价格来源：POST /api/orders/preview）
 * 金额/天数/计费小时一律由后端返回，前端不做任何乘除，
 * 保证「显示价」与「下单扣款价」同源。
 * ========================================================================== */
const quoteState = reactive({ loading: false, error: '', single: null })
// 并发守卫：快速改日期/数量时只有最后一次结果生效
let quoteSeq = 0
// PRICE_CHANGED 自动重载的防重入标记：重载后仍失败就停在错误态
let quoteReloading = false

// 可预约性状态（由服务端 availability 决定）
const availabilityLoading = ref(false)
const availabilityError = ref(false)
const availabilityDays = ref([])
// 并发守卫：换商家/服务时丢弃过期响应
let availabilitySeq = 0
// 服务端返回的预约窗口天数（含首尾）；0 = 尚未查询
const maxBookingDays = ref(0)

// 服务元信息（决定表单形态与计费方式）
const serviceUnit = ref('')
const serviceBookingMode = ref('')
const serviceDurationMinutes = ref(null)
const slotQuantity = ref(1)
const merchantNameFromService = ref('')

// 表单数据（字段与后端 DTO 一一对应，统一 _wsh 后缀）
const form = reactive({
  pet_id_wsh: '',
  merchant_id_wsh: '',
  delivery_address_wsh: '',
  delivery_location_source_wsh: '',
  delivery_latitude_wsh: null,
  delivery_longitude_wsh: null,
  delivery_date_wsh: '',
  delivery_slot_wsh: '',
  pickup_date_wsh: '',
  pickup_slot_wsh: '',
  // 按夜寄养：用户选夜数，离店日由它推导，不能直接编辑
  nights_wsh: 1,
  emergency_contact_name_wsh: '',
  emergency_contact_phone_wsh: '',
  user_coupon_id_wsh: '',
  remark_wsh: '',
})

/** 预订夜数上限（与后端 booking.max-order-days 默认 365 一致） */
const MAX_NIGHTS = 365

// 计算属性 —— 单位 / 模式 / 表单形态

/** 按夜寄养的离店日期 = 入住日 + 夜数（仅表单推导与展示，金额仍由后端算） */
const nightCheckoutDate = computed(() => standardCheckoutDate(form.delivery_date_wsh, form.nights_wsh))

// 夜数或入住日变化 → 同步离店日期
watch([() => form.nights_wsh, () => form.delivery_date_wsh], () => {
  if (isNightUnit.value && form.delivery_date_wsh) {
    form.pickup_date_wsh = nightCheckoutDate.value
  }
})

// 商家名展示优先级：已加载商家对象 > 服务详情里的商家名 > 占位
const merchantDisplayName = computed(() => {
  const m = selectedMerchant.value
  if (m) return m.name_wsh
  return merchantNameFromService.value || '加载中...'
})

// 非标准单位直接拦下，绝不静默当成标准单位继续下单
const unitDataError = computed(() => {
  if (!serviceUnit.value) return ''
  return isStandardUnit(serviceUnit.value)
    ? ''
    : `服务计价单位数据异常（${serviceUnit.value}），请联系商家`
})

const isNightUnit = computed(() => serviceUnit.value === 'night')

// slot 模式：用户选「开始时间 + 数量」，结束时间系统推导；
// 非 slot 模式（按夜寄养）：用户分别选送达/接回时间
const slotMode = computed(() => {
  const mode = serviceBookingMode.value
  if (mode) return mode === 'slot'
  const unit = serviceUnit.value
  return unit === 'session' || unit === 'hour'
})

const isHourUnit = computed(() => serviceUnit.value === 'hour')

/** 计费粒度（分钟）：服务可配 30/60/90…，缺省 60 */
const slotGranularityMinutes = computed(() => {
  const value = Number(serviceDurationMinutes.value)
  return Number.isFinite(value) && value > 0 ? value : 60
})

const slotUnitCount = computed(() => Math.max(1, Number(slotQuantity.value) || 1))
const slotTotalMinutes = computed(() => slotUnitCount.value * slotGranularityMinutes.value)
const slotMaxQuantity = computed(() => Math.max(1, Math.floor(1440 / slotGranularityMinutes.value)))

const slotQuantityLabel = computed(() => slotGranularityMinutes.value === 60
  ? '服务数量（小时）'
  : `服务数量（每单位 ${slotGranularityMinutes.value} 分钟）`)

const slotQuantityHint = computed(() => {
  const total = slotTotalMinutes.value
  // 保留两位再去尾零：1.5 小时 / 0.75 小时都显示正确
  const hours = Number((total / 60).toFixed(2)).toString()
  return `连续服务时长 = ${slotUnitCount.value} × ${slotGranularityMinutes.value} 分钟 = ${total} 分钟（${hours} 小时）`
})

// 计算属性 —— 可预约日期与时间槽
const bookableDates = computed(() =>
  availabilityDays.value
    .filter(day => day.bookable_wsh)
    .map(day => String(day.date_wsh)))

function slotsForDate(dateValue) {
  if (!dateValue) return []
  const day = availabilityDays.value.find(item => String(item.date_wsh) === dateValue)
  if (!day || !day.bookable_wsh) return []
  return day.windows_wsh.flatMap(window => window.slots_wsh || [])
}

const deliverySlotOptions = computed(() => slotsForDate(form.delivery_date_wsh))

const pickupSlotOptions = computed(() => {
  const slots = slotsForDate(form.pickup_date_wsh)
  // 同日接回必须晚于送达时间；跨天接回由日期本身保证顺序
  if (String(form.pickup_date_wsh) === String(form.delivery_date_wsh) && form.delivery_slot_wsh) {
    return slots.filter(slot => slot > form.delivery_slot_wsh)
  }
  return slots
})

// 计算属性 —— 金额展示
const ZERO_ESTIMATE = {
  total: '0.00', discount: '0.00', couponDiscount: '0.00',
  membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00',
  overtime: '0.00', overtimeHours: 0,
}

function fmtAmount(value) {
  return Math.max(0, Number(value || 0)).toFixed(2)
}

/** 直接取服务端报价，前端不参与任何计算 */
const estimatedAmount = computed(() => {
  const row = quoteState.single
  if (!row) return ZERO_ESTIMATE
  return {
    total: fmtAmount(row.total_amount_wsh),
    discount: fmtAmount(row.long_stay_discount_wsh),
    couponDiscount: fmtAmount(row.coupon_discount_wsh),
    membershipDiscount: fmtAmount(row.membership_discount_wsh),
    platformSubsidy: fmtAmount(row.platform_subsidy_wsh),
    final: fmtAmount(row.final_amount_wsh),
    // 预计晚接费：仅展示，不参与本次预付
    overtime: fmtAmount(row.estimated_overtime_amount_wsh),
    overtimeHours: Number(row.estimated_overtime_hours_wsh || 0),
  }
})

// 请求载荷构造（预览与提交同源，杜绝「预览一套、提交另一套」）

/**
 * 预览与提交共用的时间字段。返回 null 表示信息还不完整。
 */
function buildTimeFields() {
  // —— slot 模式：只要开始时间 + 数量 ——
  if (slotMode.value) {
    const start = slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
    if (!start) return null
    return {
      start_time_wsh: toApiDateTime(start),
      quantity_wsh: serviceUnit.value === 'hour' ? Math.max(1, Number(slotQuantity.value) || 1) : 1,
    }
  }

  // —— 非 slot 模式：送达 + 接回 ——
  const deliveryTime = slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
  const pickupTime = slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)
  if (!deliveryTime || !pickupTime) return null
  const start = new Date(deliveryTime)
  const end = new Date(pickupTime)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) return null

  const fields = {
    delivery_time_wsh: toApiDateTime(deliveryTime),
    pickup_time_wsh: toApiDateTime(pickupTime),
  }
  if (isNightUnit.value) {
    // 按夜寄养：quantity = 夜数，start_date = 入住日，end_date = 离店日（D+N）
    const nights = Math.max(1, Number(form.nights_wsh) || 1)
    const startDate = formatDateInput(start)
    fields.quantity_wsh = nights
    fields.start_date_wsh = startDate
    fields.end_date_wsh = standardCheckoutDate(startDate, nights)
  } else {
    const serviceDates = buildServiceDates(start, end)
    fields.start_date_wsh = serviceDates.startDate
    fields.end_date_wsh = serviceDates.endDate
  }
  return fields
}

/**
 * 组装预览请求。
 * 除时间字段外，还带上 service_version / billing_unit / expected_unit_price，
 * 让「商家改价 / 版本变更 / 单位不一致」在询价阶段就暴露，而不是等到提交才报 PRICE_CHANGED。
 */
function buildQuotePayload() {
  const petId = form.pet_id_wsh
  if (!serviceId.value || !petId) return null
  const timeFields = buildTimeFields()
  if (!timeFields) return null
  const payload = {
    service_id_wsh: Number(serviceId.value),
    pet_id_wsh: Number(petId),
    ...timeFields,
  }
  if (form.merchant_id_wsh) payload.merchant_id_wsh = Number(form.merchant_id_wsh)
  if (form.user_coupon_id_wsh) payload.user_coupon_id_wsh = Number(form.user_coupon_id_wsh)
  if (serviceVersion.value) payload.service_version_wsh = serviceVersion.value
  if (serviceUnit.value) payload.billing_unit_wsh = serviceUnit.value
  if (price.value != null && price.value !== '') {
    payload.expected_unit_price_wsh = Number(price.value)
  }
  return payload
}

/** 提交单只订单时与服务无关的公共字段 */
function buildSharedPayload() {
  return {
    merchant_id_wsh: Number(form.merchant_id_wsh),
    service_version_wsh: serviceVersion.value || null,
    user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
    billing_unit_wsh: serviceUnit.value,
    expected_unit_price_wsh: Number(price.value || 0),
    delivery_address_wsh: form.delivery_address_wsh,
    delivery_location_source_wsh: form.delivery_location_source_wsh || 'merchant',
    delivery_latitude_wsh: form.delivery_latitude_wsh,
    delivery_longitude_wsh: form.delivery_longitude_wsh,
    pickup_address_wsh: form.delivery_address_wsh,
    pickup_location_source_wsh: form.delivery_location_source_wsh || 'merchant',
    emergency_contact_name_wsh: form.emergency_contact_name_wsh,
    emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
    remark_wsh: form.remark_wsh,
  }
}

// 报价流程
function clearQuote() {
  quoteState.single = null
  quoteState.error = ''
  quoteState.loading = false
}

/**
 * 询价。失败时清空金额并记录错误，绝不回退到前端自算。
 * 并发用 quoteSeq 守卫：只有最后一次请求的结果生效。
 */
async function refreshQuote() {
  const seq = ++quoteSeq
  const payload = buildQuotePayload()
  if (!payload) {
    if (seq === quoteSeq) clearQuote()
    return
  }
  quoteState.loading = true
  quoteState.error = ''
  try {
    const res = await fetchOrderQuote(payload)
    if (seq !== quoteSeq) return
    quoteState.single = res.data ?? res
    refreshCoupons()
  } catch (e) {
    if (seq !== quoteSeq) return
    quoteState.single = null
    const data = e?.response?.data
    const code = data?.errorCode || data?.error_code_wsh
    if (code === 'PRICE_CHANGED' && !quoteReloading) {
      // ⚠️ 商家改价/版本变化：自动重载服务信息后重新询价。
      // 否则按钮会永久 disabled（错误文案让用户「刷新」，但弹窗里没有刷新入口）。
      quoteReloading = true
      reloadServiceState().finally(() => { quoteReloading = false })
      return
    }
    quoteState.error = data?.message || e?.message || '价格计算失败'
  } finally {
    if (seq === quoteSeq) quoteState.loading = false
  }
}

/**
 * 触发询价。日期/槽位都是离散事件，不做防抖：
 * 少一次定时器就少一处「用户已改时间但金额还是旧的」的窗口。
 * 并发由 refreshQuote 内部的 seq 守卫保证。
 */
function scheduleQuote() {
  refreshQuote()
}

// 数量手输可能超过单笔上限（如 30 分钟粒度最多 48 个单位），超了钳回上限
watch([slotQuantity, slotMaxQuantity], () => {
  const max = slotMaxQuantity.value
  const current = Number(slotQuantity.value)
  if (!Number.isFinite(current) || current < 1) {
    slotQuantity.value = 1
  } else if (current > max) {
    slotQuantity.value = max
  }
})

// 优惠券

/** 券门槛基数 = 基础价 − 长住折扣（与后端一致） */
function couponBaseAmount() {
  const row = quoteState.single
  if (!row) return 0
  return Math.max(0, Number(row.total_amount_wsh || 0) - Number(row.long_stay_discount_wsh || 0))
}

const selectedMerchant = computed(() =>
  merchants.value.find(item => Number(item.id_wsh) === Number(form.merchant_id_wsh)))

/**
 * 拉取可用券并自动勾选最优（后端已排序）。
 * 金额没变不重复拉，避免「报价→拉券→改券→再报价」循环。
 */
async function refreshCoupons(force = false) {
  const baseAmount = couponBaseAmount()
  if (!force && baseAmount === couponLoadedAmount) return
  const seq = ++couponSeq
  if (!form.merchant_id_wsh || baseAmount <= 0) {
    couponLoadedAmount = baseAmount
    availableCoupons.value = []
    form.user_coupon_id_wsh = ''
    couponLoading.value = false
    return
  }
  couponLoading.value = true
  try {
    const res = await getAvailableCoupons({
      merchant_id_wsh: Number(form.merchant_id_wsh),
      order_amount_wsh: baseAmount,
    })
    if (seq !== couponSeq) return
    const loaded = res.code === 200 && Array.isArray(res.data)
    const coupons = loaded ? res.data : []
    // 只有真正拿到列表才记住「这个金额已加载过」，业务失败也要允许下次重试
    couponLoadedAmount = loaded ? baseAmount : null
    availableCoupons.value = coupons
    if (form.user_coupon_id_wsh && !coupons.some(item => Number(item.id_wsh) === Number(form.user_coupon_id_wsh))) {
      form.user_coupon_id_wsh = ''
    }
    if (!couponTouched.value && !form.user_coupon_id_wsh && coupons.length > 0) {
      // 后端已按门槛过滤并排序，首张即最优；前端不估算优惠金额
      const best = coupons[0]
      if (best) form.user_coupon_id_wsh = String(best.id_wsh)
    }
  } catch (e) {
    if (seq !== couponSeq) return
    availableCoupons.value = []
    form.user_coupon_id_wsh = ''
    couponLoadedAmount = null
  } finally {
    if (seq === couponSeq) couponLoading.value = false
  }
}

function couponText(coupon) {
  if (coupon.type_wsh === 'percent') {
    const rate = Number(coupon.discount_rate_wsh || 0)
    const max = Number(coupon.max_discount_amount_wsh || 0)
    return `${(rate * 10).toFixed(1)}折${max > 0 ? `，最高减￥${max.toFixed(2)}` : ''}`
  }
  const amount = Number(coupon.discount_amount_template_wsh || 0)
  return `满￥${Number(coupon.threshold_amount_wsh || 0).toFixed(2)}减￥${amount.toFixed(2)}`
}

function onCouponChange() {
  couponTouched.value = true
}

// 生命周期：打开弹窗时初始化

// 打开弹窗 → 重置表单 → 拉宠物/商家 → 拉服务详情 → 回填商家地址 → 拉券
watch(
  () => props.visible,
  async (val) => {
    if (!val) return
    nowTick.value = Date.now()
    resetForm()
    serviceId.value = props.initialServiceId
    serviceVersion.value = ''
    await Promise.all([loadPets(), loadMerchants()])
    if (form.merchant_id_wsh && !hasLoadedMerchant(form.merchant_id_wsh)) {
      form.merchant_id_wsh = ''
    }
    if (serviceId.value) {
      try {
        const r = await getServiceDetail(serviceId.value)
        if (r.code === 200 && r.data) {
          serviceName.value = r.data.name_wsh || serviceName.value
          serviceVersion.value = r.data.service_version_wsh || ''
          price.value = r.data.price_wsh ?? price.value
          serviceUnit.value = r.data.unit_wsh || ''
          serviceBookingMode.value = r.data.booking_mode_wsh || ''
          serviceDurationMinutes.value = r.data.duration_minutes_wsh || null
          const merchantId = r.data.merchant_id_wsh
          merchantNameFromService.value = r.data.merchant_name_wsh || ''

          // 商家列表未覆盖该商家时，单独拉一次详情补齐门店地址与坐标
          // （服务详情 VO 不含这些字段，缺坐标会导致无法下单）
          let merchant = merchants.value.find(m => Number(m.id_wsh) === Number(merchantId))
          if (!merchant && merchantId) {
            try {
              const res = await getMerchant(merchantId)
              if (res?.code === 200 && res.data) merchant = res.data
            } catch (e) {}
            if (!merchant) {
              merchant = { id_wsh: merchantId, name_wsh: merchantNameFromService.value, address_wsh: '' }
            }
            merchants.value = [...merchants.value, merchant]
          }
          if (merchant) {
            form.merchant_id_wsh = merchant.id_wsh
            fillDeliveryFromMerchant()
            await refreshCoupons()
          }
        }
      } catch (e) {
        const res = e?.response?.data
        const code = res?.errorCode || res?.error_code_wsh
        if (code === 'SERVICE_NOT_FOUND' || code === 'SERVICE_OFF_SHELF') {
          appStore.addToast('服务不存在或已下架，请选择其他服务', 'error')
          emit('close')
        }
      }
    }
  },
  { immediate: true }
)

// 商家变化 → 换券 + 重查可预约性
watch(
  () => form.merchant_id_wsh,
  async () => {
    refreshCoupons(true)
    await loadAvailability()
  }
)

// 输入变化 → 重新询价（金额只由后端算）
watch(
  () => [
    form.pet_id_wsh,
    form.delivery_date_wsh,
    form.delivery_slot_wsh,
    form.pickup_date_wsh,
    form.pickup_slot_wsh,
    slotQuantity.value,
    form.user_coupon_id_wsh,
    serviceId.value,
  ],
  () => {
    scheduleQuote()
  }
)

// 数据加载
async function loadPets() {
  const res = await getPets()
  if (res.code === 200) pets.value = res.data || []
}

async function loadMerchants() {
  merchantLoading.value = true
  try {
    const res = await getMerchants()
    if (res.code !== 200) {
      merchants.value = []
      return
    }
    merchants.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    merchants.value = []
  } finally {
    merchantLoading.value = false
  }
}

function hasLoadedMerchant(merchantId) {
  return merchants.value.some(item => Number(item.id_wsh) === Number(merchantId))
}

/**
 * 查可预约性。换服务/商家时清空日期槽位，但保留宠物选择。
 */
async function loadAvailability() {
  if (!serviceId.value) return
  const seq = ++availabilitySeq
  availabilityLoading.value = true
  availabilityError.value = false
  form.delivery_date_wsh = ''
  form.delivery_slot_wsh = ''
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
  try {
    const res = await getServiceAvailability(serviceId.value, minDate(), maxDate())
    if (seq !== availabilitySeq) return
    if (res.code === 200 && res.data?.days_wsh) {
      availabilityDays.value = res.data.days_wsh
      // 服务端权威预约窗口（含首尾）；0 或缺省保持请求上界
      const windowDays = Number(res.data.booking_window_days_wsh)
      if (windowDays > 0) maxBookingDays.value = windowDays
    } else {
      availabilityDays.value = []
      availabilityError.value = true
    }
  } catch (e) {
    if (seq !== availabilitySeq) return
    availabilityDays.value = []
    availabilityError.value = true
  } finally {
    if (seq === availabilitySeq) availabilityLoading.value = false
  }
}

// 表单重置与联动
function resetForm() {
  Object.assign(form, {
    pet_id_wsh: '',
    merchant_id_wsh: '',
    delivery_address_wsh: '',
    delivery_location_source_wsh: '',
    delivery_latitude_wsh: null,
    delivery_longitude_wsh: null,
    delivery_date_wsh: '',
    delivery_slot_wsh: '',
    pickup_date_wsh: '',
    pickup_slot_wsh: '',
    nights_wsh: 1,
    emergency_contact_name_wsh: '',
    emergency_contact_phone_wsh: '',
    user_coupon_id_wsh: '',
    remark_wsh: '',
  })
  serviceName.value = ''
  price.value = ''
  merchants.value = []
  availabilityDays.value = []
  availabilityError.value = false
  maxBookingDays.value = 0
  serviceUnit.value = ''
  serviceBookingMode.value = ''
  serviceDurationMinutes.value = null
  slotQuantity.value = 1
  merchantNameFromService.value = ''
  availableCoupons.value = []
  couponLoading.value = false
  couponTouched.value = false
  merchantLoading.value = false
  couponLoadedAmount = null
  couponSeq += 1
  quoteSeq += 1
  clearQuote()
}

function handleClose() {
  emit('close')
}

/** 送达日期变化：清空已选时间槽，按夜寄养同步离店日 */
function onDeliveryDateChange() {
  form.delivery_slot_wsh = ''
  if (!form.delivery_date_wsh) {
    form.pickup_date_wsh = ''
    form.pickup_slot_wsh = ''
    return
  }
  if (isNightUnit.value) {
    form.pickup_date_wsh = nightCheckoutDate.value
    return
  }
  // 接回日早于新的送达日时清空
  if (form.pickup_date_wsh && form.pickup_date_wsh < form.delivery_date_wsh) {
    form.pickup_date_wsh = ''
    form.pickup_slot_wsh = ''
  }
}

/** 送达时间变化：清空接回时间；按夜寄养只同步离店日 */
function onDeliverySlotChange() {
  if (isNightUnit.value) {
    form.pickup_date_wsh = nightCheckoutDate.value
    return
  }
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
}

// 提交校验

/**
 * 加载/错误态的阻断原因：按钮禁用与提交守卫共用。
 * 不含表单完整性 —— 表单不完整时仍允许点击，由 submitBlockReason 给出具体提示。
 */
const submitLoadingReason = computed(() => {
  if (submitting.value) return '正在提交中，请稍候'
  if (merchantLoading.value || availabilityLoading.value) return '接单信息正在加载，请稍后再提交'
  if (quoteState.loading) return '价格正在计算，请稍后再提交'
  if (quoteState.error) return quoteState.error
  if (unitDataError.value) return unitDataError.value
  return ''
})

/** 提交前的完整阻断原因（空字符串 = 可提交） */
const submitBlockReason = computed(() => {
  if (submitLoadingReason.value) return submitLoadingReason.value

  const isSlot = slotMode.value
  const missingText = isSlot
    ? '请补全宠物、商家和开始时间'
    : '请补全宠物、商家、送达时间和接回时间'

  if (!form.pet_id_wsh || !form.merchant_id_wsh) return missingText
  const deliveryTime = new Date(slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh))
  if (Number.isNaN(deliveryTime.getTime())) return missingText

  let pickupTime = null
  if (!isSlot) {
    pickupTime = new Date(slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh))
    if (Number.isNaN(pickupTime.getTime())) return missingText
  }

  if (!bookableDates.value.includes(form.delivery_date_wsh)) return '所选日期不可预约，请重新选择'
  if (!isSlot && !bookableDates.value.includes(form.pickup_date_wsh)) return '所选日期不可预约，请重新选择'
  if (!hasLoadedMerchant(form.merchant_id_wsh)) return '请选择可接单商家'
  if (!form.emergency_contact_name_wsh || !form.emergency_contact_phone_wsh) return '请填写紧急联系人和联系电话'
  if (deliveryTime.getTime() < nowTick.value) return '送达时间不能早于当前时间'
  if (!isSlot && pickupTime.getTime() <= deliveryTime.getTime()) return '接回时间必须晚于送达时间'
  return ''
})

// 提交
async function submitOrder() {
  if (submitting.value) return
  // 刷新一次当前时间，防止用户开着弹窗很久后才提交导致时间校验失效
  nowTick.value = Date.now()
  // 1. 资料完整度检查（资料不全去补资料）
  const profileOk = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!profileOk) return
  // 2. 业务校验
  const reason = submitBlockReason.value
  if (reason) {
    const isError = reason === quoteState.error || reason === unitDataError.value
    appStore.addToast(reason, isError ? 'error' : 'warning')
    return
  }
  // 3. 兜底填充送达地址（避免商家信息异步未就绪时提交空地址）
  if (!ensureDeliveryLocation()) return
  // 4. 提交；finally 保证 submitting 一定复位
  submitting.value = true
  try {
    await submitSingleOrder()
  } finally {
    submitting.value = false
  }
}

/** 单只模式：组装 payload 并创建订单 */
async function submitSingleOrder() {
  const isSlot = slotMode.value
  try {
    const timeFields = buildTimeFields()
    if (!timeFields) {
      appStore.addToast('请补全预约时间', 'warning')
      return
    }
    const payload = {
      ...buildSharedPayload(),
      pet_id_wsh: Number(form.pet_id_wsh),
      service_id_wsh: serviceId.value ? Number(serviceId.value) : null,
      ...timeFields,
    }
    if (isSlot) {
      payload.start_date_wsh = form.delivery_date_wsh
    } else {
      // ⚠️ 优先提交预览返回的规范化时间：预览与下单输入完全相同，金额才可能完全相同
      const quote = quoteState.single
      if (quote?.start_date_wsh) {
        Object.assign(payload, {
          quantity_wsh: quote.quantity_wsh,
          start_date_wsh: quote.start_date_wsh,
          end_date_wsh: quote.end_date_wsh,
          delivery_time_wsh: quote.delivery_time_wsh,
          pickup_time_wsh: quote.pickup_time_wsh,
        })
      }
    }
    const res = await createOrder(payload)
    finishCreate(res, singleCreatedData(res))
  } catch (e) {
    handleSubmitException(e)
  }
}

/** 从创建响应中取可 emit 的订单数据；空数据返回 null */
function singleCreatedData(res) {
  const data = res?.data
  return data && (data.id_wsh || data.order_no_wsh) ? data : null
}

/** 统一处理创建响应：成功 emit、空数据兜底关闭、失败走错误码映射 */
function finishCreate(res, createdData) {
  if (res?.code !== 200) {
    handleCreateError(res)
    return
  }
  if (!createdData) {
    appStore.addToast('订单已提交，请稍后在订单页查看', 'warning')
    emit('close')
    return
  }
  emit('created', createdData)
}

// 错误处理
const BOOKING_ERROR_MESSAGES = {
  FUTURE_BOOKING_DISABLED: '该商家当前未开放未来预约',
  FULFILLMENT_OUTSIDE_BUSINESS_HOURS: '送达或接回时间不在商家营业时段内',
  CAPACITY_EXCEEDED: '该时段看护人已预约满，请更换时段',
  MERCHANT_NOT_APPROVED: '商家未通过审核，暂不能预约',
  SERVICE_NOT_FOUND: '服务不存在或已下架，请刷新后重试',
  SERVICE_OFF_SHELF: '服务已下架，请选择其他服务',
  SERVICE_MERCHANT_MISMATCH: '服务与所选商家不一致，请刷新后重试',
  UNSUPPORTED_SERVICE_UNIT: '该服务暂不支持当前预约方式，请更换服务或时段',
  MERCHANT_REST_DAY: '商家当日休息，请更换日期',
}

function handleCreateError(res) {
  const { errorCode, error_code_wsh, message } = res || {}
  const code = errorCode || error_code_wsh
  if (code === 'PRICE_CHANGED') {
    appStore.addToast('服务价格或版本已更新，请重新确认后提交', 'error')
    reloadServiceState()
    return
  }
  const messageKey = code ? BOOKING_ERROR_MESSAGES[code] : null
  if (messageKey) {
    appStore.addToast(messageKey, 'error')
    return
  }
  appStore.addToast(message || '订单提交失败', 'error')
}

/** 网络异常：后端错误体交给错误码映射，其余给通用提示 */
function handleSubmitException(e) {
  const res = e?.response?.data
  if (res && typeof res === 'object') {
    handleCreateError(res)
    return
  }
  appStore.addToast('订单提交失败，请稍后重试', 'error')
}

/**
 * 服务价格/版本变化后：重拉服务详情 → 清空所有依赖（日期/槽位/券/报价） → 重查可约 → 重新询价。
 */
async function reloadServiceState() {
  if (serviceId.value) {
    try {
      const r = await getServiceDetail(serviceId.value)
      if (r.code === 200 && r.data) {
        serviceName.value = r.data.name_wsh || serviceName.value
        serviceVersion.value = r.data.service_version_wsh || ''
        price.value = r.data.price_wsh ?? price.value
        serviceUnit.value = r.data.unit_wsh || ''
        serviceBookingMode.value = r.data.booking_mode_wsh || ''
        serviceDurationMinutes.value = r.data.duration_minutes_wsh || null
      }
    } catch (e) {}
  }
  form.delivery_date_wsh = ''
  form.delivery_slot_wsh = ''
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
  form.nights_wsh = 1
  slotQuantity.value = 1
  form.user_coupon_id_wsh = ''
  availabilityDays.value = []
  availabilityError.value = false
  availableCoupons.value = []
  clearQuote()
  couponLoadedAmount = null
  await loadAvailability()
  scheduleQuote()
}

// 到店交接（地址固定取门店）
function hasDeliveryLocation() {
  const hasCoords = form.delivery_latitude_wsh != null && form.delivery_longitude_wsh != null
  return Boolean(form.delivery_address_wsh && hasCoords
    && isConfirmedDeliverySource(form.delivery_location_source_wsh))
}

/** 配送地址来源是否确认过（非空） */
function isConfirmedDeliverySource(source) {
  const text = String(source || '').trim()
  return text === 'merchant' || text.startsWith('amap')
}

/** 送达地址固定取门店地址与门店坐标，客户不可修改 */
function fillDeliveryFromMerchant() {
  const merchant = selectedMerchant.value
  if (!merchant) return false
  form.delivery_address_wsh = merchant.address_wsh || ''
  form.delivery_location_source_wsh = form.delivery_address_wsh ? 'merchant' : ''
  form.delivery_latitude_wsh = merchant.latitude_wsh ?? null
  form.delivery_longitude_wsh = merchant.longitude_wsh ?? null
  return hasDeliveryLocation()
}

/** 提交前确保已按所选商家填好地址与坐标 */
function ensureDeliveryLocation() {
  if (hasDeliveryLocation()) return true
  const filled = !form.delivery_address_wsh && fillDeliveryFromMerchant()
  if (!filled) {
    appStore.addToast('该门店未配置地址或位置坐标，暂不可下单', 'warning')
    return false
  }
  return true
}

// 展示辅助
const availabilityHint = computed(() => {
  if (availabilityLoading.value) return '正在查询可预约日期...'
  if (availabilityError.value) return '可预约日期查询失败，请刷新重试'
  // 负责人由系统分配，日期可约性只看商家营业时间与整体容量
  if (bookableDates.value.length === 0) {
    return maxBookingDays.value > 0 ? `近 ${maxBookingDays.value} 天暂无可预约日期` : '暂无可预约日期'
  }
  return `仅展示可预约日期（${bookableDates.value.length} 天可约）`
})

function slotPlaceholder(dateValue, options, emptyText, readyText) {
  if (!dateValue) return emptyText
  if (availabilityLoading.value) return '正在加载时间槽位...'
  if (options.length === 0) return '该日暂无可约时间'
  return readyText
}

const deliverySlotPlaceholder = computed(() =>
  slotPlaceholder(form.delivery_date_wsh, deliverySlotOptions.value, '请先选择送达日期', '请选择送达时间'))
const pickupSlotPlaceholder = computed(() =>
  slotPlaceholder(form.pickup_date_wsh, pickupSlotOptions.value, '请先选择接回日期', '请选择接回时间'))

// 日期/时间工具
function formatDateInput(d) {
  const pad = value => String(value).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function minDate() {
  return formatDateInput(new Date(nowTick.value))
}

function maxDate() {
  const d = new Date(nowTick.value)
  // 以后端 booking_window_days_wsh 为准（含首尾，偏移 = 窗口-1）；未查询前用请求上界兜底
  const offset = maxBookingDays.value > 0 ? maxBookingDays.value - 1 : AVAILABILITY_REQUEST_WINDOW_DAYS
  d.setDate(d.getDate() + offset)
  return formatDateInput(d)
}

/** 只展示时间部分 HH:mm */
function displaySlot(slot) {
  return slot ? String(slot).slice(11, 16) : ''
}

/** 日期 + 时间槽 → 本地 ISO 串（不带秒） */
function slotToDateTime(dateValue, slotValue) {
  return slotValue ? `${dateValue}T${slotValue.slice(11, 16)}` : ''
}

/** 本地 ISO 串 → 带秒的 API 时间格式 */
function toApiDateTime(value) {
  return value ? `${value}:00` : null
}

/**
 * 服务日期区间：end_date 是「最后一个服务日」。
 * 数量与金额一律以服务端报价为准，前端不按天数推算。
 */
function buildServiceDates(deliveryTime, pickupTime) {
  return {
    startDate: formatDateInput(deliveryTime),
    endDate: formatDateInput(pickupTime),
  }
}
</script>

<style scoped>
.order-modal__header { text-align: center; margin-bottom: 4px; }
.order-modal__header h2 { margin-bottom: 6px; font-size: 22px; }
.order-modal__subtitle { color: var(--color-muted-foreground); font-size: 13px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 16px 18px; margin-top: 20px; }
.form-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); }
.form-grid__wide { grid-column: 1 / -1; }

/* 分步标题：让流程一目了然，先选看护人再选时间 */
.form-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-foreground);
}
.form-section-title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
  margin-left: 4px;
}
.form-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-on-primary);
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.field-hint { min-height: 18px; color: var(--color-muted-foreground); font-size: 12px; line-height: 1.5; }
.estimate {
  margin-top: 20px;
  padding: 14px 18px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.1), rgba(251, 146, 60, 0.04));
  border: 1px solid rgba(249, 115, 22, 0.28);
  font-weight: 700;
  color: var(--color-primary);
}
.estimate-note { color: var(--color-muted-foreground); font-size: 13px; font-weight: 400; margin-top: 4px; }

.service-info-card {
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--color-muted);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);
}

.service-info-row { display: flex; align-items: baseline; gap: 12px; font-size: 14px; line-height: 1.8; }
.service-info-label { color: var(--color-muted-foreground); min-width: 40px; font-size: 13px; }
.service-info-value { color: var(--color-foreground); font-weight: 600; }

.readonly-field {
  display: grid;
  align-content: start;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.readonly-label { font-size: 13px; }
/* 高度与 .form-control 对齐（全局 input/select 控件高度） */
.readonly-value-row { display: flex; align-items: center; gap: 8px; min-height: var(--control-height); }
.readonly-value { font-size: 14px; color: var(--color-foreground); font-weight: 500; }
.readonly-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-inline);
  white-space: nowrap;
}
</style>
