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
        <label>
          送达时间
          <input v-model="form.delivery_time_wsh" type="datetime-local" class="form-control" :min="minDateTime">
        </label>
        <label>
          接回时间
          <input v-model="form.pickup_time_wsh" type="datetime-local" class="form-control" :min="pickupMinDateTime">
        </label>
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
import { getService } from '@/api/service'
import { createOrder } from '@/api/order'
import { getAvailableCoupons, quoteCoupon } from '@/api/coupon'
import { quoteMembershipOrderDiscount } from '@/api/membership'

const props = defineProps({
  visible: Boolean,
  initialServiceName: { type: String, default: '' },
  initialServiceId: { type: String, default: '' },
  initialPrice: { type: String, default: '' },
  initialMerchantId: { type: String, default: '' },
  initialKeeperId: { type: String, default: '' },
})
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
const price = ref('')
const nowTick = ref(Date.now())
const availableCoupons = ref([])
const couponQuote = ref(null)
const couponLoading = ref(false)
let couponQuoteSeq = 0
const membershipQuote = ref(null)
const membershipLoading = ref(false)
let membershipQuoteSeq = 0

const form = reactive({
  pet_id_wsh: '',
  merchant_id_wsh: '',
  keeper_id_wsh: '',
  delivery_address_wsh: '',
  delivery_location_source_wsh: '',
  delivery_time_wsh: '',
  pickup_time_wsh: '',
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
  if (!form.delivery_time_wsh || !form.pickup_time_wsh) {
    return { total: '0.00', discount: '0.00', couponDiscount: '0.00', membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00' }
  }
  const start = new Date(form.delivery_time_wsh)
  const end = new Date(form.pickup_time_wsh)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) {
    return { total: '0.00', discount: '0.00', couponDiscount: '0.00', membershipDiscount: '0.00', platformSubsidy: '0.00', final: '0.00' }
  }
  const serviceDates = buildServiceDates(start, end)
  const days = Math.max(0, diffDays(serviceDates.startDate, serviceDates.endDate))
  const keeper = keepers.value.find(item => Number(item.id_wsh) === Number(form.keeper_id_wsh))
  const dayPrice = Number(keeper?.price_per_day_wsh || price.value || 0)
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
  if (merchantLoading.value) return '正在校验商家是否有已通过资质并在线的看护人'
  if (merchants.value.length === 0) return '当前没有满足接单条件的商家，请稍后再试'
  return '仅展示有合格看护人的商家'
})
const keeperHint = computed(() => {
  if (!form.merchant_id_wsh) return '选择商家后再选择看护人'
  if (keeperLoading.value) return '正在同步该商家的可接单看护人'
  if (keepers.value.length === 0) return '该商家暂无可接单看护人'
  return '仅展示已通过资质审核且在线的看护人'
})

watch(
  () => props.visible,
  async (val) => {
    if (!val) return
    nowTick.value = Date.now()
    resetForm()
    serviceName.value = props.initialServiceName
    serviceId.value = props.initialServiceId
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
        const r = await getService(serviceId.value)
        if (r.code === 200 && r.data) {
          serviceName.value = r.data.name_wsh || serviceName.value
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
      } catch (e) {}
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
  () => {
    refreshCoupons()
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
  return Number(keeper?.status_wsh) === 1
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
    emergency_contact_name_wsh: '',
    emergency_contact_phone_wsh: '',
    user_coupon_id_wsh: '',
    remark_wsh: '',
  })
  merchants.value = []
  keepers.value = []
  orderableKeepersByMerchantId.value = {}
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
    price.value = ''
  }
}

function handleClose() {
  emit('close')
}

async function submitOrder() {
  nowTick.value = Date.now()
  const profileOk = await ensureProfileRequirement(PROFILE_ACTIONS.CREATE_ORDER, { authStore, appStore, router })
  if (!profileOk) return
  if (merchantLoading.value || keeperLoading.value) {
    appStore.addToast('接单信息正在加载，请稍后再提交', 'warning')
    return
  }
  if (!form.pet_id_wsh || !form.merchant_id_wsh || !form.keeper_id_wsh || !form.delivery_time_wsh || !form.pickup_time_wsh) {
    appStore.addToast('请补全宠物、商家、看护人、送达时间和接回时间', 'warning')
    return
  }
  if (!hasLoadedMerchant(form.merchant_id_wsh) || !hasLoadedKeeper(form.keeper_id_wsh)) {
    appStore.addToast('请选择可接单商家和看护人', 'warning')
    return
  }
  if (!form.emergency_contact_name_wsh || !form.emergency_contact_phone_wsh) {
    appStore.addToast('请填写紧急联系人和联系电话', 'warning')
    return
  }
  const deliveryTime = new Date(form.delivery_time_wsh)
  const pickupTime = new Date(form.pickup_time_wsh)
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
      user_coupon_id_wsh: form.user_coupon_id_wsh ? Number(form.user_coupon_id_wsh) : null,
      start_date_wsh: serviceDates.startDate,
      end_date_wsh: serviceDates.endDate,
      delivery_address_wsh: form.delivery_address_wsh,
      delivery_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      pickup_address_wsh: form.delivery_address_wsh,
      pickup_location_source_wsh: form.delivery_location_source_wsh || 'amap',
      delivery_time_wsh: toApiDateTime(form.delivery_time_wsh),
      pickup_time_wsh: toApiDateTime(form.pickup_time_wsh),
      emergency_contact_name_wsh: form.emergency_contact_name_wsh,
      emergency_contact_phone_wsh: form.emergency_contact_phone_wsh,
      remark_wsh: form.remark_wsh,
    }
    const res = await createOrder(payload)
    if (res.code === 200) {
      emit('created', res.data)
    }
  } catch (e) {
    appStore.addToast('订单提交失败', 'error')
  } finally {
    submitting.value = false
  }
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
