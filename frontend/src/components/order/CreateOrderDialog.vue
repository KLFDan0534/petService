<template>
  <AppDialog :visible="visible" :width="960" @close="handleClose">
    <template #header>
      <div class="order-modal__header">
        <h2>创建订单</h2>
        <p class="order-modal__subtitle">先选好宠物，再安排送达、接回时间（看护人由商家自动分配）</p>
      </div>
    </template>

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
          <span>宠物与负责人</span>
        </div>

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

        <!-- 负责人：不再由用户指定，支付后由看护者抢单 / 商家指派 / 超时自动分配 -->
        <div v-if="!isBatchMode && serviceId" class="readonly-field">
          <span class="readonly-label">看护人</span>
          <div class="readonly-value-row">
            <span class="readonly-value">由商家自动分配</span>
            <span class="readonly-badge">支付后 30 分钟内确定</span>
          </div>
        </div>

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
              <input :value="rowCheckoutDate(row)" type="date" class="form-control" disabled>
            </label>
            <label v-if="isNightUnit">
              夜数
              <input v-model.number="row.nights_wsh" type="number" class="form-control" min="1" :max="MAX_NIGHTS">
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
            {{ slotQuantityLabel }}
            <input data-testid="quantity-input" v-model.number="slotQuantity" type="number" class="form-control" min="1" :max="slotMaxQuantity">
            <span class="field-hint">{{ slotQuantityHint }}</span>
          </label>
          <label v-if="slotMode && !isHourUnit">
            服务数量
            <input class="form-control" :value="1" disabled>
            <span class="field-hint">每次服务一个连续时段</span>
          </label>
          <label v-if="isNightUnit">
            预订夜数
            <input data-testid="nights-input" v-model.number="form.nights_wsh" type="number" class="form-control" min="1" :max="MAX_NIGHTS">
            <span class="field-hint">￥{{ price || '--' }} / 夜；夜价 ÷ 24 按小时计费</span>
          </label>
          <label v-if="isNightUnit">
            离店日期（自动推导）
            <input class="form-control" :value="nightCheckoutDate || '--'" disabled data-testid="checkout-date">
            <span class="field-hint">离店日 14:00 标准离店，14:30 前免费；提前送达只加收费用、不顺延离店</span>
          </label>
          <label v-if="!slotMode && !isNightUnit">
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
            <AppIcon v-if="!distanceError" class="delivery-distance__icon"><Location /></AppIcon>
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
      <div class="estimate estimate--error" v-if="quoteState.error">
        价格计算失败：{{ quoteState.error }}
      </div>
      <div class="estimate" v-else-if="quoteState.loading">
        正在计算价格...
      </div>
      <div class="estimate" v-else>
        <template v-if="estimatedAmount.total && estimatedAmount.total !== '0.00'">
        <div>本次预付：￥{{ estimatedAmount.final }}</div>
        <div style="font-weight:400;font-size:13px;margin-top:4px">
          原价 ￥{{ estimatedAmount.total }}
          <span v-if="Number(estimatedAmount.discount) > 0" style="color:var(--color-danger);margin-left:8px">优惠 ￥{{ estimatedAmount.discount }}</span>
          <span v-if="Number(estimatedAmount.couponDiscount) > 0" style="color:var(--color-danger);margin-left:8px">券减 ￥{{ estimatedAmount.couponDiscount }}</span>
          <span v-if="Number(estimatedAmount.membershipDiscount) > 0" style="color:var(--color-danger);margin-left:8px">会员减 ￥{{ estimatedAmount.membershipDiscount }}</span>
        </div>
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
      <button class="btn btn-primary btn-sm" type="button" :disabled="submitting || merchantLoading || keeperLoading || quoteState.loading || Boolean(quoteState.error)" @click="submitOrder">
        {{ submitting ? '提交中...' : '确认下单' }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
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
import { createOrder, createOrdersBatch, previewOrder as fetchOrderQuote } from '@/api/order'
import { getAvailableCoupons } from '@/api/coupon'
import { normalizeUnit, standardCheckoutDate } from '@/domain/BookingUnit'
import { AVAILABILITY_REQUEST_WINDOW_DAYS, BATCH_MAX_PETS } from '@/domain/BookingWindow'

// 创建订单参数对象
const props = defineProps({
  visible: Boolean,
  initialServiceName: { type: String, default: '' },
  initialServiceId: { type: String, default: '' },
  initialPrice: { type: String, default: '' },
  initialMerchantId: { type: String, default: '' },
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
const couponLoading = ref(false)
// 用户是否手动操作过优惠券：true 后不再自动覆盖选择
const couponTouched = ref(false)
let couponSeq = 0
// 券列表是按哪个金额基数拉取的：金额没变就不重复拉，避免「报价→拉券→改券→再报价」循环
let couponLoadedAmount = null
/**
 * 服务端报价 —— 全前端唯一的价格来源。
 * 金额、天数、计费小时全部由 POST /api/orders/preview 返回，前端不再做任何乘除，
 * 与真实下单共用同一段后端代码，杜绝「显示一个价、扣款另一个价」。
 */
const quoteState = reactive({ loading: false, error: '', single: null, rows: [] })
let quoteSeq = 0
// PRICE_CHANGED 自动重载的防重入标记：重载后若仍失败，就停在错误态等用户关掉弹窗
let quoteReloading = false

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
    // 按夜寄养：每行独立选择预订夜数
    nights_wsh: 1,
  }
}

const form = reactive({
  pet_id_wsh: '',
  merchant_id_wsh: '',
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
  // 按夜寄养：用户明确选择的预订夜数；离店日期由它推导，不能直接编辑
  nights_wsh: 1,
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

/** 预订夜数上限（与后端 booking.max-order-days 默认 365 一致） */
const MAX_NIGHTS = 365

/**
 * 按夜寄养的离店日期 = 入住日 + 夜数。
 * 只用于表单推导与展示，金额一律由后端 preview 返回。
 */
const nightCheckoutDate = computed(() => standardCheckoutDate(form.delivery_date_wsh, form.nights_wsh))

// 夜数变化后同步离店日期，接回时间仍由用户在该日窗口内选择
watch([() => form.nights_wsh, () => form.delivery_date_wsh], () => {
  if (isNightUnit.value && form.delivery_date_wsh) {
    form.pickup_date_wsh = nightCheckoutDate.value
  }
})

// 批量行同理：任何一行的入住日期或夜数变化都要把该行离店日期同步过去
watch(petRows, () => {
  if (!isNightUnit.value) return
  petRows.forEach(row => {
    const derived = rowCheckoutDate(row)
    if (derived && row.pickup_date_wsh !== derived) {
      row.pickup_date_wsh = derived
    }
  })
}, { deep: true })

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
// 批量模式：服务驱动 + 连续区间（night）；session/hour 保持单笔
const isBatchMode = computed(() => availabilityEnabled.value && !slotMode.value)
const normalizedUnit = computed(() => normalizeUnit(serviceUnit.value) || 'night')
const merchantDisplayName = computed(() => {
  const m = selectedMerchant.value
  if (m) return m.name_wsh
  return merchantNameFromService.value || '加载中...'
})
const unitLabel = computed(() => {
  const unit = normalizedUnit.value
  if (unit === 'hour') return '小时'
  if (unit === 'session') return '次'
  if (unit === 'night') return '夜'
  return '夜'
})
const isNightUnit = computed(() => normalizedUnit.value === 'night')
const slotMode = computed(() => {
  if (!availabilityEnabled.value) return false
  const mode = serviceBookingMode.value
  if (mode) return mode === 'slot'
  const unit = normalizeUnit(serviceUnit.value)
  return unit === 'session' || unit === 'hour'
})
const isHourUnit = computed(() => normalizedUnit.value === 'hour')
// 计费粒度（分钟）：服务可配 30 / 60 / 90…，缺省 60
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
  // 保留两位再去掉尾零：1.5 小时 / 0.75 小时都显示正确
  const hours = Number((total / 60).toFixed(2)).toString()
  return `连续服务时长 = ${slotUnitCount.value} × ${slotGranularityMinutes.value} 分钟 = ${total} 分钟（${hours} 小时）`
})
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

const ZERO_ESTIMATE = {
  total: '0.00', discount: '0.00', couponDiscount: '0.00',
  membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00',
  overtime: '0.00', overtimeHours: 0,
}

function fmtAmount(value) {
  return Math.max(0, Number(value || 0)).toFixed(2)
}

/** 展示用金额：直接取服务端报价，前端不参与任何计算 */
const estimatedAmount = computed(() => {
  const rows = isBatchMode.value
    ? quoteState.rows
    : (quoteState.single ? [quoteState.single] : [])
  if (rows.length === 0) return ZERO_ESTIMATE
  const sum = key => rows.reduce((total, row) => total + Number(row[key] || 0), 0)
  return {
    total: fmtAmount(sum('total_amount_wsh')),
    discount: fmtAmount(sum('long_stay_discount_wsh')),
    couponDiscount: fmtAmount(sum('coupon_discount_wsh')),
    membershipDiscount: fmtAmount(sum('membership_discount_wsh')),
    platformSubsidy: fmtAmount(sum('platform_subsidy_wsh')),
    final: fmtAmount(sum('final_amount_wsh')),
    // 预计晚接费：只是预估展示，不参与本次预付
    overtime: fmtAmount(sum('estimated_overtime_amount_wsh')),
    overtimeHours: rows.reduce((total, row) => total + Number(row.estimated_overtime_hours_wsh || 0), 0),
  }
})

/**
 * 组装预览请求。字段与提交 payload 完全一致 —— 预览什么、提交什么。
 * 返回 null 表示信息还不完整，此时不发起请求。
 */
function buildQuotePayload(row) {
  const petId = row ? row.pet_id_wsh : form.pet_id_wsh
  if (!serviceId.value || !petId) return null
  const payload = {
    service_id_wsh: Number(serviceId.value),
    pet_id_wsh: Number(petId),
  }
  if (form.merchant_id_wsh) payload.merchant_id_wsh = Number(form.merchant_id_wsh)
  if (form.user_coupon_id_wsh) payload.user_coupon_id_wsh = Number(form.user_coupon_id_wsh)
  // 与提交 payload 同源：让「商家改价 / 服务版本变更 / 单位不一致」在询价阶段就暴露，
  // 而不是等到点提交才报 PRICE_CHANGED
  if (serviceVersion.value) payload.service_version_wsh = serviceVersion.value
  if (normalizedUnit.value) payload.billing_unit_wsh = normalizedUnit.value
  if (price.value !== '' && price.value !== null && price.value !== undefined) {
    payload.expected_unit_price_wsh = Number(price.value)
  }

  if (slotMode.value) {
    const start = slotToDateTime(
      row ? row.delivery_date_wsh : form.delivery_date_wsh,
      row ? row.delivery_slot_wsh : form.delivery_slot_wsh,
    )
    if (!start) return null
    payload.start_time_wsh = toApiDateTime(start)
    payload.quantity_wsh = normalizedUnit.value === 'hour' ? Math.max(1, Number(slotQuantity.value) || 1) : 1
    return payload
  }

  const deliveryTime = availabilityEnabled.value
    ? slotToDateTime(row ? row.delivery_date_wsh : form.delivery_date_wsh,
                     row ? row.delivery_slot_wsh : form.delivery_slot_wsh)
    : form.delivery_time_wsh
  const pickupTime = availabilityEnabled.value
    ? slotToDateTime(row ? row.pickup_date_wsh : form.pickup_date_wsh,
                     row ? row.pickup_slot_wsh : form.pickup_slot_wsh)
    : form.pickup_time_wsh
  if (!deliveryTime || !pickupTime) return null
  const start = new Date(deliveryTime)
  const end = new Date(pickupTime)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) return null

  payload.delivery_time_wsh = toApiDateTime(deliveryTime)
  payload.pickup_time_wsh = toApiDateTime(pickupTime)
  if (isNightUnit.value) {
    // 按夜寄养：quantity = 预订夜数 N，start_date = 入住日，end_date = 离店日（D+N）
    const nights = Math.max(1, Number(row ? row.nights_wsh : form.nights_wsh) || 1)
    payload.quantity_wsh = nights
    payload.start_date_wsh = toDateOnly(start)
    payload.end_date_wsh = standardCheckoutDate(payload.start_date_wsh, nights)
    return payload
  }
  const serviceDates = buildServiceDates(start, end)
  payload.start_date_wsh = serviceDates.startDate
  payload.end_date_wsh = serviceDates.endDate
  return payload
}

