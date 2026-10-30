import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockMerchant, mockServices, mockBusinessHours, mockRatings } from '@/services/__tests__/mockData'

vi.mock('@/services/merchantService', () => ({
  getById: vi.fn(),
  getServices: vi.fn(),
  getBusinessHours: vi.fn(),
}))

vi.mock('@/services/ratingService', () => ({
  getByTarget: vi.fn(),
}))

vi.mock('@/services/favoriteService', () => ({
  toggle: vi.fn(),
}))

import * as MerchantDomain from '@/domain/MerchantDomain'
import * as merchantService from '@/services/merchantService'
import * as ratingService from '@/services/ratingService'
import * as favoriteService from '@/services/favoriteService'

// MerchantDomain 把商家本身、服务、营业时间、评价四个接口的结果攒成一份，页面只跟它打交道
describe('MerchantDomain', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // 四个接口一起发出去，都回来之后再拼成一份，拼的时候顺手算个平均分和评价条数
  describe('getFullProfile', () => {
    it('merges merchant, services, hours, ratings into single profile', async () => {
      merchantService.getById.mockResolvedValue(mockMerchant)
      merchantService.getServices.mockResolvedValue(mockServices)
      merchantService.getBusinessHours.mockResolvedValue(mockBusinessHours)
      ratingService.getByTarget.mockResolvedValue(mockRatings)

      const profile = await MerchantDomain.getFullProfile(1)

      expect(profile.merchant).toEqual(mockMerchant)
      expect(profile.services).toEqual(mockServices)
      expect(profile.hours).toEqual(mockBusinessHours)
      expect(profile.ratings).toEqual(mockRatings)
      expect(profile.qualifications).toEqual(mockMerchant.qualifications_wsh)
    })

    it('computes avgScore from ratings correctly', async () => {
      merchantService.getById.mockResolvedValue(mockMerchant)
      merchantService.getServices.mockResolvedValue([])
      merchantService.getBusinessHours.mockResolvedValue([])
      ratingService.getByTarget.mockResolvedValue(mockRatings)

      const profile = await MerchantDomain.getFullProfile(1)
      expect(profile.stats.avgScore).toBe('4.5')
      expect(profile.stats.ratingCount).toBe(2)
    })

    // 一条评价都没有的时候不能算成 0 分，页面上要显示一个横杠才不吓人
    it('returns "-" for avgScore when no ratings exist', async () => {
      merchantService.getById.mockResolvedValue(mockMerchant)
      merchantService.getServices.mockResolvedValue([])
      merchantService.getBusinessHours.mockResolvedValue([])
      ratingService.getByTarget.mockResolvedValue([])

      const profile = await MerchantDomain.getFullProfile(1)
      expect(profile.stats.avgScore).toBe('-')
    })

    it('calls all services in parallel via Promise.all', async () => {
      merchantService.getById.mockResolvedValue(mockMerchant)
      merchantService.getServices.mockResolvedValue([])
      merchantService.getBusinessHours.mockResolvedValue([])
      ratingService.getByTarget.mockResolvedValue([])

      await MerchantDomain.getFullProfile(1)

      expect(merchantService.getById).toHaveBeenCalledWith(1)
      expect(merchantService.getServices).toHaveBeenCalledWith(1)
      expect(merchantService.getBusinessHours).toHaveBeenCalledWith(1)
      expect(ratingService.getByTarget).toHaveBeenCalledWith(1, 'merchant')
    })

    it('throws when merchant service fails', async () => {
      merchantService.getById.mockRejectedValue(new Error('网络错误'))
      merchantService.getServices.mockResolvedValue([])
      merchantService.getBusinessHours.mockResolvedValue([])
      ratingService.getByTarget.mockResolvedValue([])

      await expect(MerchantDomain.getFullProfile(1)).rejects.toThrow('网络错误')
    })
  })

  // 收藏就是把商家 id 连同类型一起丢给收藏接口，类型写错了收藏列表里就翻不到这一条
  describe('toggleFavorite', () => {
    it('calls favoriteService.toggle with correct params', async () => {
      favoriteService.toggle.mockResolvedValue({})
      await MerchantDomain.toggleFavorite(1)
      expect(favoriteService.toggle).toHaveBeenCalledWith(1, 'merchant')
    })
  })
})
