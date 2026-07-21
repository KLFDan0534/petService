import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockKeeper, mockRatings } from '@/services/__tests__/mockData'

vi.mock('@/services/keeperService', () => ({
  getById: vi.fn(),
}))

vi.mock('@/services/ratingService', () => ({
  getByTarget: vi.fn(),
}))

vi.mock('@/services/favoriteService', () => ({
  toggle: vi.fn(),
}))

import * as KeeperDomain from '../KeeperDomain'
import * as keeperService from '@/services/keeperService'
import * as ratingService from '@/services/ratingService'
import * as favoriteService from '@/services/favoriteService'

describe('KeeperDomain', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getFullProfile', () => {
    it('merges keeper and ratings into single profile', async () => {
      keeperService.getById.mockResolvedValue(mockKeeper)
      ratingService.getByTarget.mockResolvedValue(mockRatings)

      const profile = await KeeperDomain.getFullProfile(10)

      expect(profile.keeper).toEqual(mockKeeper)
      expect(profile.ratings).toEqual(mockRatings)
      expect(profile.qualifications).toEqual(mockKeeper.qualifications_wsh)
    })

    it('computes avgScore correctly', async () => {
      keeperService.getById.mockResolvedValue(mockKeeper)
      ratingService.getByTarget.mockResolvedValue(mockRatings)

      const profile = await KeeperDomain.getFullProfile(10)
      expect(profile.stats.avgScore).toBe('4.5')
      expect(profile.stats.ratingCount).toBe(2)
    })

    it('handles empty ratings', async () => {
      keeperService.getById.mockResolvedValue(mockKeeper)
      ratingService.getByTarget.mockResolvedValue([])

      const profile = await KeeperDomain.getFullProfile(10)
      expect(profile.stats.avgScore).toBe('-')
      expect(profile.ratings).toEqual([])
    })

    it('qualifications defaults to empty array when missing', async () => {
      keeperService.getById.mockResolvedValue({ ...mockKeeper, qualifications_wsh: undefined })
      ratingService.getByTarget.mockResolvedValue([])

      const profile = await KeeperDomain.getFullProfile(10)
      expect(profile.qualifications).toEqual([])
    })
  })

  describe('toggleFavorite', () => {
    it('calls favoriteService.toggle with keeper type', async () => {
      favoriteService.toggle.mockResolvedValue({})
      await KeeperDomain.toggleFavorite(10)
      expect(favoriteService.toggle).toHaveBeenCalledWith(10, 'keeper')
    })
  })
})