function clearQuote() {
  quoteState.single = null
  quoteState.rows = []
  quoteState.error = ''
  quoteState.loading = false
}

/**
 * 批量预览：优惠券只作用于基础价最大的一单（与后端券归属规则一致），
 * 先不带券算一遍定位最大单，再单独给那一单带上券重算。
 */
async function fetchQuotes(payloads) {
  if (payloads.length === 1) {
    const res = await fetchOrderQuote(payloads[0])
    return [res.data ?? res]
  }
  const stripCoupon = payloads.map(({ user_coupon_id_wsh, ...rest }) => rest)
  const rows = await Promise.all(stripCoupon.map(item => fetchOrderQuote(item).then(res => res.data ?? res)))
  if (!form.user_coupon_id_wsh) return rows
  let largest = 0
  rows.forEach((row, index) => {
    if (Number(row.total_amount_wsh || 0) > Number(rows[largest].total_amount_wsh || 0)) largest = index
  })
  const withCoupon = { ...stripCoupon[largest], user_coupon_id_wsh: Number(form.user_coupon_id_wsh) }
  rows[largest] = await fetchOrderQuote(withCoupon).then(res => res.data ?? res)
  return rows
}

/** 报价：全前端唯一的金额来源。失败时清空金额并记录错误，绝不回退到前端自算。 */
async function refreshQuote() {
  const seq = ++quoteSeq
  const payloads = isBatchMode.value
    ? petRows.map(row => buildQuotePayload(row))
    : [buildQuotePayload(null)]
  if (payloads.some(item => !item)) {
    if (seq === quoteSeq) clearQuote()
    return
  }
  quoteState.loading = true
  quoteState.error = ''
  try {
    const rows = await fetchQuotes(payloads)
    if (seq !== quoteSeq) return
    if (isBatchMode.value) {
      quoteState.rows = rows
      quoteState.single = null
    } else {
      quoteState.single = rows[0]
      quoteState.rows = []
    }
    refreshCoupons()
  } catch (e) {
    if (seq !== quoteSeq) return
    quoteState.single = null
    quoteState.rows = []
    const data = e?.response?.data
    const code = data?.errorCode || data?.error_code_wsh
    if (code === 'PRICE_CHANGED' && !quoteReloading) {
      // 商家改价/服务版本变化：自动重载服务信息再用新版本重新询价，
      // 否则按钮会永久 disabled（错误文案让用户「刷新」，但弹窗里没有刷新入口）
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
 * 触发一次询价。日期/槽位选择都是离散事件，不做防抖：
 * 少一次定时器就少一处「用户已改时间但金额还是旧的」的窗口。
 * 并发由 refreshQuote 内部的 seq 守卫保证只有最后一次结果生效。
 */
function scheduleQuote() {
  refreshQuote()
}

// 数量手输可能超过单笔上限（如 30 分钟粒度最多 48 个单位），超了就钳回上限
watch([slotQuantity, slotMaxQuantity], () => {
  const max = slotMaxQuantity.value
  const current = Number(slotQuantity.value)
  if (!Number.isFinite(current) || current < 1) {
    slotQuantity.value = 1
  } else if (current > max) {
    slotQuantity.value = max
  }
})

/** 优惠券门槛基数：券作用于「基础价 − 长住折扣」，批量取金额最大的那一单 */
function couponBaseAmount() {
  const rows = isBatchMode.value
    ? quoteState.rows
    : (quoteState.single ? [quoteState.single] : [])
  if (rows.length === 0) return 0
  const largest = rows.reduce((a, b) =>
    (Number(a.total_amount_wsh || 0) >= Number(b.total_amount_wsh || 0) ? a : b))
  return Math.max(0, Number(largest.total_amount_wsh || 0) - Number(largest.long_stay_discount_wsh || 0))
}

const selectedMerchant = computed(() =>
  merchants.value.find(item => Number(item.id_wsh) === Number(form.merchant_id_wsh)))
const merchantSelectPlaceholder = computed(() => {
  if (merchantLoading.value) return '正在筛选可接单商家...'
  if (merchants.value.length === 0) return '暂无可接单商家'
  return '请选择商家'
})
const merchantHint = computed(() => {
  if (merchantLoading.value) return '正在校验商家是否有资质通过的在职看护人'
  if (merchants.value.length === 0) return '当前没有满足接单条件的商家，请稍后再试'
  return '仅展示有合格看护人的商家'
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
      await refreshCoupons()
    }
  },
  { immediate: true }
)

watch(
  () => [
    form.merchant_id_wsh,
    keepers.value.length,
  ],
  async () => {
    // 商家变化会换掉可用的券，必须强制重拉
    refreshCoupons(true)
    if (availabilityEnabled.value) {
      await loadAvailability()
    }
  }
)

watch(
  () => [
    form.pet_id_wsh,
    form.delivery_time_wsh,
    form.pickup_time_wsh,
    form.delivery_date_wsh,
    form.delivery_slot_wsh,
    form.pickup_date_wsh,
    form.pickup_slot_wsh,
    slotQuantity.value,
    form.user_coupon_id_wsh,
    serviceId.value,
  ],
  () => {
    // 输入变化 → 重新向服务端询价（金额只由后端算）
    scheduleQuote()
  }
)

// 批量行变化（宠物/日期/槽位）也触发报价刷新
watch(
  () => petRows.map(row =>
    [row.pet_id_wsh, row.delivery_date_wsh, row.delivery_slot_wsh, row.pickup_date_wsh, row.pickup_slot_wsh].join('|')).join(';'),
  () => {
    scheduleQuote()
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

function resetForm() {
  Object.assign(form, {
    pet_id_wsh: '',
    merchant_id_wsh: '',
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
  couponLoading.value = false
  couponTouched.value = false
  merchantLoading.value = false
  keeperLoading.value = false
  couponLoadedAmount = null
  couponSeq += 1
  quoteSeq += 1
  clearQuote()
}

function onMerchantChange() {
  if (serviceId.value) return
  keepers.value = []
  if (form.merchant_id_wsh) {
    fillDeliveryFromMerchant()
    loadKeepers(form.merchant_id_wsh).then(() => refreshCoupons(true))
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
  // 负责人由系统分配，日期可约性只看商家营业时间与整体容量，不再依赖具体看护人
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
  // 按夜寄养的接回日期恒为「入住日 + 夜数」，与是否手动改过无关
  const pickupDate = isNightUnit.value ? rowCheckoutDate(row) : row.pickup_date_wsh
  const slots = rowSlots(pickupDate)
  // 接回时间必须晚于送达时间；跨天接回不在此过滤
  if (String(pickupDate) === String(row.delivery_date_wsh) && row.delivery_slot_wsh) {
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

/** 按夜寄养：某行的离店日期 = 该行送达日期 + 夜数 */
function rowCheckoutDate(row) {
  return standardCheckoutDate(row.delivery_date_wsh, row.nights_wsh)
}

function syncRowPickupDate(row) {
  if (!isNightUnit.value) return
  row.pickup_date_wsh = rowCheckoutDate(row)
}

function onRowDeliveryDateChange(row) {
  row.delivery_slot_wsh = ''
  if (isNightUnit.value) {
    syncRowPickupDate(row)
    return
  }
  if (row.pickup_date_wsh && row.pickup_date_wsh < row.delivery_date_wsh) {
    row.pickup_date_wsh = ''
    row.pickup_slot_wsh = ''
  }
}

function onRowDeliverySlotChange(row) {
  if (isNightUnit.value) {
    syncRowPickupDate(row)
    return
  }
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
    return
  }
  if (isNightUnit.value) {
    form.pickup_date_wsh = nightCheckoutDate.value
    return
  }
  if (form.pickup_date_wsh && form.pickup_date_wsh < form.delivery_date_wsh) {
    form.pickup_date_wsh = ''
    form.pickup_slot_wsh = ''
  }
}

function onDeliverySlotChange() {
  if (isNightUnit.value) {
    form.pickup_date_wsh = nightCheckoutDate.value
    return
  }
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
}

function onPickupDateChange() {
  form.pickup_slot_wsh = ''
}

async function loadAvailability() {
  if (!serviceId.value) return
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

async function submitOrder() {
  if (submitting.value) return
  nowTick.value = Date.now() //时间戳
  const profileOk = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!profileOk) return
  if (merchantLoading.value || keeperLoading.value || availabilityLoading.value) {
    appStore.addToast('接单信息正在加载，请稍后再提交', 'warning')
    return
  }
  // 价格必须由服务端算完才允许提交：否则用户可能在「正在计算价格」或报价失败时下单，
  // 提交用的时间就会退回前端本地推算，「预览什么、提交什么」当场失效。
  if (quoteState.loading) {
    appStore.addToast('价格正在计算，请稍后再提交', 'warning')
    return
  }
  if (quoteState.error) {
    appStore.addToast(quoteState.error, 'error')
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
    const items = [] //
    for (let rowIndex = 0; rowIndex < petRows.length; rowIndex++) { // 遍历所有宠物行
      const row = petRows[rowIndex]
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
      // 直接使用服务端预览返回的规范化时间与数量：
      // 预览与下单的输入完全相同，金额才可能完全相同（不依赖前端本地推算与后端口径恰好一致）
      const quote = quoteState.rows[rowIndex]
      if (!quote?.start_date_wsh) {
        appStore.addToast('价格尚未计算完成，请稍后重试', 'warning')
        return
      }
      items.push({
        pet_id_wsh: Number(row.pet_id_wsh),
        quantity_wsh: quote.quantity_wsh,
        start_date_wsh: quote.start_date_wsh,
        end_date_wsh: quote.end_date_wsh,
        delivery_time_wsh: quote.delivery_time_wsh,
        pickup_time_wsh: quote.pickup_time_wsh,
      })
    }
    const shared = {
      merchant_id_wsh: Number(form.merchant_id_wsh), // 商家id
      service_id_wsh: Number(serviceId.value),  // 服务id
      service_version_wsh: serviceVersion.value || null, // 服务版本
      billing_unit_wsh: normalizedUnit.value, // 计费单位
      expected_unit_price_wsh: Number(price.value || 0), // 预期单价
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null, // 用户优惠券id
      delivery_address_wsh: form.delivery_address_wsh, // 送达地址
      delivery_location_source_wsh: form.delivery_location_source_wsh || 'amap', // 送达位置来源
      // 交接距离校验要用这两个坐标：不提交的话看护者「确认接收」会直接 400
      delivery_latitude_wsh: form.delivery_latitude_wsh, // 送达纬度
      delivery_longitude_wsh: form.delivery_longitude_wsh, // 送达经度
      pickup_address_wsh: form.delivery_address_wsh, // 接回地址
      pickup_location_source_wsh: form.delivery_location_source_wsh || 'amap', // 接回位置来源
      emergency_contact_name_wsh: form.emergency_contact_name_wsh, // 紧急联系人
      emergency_contact_phone_wsh: form.emergency_contact_phone_wsh, // 紧急联系电话
      remark_wsh: form.remark_wsh, // 备注
    }
    try {
      // 单只宠物：走单笔接口（与旧路径完全一致）；多只：批量接口
      if (items.length === 1) {
        const item = items[0]
        const payload = {
          ...shared,
          pet_id_wsh: item.pet_id_wsh,
          quantity_wsh: item.quantity_wsh,
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
  if (!form.pet_id_wsh || !form.merchant_id_wsh || !deliveryValid || !pickupValid) {
    appStore.addToast(isSlot ? '请补全宠物、商家和开始时间' : '请补全宠物、商家、送达时间和接回时间', 'warning')
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
  if (!hasLoadedMerchant(form.merchant_id_wsh)) {
    appStore.addToast('请选择可接单商家', 'warning')
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
      service_id_wsh: serviceId.value ? Number(serviceId.value) : null,
      service_version_wsh: serviceVersion.value || null,
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
      billing_unit_wsh: unit,
      expected_unit_price_wsh: unitPrice,
      delivery_address_wsh: form.delivery_address_wsh,
      delivery_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      // 交接距离校验要用这两个坐标：不提交的话看护者「确认接收」会直接 400
      delivery_latitude_wsh: form.delivery_latitude_wsh,
      delivery_longitude_wsh: form.delivery_longitude_wsh,
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
      // 优先提交预览返回的规范化时间：预览与下单输入完全相同，金额才可能完全相同
      const quote = quoteState.single
      if (quote?.start_date_wsh) {
        payload.quantity_wsh = quote.quantity_wsh
        payload.start_date_wsh = quote.start_date_wsh
        payload.end_date_wsh = quote.end_date_wsh
        payload.delivery_time_wsh = quote.delivery_time_wsh
        payload.pickup_time_wsh = quote.pickup_time_wsh
      } else if (isNightUnit.value) {
        // 兜底（正常走 quote 分支）：夜数 + 推导离店日，不再用「含首尾天数」
        const nights = Math.max(1, Number(form.nights_wsh) || 1)
        payload.quantity_wsh = nights
        payload.start_date_wsh = form.delivery_date_wsh
        payload.end_date_wsh = nightCheckoutDate.value
        payload.delivery_time_wsh = toApiDateTime(slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh))
        payload.pickup_time_wsh = toApiDateTime(slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh))
      } else {
        const serviceDates = buildServiceDates(deliveryTime, pickupTime)
        payload.quantity_wsh = Math.max(1, diffDays(serviceDates.startDate, serviceDates.endDate) + 1)
        payload.start_date_wsh = serviceDates.startDate
        payload.end_date_wsh = serviceDates.endDate
        payload.delivery_time_wsh = toApiDateTime(availabilityEnabled.value
          ? slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
          : form.delivery_time_wsh)
        payload.pickup_time_wsh = toApiDateTime(availabilityEnabled.value
          ? slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)
          : form.pickup_time_wsh)
      }
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
  clearQuote()
  couponLoadedAmount = null
  if (availabilityEnabled.value) {
    await loadAvailability()
  }
  scheduleQuote()
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

/**
 * 服务日期区间：end_date 是「最后一个服务日」（含离店日），
 * 与后端 OrderPricing.dayCount（DAYS.between + 1）保持同一口径。
 */
function buildServiceDates(deliveryTime, pickupTime) {
  return {
    startDate: toDateOnly(deliveryTime),
    endDate: toDateOnly(pickupTime),
  }
}

/**
 * 拉取可用券并按最优惠自动勾选。
 * 金额门槛取服务端报价，前端不再估算；金额没变则不重复拉取，避免报价与拉券互相触发。
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
      const best = pickBestCoupon(coupons, baseAmount)
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
