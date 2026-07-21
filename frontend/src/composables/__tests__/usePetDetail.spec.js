import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockPet, mockOrders, mockReports } from '@/services/__tests__/mockData'

vi.mock('@/domain/PetDomain', () => ({
  getFullProfile: vi.fn(),
  updateAvatar: vi.fn(),
  updateProfile: vi.fn(),
  formatAge: (a) => a ? a + '个月' : '-',
  genderLabel: (g) => { const v = Number(g); return v === 1 ? '公' : v === 2 ? '母' : '未知' },
}))

vi.mock('@/constants/statusMaps', () => ({
  OrderStatus: {},
  getStatusLabel: (map, key) => key ?? '-',
  getStatusBadge: (map, key) => 'badge-info',
}))

import { usePetDetail } from '../usePetDetail'
import * as PetDomain from '@/domain/PetDomain'

describe('usePetDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads pet profile successfully', async () => {
    PetDomain.getFullProfile.mockResolvedValue({
      pet: mockPet,
      orders: mockOrders,
      reports: mockReports,
      canGenerateReport: true,
    })

    const composable = usePetDetail(100)
    await composable.load()

    expect(composable.loading.value).toBe(false)
    expect(composable.pet.value.name_wsh).toBe('旺财')
    expect(composable.orders.value.length).toBe(2)
    expect(composable.reports.value.length).toBe(1)
    expect(composable.canGenerate.value).toBe(true)
  })

  it('sets error on failure', async () => {
    PetDomain.getFullProfile.mockRejectedValue(new Error('加载失败'))

    const composable = usePetDetail(100)
    await composable.load()

    expect(composable.error.value).toBe('加载失败')
    expect(composable.pet.value).toBe(null)
  })

  it('updateAvatar updates pet state reactively', async () => {
    PetDomain.getFullProfile.mockResolvedValue({
      pet: { ...mockPet, avatar_wsh: null },
      orders: [], reports: [], canGenerateReport: false,
    })
    PetDomain.updateAvatar.mockResolvedValue('/uploads/pets/new-avatar.jpg')

    const composable = usePetDetail(100)
    await composable.load()
    expect(composable.pet.value.avatar_wsh).toBe(null)

    await composable.updateAvatar(new File([''], 'test.jpg'))
    expect(composable.pet.value.avatar_wsh).toBe('/uploads/pets/new-avatar.jpg')
  })

  it('updateProfile updates pet data reactively', async () => {
    const updatedPet = { ...mockPet, name_wsh: '旺财改名' }
    PetDomain.getFullProfile.mockResolvedValue({
      pet: mockPet, orders: [], reports: [], canGenerateReport: false,
    })
    PetDomain.updateProfile.mockResolvedValue(updatedPet)

    const composable = usePetDetail(100)
    await composable.load()

    await composable.updateProfile({ name_wsh: '旺财改名' })
    expect(composable.pet.value.name_wsh).toBe('旺财改名')
  })

  it('retry re-fetches after error', async () => {
    PetDomain.getFullProfile
      .mockRejectedValueOnce(new Error('网络错误'))
      .mockResolvedValueOnce({ pet: mockPet, orders: [], reports: [], canGenerateReport: false })

    const composable = usePetDetail(100)
    await composable.load()
    expect(composable.error.value).toBe('网络错误')

    await composable.retry()
    expect(composable.error.value).toBe(null)
    expect(composable.pet.value).not.toBe(null)
  })
})
