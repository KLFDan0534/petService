import { describe, it, expect, vi, beforeEach } from 'vitest'
import { formatServiceDistance, formatDistanceKm, useServiceDistance } from '@/composables/useServiceDistance'

const amapMocks = vi.hoisted(() => ({
  getCurrentAddress: vi.fn(),
  searchAmapAddress: vi.fn(),
}))

vi.mock('@/composables/useAmapLocation', () => amapMocks)

// 距离显示分两档：不到 1 公里按米说，够 1 公里就换成公里并留一位小数
describe('formatServiceDistance', () => {
  it('小于 1km 显示米，只保留数字', () => {
    expect(formatServiceDistance(850)).toBe('850m')
    expect(formatServiceDistance(999)).toBe('999m')
  })

  it('不小于 1km 显示公里（保留 1 位小数）', () => {
    expect(formatServiceDistance(1000)).toBe('1.0km')
    expect(formatServiceDistance(2460)).toBe('2.5km')
  })

  it('非法值返回空字符串', () => {
    expect(formatServiceDistance(null)).toBe('')
    expect(formatServiceDistance(-1)).toBe('')
  })
})

describe('formatDistanceKm', () => {
  it('小于 1km 显示米（输入 0.5 → 500m）', () => {
    expect(formatDistanceKm(0.5)).toBe('500m')
    expect(formatDistanceKm(0.001)).toBe('1m')
  })

  it('不小于 1km 显示公里（保留 1 位小数）', () => {
    expect(formatDistanceKm(1.2)).toBe('1.2km')
    expect(formatDistanceKm(5.67)).toBe('5.7km')
    expect(formatDistanceKm(10)).toBe('10.0km')
  })

  it('非法值返回空字符串', () => {
    expect(formatDistanceKm(null)).toBe('')
    expect(formatDistanceKm(undefined)).toBe('')
    expect(formatDistanceKm(-1)).toBe('')
    expect(formatDistanceKm('abc')).toBe('')
  })

  it('0 返回 1m（最小显示）', () => {
    expect(formatDistanceKm(0)).toBe('1m')
  })
})

// 这组干的事是给服务列表补上"离我多远"：先定位自己，再按商家地址查坐标，最后算直线距离
describe('useServiceDistance attachDistances', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // 用户不给定位权限就先不算了，跳过就好，不能因为定位失败把整个列表搞挂
  it('定位失败时静默跳过，不调用高德地理编码、不写距离', async () => {
    amapMocks.getCurrentAddress.mockRejectedValue(new Error('denied'))
    const services = [{ id_wsh: 1, merchant_id_wsh: 10 }]
    const merchants = [{ id_wsh: 10, address_wsh: '测试路1号' }]
    const { attachDistances } = useServiceDistance()
    await attachDistances(services, merchants)
    expect(amapMocks.searchAmapAddress).not.toHaveBeenCalled()
    expect(services[0].distance_m_wsh).toBeUndefined()
  })

  it('定位成功后按商家地址走高德地理编码，再计算球面距离', async () => {
    amapMocks.getCurrentAddress.mockResolvedValue({ latitude_wsh: 31.23, longitude_wsh: 121.47 })
    amapMocks.searchAmapAddress.mockResolvedValue([{ latitude_wsh: 31.24, longitude_wsh: 121.48 }])
    const services = [{ id_wsh: 1, merchant_id_wsh: 10 }]
    const merchants = [{ id_wsh: 10, address_wsh: '测试路1号' }]
    const { attachDistances } = useServiceDistance()
    await attachDistances(services, merchants)
    expect(amapMocks.searchAmapAddress).toHaveBeenCalledWith('测试路1号', { withNearby: false, limit: 1 })
    const meters = services[0].distance_m_wsh
    expect(meters).toBeGreaterThan(1000)
    expect(meters).toBeLessThan(2000)
  })

  // 商家地址空着、或者地址查不出坐标，这两条服务就先不给距离，但不该拖累别的服务
  it('商家无地址或解析不到坐标时跳过该服务', async () => {
    amapMocks.searchAmapAddress.mockResolvedValue([])
    const services = [
      { id_wsh: 1, merchant_id_wsh: 11 },
      { id_wsh: 2, merchant_id_wsh: 12 },
    ]
    const merchants = [
      { id_wsh: 11, address_wsh: '' },
      { id_wsh: 12, address_wsh: '无法解析的地址' },
    ]
    const { attachDistances } = useServiceDistance()
    await attachDistances(services, merchants)
    expect(services[0].distance_m_wsh).toBeUndefined()
    expect(services[1].distance_m_wsh).toBeUndefined()
  })
})
