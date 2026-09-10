import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockPet, mockOrders, mockReports } from '@/services/__tests__/mockData'

vi.mock('@/services/petService', () => ({
  getById: vi.fn(),
  update: vi.fn(),
  updateAvatar: vi.fn(),
}))

vi.mock('@/services/orderService', () => ({
  getByPetId: vi.fn(),
}))

vi.mock('@/services/fileService', () => ({
  uploadToDirectory: vi.fn(),
}))

vi.mock('@/api/ai', () => ({
  getPetReports: vi.fn(),
}))

import * as PetDomain from '@/domain/PetDomain'
import * as petService from '@/services/petService'
import * as orderService from '@/services/orderService'
import * as fileService from '@/services/fileService'
import { getPetReports } from '@/api/ai'

describe('PetDomain', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getFullProfile', () => {
    it('merges pet, orders, and reports into single profile', async () => {
      petService.getById.mockResolvedValue(mockPet)
      orderService.getByPetId.mockResolvedValue(mockOrders)
      getPetReports.mockResolvedValue({ code: 200, data: mockReports })

      const profile = await PetDomain.getFullProfile(100)

      expect(profile.pet).toEqual(mockPet)
      expect(profile.orders).toEqual(mockOrders)
      expect(profile.reports).toEqual(mockReports)
    })

    it('detects canGenerateReport when any order is completed', async () => {
      petService.getById.mockResolvedValue(mockPet)
      orderService.getByPetId.mockResolvedValue(mockOrders)
      getPetReports.mockResolvedValue({ code: 200, data: [] })

      const profile = await PetDomain.getFullProfile(100)
      expect(profile.canGenerateReport).toBe(true)
    })

    it('canGenerateReport is false when no completed orders', async () => {
      const pendingOrders = [{ ...mockOrders[0], status_wsh: 'pending' }]
      petService.getById.mockResolvedValue(mockPet)
      orderService.getByPetId.mockResolvedValue(pendingOrders)
      getPetReports.mockResolvedValue({ code: 200, data: [] })

      const profile = await PetDomain.getFullProfile(100)
      expect(profile.canGenerateReport).toBe(false)
    })

    it('handles reports API failure gracefully', async () => {
      petService.getById.mockResolvedValue(mockPet)
      orderService.getByPetId.mockResolvedValue([])
      getPetReports.mockRejectedValue(new Error('网络错误'))

      const profile = await PetDomain.getFullProfile(100)
      expect(profile.reports).toEqual([])
    })
  })

  describe('updateAvatar', () => {
    it('uploads file then updates pet avatar url', async () => {
      const mockFile = new File([''], 'pet.jpg', { type: 'image/jpeg' })
      fileService.uploadToDirectory.mockResolvedValue({ url_wsh: '/uploads/pets/abc.jpg' })
      petService.updateAvatar.mockResolvedValue({ ...mockPet, avatar_wsh: '/uploads/pets/abc.jpg' })

      const url = await PetDomain.updateAvatar(100, mockFile)

      expect(fileService.uploadToDirectory).toHaveBeenCalledWith('pets', mockFile)
      expect(petService.updateAvatar).toHaveBeenCalledWith(100, '/uploads/pets/abc.jpg')
      expect(url).toBe('/uploads/pets/abc.jpg')
    })

    it('throws when upload returns no url', async () => {
      fileService.uploadToDirectory.mockResolvedValue({})
      await expect(PetDomain.updateAvatar(100, new File([''], 'test.jpg'))).rejects.toThrow('上传失败')
    })
  })

  describe('helpers', () => {
    it('petTypeLabel returns correct labels', () => {
      expect(PetDomain.petTypeLabel('dog')).toBe('狗')
      expect(PetDomain.petTypeLabel('cat')).toBe('猫')
      expect(PetDomain.petTypeLabel('unknown')).toBe('unknown')
    })

    it('genderLabel handles all values', () => {
      expect(PetDomain.genderLabel(1)).toBe('公')
      expect(PetDomain.genderLabel(2)).toBe('母')
      expect(PetDomain.genderLabel(0)).toBe('未知')
    })

    it('formatAge returns months suffix', () => {
      expect(PetDomain.formatAge(24)).toBe('24个月')
      expect(PetDomain.formatAge(undefined)).toBe('-')
    })
  })
})
