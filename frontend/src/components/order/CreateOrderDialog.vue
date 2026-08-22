<template>
  <div v-if="visible" class="modal-overlay" @mousedown.self="handleClose">
    <div class="modal order-modal">
      <div class="order-modal__header">
        <h2>创建订单</h2>
        <p class="order-modal__subtitle">先选好宠物与看护人，再安排送达、接回时间</p>
      </div>

      <!-- 服务信息卡片 -->
      <div v-if="serviceId" class="service-info-card">
        <div class="service-info-row">
          <span class="service-info-label">服务</span>
          <span class="service-info-value">{{ serviceName || '加载中...' }}</span>
        </div>
        <div class="service-info-row">
          <span class="service-info-label">计价</span>
          <span class="service-info-value">￥{{ price || '--' }} / {{ unitLabel }}</span>
        </div>
      </div>

      <div class="form-grid">
        <!-- ============ 第一步：宠物与看护人 ============ -->
        <div class="form-section-title form-grid__wide">
          <span class="form-step">1</span>
          <span>宠物与看护人</span>
        </div>

        <!-- 批量模式：看护人优先选择（解锁后续日期），宠物在每行内选择 -->
        <label v-if="isBatchMode">
          看护人
          <select
            v-model="form.keeper_id_wsh"
            class="form-control"
            :disabled="!form.merchant_id_wsh || keeperLoading || keepers.length === 0"
          >
            <option value="">{{ keeperSelectPlaceholder }}</option>
            <option v-for="keeper in keepers" :key="keeper.id_wsh" :value="keeper.id_wsh">
              {{ keeperOptionLabel(keeper) }}
            </option>
          </select>
          <span class="field-hint">{{ keeperHint }}</span>
        </label>
        <!-- 单宠物模式：宠物 -->
        <label v-if="!isBatchMode">
          宠物
          <select v-model="form.pet_id_wsh" class="form-control">
            <option value="">请选择宠物</option>
            <option v-for="pet in pets" :key="pet.id_wsh" :value="pet.id_wsh">
              {{ pet.name_wsh }} {{ pet.breed_wsh ? `(${pet.breed_wsh})` : '' }}
            </option>
          </select>
        </label>

        <!-- 服务模式：商家只读展示（批量与单笔均展示） -->
        <div v-if="serviceId" class="readonly-field">
          <span class="readonly-label">商家</span>
          <div class="readonly-value-row">
            <span class="readonly-value">{{ merchantDisplayName }}</span>
            <span class="readonly-badge">由当前服务指定</span>
          </div>
        </div>
        <!-- 非服务模式：商家可选 -->
        <label v-else>
          商家
          <select
            v-model="form.merchant_id_wsh"
            class="form-control"
            :disabled="merchantLoading || merchants.length === 0"
            @change="onMerchantChange"
          >
            <option value="">{{ merchantSelectPlaceholder }}</option>
            <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">
              {{ merchant.name_wsh }}
            </option>
          </select>
          <span class="field-hint">{{ merchantHint }}</span>
        </label>

        <!-- 单宠物模式：看护人 -->
        <label v-if="!isBatchMode">
          看护人
          <select
            v-model="form.keeper_id_wsh"
            class="form-control"
            :disabled="!form.merchant_id_wsh || keeperLoading || keepers.length === 0"
          >
            <option value="">{{ keeperSelectPlaceholder }}</option>
            <option v-for="keeper in keepers" :key="keeper.id_wsh" :value="keeper.id_wsh">
              {{ keeperOptionLabel(keeper) }}
            </option>
          </select>
          <span class="field-hint">{{ keeperHint }}</span>
        </label>

        <!-- ============ 第二步：预约时间 ============ -->
        <div class="form-section-title form-grid__wide">
          <span class="form-step">2</span>
          <span>预约时间</span>
        </div>

        <!-- 批量模式：每只宠物一行，含送达/接回日期与时间 -->
        <template v-if="isBatchMode">
          <div v-for="(row, index) in petRows" :key="index" class="pet-row form-grid__wide">
            <div class="pet-row__title">
              <span>宠物 {{ index + 1 }}</span>
              <button v-if="petRows.length > 1" type="button" class="btn btn-outline btn-sm" @click.prevent="removePetRow(index)">移除</button>
            </div>
            <label>
              宠物
              <select v-model="row.pet_id_wsh" class="form-control">
                <option value="">请选择宠物</option>
                <option v-for="pet in pets" :key="pet.id_wsh" :value="pet.id_wsh">
                  {{ pet.name_wsh }} {{ pet.breed_wsh ? `(${pet.breed_wsh})` : '' }}
                </option>
              </select>
            </label>
            <label>
              送达日期
              <input v-model="row.delivery_date_wsh" type="date" class="form-control" :min="minDate()" :max="maxDate()" :disabled="availabilityLoading || bookableDates.length === 0" @change="onRowDeliveryDateChange(row)">
            </label>
            <label>
              送达时间
              <select v-model="row.delivery_slot_wsh" class="form-control" :disabled="!row.delivery_date_wsh || rowSlots(row.delivery_date_wsh).length === 0" @change="onRowDeliverySlotChange(row)">
                <option value="">{{ rowSlotPlaceholder(row.delivery_date_wsh) }}</option>
                <option v-for="slot in rowSlots(row.delivery_date_wsh)" :key="slot" :value="slot">{{ displaySlot(slot) }}</option>
              </select>
            </label>
            <label>
              接回日期
              <input v-model="row.pickup_date_wsh" type="date" class="form-control" :min="row.delivery_date_wsh || minDate()" :max="maxDate()" :disabled="!row.delivery_date_wsh || availabilityLoading || bookableDates.length === 0" @change="onRowPickupDateChange(row)">
            </label>
            <label>
              接回时间
              <select v-model="row.pickup_slot_wsh" class="form-control" :disabled="!row.pickup_date_wsh || rowPickupSlotOptions(row).length === 0">
                <option value="">{{ rowSlotPlaceholder(row.pickup_date_wsh) }}</option>
                <option v-for="slot in rowPickupSlotOptions(row)" :key="slot" :value="slot">{{ displaySlot(slot) }}</option>
              </select>
            </label>
          </div>
          <div class="batch-actions form-grid__wide">
            <button type="button" class="btn btn-outline btn-sm" :disabled="petRows.length >= BATCH_MAX_PETS" @click.prevent="addPetRow">
              {{ petRows.length >= BATCH_MAX_PETS ? `最多可添加 ${BATCH_MAX_PETS} 只宠物` : '+ 添加宠物' }}
            </button>
            <span class="field-hint">{{ availabilityHint }}</span>
          </div>
        </template>
        <!-- 单宠物模式：送达/接回日期与时间 -->
        <template v-else-if="availabilityEnabled">
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
          <label v-if="isHourUnit">
            服务数量（小时）
            <input data-testid="quantity-input" v-model.number="slotQuantity" type="number" class="form-control" min="1" max="24">
            <span class="field-hint">连续服务时长 = 数量 × 60 分钟</span>
          </label>
          <label v-if="slotMode && !isHourUnit">
            服务数量
            <input class="form-control" :value="1" disabled>
            <span class="field-hint">每次服务一个连续时段</span>
          </label>
          <label v-if="!slotMode">
            接回日期
            <input v-model="form.pickup_date_wsh" type="date" class="form-control" :min="form.delivery_date_wsh || minDate()" :max="maxDate()" :disabled="!form.delivery_date_wsh || availabilityLoading || bookableDates.length === 0" @change="onPickupDateChange">
            <span class="field-hint">接回日期不早于送达日期</span>
          </label>
          <label v-if="!slotMode">
            接回时间
            <select v-model="form.pickup_slot_wsh" class="form-control" :disabled="!form.pickup_date_wsh || pickupSlotOptions.length === 0">
              <option value="">{{ pickupSlotPlaceholder }}</option>
              <option v-for="slot in pickupSlotOptions" :key="slot" :value="slot">{{ displaySlot(slot) }}</option>
            </select>
          </label>
        </template>
        <template v-else>
          <label>
            送达时间
            <input v-model="form.delivery_time_wsh" type="datetime-local" class="form-control" :min="minDateTime">
          </label>
          <label>
            接回时间
            <input v-model="form.pickup_time_wsh" type="datetime-local" class="form-control" :min="pickupMinDateTime">
          </label>
        </template>

        <!-- ============ 第三步：宠物送达地址 ============ -->
        <div class="form-section-title form-grid__wide">
          <span class="form-step">3</span>
          <span>宠物送达地址</span>
        </div>
        <label class="form-grid__wide">
          <div class="field-title-row">
            <span>宠物送达地址</span>
            <button class="btn btn-outline btn-sm" type="button" :disabled="!form.merchant_id_wsh" @click.prevent="fillDeliveryFromMerchant(true)">
              商家位置
            </button>
          </div>
          <AmapAddressPicker
            v-model="form.delivery_address_wsh"
            v-model:source="form.delivery_location_source_wsh"
            v-model:latitude="form.delivery_latitude_wsh"
            v-model:longitude="form.delivery_longitude_wsh"
            :show-locate-button="false"
            placeholder="搜索地点，或点击地图选址"
          />
          <div v-if="deliveryDistanceVisible" class="delivery-distance" :class="{ 'delivery-distance--loading': distanceLoading, 'delivery-distance--error': distanceError }">
            <el-icon v-if="!distanceError" class="delivery-distance__icon"><Location /></el-icon>
            <span class="delivery-distance__text">{{ deliveryDistanceText }}</span>
            <button v-if="distanceError" type="button" class="btn btn-outline btn-sm" @click.prevent="retryDeliveryDistance">重试定位</button>
          </div>
        </label>

        <!-- ============ 第四步：联系信息与备注 ============ -->
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
      <div class="estimate" v-if="estimatedAmount.total">
        预计金额：￥{{ estimatedAmount.total }}
        <span v-if="Number(estimatedAmount.discount) > 0" style="color:var(--color-danger);font-weight:400;font-size:13px;margin-left:8px">优惠 ￥{{ estimatedAmount.discount }}</span>
        <span v-if="Number(estimatedAmount.couponDiscount) > 0" style="color:var(--color-danger);font-weight:400;font-size:13px;margin-left:8px">券减 ￥{{ estimatedAmount.couponDiscount }}</span>
        <span v-if="Number(estimatedAmount.membershipDiscount) > 0" style="color:var(--color-danger);font-weight:400;font-size:13px;margin-left:8px">会员减 ￥{{ estimatedAmount.membershipDiscount }}</span>
        <div style="font-weight:400;font-size:14px;margin-top:4px">实付：￥{{ estimatedAmount.final }}</div>
        <div v-if="Number(estimatedAmount.platformSubsidy) > 0" class="estimate-note">
          优惠券由平台补贴，商家结算不受影响
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn btn-secondary btn-sm" type="button" @click="handleClose">取消</button>
        <button class="btn btn-primary btn-sm" type="button" :disabled="submitting || merchantLoading || keeperLoading" @click="submitOrder">
          {{ submitting ? '提交中...' : '确认下单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElIcon } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import AmapAddressPicker from '@/components/common/AmapAddressPicker.vue'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { haversineMeters, formatDistanceMeters } from '@/utils/geo'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { getPets } from '@/api/pet'
import { getMerchants } from '@/api/merchant'
import { getKeepersByMerchant } from '@/api/keeper'
import { getServiceDetail, getServiceAvailability } from '@/api/service'
import { createOrder, createOrdersBatch } from '@/api/order'
import { getAvailableCoupons, quoteCoupon } from '@/api/coupon'
import { quoteMembershipOrderDiscount } from '@/api/membership'
import { normalizeUnit } from '@/domain/BookingUnit'
import { AVAILABILITY_REQUEST_WINDOW_DAYS, BATCH_MAX_PETS } from '@/domain/BookingWindow'

// 创建订单参数对象
const props = defineProps({
  visible: Boolean,
  initialServiceName: { type: String, default: '' },
  initialServiceId: { type: String, default: '' },
  initialPrice: { type: String, default: '' },
  initialMerchantId: { type: String, default: '' },
  initialKeeperId: { type: String, default: '' },
})

// 订单状态
const emit = defineEmits(['close', 'created'])

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const submitting = ref(false)
const merchantLoading = ref(false)
const keeperLoading = ref(false)
const pets = ref([])
const merchants = ref([])
const keepers = ref([])
const orderableKeepersByMerchantId = ref({})
const serviceName = ref('')
const serviceId = ref('')
const serviceVersion = ref('')
const price = ref('')
const nowTick = ref(Date.now())
const availableCoupons = ref([])
const couponQuote = ref(null)
const couponLoading = ref(false)
// 用户是否手动操作过优惠券：true 后不再自动覆盖选择
const couponTouched = ref(false)
let couponQuoteSeq = 0
const membershipQuote = ref(null)
const membershipLoading = ref(false)
let membershipQuoteSeq = 0

const availabilityLoading = ref(false)
// 送达地址到用户当前位置的距离（米）；null = 未计算
const deliveryDistanceMeters = ref(null)
const distanceLoading = ref(false)
const distanceError = ref('')
let userLocation = null
const availabilityError = ref(false)
const availabilityDays = ref([])
let availabilitySeq = 0
// 服务端返回的预约窗口天数（含首尾，booking_window_days_wsh）；0 = 未知（尚未查询）
const maxBookingDays = ref(0)
const serviceUnit = ref('')
const serviceBookingMode = ref('')
const serviceDurationMinutes = ref(null)
const slotQuantity = ref(1)
const merchantNameFromService = ref('')
// 批量下单行（仅 day 单位服务）：每行一只宠物 + 各自日期区间
const petRows = reactive([createPetRow()])
function createPetRow() {
  return {
    pet_id_wsh: '',
    delivery_date_wsh: '',
    delivery_slot_wsh: '',
    pickup_date_wsh: '',
    pickup_slot_wsh: '',
  }
}

const form = reactive({
  pet_id_wsh: '',
  merchant_id_wsh: '',
  keeper_id_wsh: '',
  delivery_address_wsh: '',
  delivery_location_source_wsh: '',
  delivery_latitude_wsh: null,
  delivery_longitude_wsh: null,
  delivery_time_wsh: '',
  pickup_time_wsh: '',
  delivery_date_wsh: '',
  delivery_slot_wsh: '',
  pickup_date_wsh: '',
  pickup_slot_wsh: '',
  emergency_contact_name_wsh: '',
  emergency_contact_phone_wsh: '',
  user_coupon_id_wsh: '',
  remark_wsh: '',
})

const minDateTime = computed(() => toLocalDateTimeInput(new Date(nowTick.value)))
const pickupMinDateTime = computed(() => {
  if (!form.delivery_time_wsh) return minDateTime.value
  return toLocalDateTimeInput(new Date(new Date(form.delivery_time_wsh).getTime() + 600000))
})

const availabilityEnabled = computed(() => Boolean(serviceId.value))
const deliveryDistanceVisible = computed(() => {
  const lat = Number(form.delivery_latitude_wsh)
  const lng = Number(form.delivery_longitude_wsh)
  return Number.isFinite(lat) && Number.isFinite(lng)
})
const deliveryDistanceText = computed(() => {
  if (distanceLoading.value) return '正在定位您的位置，计算送达距离...'
  if (distanceError.value) return `无法获取您的位置：${distanceError.value}`
  if (deliveryDistanceMeters.value != null) return `距您约 ${formatDistanceMeters(deliveryDistanceMeters.value)}`
  return '正在获取您的位置...'
})
// 批量模式：服务驱动 + 按天计费（date_range）；session/hour 保持单笔
const isBatchMode = computed(() => availabilityEnabled.value && !slotMode.value)
const normalizedUnit = computed(() => normalizeUnit(serviceUnit.value) || 'day')
const merchantDisplayName = computed(() => {
  const m = selectedMerchant.value
  if (m) return m.name_wsh
  return merchantNameFromService.value || '加载中...'
})
const unitLabel = computed(() => {
  const unit = normalizedUnit.value
  if (unit === 'hour') return '小时'
  if (unit === 'session') return '次'
  return '天'
})
const slotMode = computed(() => {
  if (!availabilityEnabled.value) return false
  const mode = serviceBookingMode.value
  if (mode) return mode === 'slot'
  const unit = normalizeUnit(serviceUnit.value)
  return unit === 'session' || unit === 'hour'
})
const isHourUnit = computed(() => normalizedUnit.value === 'hour')
const bookableDates = computed(() =>
  availabilityDays.value
    .filter(day => day.bookable_wsh)
    .map(day => String(day.date_wsh)))
const deliverySlotOptions = computed(() => {
  if (!form.delivery_date_wsh) return []
  const day = availabilityDays.value.find(item => String(item.date_wsh) === form.delivery_date_wsh)
  if (!day || !day.bookable_wsh) return []
  return day.windows_wsh.flatMap(window => window.slots_wsh || [])
})
const pickupSlotOptions = computed(() => {
  if (!form.pickup_date_wsh) return []
  const day = availabilityDays.value.find(item => String(item.date_wsh) === form.pickup_date_wsh)
  if (!day || !day.bookable_wsh) return []
  const slots = day.windows_wsh.flatMap(window => window.slots_wsh || [])
  // 接回时间必须晚于送达时间；跨天接回不在此过滤（日期本身已保证顺序）
  if (String(form.pickup_date_wsh) === String(form.delivery_date_wsh) && form.delivery_slot_wsh) {
    return slots.filter(slot => slot > form.delivery_slot_wsh)
  }
  return slots
})

const estimatedAmount = computed(() => {
  if (isBatchMode.value) return batchEstimatedAmount()
  const estimate = baseEstimate()
  const quote = couponQuote.value
  let current = estimate
  if (quote && Number(quote.total_amount_wsh || 0).toFixed(2) === estimate.total) {
    const couponDiscount = Number(quote.coupon_discount_wsh || 0)
    const longStayDiscount = Number(quote.long_stay_discount_wsh || estimate.discount)
    current = {
      total: estimate.total,
      discount: longStayDiscount.toFixed(2),
      couponDiscount: couponDiscount.toFixed(2),
      membershipDiscount: '0.00',
      platformSubsidy: Number(quote.platform_subsidy_wsh || 0).toFixed(2),
      final: Number(quote.final_amount_wsh || 0).toFixed(2),
    }
  }
  const member = membershipQuote.value
  if (member && Number(member.base_amount_wsh || 0).toFixed(2) === current.final) {
    const memberDiscount = Number(member.membership_discount_wsh || 0)
    return {
      ...current,
      membershipDiscount: memberDiscount.toFixed(2),
      platformSubsidy: (Number(current.platformSubsidy || 0) + memberDiscount).toFixed(2),
      final: Number(member.final_amount_wsh || 0).toFixed(2),
    }
  }
  return current
})

function fmtAmount(value) {
  return Math.max(0, Number(value || 0)).toFixed(2)
}

function sumAmounts(rows, key) {
  return rows.reduce((sum, item) => sum + Number(item[key] || 0), 0)
}

/** 单只宠物按天计费的估算（含长住折扣），供单笔与批量行共用 */
function dayEstimate(deliveryTime, pickupTime, dayPrice) {
  const ZERO = { total: '0.00', discount: '0.00', couponDiscount: '0.00', membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00' }
  if (!deliveryTime || !pickupTime) return ZERO
  const start = new Date(deliveryTime)
  const end = new Date(pickupTime)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) return ZERO
  const serviceDates = buildServiceDates(start, end)
  const days = Math.max(0, diffDays(serviceDates.startDate, serviceDates.endDate))
  const total = days * Number(dayPrice || 0)
  let discount = 0
  if (days >= 30) discount = total * 0.1
  else if (days >= 7) discount = total * 0.05
  return {
    total: total.toFixed(2),
    discount: discount.toFixed(2),
    couponDiscount: '0.00',
    membershipDiscount: '0.00',
    platformSubsidy: '0.00',
    final: Math.max(0, total - discount).toFixed(2),
  }
}

function baseEstimate() {
  if (isBatchMode.value) return batchBaseEstimate()
  const ZERO = { total: '0.00', discount: '0.00', couponDiscount: '0.00', membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00' }
  if (slotMode.value) {
    const startDt = slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
    if (!startDt) return ZERO
    const unitPrice = Number(price.value || 0)
    const unit = normalizedUnit.value
    const quantity = unit === 'hour' ? Math.max(1, Number(slotQuantity.value) || 1) : 1
    const total = quantity * unitPrice
    return {
      total: total.toFixed(2),
      discount: '0.00',
      couponDiscount: '0.00',
      membershipDiscount: '0.00',
      platformSubsidy: '0.00',
      final: total.toFixed(2),
    }
  }
  const deliveryTime = availabilityEnabled.value
    ? slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
    : form.delivery_time_wsh
  const pickupTime = availabilityEnabled.value
    ? slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)
    : form.pickup_time_wsh
  const keeper = keepers.value.find(item => Number(item.id_wsh) === Number(form.keeper_id_wsh))
  const dayPrice = availabilityEnabled.value
    ? Number(price.value || 0)
    : Number(keeper?.price_per_day_wsh || price.value || 0)
  return dayEstimate(deliveryTime, pickupTime, dayPrice)
}

function rowEstimate(row) {
  return dayEstimate(
    slotToDateTime(row.delivery_date_wsh, row.delivery_slot_wsh),
    slotToDateTime(row.pickup_date_wsh, row.pickup_slot_wsh),
    Number(price.value || 0),
  )
}

function batchBase() {
  return petRows.map(rowEstimate).filter(item => Number(item.total) > 0)
}

function batchBaseEstimate() {
  const rows = batchBase()
  const total = sumAmounts(rows, 'total')
  const discount = sumAmounts(rows, 'discount')
  return {
    total: fmtAmount(total),
    discount: fmtAmount(discount),
    couponDiscount: '0.00',
    membershipDiscount: '0.00',
    platformSubsidy: '0.00',
    final: fmtAmount(total - discount),
  }
}

/** 优惠券报价基准：批量 = 金额最大的单（与后端券归属规则一致）；单笔 = 整单 */
function couponQuoteBase() {
  if (isBatchMode.value) {
    const rows = batchBase()
    if (rows.length === 0) return { total: 0, discount: 0 }
    const largest = rows.reduce((a, b) => (Number(a.total) >= Number(b.total) ? a : b))
    return { total: Number(largest.total), discount: Number(largest.discount) }
  }
  return { total: Number(baseEstimate().total), discount: Number(baseEstimate().discount) }
}

function batchEstimatedAmount() {
  const estimate = batchBaseEstimate()
  const total = Number(estimate.total)
  const discount = Number(estimate.discount)
  const base = couponQuoteBase()
  let couponDiscount = 0
  const quote = couponQuote.value
  if (quote && Number(quote.total_amount_wsh || 0).toFixed(2) === base.total.toFixed(2)) {
    couponDiscount = Number(quote.coupon_discount_wsh || 0)
  }
  const afterCoupon = Math.max(0, total - discount - couponDiscount)
  let memberDiscount = 0
  const member = membershipQuote.value
  if (member && Number(member.base_amount_wsh || 0).toFixed(2) === afterCoupon.toFixed(2)) {
    memberDiscount = Number(member.membership_discount_wsh || 0)
  }
  return {
    total: fmtAmount(total),
    discount: fmtAmount(discount),
    couponDiscount: fmtAmount(couponDiscount),
    membershipDiscount: fmtAmount(memberDiscount),
    platformSubsidy: fmtAmount(Number(quote?.platform_subsidy_wsh || 0) + memberDiscount),
    final: fmtAmount(afterCoupon - memberDiscount),
  }
}

const selectedMerchant = computed(() =>
  merchants.value.find(item => Number(item.id_wsh) === Number(form.merchant_id_wsh)))
const merchantSelectPlaceholder = computed(() => {
  if (merchantLoading.value) return '正在筛选可接单商家...'
  if (merchants.value.length === 0) return '暂无可接单商家'
  return '请选择商家'
})
const keeperSelectPlaceholder = computed(() => {
  if (!form.merchant_id_wsh) return '请先选择商家'
  if (keeperLoading.value) return '正在加载看护人...'
  if (keepers.value.length === 0) return '暂无可接单看护人'
  return '请选择看护人'
})
const keeperOptionLabel = (keeper) => {
  // 服务驱动下单：展示服务价格口径（price/次|小时|天），避免与看护人个人价混淆
  const inServiceMode = availabilityEnabled.value
  const unitPrice = inServiceMode ? Number(price.value || 0) : Number(keeper?.price_per_day_wsh || 0)
  const unitSuffix = inServiceMode ? `/${unitLabel.value}` : '/天'
  const priceText = unitPrice > 0 ? ` ￥${unitPrice}${unitSuffix}` : ''
  const capacity = `当前 ${keeper.current_pets_wsh || 0}/${keeper.max_pets_wsh || '-'}`
  return `${keeper.name_wsh}${priceText} · ${capacity}`
}
const merchantHint = computed(() => {
  if (merchantLoading.value) return '正在校验商家是否有资质通过的在职看护人'
  if (merchants.value.length === 0) return '当前没有满足接单条件的商家，请稍后再试'
  return '仅展示有合格看护人的商家'
})
const keeperHint = computed(() => {
  if (!form.merchant_id_wsh) return '选择商家后再选择看护人'
  if (keeperLoading.value) return '正在同步该商家的可接单看护人'
  if (keepers.value.length === 0) return '该商家暂无可接单看护人'
  return '仅展示资质通过且在职的看护人（休息中也接受未来预约）'
})

watch(
  () => props.visible,
  async (val) => {
    if (!val) return
    nowTick.value = Date.now()
    resetForm()
    serviceName.value = props.initialServiceName
    serviceId.value = props.initialServiceId
    serviceVersion.value = ''
    price.value = props.initialPrice
    form.merchant_id_wsh = props.initialMerchantId || ''
    form.keeper_id_wsh = props.initialKeeperId || ''
    await Promise.all([loadPets(), loadMerchants()])
    if (form.merchant_id_wsh && !hasLoadedMerchant(form.merchant_id_wsh)) {
      form.merchant_id_wsh = ''
      form.keeper_id_wsh = ''
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
          // 即使商家不在过滤后列表中（无可接单看护人被过滤），也直接使用该商家
          let merchant = merchants.value.find(m => Number(m.id_wsh) === Number(merchantId))
          if (!merchant && merchantId) {
            merchant = { id_wsh: merchantId, name_wsh: merchantNameFromService.value, address_wsh: r.data.merchant_address_wsh || '' }
            merchants.value = [...merchants.value, merchant]
          }
          if (merchant) {
            form.merchant_id_wsh = merchant.id_wsh
            fillDeliveryFromMerchant()
            // 强制重新加载看护人（绕过缓存，因为 loadMerchants 可能缓存了空数组）
            await loadKeepers(merchant.id_wsh, true)
            if (props.initialKeeperId) {
              const exists = keepers.value.some(item => Number(item.id_wsh) === Number(props.initialKeeperId))
              form.keeper_id_wsh = exists ? props.initialKeeperId : ''
            }
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
    } else if (form.merchant_id_wsh) {
      fillDeliveryFromMerchant()
      await loadKeepers(form.merchant_id_wsh)
      if (props.initialKeeperId) {
        const exists = keepers.value.some(item => Number(item.id_wsh) === Number(props.initialKeeperId))
        form.keeper_id_wsh = exists ? props.initialKeeperId : ''
      }
      await refreshCoupons()
    }
  },
  { immediate: true }
)

watch(
  () => [
    form.merchant_id_wsh,
    form.keeper_id_wsh,
    keepers.value.length,
  ],
  async () => {
    refreshCoupons()
    if (availabilityEnabled.value && form.keeper_id_wsh) {
      await loadAvailability()
    }
  }
)

watch(
  () => [
    form.delivery_time_wsh,
    form.pickup_time_wsh,
    form.delivery_date_wsh,
    form.delivery_slot_wsh,
    form.pickup_date_wsh,
    form.pickup_slot_wsh,
    slotQuantity.value,
    form.user_coupon_id_wsh,
  ],
  () => {
    refreshCoupons()
  }
)

// 批量行变化（宠物/日期/槽位）也触发报价刷新
watch(
  () => petRows.map(row =>
    [row.pet_id_wsh, row.delivery_date_wsh, row.delivery_slot_wsh, row.pickup_date_wsh, row.pickup_slot_wsh].join('|')).join(';'),
  () => {
    refreshCoupons()
  }
)

// 送达地址坐标变化（搜索选中/地图选址/商家位置）时计算距离
watch(
  () => [form.delivery_latitude_wsh, form.delivery_longitude_wsh],
  () => {
    updateDeliveryDistance()
  }
)

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
    const list = Array.isArray(res.data) ? res.data : []
    const entries = await Promise.all(list.map(async merchant => {
      const orderableKeepers = await fetchOrderableKeepers(merchant.id_wsh)
      return { merchant, orderableKeepers }
    }))
    merchants.value = entries
      .filter(entry => entry.orderableKeepers.length > 0)
      .map(entry => entry.merchant)
  } catch (e) {
    merchants.value = []
  } finally {
    merchantLoading.value = false
  }
}

async function loadKeepers(merchantId, forceRefresh = false) {
  if (!merchantId) {
    keepers.value = []
    return []
  }
  keeperLoading.value = true
  const cacheKey = merchantKey(merchantId)
  const cached = orderableKeepersByMerchantId.value[cacheKey]
  try {
    if (cached && !forceRefresh) {
      keepers.value = cached
      return cached
    }
    const orderableKeepers = await fetchOrderableKeepers(merchantId)
    keepers.value = orderableKeepers
    return orderableKeepers
  } finally {
    keeperLoading.value = false
  }
}

async function fetchOrderableKeepers(merchantId) {
  if (!merchantId) return []
  try {
    const res = await getKeepersByMerchant(merchantId)
    const list = res.code === 200 && Array.isArray(res.data) ? res.data : []
    const orderableKeepers = list.filter(isOrderableKeeper)
    orderableKeepersByMerchantId.value = {
      ...orderableKeepersByMerchantId.value,
      [merchantKey(merchantId)]: orderableKeepers,
    }
    return orderableKeepers
  } catch (e) {
    orderableKeepersByMerchantId.value = {
      ...orderableKeepersByMerchantId.value,
      [merchantKey(merchantId)]: [],
    }
    return []
  }
}

function isOrderableKeeper(keeper) {
  const status = Number(keeper?.status_wsh)
  const inService = status === 1 || status === 3 || status === 4
  return inService
    && Array.isArray(keeper.qualifications_wsh)
    && keeper.qualifications_wsh.some(item => item?.status_wsh === 'approved')
}

function merchantKey(merchantId) {
  return String(merchantId)
}

function hasLoadedMerchant(merchantId) {
  return merchants.value.some(item => Number(item.id_wsh) === Number(merchantId))
}

function hasLoadedKeeper(keeperId) {
  return keepers.value.some(item => Number(item.id_wsh) === Number(keeperId))
}

function resetForm() {
  Object.assign(form, {
    pet_id_wsh: '',
    merchant_id_wsh: '',
    keeper_id_wsh: '',
    delivery_address_wsh: '',
    delivery_location_source_wsh: '',
    delivery_latitude_wsh: null,
    delivery_longitude_wsh: null,
    delivery_time_wsh: '',
    pickup_time_wsh: '',
    delivery_date_wsh: '',
    delivery_slot_wsh: '',
    pickup_date_wsh: '',
    pickup_slot_wsh: '',
    emergency_contact_name_wsh: '',
    emergency_contact_phone_wsh: '',
    user_coupon_id_wsh: '',
    remark_wsh: '',
  })
  deliveryDistanceMeters.value = null
  distanceLoading.value = false
  distanceError.value = ''
  userLocation = null
  merchants.value = []
  keepers.value = []
  orderableKeepersByMerchantId.value = {}
  availabilityDays.value = []
  availabilityError.value = false
  maxBookingDays.value = 0
  petRows.splice(0, petRows.length, createPetRow())
  serviceUnit.value = ''
  serviceBookingMode.value = ''
  serviceDurationMinutes.value = null
  slotQuantity.value = 1
  merchantNameFromService.value = ''
  availableCoupons.value = []
  couponQuote.value = null
  membershipQuote.value = null
  couponLoading.value = false
  membershipLoading.value = false
  couponTouched.value = false
  merchantLoading.value = false
  keeperLoading.value = false
  couponQuoteSeq += 1
  membershipQuoteSeq += 1
}

function onMerchantChange() {
  if (serviceId.value) return
  form.keeper_id_wsh = ''
  keepers.value = []
  if (form.merchant_id_wsh) {
    fillDeliveryFromMerchant()
    loadKeepers(form.merchant_id_wsh).then(refreshCoupons)
  } else {
    clearDeliveryLocation()
  }
  if (serviceId.value) {
    serviceId.value = ''
    serviceName.value = ''
    serviceVersion.value = ''
    price.value = ''
  }
}

function handleClose() {
  emit('close')
}

function minDate() {
  const d = new Date(nowTick.value)
  const pad = value => String(value).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function maxDate() {
  const d = new Date(nowTick.value)
  // 以后端 booking_window_days_wsh 为准（含首尾，偏移 = 窗口-1）；未查询前用请求上界兜底
  const offset = maxBookingDays.value > 0 ? maxBookingDays.value - 1 : AVAILABILITY_REQUEST_WINDOW_DAYS
  d.setDate(d.getDate() + offset)
  const pad = value => String(value).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function displaySlot(slot) {
  return slot ? String(slot).slice(11, 16) : ''
}

const availabilityHint = computed(() => {
  if (availabilityLoading.value) return '正在查询可预约日期...'
  if (availabilityError.value) return '可预约日期查询失败，请刷新重试'
  // 服务驱动下单必须先选择可接单看护人，才会查询并解锁日期/时间选择；
  // 商家无合格看护人时明确提示，避免用户误以为日期控件损坏。
  if (availabilityEnabled.value && !form.keeper_id_wsh) {
    return keepers.value.length === 0 ? '该商家暂无可接单看护人，暂无法选择日期' : '请先选择看护人，再选择送达/接回日期'
  }
  if (bookableDates.value.length === 0) {
    return maxBookingDays.value > 0 ? `近 ${maxBookingDays.value} 天暂无可预约日期` : '暂无可预约日期'
  }
  return `仅展示可预约日期（${bookableDates.value.length} 天可约）`
})

// ============ 批量行辅助 ============

function rowSlots(dateValue) {
  if (!dateValue) return []
  const day = availabilityDays.value.find(item => String(item.date_wsh) === dateValue)
  if (!day || !day.bookable_wsh) return []
  return day.windows_wsh.flatMap(window => window.slots_wsh || [])
}

function rowPickupSlotOptions(row) {
  const slots = rowSlots(row.pickup_date_wsh)
  // 接回时间必须晚于送达时间；跨天接回不在此过滤
  if (String(row.pickup_date_wsh) === String(row.delivery_date_wsh) && row.delivery_slot_wsh) {
    return slots.filter(slot => slot > row.delivery_slot_wsh)
  }
  return slots
}

function rowSlotPlaceholder(dateValue) {
  if (!dateValue) return '请先选择送达日期'
  if (availabilityLoading.value) return '正在加载时间槽位...'
  if (rowSlots(dateValue).length === 0) return '该日暂无可约时间'
  return '请选择送达时间'
}

function onRowDeliveryDateChange(row) {
  row.delivery_slot_wsh = ''
  if (row.pickup_date_wsh && row.pickup_date_wsh < row.delivery_date_wsh) {
    row.pickup_date_wsh = ''
    row.pickup_slot_wsh = ''
  }
}

function onRowDeliverySlotChange(row) {
  row.pickup_date_wsh = ''
  row.pickup_slot_wsh = ''
}

function onRowPickupDateChange(row) {
  row.pickup_slot_wsh = ''
}

function addPetRow() {
  if (petRows.length >= BATCH_MAX_PETS) return
  petRows.push(createPetRow())
}

function removePetRow(index) {
  if (petRows.length <= 1) return
  petRows.splice(index, 1)
}

const deliverySlotPlaceholder = computed(() => {
  if (!form.delivery_date_wsh) return '请先选择送达日期'
  if (availabilityLoading.value) return '正在加载时间槽位...'
  if (deliverySlotOptions.value.length === 0) return '该日暂无可约时间'
  return '请选择送达时间'
})

const pickupSlotPlaceholder = computed(() => {
  if (!form.pickup_date_wsh) return '请先选择接回日期'
  if (availabilityLoading.value) return '正在加载时间槽位...'
  if (pickupSlotOptions.value.length === 0) return '该日暂无可约时间'
  return '请选择接回时间'
})

function onDeliveryDateChange() {
  form.delivery_slot_wsh = ''
  if (!form.delivery_date_wsh) {
    form.pickup_date_wsh = ''
    form.pickup_slot_wsh = ''
  }
  if (form.pickup_date_wsh && form.pickup_date_wsh < form.delivery_date_wsh) {
    form.pickup_date_wsh = ''
    form.pickup_slot_wsh = ''
  }
}

function onDeliverySlotChange() {
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
}

function onPickupDateChange() {
  form.pickup_slot_wsh = ''
}

async function loadAvailability() {
  if (!serviceId.value || !form.keeper_id_wsh) return
  const seq = ++availabilitySeq
  availabilityLoading.value = true
  availabilityError.value = false
  form.delivery_date_wsh = ''
  form.delivery_slot_wsh = ''
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
  // 换看护人/商家时只清空日期槽位，保留宠物选择与行数
  petRows.forEach(row => {
    row.delivery_date_wsh = ''
    row.delivery_slot_wsh = ''
    row.pickup_date_wsh = ''
    row.pickup_slot_wsh = ''
  })
  try {
    const res = await getServiceAvailability(serviceId.value, minDate(), maxDate(), form.keeper_id_wsh)
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

async function submitOrder() {
  if (submitting.value) return
  nowTick.value = Date.now()
  const profileOk = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!profileOk) return
  if (merchantLoading.value || keeperLoading.value || availabilityLoading.value) {
    appStore.addToast('接单信息正在加载，请稍后再提交', 'warning')
    return
  }
  // ============ 批量模式（多宠物连续下单） ============
  if (isBatchMode.value) {
    if (!hasDeliveryLocation()) {
      const filledFromMerchant = !form.delivery_address_wsh && fillDeliveryFromMerchant()
      if (!filledFromMerchant || !hasDeliveryLocation()) {
        appStore.addToast('请使用商家位置，或从地址搜索结果中选择送达地址', 'warning')
        return
      }
    }
    const items = []
    for (const row of petRows) {
      if (!row.pet_id_wsh || !row.delivery_date_wsh || !row.delivery_slot_wsh
        || !row.pickup_date_wsh || !row.pickup_slot_wsh) {
        appStore.addToast('请为每只宠物补全送达/接回日期与时间', 'warning')
        return
      }
      if (!bookableDates.value.includes(row.delivery_date_wsh) || !bookableDates.value.includes(row.pickup_date_wsh)) {
        appStore.addToast('所选日期不可预约，请重新选择', 'warning')
        return
      }
      const deliveryTime = new Date(slotToDateTime(row.delivery_date_wsh, row.delivery_slot_wsh))
      const pickupTime = new Date(slotToDateTime(row.pickup_date_wsh, row.pickup_slot_wsh))
      if (Number.isNaN(deliveryTime.getTime()) || Number.isNaN(pickupTime.getTime())) {
        appStore.addToast('请选择有效的送达/接回时间', 'warning')
        return
      }
      if (deliveryTime.getTime() < nowTick.value) {
        appStore.addToast('送达时间不能早于当前时间', 'warning')
        return
      }
      if (pickupTime.getTime() <= deliveryTime.getTime()) {
        appStore.addToast('接回时间必须晚于送达时间', 'warning')
        return
      }
      const serviceDates = buildServiceDates(deliveryTime, pickupTime)
      items.push({
        pet_id_wsh: Number(row.pet_id_wsh),
        start_date_wsh: serviceDates.startDate,
        end_date_wsh: serviceDates.endDate,
        delivery_time_wsh: toApiDateTime(slotToDateTime(row.delivery_date_wsh, row.delivery_slot_wsh)),
        pickup_time_wsh: toApiDateTime(slotToDateTime(row.pickup_date_wsh, row.pickup_slot_wsh)),
      })
    }
    const shared = {
      keeper_id_wsh: Number(form.keeper_id_wsh),
      merchant_id_wsh: Number(form.merchant_id_wsh),
      service_id_wsh: Number(serviceId.value),
      service_version_wsh: serviceVersion.value || null,
      billing_unit_wsh: normalizedUnit.value,
      expected_unit_price_wsh: Number(price.value || 0),
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
      delivery_address_wsh: form.delivery_address_wsh,
      delivery_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      pickup_address_wsh: form.delivery_address_wsh,
      pickup_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      emergency_contact_name_wsh: form.emergency_contact_name_wsh,
      emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
      remark_wsh: form.remark_wsh,
    }
    try {
      // 单只宠物：走单笔接口（与旧路径完全一致）；多只：批量接口
      if (items.length === 1) {
        const item = items[0]
        const payload = {
          ...shared,
          pet_id_wsh: item.pet_id_wsh,
          quantity_wsh: Math.max(0, diffDays(item.start_date_wsh, item.end_date_wsh)),
          start_date_wsh: item.start_date_wsh,
          end_date_wsh: item.end_date_wsh,
          delivery_time_wsh: item.delivery_time_wsh,
          pickup_time_wsh: item.pickup_time_wsh,
        }
        const res = await createOrder(payload)
        if (res.code === 200) {
          if (!res.data?.id_wsh && !res.data?.order_no_wsh) {
            appStore.addToast('订单已提交，请稍后在订单页查看', 'warning')
            emit('close')
            return
          }
          emit('created', res.data)
          return
        }
        handleCreateError(res)
        return
      }
      const res = await createOrdersBatch({ ...shared, items })
      if (res.code === 200) {
        const orders = Array.isArray(res.data) ? res.data : []
        if (orders.length === 0) {
          appStore.addToast('订单已提交，请稍后在订单页查看', 'warning')
          emit('close')
          return
        }
        emit('created', { batch: true, orders })
        return
      }
      handleCreateError(res)
    } catch (e) {
      const res = e?.response?.data
      if (res && typeof res === 'object') {
        handleCreateError(res)
        return
      }
      appStore.addToast('订单提交失败，请稍后重试', 'error')
    }
    return
  }
  const isSlot = slotMode.value
  const deliveryTime = availabilityEnabled.value
    ? new Date(slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh))
    : new Date(form.delivery_time_wsh)
  const pickupTime = isSlot
    ? null
    : (availabilityEnabled.value
      ? new Date(slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh))
      : new Date(form.pickup_time_wsh))
  const deliveryValid = !Number.isNaN(deliveryTime.getTime())
  const pickupValid = isSlot || !Number.isNaN(pickupTime.getTime())
  if (!form.pet_id_wsh || !form.merchant_id_wsh || !form.keeper_id_wsh || !deliveryValid || !pickupValid) {
    appStore.addToast(isSlot ? '请补全宠物、商家、看护人和开始时间' : '请补全宠物、商家、看护人、送达时间和接回时间', 'warning')
    return
  }
  if (availabilityEnabled.value) {
    const deliveryDateBookable = bookableDates.value.includes(form.delivery_date_wsh)
    const pickupDateBookable = isSlot || bookableDates.value.includes(form.pickup_date_wsh)
    if (!deliveryDateBookable || !pickupDateBookable) {
      appStore.addToast('所选日期不可预约，请重新选择', 'warning')
      return
    }
  }
  if (!hasLoadedMerchant(form.merchant_id_wsh) || !hasLoadedKeeper(form.keeper_id_wsh)) {
    appStore.addToast('请选择可接单商家和看护人', 'warning')
    return
  }
  if (!form.emergency_contact_name_wsh || !form.emergency_contact_phone_wsh) {
    appStore.addToast('请填写紧急联系人和联系电话', 'warning')
    return
  }
  if (Number.isNaN(deliveryTime.getTime())) {
    appStore.addToast('请选择有效的送达时间', 'warning')
    return
  }
  if (deliveryTime.getTime() < nowTick.value) {
    appStore.addToast('送达时间不能早于当前时间', 'warning')
    return
  }
  if (!isSlot && pickupTime.getTime() <= deliveryTime.getTime()) {
    appStore.addToast('接回时间必须晚于送达时间', 'warning')
    return
  }
  if (!hasDeliveryLocation()) {
    const filledFromMerchant = !form.delivery_address_wsh && fillDeliveryFromMerchant()
    if (!filledFromMerchant || !hasDeliveryLocation()) {
      appStore.addToast('请使用商家位置，或从地址搜索结果中选择送达地址', 'warning')
      return
    }
  }

  submitting.value = true
  try {
    const unit = normalizedUnit.value
    const unitPrice = Number(price.value || 0)
    const payload = {
      pet_id_wsh: Number(form.pet_id_wsh),
      merchant_id_wsh: Number(form.merchant_id_wsh),
      keeper_id_wsh: Number(form.keeper_id_wsh),
      service_id_wsh: serviceId.value ? Number(serviceId.value) : null,
      service_version_wsh: serviceVersion.value || null,
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
      billing_unit_wsh: unit,
      expected_unit_price_wsh: unitPrice,
      delivery_address_wsh: form.delivery_address_wsh,
      delivery_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      pickup_address_wsh: form.delivery_address_wsh,
      pickup_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      emergency_contact_name_wsh: form.emergency_contact_name_wsh,
      emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
      remark_wsh: form.remark_wsh,
    }
    if (isSlot) {
      const quantity = unit === 'hour' ? Math.max(1, Number(slotQuantity.value) || 1) : 1
      payload.quantity_wsh = quantity
      payload.start_time_wsh = toApiDateTime(slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh))
      payload.start_date_wsh = form.delivery_date_wsh
    } else {
      const serviceDates = buildServiceDates(deliveryTime, pickupTime)
      const days = Math.max(0, diffDays(serviceDates.startDate, serviceDates.endDate))
      payload.quantity_wsh = days
      payload.start_date_wsh = serviceDates.startDate
      payload.end_date_wsh = serviceDates.endDate
      payload.delivery_time_wsh = toApiDateTime(availabilityEnabled.value
        ? slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
        : form.delivery_time_wsh)
      payload.pickup_time_wsh = toApiDateTime(availabilityEnabled.value
        ? slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)
        : form.pickup_time_wsh)
    }
    const res = await createOrder(payload)
    if (res.code === 200) {
      if (!res.data?.id_wsh && !res.data?.order_no_wsh) {
        appStore.addToast('订单已提交，请稍后在订单页查看', 'warning')
        emit('close')
        return
      }
      emit('created', res.data)
      return
    }
    handleCreateError(res)
  } catch (e) {
    const res = e?.response?.data
    if (res && typeof res === 'object') {
      handleCreateError(res)
      return
    }
    appStore.addToast('订单提交失败，请稍后重试', 'error')
  } finally {
    submitting.value = false
  }
}

