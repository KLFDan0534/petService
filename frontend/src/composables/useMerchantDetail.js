import { ref, computed } from 'vue'
import * as MerchantDomain from '@/domain/MerchantDomain'
import { MerchantStatus, getStatusLabel, getStatusBadge } from '@/constants/statusMaps'

export function useMerchantDetail(id) {
  const loading = ref(false)
  const error = ref(null)
  const data = ref(null)

  // 详情接口一次返回很多块，这里拆开给页面各处用
  const merchant = computed(() => data.value?.merchant ?? null)
  const services = computed(() => data.value?.services ?? [])
  const hours = computed(() => data.value?.hours ?? [])
  const ratings = computed(() => data.value?.ratings ?? [])
  const qualifications = computed(() => data.value?.qualifications ?? [])
  const stats = computed(() => data.value?.stats ?? {})
  const dayLabels = computed(() => data.value?.dayLabels ?? {})
  const avgScore = computed(() => stats.value.avgScore ?? '-')

  // 商家还没加载出来时先给个默认值，免得页面渲染时取到 undefined
  const statusLabel = computed(() => {
    const m = merchant.value
    return m ? getStatusLabel(MerchantStatus, m.status_wsh) : '-'
  })

  const statusBadge = computed(() => {
    const m = merchant.value
    return m ? getStatusBadge(MerchantStatus, m.status_wsh) : 'badge-info'
  })

  // 把商家信息和服务、营业时间、评价一起拿回来
  async function load() {
    if (!id) {
      error.value = '缺少商家ID'
      loading.value = false
      return
    }
    loading.value = true
    error.value = null
    try {
      data.value = await MerchantDomain.getFullProfile(id)
    } catch (e) {
      error.value = e.message || '加载商家信息失败'
      data.value = null
    } finally {
      loading.value = false
    }
  }

  async function retry() {
    await load()
  }

  return {
    loading, error, data,
    merchant, services, hours, ratings, qualifications,
    stats, dayLabels, avgScore,
    statusLabel, statusBadge,
    load, retry,
  }
}
