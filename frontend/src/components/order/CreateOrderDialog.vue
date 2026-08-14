<template>
  <div v-if="visible" class="modal-overlay" @mousedown.self="handleClose">
    <div class="modal order-modal">
      <h2>创建订单</h2>
      <div class="form-grid">
        <label>
          宠物
          <select v-model="form.pet_id_wsh" class="form-control">
            <option value="">请选择宠物</option>
            <option v-for="pet in pets" :key="pet.id_wsh" :value="pet.id_wsh">
              {{ pet.name_wsh }} {{ pet.breed_wsh ? `(${pet.breed_wsh})` : '' }}
            </option>
          </select>
        </label>
        <label>
          商家
          <select
            v-model="form.merchant_id_wsh"
            class="form-control"
            :disabled="merchantLoading || merchants.length === 0 || !!serviceId"
            @change="onMerchantChange"
          >
            <option value="">{{ merchantSelectPlaceholder }}</option>
            <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">
              {{ merchant.name_wsh }}
            </option>
          </select>
          <span class="field-hint">{{ serviceId ? '该服务由所选商家提供，不可更换' : merchantHint }}</span>
        </label>
        <label>
          看护人
          <select
            v-model="form.keeper_id_wsh"
            class="form-control"
            :disabled="!form.merchant_id_wsh || keeperLoading || keepers.length === 0"
          >
            <option value="">{{ keeperSelectPlaceholder }}</option>
            <option v-for="keeper in keepers" :key="keeper.id_wsh" :value="keeper.id_wsh">
              {{ keeper.name_wsh }} ￥{{ keeper.price_per_day_wsh }}/天 · 当前 {{ keeper.current_pets_wsh || 0 }}/{{ keeper.max_pets_wsh || '-' }}
            </option>
          </select>
          <span class="field-hint">{{ keeperHint }}</span>
        </label>
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
            :show-locate-button="false"
            placeholder="搜索地点，或点击地图选址"
          />
        </label>
        <template v-if="availabilityEnabled">
          <label>
            送达日期
            <input v-model="form.delivery_date_wsh" type="date" class="form-control" :min="minDate()" :max="maxDate()" :disabled="availabilityLoading || bookableDates.length === 0" @change="onDeliveryDateChange">
            <span class="field-hint">{{ availabilityHint }}</span>
          </label>
          <label>
            送达时间
            <select v-model="form.delivery_slot_wsh" class="form-control" :disabled="!form.delivery_date_wsh || deliverySlotOptions.length === 0" @change="onDeliverySlotChange">
              <option value="">{{ deliverySlotPlaceholder }}</option>
              <option v-for="slot in deliverySlotOptions" :key="slot" :value="slot">{{ displaySlot(slot) }}</option>
            </select>
          </label>
          <label>
            接回日期
            <input v-model="form.pickup_date_wsh" type="date" class="form-control" :min="form.delivery_date_wsh || minDate()" :max="maxDate()" :disabled="!form.delivery_date_wsh || availabilityLoading || bookableDates.length === 0" @change="onPickupDateChange">
            <span class="field-hint">接回日期不早于送达日期</span>
          </label>
          <label>
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
          <select v-model="form.user_coupon_id_wsh" class="form-control" :disabled="couponLoading || availableCoupons.length === 0">
            <option value="">{{ couponLoading ? '正在加载优惠券...' : '不使用优惠券' }}</option>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import AmapAddressPicker from '@/components/common/AmapAddressPicker.vue'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { getPets } from '@/api/pet'
import { getMerchants } from '@/api/merchant'
import { getKeepersByMerchant } from '@/api/keeper'
import { getServiceDetail, getServiceAvailability } from '@/api/service'
import { createOrder } from '@/api/order'
import { getAvailableCoupons, quoteCoupon } from '@/api/coupon'
import { quoteMembershipOrderDiscount } from '@/api/membership'

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
let couponQuoteSeq = 0
const membershipQuote = ref(null)
const membershipLoading = ref(false)
let membershipQuoteSeq = 0

const availabilityLoading = ref(false)
const availabilityError = ref(false)
const availabilityDays = ref([])
let availabilitySeq = 0