const BOOKING_ERROR_MESSAGES = {
  FUTURE_BOOKING_DISABLED: '该商家当前未开放未来预约',
  FULFILLMENT_OUTSIDE_BUSINESS_HOURS: '送达或接回时间不在商家营业时段内',
  CAPACITY_EXCEEDED: '该时段看护人已预约满，请更换时段',
  KEEPER_NOT_BOOKABLE: '该看护人当前不可接单',
  MERCHANT_NOT_APPROVED: '商家未通过审核，暂不能预约',
  SERVICE_NOT_FOUND: '服务不存在或已下架，请刷新后重试',
  SERVICE_OFF_SHELF: '服务已下架，请选择其他服务',
  SERVICE_MERCHANT_MISMATCH: '服务与所选商家不一致，请刷新后重试',
  UNSUPPORTED_SERVICE_UNIT: '该服务暂不支持当前预约方式，请更换服务或时段',
  PRICE_CHANGED: '服务价格或版本已更新，请刷新后重新确认',
  KEEPER_MERCHANT_MISMATCH: '看护人不属于该商家，请刷新后重试',
  KEEPER_ON_LEAVE: '该看护人当前请假，请更换看护人',
  KEEPER_NOT_QUALIFIED: '该看护人资质未通过，暂不能预约',
  MERCHANT_REST_DAY: '商家当日休息，请更换日期',
  PET_BOOKING_CONFLICT: '宠物在该时段已有预约，请更换日期',
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
  form.delivery_time_wsh = ''
  form.pickup_time_wsh = ''
  form.delivery_date_wsh = ''
  form.delivery_slot_wsh = ''
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
  slotQuantity.value = 1
  form.user_coupon_id_wsh = ''
  availabilityDays.value = []
  availabilityError.value = false
  availableCoupons.value = []
  couponQuote.value = null
  membershipQuote.value = null
  if (availabilityEnabled.value && form.keeper_id_wsh) {
    await loadAvailability()
  }
  await refreshCoupons()
}

