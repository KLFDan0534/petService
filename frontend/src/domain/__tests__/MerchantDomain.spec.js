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

import * as MerchantDomain from '../MerchantDomain'
import * as merchantService from '@/services/merchantService'
import * as ratingService from '@/services/ratingService'
import * as favoriteService from '@/services/favoriteService'

describe('MerchantDomain', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

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

  describe('toggleFavorite', () => {
    it('calls favoriteService.toggle with correct params', async () => {
      favoriteService.toggle.mockResolvedValue({})
      await MerchantDomain.toggleFavorite(1)
      expect(favoriteService.toggle).toHaveBeenCalledWith(1, 'merchant')
    })
  })
})
