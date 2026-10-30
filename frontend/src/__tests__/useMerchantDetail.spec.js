import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockMerchant, mockServices, mockBusinessHours, mockRatings } from '@/services/__tests__/mockData'

vi.mock('@/domain/MerchantDomain', () => ({
  getFullProfile: vi.fn(),
  toggleFavorite: vi.fn(),
}))

vi.mock('@/constants/statusMaps', () => ({
  MerchantStatus: { 1: { label: '已通过', badge: 'badge-success' } },
  getStatusLabel: (map, key) => map[key]?.label ?? '-',
  getStatusBadge: (map, key) => map[key]?.badge ?? 'badge-info',
}))

import { useMerchantDetail } from '@/composables/useMerchantDetail'
import * as MerchantDomain from '@/domain/MerchantDomain'

// 这个组合式函数管的是商家详情的加载过程：什么时候在转圈、成功给数据、失败给一句错误话
describe('useMerchantDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('starts with loading=false and error=null', () => {
    const { loading, error, data } = useMerchantDetail(1)
    expect(loading.value).toBe(false)
    expect(error.value).toBe(null)
    expect(data.value).toBe(null)
  })

  it('sets loading=true during load, then sets data on success', async () => {
    MerchantDomain.getFullProfile.mockResolvedValue({
      merchant: mockMerchant,
      services: mockServices,
      hours: mockBusinessHours,
      ratings: mockRatings,
      dayLabels: {},
      stats: { avgScore: '4.5', serviceCount: 2, ratingCount: 2 },
      qualifications: [],
    })

    const composable = useMerchantDetail(1)

    // 这里故意先不 await，趁请求还没回来的时候看 loading 是不是已经翻成 true，然后再等它结束
    const loadPromise = composable.load()
    expect(composable.loading.value).toBe(true)

    await loadPromise

    expect(composable.loading.value).toBe(false)
    expect(composable.error.value).toBe(null)
    expect(composable.data.value).not.toBe(null)
    expect(composable.merchant.value.name_wsh).toBe('PetCare宠物生活馆')
    expect(composable.services.value.length).toBe(2)
    expect(composable.avgScore.value).toBe('4.5')
  })

  // 出错时要把数据清干净，不然页面上会残留上一家商家的信息，看着像加载成功了
  it('sets error on failure and clears data', async () => {
    MerchantDomain.getFullProfile.mockRejectedValue(new Error('网络错误'))

    const composable = useMerchantDetail(1)
    await composable.load()

    expect(composable.loading.value).toBe(false)
    expect(composable.error.value).toBe('网络错误')
    expect(composable.data.value).toBe(null)
    expect(composable.merchant.value).toBe(null)
  })

  it('retry() re-fetches data', async () => {
    MerchantDomain.getFullProfile
      .mockRejectedValueOnce(new Error('第一次失败'))
      .mockResolvedValueOnce({
        merchant: mockMerchant, services: [], hours: [], ratings: [],
        dayLabels: {}, stats: { avgScore: '-', serviceCount: 0, ratingCount: 0 }, qualifications: [],
      })

    const composable = useMerchantDetail(1)
    await composable.load()
    expect(composable.error.value).toBe('第一次失败')

    await composable.retry()
    expect(composable.error.value).toBe(null)
    expect(composable.merchant.value).not.toBe(null)
  })

  // 连 id 都没有就别去发请求了，直接给一句缺参数的提示
  it('sets error when id is missing', async () => {
    const composable = useMerchantDetail(null)
    await composable.load()
    expect(composable.error.value).toBe('缺少商家ID')
  })

  // 状态文案和颜色都是拿状态码去状态表里查出来的，文件顶部那张表已经换成了固定的那一份
  it('computed statusLabel reflects merchant status', async () => {
    MerchantDomain.getFullProfile.mockResolvedValue({
      merchant: mockMerchant, services: [], hours: [], ratings: [],
      dayLabels: {}, stats: { avgScore: '-', serviceCount: 0, ratingCount: 0 }, qualifications: [],
    })

    const composable = useMerchantDetail(1)
    await composable.load()

    expect(composable.statusLabel.value).toBe('已通过')
    expect(composable.statusBadge.value).toBe('badge-success')
  })
})