function hasDeliveryLocation() {
  return Boolean(form.delivery_address_wsh && isConfirmedDeliverySource(form.delivery_location_source_wsh))
}

function isConfirmedDeliverySource(source) {
  const text = String(source || '').trim()
  return text === 'merchant' || text.startsWith('amap')
}

function fillDeliveryFromMerchant(showWarning = false) {
  const merchant = selectedMerchant.value
  if (!merchant) {
    if (showWarning) appStore.addToast('请先选择商家', 'warning')
    return false
  }
  form.delivery_address_wsh = merchant.address_wsh || ''
  form.delivery_location_source_wsh = form.delivery_address_wsh ? 'merchant' : ''
  form.delivery_latitude_wsh = merchant.latitude_wsh ?? null
  form.delivery_longitude_wsh = merchant.longitude_wsh ?? null
  if (!hasDeliveryLocation()) {
    if (showWarning) appStore.addToast('商家位置缺少地址，请搜索选择送达地址', 'warning')
    return false
  }
  return true
}

function clearDeliveryLocation() {
  form.delivery_address_wsh = ''
  form.delivery_location_source_wsh = ''
  form.delivery_latitude_wsh = null
  form.delivery_longitude_wsh = null
}

// ============ 送达距离 ============

async function updateDeliveryDistance() {
  const lat = Number(form.delivery_latitude_wsh)
  const lng = Number(form.delivery_longitude_wsh)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    deliveryDistanceMeters.value = null
    distanceLoading.value = false
    distanceError.value = ''
    return
  }
  distanceLoading.value = true
  distanceError.value = ''
  try {
    const location = await ensureUserLocation()
    deliveryDistanceMeters.value = haversineMeters(
      location.latitude_wsh,
      location.longitude_wsh,
      lat,
      lng,
    )
  } catch (error) {
    deliveryDistanceMeters.value = null
    distanceError.value = error?.message || '定位失败'
  } finally {
    distanceLoading.value = false
  }
}