const form = reactive({
  pet_id_wsh: '',
  merchant_id_wsh: '',
  keeper_id_wsh: '',
  delivery_address_wsh: '',
  delivery_location_source_wsh: '',
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
  return day.windows_wsh.flatMap(window => window.slots_wsh || [])
})

const estimatedAmount = computed(() => {
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

function baseEstimate() {
  const deliveryTime = availabilityEnabled.value
    ? slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
    : form.delivery_time_wsh
  const pickupTime = availabilityEnabled.value
    ? slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)
    : form.pickup_time_wsh
  if (!deliveryTime || !pickupTime) {
    return { total: '0.00', discount: '0.00', couponDiscount: '0.00', membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00' }
  }
  const start = new Date(deliveryTime)
  const end = new Date(pickupTime)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) {
    return { total: '0.00', discount: '0.00', couponDiscount: '0.00', membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00' }
  }
  const serviceDates = buildServiceDates(start, end)
  const days = Math.max(0, diffDays(serviceDates.startDate, serviceDates.endDate))
  const keeper = keepers.value.find(item => Number(item.id_wsh) === Number(form.keeper_id_wsh))
  const dayPrice = availabilityEnabled.value
    ? Number(price.value || 0)
    : Number(keeper?.price_per_day_wsh || price.value || 0)
  const total = days * dayPrice
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
          const merchant = merchants.value.find(m => Number(m.id_wsh) === Number(r.data.merchant_id_wsh))
          if (merchant) {
            form.merchant_id_wsh = merchant.id_wsh
            fillDeliveryFromMerchant()
            await loadKeepers(merchant.id_wsh)
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
    form.delivery_time_wsh,
    form.pickup_time_wsh,
    form.user_coupon_id_wsh,
    keepers.value.length,
  ],
  async () => {
    refreshCoupons()
    if (availabilityEnabled.value && form.keeper_id_wsh) {
      await loadAvailability()
    }
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

async function loadKeepers(merchantId) {
  if (!merchantId) {
    keepers.value = []
    return []
  }
  keeperLoading.value = true
  const cached = orderableKeepersByMerchantId.value[merchantKey(merchantId)]
  try {
    if (cached) {
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
  merchants.value = []
  keepers.value = []
  orderableKeepersByMerchantId.value = {}
  availabilityDays.value = []
  availabilityError.value = false
  availableCoupons.value = []
  couponQuote.value = null
  membershipQuote.value = null
  couponLoading.value = false
  membershipLoading.value = false
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
  d.setDate(d.getDate() + 30)
  const pad = value => String(value).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function displaySlot(slot) {
  return slot ? String(slot).slice(11, 16) : ''
}

const availabilityHint = computed(() => {
  if (availabilityLoading.value) return '正在查询可预约日期...'
  if (availabilityError.value) return '可预约日期查询失败，请刷新重试'
  if (bookableDates.value.length === 0) return '近 31 天暂无可预约日期'
  return `仅展示可预约日期（${bookableDates.value.length} 天可约）`
})

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
  try {
    const res = await getServiceAvailability(serviceId.value, minDate(), maxDate(), form.keeper_id_wsh)
    if (seq !== availabilitySeq) return
    if (res.code === 200 && res.data?.days_wsh) {
      availabilityDays.value = res.data.days_wsh
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
  const deliveryTime = availabilityEnabled.value
    ? new Date(slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh))
    : new Date(form.delivery_time_wsh)
  const pickupTime = availabilityEnabled.value
    ? new Date(slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh))
    : new Date(form.pickup_time_wsh)
  if (!form.pet_id_wsh || !form.merchant_id_wsh || !form.keeper_id_wsh || Number.isNaN(deliveryTime.getTime()) || Number.isNaN(pickupTime.getTime())) {
    appStore.addToast('请补全宠物、商家、看护人、送达时间和接回时间', 'warning')
    return
  }
  if (availabilityEnabled.value) {
    const deliveryDateBookable = bookableDates.value.includes(form.delivery_date_wsh)
    const pickupDateBookable = bookableDates.value.includes(form.pickup_date_wsh)
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
  if (Number.isNaN(deliveryTime.getTime()) || Number.isNaN(pickupTime.getTime())) {
    appStore.addToast('请选择有效的送达和接回时间', 'warning')
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
  if (!hasDeliveryLocation()) {
    const filledFromMerchant = !form.delivery_address_wsh && fillDeliveryFromMerchant()
    if (!filledFromMerchant || !hasDeliveryLocation()) {
      appStore.addToast('请使用商家位置，或从地址搜索结果中选择送达地址', 'warning')
      return
    }
  }

  submitting.value = true
  try {
    const serviceDates = buildServiceDates(deliveryTime, pickupTime)
    const payload = {
      pet_id_wsh: Number(form.pet_id_wsh),
      merchant_id_wsh: Number(form.merchant_id_wsh),
      keeper_id_wsh: Number(form.keeper_id_wsh),
      service_id_wsh: serviceId.value ? Number(serviceId.value) : null,
      service_version_wsh: serviceVersion.value || null,
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
      start_date_wsh: serviceDates.startDate,
      end_date_wsh: serviceDates.endDate,
      delivery_address_wsh: form.delivery_address_wsh,
      delivery_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      pickup_address_wsh: form.delivery_address_wsh,
      pickup_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      delivery_time_wsh: toApiDateTime(availabilityEnabled.value
        ? slotToDateTime(form.delivery_date_wsh, form.delivery_slot_wsh)
        : form.delivery_time_wsh),
      pickup_time_wsh: toApiDateTime(availabilityEnabled.value
        ? slotToDateTime(form.pickup_date_wsh, form.pickup_slot_wsh)
        : form.pickup_time_wsh),
      emergency_contact_name_wsh: form.emergency_contact_name_wsh,
      emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
      remark_wsh: form.remark_wsh,
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
  UNSUPPORTED_SERVICE_UNIT: '该服务暂不支持按天预约',
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
      }
    } catch (e) {}
  }
  form.delivery_time_wsh = ''
  form.pickup_time_wsh = ''
  form.delivery_date_wsh = ''
  form.delivery_slot_wsh = ''
  form.pickup_date_wsh = ''
  form.pickup_slot_wsh = ''
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
  if (!hasDeliveryLocation()) {
    if (showWarning) appStore.addToast('商家位置缺少地址，请搜索选择送达地址', 'warning')
    return false
  }
  return true
}

function clearDeliveryLocation() {
  form.delivery_address_wsh = ''
  form.delivery_location_source_wsh = ''
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
    const params = {
      merchant_id_wsh: Number(form.merchant_id_wsh),
      order_amount_wsh: Number(estimate.final),
    }
    const res = await getAvailableCoupons(params)
    if (seq !== couponQuoteSeq) return
    const coupons = res.code === 200 && Array.isArray(res.data) ? res.data : []
    availableCoupons.value = coupons
    if (form.user_coupon_id_wsh && !coupons.some(item => Number(item.id_wsh) === Number(form.user_coupon_id_wsh))) {
      form.user_coupon_id_wsh = ''
    }
    if (form.user_coupon_id_wsh) {
      await refreshCouponQuote(seq, estimate)
    }
    if (seq !== couponQuoteSeq) return
    const memberBaseAmount = couponQuote.value?.final_amount_wsh ?? estimate.final
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

async function refreshCouponQuote(seq, estimate) {
  try {
    const res = await quoteCoupon({
      user_coupon_id_wsh: Number(form.user_coupon_id_wsh),
      total_amount_wsh: Number(estimate.total),
      long_stay_discount_wsh: Number(estimate.discount),
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
.order-modal { max-width: 760px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-top: 16px; }
.form-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); }
.form-grid__wide { grid-column: 1 / -1; }
.field-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.field-hint { min-height: 18px; color: var(--color-muted-foreground); font-size: 12px; line-height: 1.5; }
.estimate { margin-top: 14px; font-weight: 700; color: var(--color-primary); }
.estimate-note { color: var(--color-muted-foreground); font-size: 13px; font-weight: 400; margin-top: 4px; }
</style>