async function ensureUserLocation() {
  if (userLocation) return userLocation
  const location = await getCurrentAddress()
  userLocation = location
  return location
}

function retryDeliveryDistance() {
  userLocation = null
  distanceError.value = ''
  updateDeliveryDistance()
}

function buildServiceDates(deliveryTime, pickupTime) {
  return {
    startDate: toDateOnly(deliveryTime),
    endDate: toDateOnly(addDays(pickupTime, isSameDate(deliveryTime, pickupTime) ? 1 : 0)),
  }
}

async function refreshCoupons() {
  const seq = ++couponQuoteSeq
  const memberSeq = ++membershipQuoteSeq
  const estimate = baseEstimate()
  couponQuote.value = null
  membershipQuote.value = null
  if (!form.merchant_id_wsh || Number(estimate.total) <= 0) {
    availableCoupons.value = []
    form.user_coupon_id_wsh = ''
    couponLoading.value = false
    membershipLoading.value = false
    return
  }
  couponLoading.value = true
  try {
    // 批量：券只作用于金额最大的单（与后端归属规则一致），按该单金额筛选/报价
    const quoteBase = couponQuoteBase()
    const couponListingAmount = isBatchMode.value ? quoteBase.total : Number(estimate.final)
    const params = {
      merchant_id_wsh: Number(form.merchant_id_wsh),
      order_amount_wsh: couponListingAmount,
    }
    const res = await getAvailableCoupons(params)
    if (seq !== couponQuoteSeq) return
    const coupons = res.code === 200 && Array.isArray(res.data) ? res.data : []
    availableCoupons.value = coupons
    if (form.user_coupon_id_wsh && !coupons.some(item => Number(item.id_wsh) === Number(form.user_coupon_id_wsh))) {
      form.user_coupon_id_wsh = ''
    }
    // 用户未手动选择过时，自动勾选当前订单金额下最优惠的一张券
    if (!couponTouched.value && !form.user_coupon_id_wsh && coupons.length > 0) {
      const best = pickBestCoupon(coupons, couponListingAmount)
      if (best) form.user_coupon_id_wsh = String(best.id_wsh)
    }
    if (form.user_coupon_id_wsh) {
      await refreshCouponQuote(seq, quoteBase)
    }
    if (seq !== couponQuoteSeq) return
    // 会员报价基数：批量按批次合计（券后），单笔沿用券报价结果
    const memberBaseAmount = isBatchMode.value
      ? Number(estimate.final)
      : (couponQuote.value?.final_amount_wsh ?? estimate.final)
    await refreshMembershipQuote(memberSeq, memberBaseAmount)
  } catch (e) {
    if (seq !== couponQuoteSeq) return
    availableCoupons.value = []
    form.user_coupon_id_wsh = ''
    couponQuote.value = null
    membershipQuote.value = null
  } finally {
    if (seq === couponQuoteSeq) couponLoading.value = false
  }
}

async function refreshCouponQuote(seq, quoteBase) {
  try {
    const res = await quoteCoupon({
      user_coupon_id_wsh: Number(form.user_coupon_id_wsh),
      total_amount_wsh: quoteBase.total,
      long_stay_discount_wsh: quoteBase.discount,
      merchant_id_wsh: Number(form.merchant_id_wsh),
      service_id_wsh: serviceId.value ? Number(serviceId.value) : null,
    })
    if (seq === couponQuoteSeq && res.code === 200) {
      couponQuote.value = res.data
    }
  } catch (e) {
    if (seq === couponQuoteSeq) {
      couponQuote.value = null
    }
  }
}

async function refreshMembershipQuote(seq, amount) {
  const value = Number(amount || 0)
  membershipQuote.value = null
  if (value <= 0) {
    membershipLoading.value = false
    return
  }
  membershipLoading.value = true
  try {
    const res = await quoteMembershipOrderDiscount(value)
    if (seq === membershipQuoteSeq && res.code === 200) {
      membershipQuote.value = res.data
    }
  } catch (e) {
    if (seq === membershipQuoteSeq) {
      membershipQuote.value = null
    }
  } finally {
    if (seq === membershipQuoteSeq) membershipLoading.value = false
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

/**
 * 按后端 CouponServiceImpl.calculateCouponDiscount 口径估算单张券的优惠金额：
 * - percent：金额 × (1 - 折扣率)，受最大减额约束（0.80 表示 8 折）
 * - amount：满减券直接取减免金额（后端 listAvailableCoupons 已保证满足门槛）
 * 仅用于"自动选择最优惠券"的排序，实付以后端 quote 结果为准。
 */
function couponDiscountEstimate(coupon, orderAmount) {
  const amount = Number(orderAmount || 0)
  if (amount <= 0 || !coupon) return 0
  let discount = 0
  if (coupon.type_wsh === 'percent') {
    const rate = Number(coupon.discount_rate_wsh || 0)
    discount = amount * (1 - rate)
    const max = Number(coupon.max_discount_amount_wsh || 0)
    if (max > 0) discount = Math.min(discount, max)
  } else if (coupon.type_wsh === 'amount') {
    discount = Number(coupon.discount_amount_template_wsh || 0)
  }
  return Math.max(0, Math.min(discount, amount))
}

function pickBestCoupon(coupons, orderAmount) {
  let best = null
  let bestDiscount = -1
  for (const coupon of coupons) {
    const discount = couponDiscountEstimate(coupon, orderAmount)
    if (discount > bestDiscount) {
      bestDiscount = discount
      best = coupon
    }
  }
  return best
}

function slotToDateTime(dateValue, slotValue) {
  return slotValue ? `${dateValue}T${slotValue.slice(11, 16)}` : ''
}

function toApiDateTime(value) {
  return value ? `${value}:00` : null
}

function toLocalDateTimeInput(date) {
  const pad = value => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toDateOnly(date) {
  return toLocalDateTimeInput(date).slice(0, 10)
}

function diffDays(startDate, endDate) {
  return Math.ceil((new Date(endDate) - new Date(startDate)) / 86400000)
}

function addDays(date, days) {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

function isSameDate(first, second) {
  return toDateOnly(first) === toDateOnly(second)
}
</script>

<style scoped>
.order-modal { width: min(92vw, 960px); max-width: 960px; padding: 28px 32px; }
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

.field-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
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

.pet-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md);
  background: var(--color-card, #fff);
}
/* 宠物行按列数整齐换行：宽屏 5 列一行，中等 3 列，窄屏 2 列/1 列 */
@media (max-width: 1100px) {
  .pet-row { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .pet-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 420px) {
  .pet-row { grid-template-columns: 1fr; }
}
.pet-row__title {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--color-muted-foreground);
  font-weight: 600;
}
.pet-row__title button { font-weight: 400; }

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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
/* 高度与 .form-control 对齐（input/select 全局内边距 12px 16px + 边框 ≈ 51px） */
.readonly-value-row { display: flex; align-items: center; gap: 8px; min-height: 51px; }
.readonly-value { font-size: 14px; color: var(--color-foreground); font-weight: 500; }
.readonly-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 4px;
  white-space: nowrap;
}

/* 送达地址距离提示 */
.delivery-distance {
  display: flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  max-width: 100%;
  padding: 6px 12px;
  border-radius: var(--radius-full, 999px);
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.12), rgba(251, 146, 60, 0.05));
  border: 1px solid rgba(249, 115, 22, 0.32);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}
.delivery-distance__icon {
  display: inline-flex;
  flex-shrink: 0;
  font-size: 14px;
}
.delivery-distance__text { overflow-wrap: anywhere; }
.delivery-distance--loading { color: var(--color-muted-foreground); font-weight: 400; }
.delivery-distance--error {
  background: transparent;
  border-color: rgba(239, 68, 68, 0.35);
  color: var(--color-danger, #ef4444);
  font-weight: 400;
  border-radius: var(--radius-md, 8px);
}
.delivery-distance--error .btn { flex-shrink: 0; }
</style>
